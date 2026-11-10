package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.CashActionReq;
import com.ls.aegis.biz.psi.model.req.CashRefundReq;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 资金台账更正与退货退款登记。保留原单、核销和反向资金单，不删除已确认流水。
 * 所有动作是台账登记，不向银行发送支付指令。
 */
@Service
@RequiredArgsConstructor
public class CashCompensationService {
    private final ReceiptMapper receiptMapper;
    private final PsiPaymentMapper paymentMapper;
    private final StockReturnMapper returnMapper;
    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final WriteOffItemMapper writeOffItemMapper;
    private final CashLedgerMapper cashLedgerMapper;
    private final WriteOffHelper writeOffHelper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final PsiTodoHelper todoHelper;

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String voidCash(boolean receipt, Long id, CashActionReq req) {
        Cash cash = load(receipt, id, true);
        return idempotentHelper.execute(cash.getTenantId(), kind(receipt) + "-void-" + id, req.getIdempotentKey(), () -> {
            writeOffHelper.lockCashFootprint(receipt,cash.getTenantId(),id,List.of(),false);
            if (cash.getDocStatus() == DocStatusEnum.VOID) { refreshRefundTodos(receipt, cash.getTenantId(), id); return cash.getDocNo(); }
            requireNormalConfirmed(cash);
            if (nz(cash.getRefundedAmount()).signum() != 0) throw new BusinessException("已登记退款，请先撤销关联退款记录再作废原单");
            if (nz(cash.getUnallocatedAmount()).compareTo(cash.getAmount()) != 0
                || !writeOffHelper.listSummaries(receipt ? id : null, receipt ? null : id).isEmpty()) {
                throw new BusinessException("存在有效核销，请先逐批反核销后作废");
            }
            requireUnmatched(receipt, id, cash.getTenantId());
            markVoid(cash, req.getReason()); save(receipt, cash);
            refreshRefundTodos(receipt, cash.getTenantId(), id);
            return cash.getDocNo();
        });
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Long refund(boolean sourceReceipt, Long sourceId, CashRefundReq req) {
        Cash source = load(sourceReceipt, sourceId, true);
        return Long.valueOf(idempotentHelper.execute(source.getTenantId(), kind(sourceReceipt) + "-refund-" + sourceId,
            req.getIdempotentKey(), () -> {
                requireNormalConfirmed(source);
                BigDecimal amount = req.getAmount();
                if (amount == null || amount.signum() <= 0 || amount.scale() > 2) throw new BusinessException("退款金额须为最多两位小数的正数");
                StockReturnDO returned = returnMapper.selectById(req.getStockReturnId());
                if (returned == null || returned.getDocStatus() != DocStatusEnum.CONFIRMED
                    || returned.getReturnType() != (sourceReceipt ? ReturnTypeEnum.SALE_RETURN : ReturnTypeEnum.PURCHASE_RETURN)
                    || !Objects.equals(source.getUnitId(), returned.getUnitId())
                    || !Objects.equals(source.getTenantId(), returned.getTenantId())) {
                    throw new BusinessException("退款须关联同一往来单位的已确认原方向退货");
                }
                Long openId = findOpenId(sourceReceipt, returned);
                writeOffHelper.lockCashFootprint(sourceReceipt,source.getTenantId(),sourceId,List.of(openId),false);
                StockReturnDO lockedReturn = returnMapper.selectByIdForUpdate(returned.getId());
                if (lockedReturn == null || lockedReturn.getDocStatus()!=DocStatusEnum.CONFIRMED
                    || !Objects.equals(lockedReturn.getTenantId(),returned.getTenantId()) || !Objects.equals(lockedReturn.getUnitId(),returned.getUnitId())
                    || lockedReturn.getReturnType()!=returned.getReturnType() || !Objects.equals(findOpenId(sourceReceipt,lockedReturn),openId))
                    throw new BusinessException("退款退货状态或关联已变更");
                returned=lockedReturn;
                BigDecimal attributed = attributedCredit(sourceReceipt, source, openId,
                    sourceReceipt ? returned.getOriginStockOutId() : returned.getOriginStockInId());
                if (amount.compareTo(attributed) > 0) {
                    throw new BusinessException("退款超过原资金对该原单已释放的可退归属额度，请先反核销/核对");
                }
                BigDecimal already = sumRefunds(sourceReceipt, source.getTenantId(), returned.getId());
                BigDecimal remaining = returned.getTaxInclusiveAmount().subtract(already);
                if (amount.compareTo(remaining) > 0 || amount.compareTo(nz(source.getUnallocatedAmount())) > 0
                    || amount.compareTo(source.getAmount().subtract(nz(source.getRefundedAmount()))) > 0) {
                    throw new BusinessException("退款超过退货剩余额或原资金可退未分配余额，请先核对/反核销");
                }
                Cash opposite = new Cash();
                opposite.setTenantId(source.getTenantId()); opposite.setDeptId(TenantFillHelper.currentDeptId());
                opposite.setDocNo(docNoGenerator.next(source.getTenantId(), sourceReceipt ? PsiConstants.DOC_PREFIX_PAYMENT : PsiConstants.DOC_PREFIX_RECEIPT));
                opposite.setUnitId(source.getUnitId()); opposite.setAccountId(source.getAccountId()); opposite.setPayMethod(source.getPayMethod());
                opposite.setAmount(amount); opposite.setUnallocatedAmount(BigDecimal.ZERO); opposite.setRefundedAmount(BigDecimal.ZERO);
                opposite.setRefundSourceId(sourceId); opposite.setRefundReturnId(returned.getId()); opposite.setDocStatus(DocStatusEnum.CONFIRMED);
                opposite.setSourceSequence(nextSequence(source));
                opposite.setConfirmUser(UserContextHolder.getUserId()); opposite.setConfirmTime(LocalDateTime.now());
                opposite.setRemark(req.getReason()); opposite.setIdempotentKey(cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                    kind(sourceReceipt) + ":refund:" + sourceId + ":" + req.getIdempotentKey()));
                insert(!sourceReceipt, opposite);
                source.setUnallocatedAmount(source.getUnallocatedAmount().subtract(amount));
                source.setRefundedAmount(nz(source.getRefundedAmount()).add(amount)); save(sourceReceipt, source);
                refreshRefundTodos(sourceReceipt, source.getTenantId(), sourceId);
                return opposite.getId().toString();
            }));
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String reverseRefund(boolean receipt, Long id, CashActionReq req) {
        Cash probe = load(receipt, id, false);
        if (probe.getRefundSourceId() == null) throw new BusinessException("该单不是退款登记，请使用普通资金单作废");
        // 与refund统一先锁原资金单，再锁退款单，避免相反锁顺序死锁。
        Cash source = load(!receipt, probe.getRefundSourceId(), true);
        StockReturnDO returnProbe=returnMapper.selectById(probe.getRefundReturnId());
        if (returnProbe==null || returnProbe.getDocStatus()!=DocStatusEnum.CONFIRMED || !Objects.equals(source.getTenantId(),returnProbe.getTenantId()))
            throw new BusinessException("退款关联退货不是已确认状态或无权访问");
        writeOffHelper.lockCashFootprint(!receipt,source.getTenantId(),source.getId(),List.of(findOpenId(!receipt,returnProbe)),false);
        Cash refund = load(receipt, id, true);
        return idempotentHelper.execute(source.getTenantId(), kind(receipt) + "-refund-reverse-" + id, req.getIdempotentKey(), () -> {
            if (refund.getDocStatus() == DocStatusEnum.VOID) return refund.getDocNo();
            requireNormalConfirmed(source);
            if (refund.getDocStatus() != DocStatusEnum.CONFIRMED || nz(refund.getUnallocatedAmount()).signum() != 0
                || !Objects.equals(refund.getRefundSourceId(), source.getId()) || !Objects.equals(source.getTenantId(), refund.getTenantId())
                || !Objects.equals(refund.getRefundReturnId(), probe.getRefundReturnId())) {
                throw new BusinessException("退款登记状态或关联不一致，不能撤销");
            }
            requireUnmatched(receipt, id, refund.getTenantId());
            StockReturnDO returned = returnMapper.selectByIdForUpdate(refund.getRefundReturnId());
            if (returned == null || returned.getDocStatus()!=DocStatusEnum.CONFIRMED || !Objects.equals(returned.getTenantId(), source.getTenantId()) || !Objects.equals(findOpenId(!receipt,returned),findOpenId(!receipt,returnProbe))) throw new BusinessException("退款关联退货不存在或无权访问");
            BigDecimal refunded = nz(source.getRefundedAmount()).subtract(refund.getAmount());
            if (refunded.signum() < 0) throw new BusinessException("退款累计金额不一致，请先对账");
            source.setRefundedAmount(refunded); source.setUnallocatedAmount(source.getUnallocatedAmount().add(refund.getAmount()));
            if (source.getUnallocatedAmount().compareTo(source.getAmount().subtract(refunded)) > 0) throw new BusinessException("资金余额不一致");
            refund.setVoidSequence(nextSequence(source));
            save(!receipt, source); markVoid(refund, req.getReason()); save(receipt, refund);
            refreshRefundTodos(!receipt, source.getTenantId(), source.getId());
            return refund.getDocNo();
        });
    }

    public void prepareRefundFootprint(boolean receipt,Long tenantId,Long sourceId,List<Long> additional) {
        Cash source=load(receipt,sourceId,true);
        if(!Objects.equals(source.getTenantId(),tenantId)) throw new BusinessException("源资金租户不一致");
        writeOffHelper.lockCashFootprint(receipt,tenantId,sourceId,additional,true);
    }

    /** Global ledger eligibility, not the operator's list visibility. Caller already authorizes the source/return. */
    public boolean hasRefundableCredit(boolean receipt, Long openId) {
        OpenRef open = openForInvariant(receipt, TenantFillHelper.currentTenantId(), openId);
        return open != null && hasRefundableCredit(receipt, open);
    }

    private boolean hasRefundableCredit(boolean receipt, OpenRef open) {
        return cashLedgerMapper.allocationItems(open.tenantId(), receipt, null, open.id()).stream()
            .map(item -> receipt ? item.getReceiptId() : item.getPaymentId()).filter(Objects::nonNull).distinct().anyMatch(id -> {
                Object value = receipt ? cashLedgerMapper.receiptForInvariant(open.tenantId(), id) : cashLedgerMapper.paymentForInvariant(open.tenantId(), id);
                Cash cash = value == null ? null : BeanUtil.copyProperties(value, Cash.class);
                if (cash == null || !Objects.equals(open.tenantId(), cash.getTenantId()) || !Objects.equals(open.unitId(), cash.getUnitId())
                    || cash.getDocStatus() != DocStatusEnum.CONFIRMED || cash.getRefundSourceId() != null
                    || nz(cash.getUnallocatedAmount()).signum() <= 0 || nz(cash.getAmount()).subtract(nz(cash.getRefundedAmount())).signum() <= 0) return false;
                try { return attributedCredit(receipt, cash, open.id(), open.originId()).signum() > 0; }
                catch (BusinessException legacyOrInconsistent) { return false; }
            });
    }

    /** Both reopen and close affected return TODOs after any change to a source's shared refund credit. */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void refreshRefundTodos(boolean receipt, Long sourceId) {
        Cash source=load(receipt,sourceId,false);
        writeOffHelper.requireCashFootprint(receipt,source.getTenantId(),sourceId,List.of());
        refreshRefundTodos(receipt, source.getTenantId(), sourceId);
    }

    private void refreshRefundTodos(boolean receipt, Long tenantId, Long sourceId) {
        java.util.Map<Long, OpenRef> origins = new java.util.TreeMap<>();
        for (Long openId : cashLedgerMapper.allocationItems(tenantId, receipt, sourceId, null).stream()
                .map(WriteOffItemDO::getOpenId).filter(Objects::nonNull).distinct().toList()) {
            OpenRef open = openForInvariant(receipt, tenantId, openId);
            if (open != null) origins.put(open.originId(), open);
        }
        writeOffHelper.requireCashFootprint(receipt,tenantId,sourceId,origins.values().stream().map(OpenRef::id).toList());
        // All source history was pre-locked before the first stock/open/return mutation. Never append late locks here.
        for (OpenRef open : origins.values()) {
            boolean refundable = hasRefundableCredit(receipt, open);
            for (StockReturnDO returned : cashLedgerMapper.returnsForOrigin(tenantId, receipt, open.originId())) {
                boolean applicable = returned.getDocStatus() == DocStatusEnum.CONFIRMED
                    && Objects.equals(tenantId, returned.getTenantId()) && Objects.equals(open.unitId(), returned.getUnitId())
                    && returned.getReturnType() == (receipt ? ReturnTypeEnum.SALE_RETURN : ReturnTypeEnum.PURCHASE_RETURN)
                    && nz(returned.getTaxInclusiveAmount()).compareTo(sumRefunds(receipt, tenantId, returned.getId())) > 0 && refundable;
                reconcileTodo(receipt, tenantId, returned, applicable);
            }
        }
    }

    private void reconcileTodo(boolean receipt, Long tenantId, StockReturnDO returned, boolean applicable) {
        String type = receipt ? "REFUND" : "RECEIVE_REFUND";
        int matched = cashLedgerMapper.setRefundTodo(tenantId, returned.getId(), type, !applicable, UserContextHolder.getUserId());
        if (applicable && matched == 0 && cashLedgerMapper.refundTodoCount(tenantId, returned.getId(), type) == 0) {
            cashLedgerMapper.createRefundTodo(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), tenantId,
                returned.getDeptId() == null ? 0L : returned.getDeptId(), type,
                (receipt ? "待核对退款：" : "待核对收退款：") + returned.getDocNo(), returned.getReturnType().getValue(),
                returned.getId(), returned.getDocNo(), UserContextHolder.getUserId());
        }
    }

    private OpenRef openForInvariant(boolean receipt, Long tenantId, Long id) {
        if (id == null) return null;
        if (receipt) {
            ReceivableDO row = cashLedgerMapper.receivableForInvariant(tenantId, id);
            return row == null ? null : new OpenRef(row.getId(), row.getTenantId(), row.getUnitId(), row.getSourceId());
        }
        PayableDO row = cashLedgerMapper.payableForInvariant(tenantId, id);
        return row == null ? null : new OpenRef(row.getId(), row.getTenantId(), row.getUnitId(), row.getSourceId());
    }

    private Long findOpenId(boolean receipt, StockReturnDO returned) {
        if (receipt) {
            ReceivableDO row = cashLedgerMapper.receivableForOrigin(returned.getTenantId(), returned.getOriginStockOutId());
            if (row == null) throw new BusinessException("原销售单没有对应应收");
            return row.getId();
        }
        PayableDO row = cashLedgerMapper.payableForOrigin(returned.getTenantId(), returned.getOriginStockInId());
        if (row == null) throw new BusinessException("原采购单没有对应应付");
        return row.getId();
    }

    private record OpenRef(Long id, Long tenantId, Long unitId, Long originId) { }

    private BigDecimal attributedCredit(boolean receipt, Cash source, Long openId, Long originId) {
        java.util.ArrayList<RefundAttribution.Event> events = new java.util.ArrayList<>();
        for (WriteOffItemDO item : cashLedgerMapper.allocationItems(source.getTenantId(), receipt, source.getId(), openId)) {
            events.add(new RefundAttribution.Event(item.getCashSequence(), item.getDirection() == WriteOffDirectionEnum.WRITE_OFF
                ? RefundAttribution.Kind.ALLOCATE : RefundAttribution.Kind.REVERSE_ALLOCATION, item.getAmount()));
        }
        var history = receipt ? cashLedgerMapper.refundPayments(source.getTenantId(), source.getId(), originId)
            : cashLedgerMapper.refundReceipts(source.getTenantId(), source.getId(), originId);
        for (CashLedgerMapper.RefundHistory row : history) {
            events.add(new RefundAttribution.Event(row.getSourceSequence(), RefundAttribution.Kind.REFUND, row.getAmount()));
            if ("VOID".equals(row.getDocStatus())) events.add(new RefundAttribution.Event(row.getVoidSequence(), RefundAttribution.Kind.REVERSE_REFUND, row.getAmount()));
        }
        return RefundAttribution.available(events);
    }

    private BigDecimal sumRefunds(boolean sourceReceipt, Long tenantId, Long returnId) {
        return nz(sourceReceipt ? cashLedgerMapper.sumRefundPayments(tenantId, returnId) : cashLedgerMapper.sumRefundReceipts(tenantId, returnId));
    }

    private void requireNormalConfirmed(Cash cash) {
        if (cash.getDocStatus() != DocStatusEnum.CONFIRMED || cash.getRefundSourceId() != null) {
            throw new BusinessException("仅普通已确认资金单支持该操作；退款登记请走撤销退款");
        }
    }

    private void requireUnmatched(boolean receipt, Long id, Long tenantId) {
        if (cashLedgerMapper.bankMatches(tenantId, receipt ? "RECEIPT" : "PAYMENT", id) > 0) {
            throw new BusinessException("已匹配银行流水，请先解除匹配后更正台账");
        }
    }

    private Cash load(boolean receipt, Long id, boolean lock) {
        Object value = receipt ? (lock ? receiptMapper.selectByIdForUpdate(id) : receiptMapper.selectById(id))
            : (lock ? paymentMapper.selectByIdForUpdate(id) : paymentMapper.selectById(id));
        if (value == null) throw new BusinessException("资金单不存在或无权访问");
        return BeanUtil.copyProperties(value, Cash.class);
    }
    private void save(boolean receipt, Cash value) {
        int changed=receipt ? receiptMapper.updateById(BeanUtil.copyProperties(value, ReceiptDO.class))
            : paymentMapper.updateById(BeanUtil.copyProperties(value, PaymentDO.class));
        if(changed!=1) throw new BusinessException("源资金状态已变更或无权更新");
    }
    private void insert(boolean receipt, Cash value) {
        if (receipt) { ReceiptDO row = BeanUtil.copyProperties(value, ReceiptDO.class); row.setReceiptDate(LocalDateTime.now()); if(receiptMapper.insert(row)!=1) throw new BusinessException("退款资金登记失败"); value.setId(row.getId()); }
        else { PaymentDO row = BeanUtil.copyProperties(value, PaymentDO.class); row.setPaymentDate(LocalDateTime.now()); if(paymentMapper.insert(row)!=1) throw new BusinessException("退款资金登记失败"); value.setId(row.getId()); }
    }
    private void markVoid(Cash value, String reason) {
        value.setDocStatus(DocStatusEnum.VOID); value.setUnallocatedAmount(BigDecimal.ZERO);
        value.setVoidReason(reason); value.setVoidUser(UserContextHolder.getUserId()); value.setVoidTime(LocalDateTime.now());
    }
    private static String kind(boolean receipt) { return receipt ? "receipt" : "payment"; }
    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private Long nextSequence(Cash cash) {
        long next = Math.addExact(cash.getOperationSequence() == null ? 0L : cash.getOperationSequence(), 1L);
        cash.setOperationSequence(next); return next;
    }

    @Data
    public static class Cash {
        private Long id, tenantId, deptId, unitId, accountId, createUser, confirmUser;
        private String docNo, payMethod, remark, idempotentKey;
        private DocStatusEnum docStatus;
        private BigDecimal amount, unallocatedAmount, refundedAmount;
        private Long refundSourceId, refundReturnId, voidUser;
        private Long operationSequence, sourceSequence, voidSequence;
        private String voidReason;
        private LocalDateTime confirmTime, voidTime;
    }
}
