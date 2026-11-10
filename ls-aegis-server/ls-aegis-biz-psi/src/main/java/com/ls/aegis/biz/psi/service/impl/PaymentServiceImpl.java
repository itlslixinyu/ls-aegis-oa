package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.mapper.PsiPaymentMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.entity.PaymentDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.query.PaymentQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.PaymentReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import com.ls.aegis.biz.psi.model.resp.PaymentResp;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import com.ls.aegis.biz.psi.service.PaymentService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.biz.psi.support.WriteOffHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service("psiPaymentServiceImpl")
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PsiPaymentMapper paymentMapper;
    private final UnitMapper unitMapper;
    private final CompanyAccountMapper companyAccountMapper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final WriteOffHelper writeOffHelper;
    private final CashCompensationService cashCompensationService;

    @Override
    public PageResp<PaymentResp> page(PaymentQuery query, PageQuery pageQuery) {
        QueryWrapper<PaymentDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<PaymentDO> page = paymentMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<PaymentResp> resp = PageResp.build(page, PaymentResp.class);
        fillNames(resp.getList());
        return resp;
    }

    @Override
    public PaymentResp get(Long id) {
        PaymentResp resp = BeanUtil.copyProperties(require(id), PaymentResp.class);
        fillNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PaymentReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        PaymentDO entity = new PaymentDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_PAYMENT));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setPaymentDate(req.getPaymentDate());
        entity.setUnitId(req.getUnitId());
        entity.setAccountId(req.getAccountId());
        entity.setPayMethod(req.getPayMethod());
        entity.setAmount(req.getAmount());
        entity.setUnallocatedAmount(BigDecimal.ZERO);
        entity.setRemark(req.getRemark());
        paymentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentReq req, Long id) {
        PaymentDO entity = requireLocked(id);
        assertDraft(entity);
        entity.setPaymentDate(req.getPaymentDate());
        entity.setUnitId(req.getUnitId());
        entity.setAccountId(req.getAccountId());
        entity.setPayMethod(req.getPayMethod());
        entity.setAmount(req.getAmount());
        entity.setRemark(req.getRemark());
        paymentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PaymentDO entity = requireLocked(id);
        assertDraft(entity);
        paymentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String confirm(Long id, DocConfirmReq req) {
        PaymentDO locked = paymentMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("付款单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "payment-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req));
    }

    private String doConfirm(PaymentDO locked, DocConfirmReq req) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        locked.setUnallocatedAmount(locked.getAmount());
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(req.getIdempotentKey());
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        paymentMapper.updateById(locked);

        cashCompensationService.prepareRefundFootprint(false,tenantOf(locked),locked.getId(),req.getWriteOffItems()==null?List.of():req.getWriteOffItems().stream().map(WriteOffReq.Item::getOpenId).toList());
        if (CollUtil.isNotEmpty(req.getWriteOffItems())) {
            Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
            WriteOffReq writeOffReq = new WriteOffReq();
            writeOffReq.setItems(req.getWriteOffItems());
            writeOffReq.setReason(req.getWriteOffReason());
            BigDecimal total = BigDecimal.ZERO;
            for (WriteOffReq.Item line : req.getWriteOffItems()) {
                total = total.add(line.getAmount());
            }
            writeOffHelper.writeOffAp(tenantId, locked.getId(), locked.getUnitId(), locked.getUnallocatedAmount(), writeOffReq, nextSequence(locked));
            locked.setUnallocatedAmount(locked.getUnallocatedAmount().subtract(total));
            if (locked.getUnallocatedAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("即时核销金额超过付款金额");
            }
            paymentMapper.updateById(locked);
        }
        cashCompensationService.refreshRefundTodos(false,locked.getId());
        return locked.getDocNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Long writeOff(Long id, WriteOffReq req) {
        PaymentDO locked = paymentMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("付款单不存在");
        }
        if (!DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            throw new BusinessException("仅已确认付款可核销");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        BigDecimal total = BigDecimal.ZERO;
        for (WriteOffReq.Item line : req.getItems()) {
            total = total.add(line.getAmount());
        }
        Long writeOffId = writeOffHelper.writeOffAp(tenantId, locked.getId(), locked.getUnitId(), locked
            .getUnallocatedAmount(), req, nextSequence(locked));
        locked.setUnallocatedAmount(locked.getUnallocatedAmount().subtract(total));
        paymentMapper.updateById(locked);
        cashCompensationService.refreshRefundTodos(false,locked.getId());
        return writeOffId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Long reverse(Long id, WriteOffReverseReq req) {
        PaymentDO locked = paymentMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("付款单不存在");
        }
        if (!DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            throw new BusinessException("仅已确认付款可反核销");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        WriteOffHelper.ReverseResult result = writeOffHelper.reverseAp(tenantId, locked.getId(), req, nextSequence(locked));
        locked.setUnallocatedAmount(locked.getUnallocatedAmount().add(result.amount()));
        if (locked.getUnallocatedAmount().compareTo(locked.getAmount().subtract(locked.getRefundedAmount() == null ? BigDecimal.ZERO : locked.getRefundedAmount())) > 0) {
            throw new BusinessException("反核销后未分配金额异常");
        }
        paymentMapper.updateById(locked);
        cashCompensationService.refreshRefundTodos(false, locked.getId());
        return result.writeOffId();
    }

    @Override
    public List<WriteOffSummaryResp> listWriteOffs(Long id) {
        require(id);
        return writeOffHelper.listSummaries(null, id);
    }

    private Long tenantOf(PaymentDO cash) { return ObjectUtil.defaultIfNull(cash.getTenantId(),0L); }

    private Long nextSequence(PaymentDO cash) {
        long next = Math.addExact(cash.getOperationSequence() == null ? 0L : cash.getOperationSequence(), 1L);
        cash.setOperationSequence(next);
        return next;
    }

    private PaymentDO requireLocked(Long id) {
        PaymentDO entity = paymentMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private PaymentDO require(Long id) {
        PaymentDO entity = paymentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("付款单不存在");
        }
        return entity;
    }

    private void assertDraft(PaymentDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿可操作");
        }
    }

    private void fillNames(List<PaymentResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        java.util.Set<Long> unitIds = list.stream().map(PaymentResp::getUnitId)
            .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> accountIds = list.stream().map(PaymentResp::getAccountId)
            .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> unitNames = new java.util.HashMap<>();
        java.util.Map<Long, String> accountNames = new java.util.HashMap<>();
        if (!unitIds.isEmpty()) {
            for (UnitDO u : unitMapper.selectByIds(unitIds)) {
                unitNames.put(u.getId(), u.getUnitName());
            }
        }
        if (!accountIds.isEmpty()) {
            for (CompanyAccountDO a : companyAccountMapper.selectByIds(accountIds)) {
                accountNames.put(a.getId(), a.getAccountName());
            }
        }
        for (PaymentResp row : list) {
            if (row.getUnitId() != null) {
                row.setUnitName(unitNames.get(row.getUnitId()));
            }
            if (row.getAccountId() != null) {
                row.setAccountName(accountNames.get(row.getAccountId()));
            }
        }
    }
}
