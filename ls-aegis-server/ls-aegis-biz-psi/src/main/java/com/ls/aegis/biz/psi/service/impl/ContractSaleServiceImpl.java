package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.ContractSaleMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.model.entity.ContractSaleDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.query.ContractQuery;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.resp.ContractResp;
import com.ls.aegis.biz.psi.service.ContractSaleService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractSaleServiceImpl implements ContractSaleService {

    private final ContractSaleMapper contractSaleMapper;
    private final StockOutMapper stockOutMapper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard attachments;

    @Override
    public PageResp<ContractResp> page(ContractQuery query, PageQuery pageQuery) {
        QueryWrapper<ContractSaleDO> wrapper = QueryWrapperHelper.build(query);
        if (Boolean.TRUE.equals(query.getUnboundOnly())) {
            wrapper.isNull("project_id");
        }
        wrapper.orderByDesc("id");
        Page<ContractSaleDO> page = contractSaleMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<ContractResp> resp = PageResp.build(page, ContractResp.class);
        // L10：列表也要带执行进度（已确认关联出库含税合计 / 合同总额）
        if (resp.getList() != null) {
            resp.setList(page.getRecords().stream().map(this::toResp).toList());
        }
        return resp;
    }

    @Override
    public ContractResp get(Long id) {
        return toResp(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContractReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        ContractSaleDO entity = new ContractSaleDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setContractNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_CONTRACT_SALE));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        fill(entity, req);
        contractSaleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ContractReq req, Long id) {
        ContractSaleDO entity = require(id);
        assertDraft(entity);
        fill(entity, req);
        contractSaleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ContractSaleDO entity = require(id);
        assertDraft(entity);
        contractSaleMapper.deleteById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        ContractSaleDO locked = contractSaleMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("销售合同不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "contract-sale-confirm-" + id, req.getIdempotentKey(), () -> {
            if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
                return locked.getContractNo();
            }
            assertDraft(locked);
            if (!ObjectUtil.equal(locked.getCreateUser(), UserContextHolder.getUserId())) {
                throw new BusinessException("仅制单人可确认生效");
            }
            locked.setDocStatus(DocStatusEnum.CONFIRMED);
            locked.setConfirmUser(UserContextHolder.getUserId());
            locked.setConfirmTime(LocalDateTime.now());
            contractSaleMapper.updateById(locked);
            return locked.getContractNo();
        });
    }

    @Override
    public ContractResp progress(Long id) {
        return toResp(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProject(Long id, Long projectId) {
        require(id);
        contractSaleMapper.update(null, Wrappers.lambdaUpdate(ContractSaleDO.class)
            .eq(ContractSaleDO::getId, id)
            .set(ContractSaleDO::getProjectId, projectId));
    }

    private ContractResp toResp(ContractSaleDO entity) {
        ContractResp resp = BeanUtil.copyProperties(entity, ContractResp.class);
        List<StockOutDO> outs = stockOutMapper.selectList(Wrappers.lambdaQuery(StockOutDO.class)
            .eq(StockOutDO::getContractId, entity.getId())
            .eq(StockOutDO::getDocStatus, DocStatusEnum.CONFIRMED));
        BigDecimal executed = outs.stream().map(StockOutDO::getTaxInclusiveAmount)
            .filter(ObjectUtil::isNotNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = entity.getTaxInclusiveAmount();
        // 合同额未填或为 0 时不算进度（避免分母为 1 导致进度被放大成上百百分比）
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            resp.setProgress(BigDecimal.ZERO);
            return resp;
        }
        BigDecimal progress = executed.divide(total, 6, RoundingMode.HALF_UP);
        if (progress.compareTo(BigDecimal.ONE) > 0) {
            progress = BigDecimal.ONE;
        }
        resp.setProgress(progress);
        return resp;
    }

    private void fill(ContractSaleDO entity, ContractReq req) {
        attachments.requireCsv(req.getFileId());
        entity.setName(req.getName());
        entity.setSignDate(req.getSignDate());
        entity.setUnitId(req.getUnitId());
        entity.setTaxInclusiveAmount(req.getTaxInclusiveAmount());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setFileId(normalizeFileIds(req.getFileId()));
        entity.setRemark(req.getRemark());
        entity.setProjectId(req.getProjectId());
    }

    private static String normalizeFileIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String joined = java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private ContractSaleDO require(Long id) {
        ContractSaleDO entity = contractSaleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("销售合同不存在");
        }
        return entity;
    }

    private void assertDraft(ContractSaleDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }
}
