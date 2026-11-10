package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.ContractPurchaseMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.model.entity.ContractPurchaseDO;
import com.ls.aegis.biz.psi.model.entity.StockInDO;
import com.ls.aegis.biz.psi.model.query.ContractQuery;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.resp.ContractResp;
import com.ls.aegis.biz.psi.service.ContractPurchaseService;
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
public class ContractPurchaseServiceImpl implements ContractPurchaseService {

    private final ContractPurchaseMapper contractPurchaseMapper;
    private final StockInMapper stockInMapper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard attachments;

    @Override
    public PageResp<ContractResp> page(ContractQuery query, PageQuery pageQuery) {
        QueryWrapper<ContractPurchaseDO> wrapper = QueryWrapperHelper.build(query);
        if (Boolean.TRUE.equals(query.getUnboundOnly())) {
            wrapper.isNull("project_id");
        }
        wrapper.orderByDesc("id");
        Page<ContractPurchaseDO> page = contractPurchaseMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<ContractResp> resp = PageResp.build(page, ContractResp.class);
        // L10：列表也要带执行进度（已确认关联入库含税合计 / 合同总额）
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
        ContractPurchaseDO entity = new ContractPurchaseDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setContractNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_CONTRACT_PURCHASE));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        fill(entity, req);
        contractPurchaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ContractReq req, Long id) {
        ContractPurchaseDO entity = require(id);
        assertDraft(entity);
        fill(entity, req);
        contractPurchaseMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ContractPurchaseDO entity = require(id);
        assertDraft(entity);
        contractPurchaseMapper.deleteById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        ContractPurchaseDO locked = contractPurchaseMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("采购合同不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "contract-purchase-confirm-" + id, req.getIdempotentKey(), () -> {
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
            contractPurchaseMapper.updateById(locked);
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
        contractPurchaseMapper.update(null, Wrappers.lambdaUpdate(ContractPurchaseDO.class)
            .eq(ContractPurchaseDO::getId, id)
            .set(ContractPurchaseDO::getProjectId, projectId));
    }

    private ContractResp toResp(ContractPurchaseDO entity) {
        ContractResp resp = BeanUtil.copyProperties(entity, ContractResp.class);
        List<StockInDO> ins = stockInMapper.selectList(Wrappers.lambdaQuery(StockInDO.class)
            .eq(StockInDO::getContractId, entity.getId())
            .eq(StockInDO::getDocStatus, DocStatusEnum.CONFIRMED));
        BigDecimal executed = ins.stream().map(StockInDO::getTaxInclusiveAmount)
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

    private void fill(ContractPurchaseDO entity, ContractReq req) {
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

    /** 空串/空白视为清空；多 ID 去重并规范为逗号分隔 */
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

    private ContractPurchaseDO require(Long id) {
        ContractPurchaseDO entity = contractPurchaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("采购合同不存在");
        }
        return entity;
    }

    private void assertDraft(ContractPurchaseDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }
}
