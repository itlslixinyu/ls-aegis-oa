package com.ls.aegis.biz.psi.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.WriteOffDirectionEnum;
import com.ls.aegis.biz.psi.enums.WriteOffOpenTypeEnum;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.mapper.WriteOffItemMapper;
import com.ls.aegis.biz.psi.mapper.WriteOffMapper;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.entity.WriteOffDO;
import com.ls.aegis.biz.psi.model.entity.WriteOffItemDO;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 收付款核销 / 反核销共用逻辑
 */
@Component
@RequiredArgsConstructor
public class WriteOffHelper {

    private static final int SUMMARY_QUERY_BATCH_SIZE = 200;

    private final WriteOffMapper writeOffMapper;
    private final WriteOffItemMapper writeOffItemMapper;
    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final StockOutMapper stockOutMapper;
    private final StockInMapper stockInMapper;
    private final DocNoGenerator docNoGenerator;
    private final PsiTodoCloseSupport psiTodoCloseSupport;
    private final com.ls.aegis.biz.psi.mapper.CashLedgerMapper cashLedgerMapper;

    public Long writeOffAr(Long tenantId, Long receiptId, Long unitId, BigDecimal unallocated, WriteOffReq req, Long cashSequence) {
        List<WriteOffReq.Item> lines = req.getItems();
        lockCashFootprint(true, tenantId, receiptId, lines.stream().map(WriteOffReq.Item::getOpenId).toList(), true);
        BigDecimal total = sum(lines);
        if (total.compareTo(unallocated) > 0) {
            throw new BusinessException("核销合计超过回款未分配金额");
        }
        WriteOffDO master = newWriteOff(tenantId, req.getReason());
        for (WriteOffReq.Item line : lines) {
            ReceivableDO ar = cashLedgerMapper.lockReceivableForCash(tenantId, line.getOpenId());
            if (ar == null) {
                throw new BusinessException("应收开口不存在");
            }
            if (!ObjectUtil.equal(ar.getUnitId(), unitId)) {
                throw new BusinessException("应收开口与回款客户不一致");
            }
            if (line.getAmount().compareTo(ar.getUnwrittenAmount()) > 0) {
                throw new BusinessException("核销金额超过应收未核销余额");
            }
            insertItem(tenantId, master.getId(), receiptId, null, WriteOffOpenTypeEnum.AR, line.getOpenId(),
                WriteOffDirectionEnum.WRITE_OFF, line.getAmount(), req.getReason(), cashSequence);
            applyAr(ar, line.getAmount(), true);
        }
        return master.getId();
    }

    public Long writeOffAp(Long tenantId, Long paymentId, Long unitId, BigDecimal unallocated, WriteOffReq req, Long cashSequence) {
        List<WriteOffReq.Item> lines = req.getItems();
        lockCashFootprint(false, tenantId, paymentId, lines.stream().map(WriteOffReq.Item::getOpenId).toList(), true);
        BigDecimal total = sum(lines);
        if (total.compareTo(unallocated) > 0) {
            throw new BusinessException("核销合计超过付款未分配金额");
        }
        WriteOffDO master = newWriteOff(tenantId, req.getReason());
        for (WriteOffReq.Item line : lines) {
            PayableDO ap = cashLedgerMapper.lockPayableForCash(tenantId, line.getOpenId());
            if (ap == null) {
                throw new BusinessException("应付开口不存在");
            }
            if (!ObjectUtil.equal(ap.getUnitId(), unitId)) {
                throw new BusinessException("应付开口与付款供应商不一致");
            }
            if (line.getAmount().compareTo(ap.getUnwrittenAmount()) > 0) {
                throw new BusinessException("核销金额超过应付未核销余额");
            }
            insertItem(tenantId, master.getId(), null, paymentId, WriteOffOpenTypeEnum.AP, line.getOpenId(),
                WriteOffDirectionEnum.WRITE_OFF, line.getAmount(), req.getReason(), cashSequence);
            applyAp(ap, line.getAmount(), true);
        }
        return master.getId();
    }

    /**
     * @return 反核销主表 ID 与金额合计（用于回写收付未分配）
     */
    public ReverseResult reverseAr(Long tenantId, Long receiptId, WriteOffReverseReq req, Long cashSequence) {
        List<WriteOffItemDO> origins = resolveOrigins(receiptId, null, WriteOffOpenTypeEnum.AR, req);
        if(origins.stream().anyMatch(item -> !ObjectUtil.equal(tenantId,item.getTenantId()))) throw new BusinessException("核销明细租户不一致");
        lockCashFootprint(true, tenantId, receiptId, List.of(), false);
        requireCashFootprint(true,tenantId,receiptId,origins.stream().map(WriteOffItemDO::getOpenId).toList());
        WriteOffDO master = newWriteOff(tenantId, req.getReason());
        BigDecimal total = BigDecimal.ZERO;
        for (WriteOffItemDO origin : origins) {
            assertNotReversed(origin);
            ReceivableDO ar = cashLedgerMapper.lockReceivableForCash(tenantId, origin.getOpenId());
            if (ar == null) {
                throw new BusinessException("应收开口不存在");
            }
            if (origin.getAmount().compareTo(ar.getWrittenOffAmount()) > 0) {
                throw new BusinessException("反核销金额超过已核销额");
            }
            String reason = buildReverseReason(origin.getId(), req.getReason());
            insertItem(tenantId, master.getId(), receiptId, null, WriteOffOpenTypeEnum.AR, origin.getOpenId(),
                WriteOffDirectionEnum.REVERSE, origin.getAmount(), reason, cashSequence);
            applyAr(ar, origin.getAmount(), false);
            total = total.add(origin.getAmount());
        }
        return new ReverseResult(master.getId(), total);
    }

    public ReverseResult reverseAp(Long tenantId, Long paymentId, WriteOffReverseReq req, Long cashSequence) {
        List<WriteOffItemDO> origins = resolveOrigins(null, paymentId, WriteOffOpenTypeEnum.AP, req);
        if(origins.stream().anyMatch(item -> !ObjectUtil.equal(tenantId,item.getTenantId()))) throw new BusinessException("核销明细租户不一致");
        lockCashFootprint(false, tenantId, paymentId, List.of(), false);
        requireCashFootprint(false,tenantId,paymentId,origins.stream().map(WriteOffItemDO::getOpenId).toList());
        WriteOffDO master = newWriteOff(tenantId, req.getReason());
        BigDecimal total = BigDecimal.ZERO;
        for (WriteOffItemDO origin : origins) {
            assertNotReversed(origin);
            PayableDO ap = cashLedgerMapper.lockPayableForCash(tenantId, origin.getOpenId());
            if (ap == null) {
                throw new BusinessException("应付开口不存在");
            }
            if (origin.getAmount().compareTo(ap.getWrittenOffAmount()) > 0) {
                throw new BusinessException("反核销金额超过已核销额");
            }
            String reason = buildReverseReason(origin.getId(), req.getReason());
            insertItem(tenantId, master.getId(), null, paymentId, WriteOffOpenTypeEnum.AP, origin.getOpenId(),
                WriteOffDirectionEnum.REVERSE, origin.getAmount(), reason, cashSequence);
            applyAp(ap, origin.getAmount(), false);
            total = total.add(origin.getAmount());
        }
        return new ReverseResult(master.getId(), total);
    }

    public record ReverseResult(Long writeOffId, BigDecimal amount) {
    }

    /** Source cash must already be authorized and locked. Pre-lock the complete historical + new footprint. */
    public void lockCashFootprint(boolean ar, Long tenantId, Long cashId, List<Long> additional, boolean authorizeAdditional) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            || !org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            throw new BusinessException("资金锁范围必须在业务事务内建立");
        Object source = ar ? cashLedgerMapper.receiptForInvariant(tenantId, cashId) : cashLedgerMapper.paymentForInvariant(tenantId, cashId);
        if (source == null) throw new BusinessException("源资金不存在");
        Long unitId = ar ? ((com.ls.aegis.biz.psi.model.entity.ReceiptDO)source).getUnitId() : ((com.ls.aegis.biz.psi.model.entity.PaymentDO)source).getUnitId();
        java.util.TreeSet<Long> ids = new java.util.TreeSet<>();
        for (WriteOffItemDO item : cashLedgerMapper.allocationItems(tenantId, ar, cashId, null)) ids.add(item.getOpenId());
        for (Long id : additional) {
            if (id == null) throw new BusinessException("核销开口不能为空");
            if (authorizeAdditional) {
                Object visible = ar ? receivableMapper.selectById(id) : payableMapper.selectById(id);
                if (visible == null) throw new BusinessException("新增核销开口不存在或无权访问");
            }
            ids.add(id);
        }
        Footprints context = footprints(); Scope scope = new Scope(ar, tenantId, cashId);
        java.util.Set<Long> held = context.scopes.get(scope);
        if (held != null) {
            if (!held.containsAll(ids)) throw new BusinessException("资金关联范围已变化，请重新提交");
            return;
        }
        java.util.Map<Long, OpenLock> opens = new java.util.TreeMap<>(); java.util.TreeSet<Long> headers = new java.util.TreeSet<>();
        for (Long id : ids) {
            OpenLock open = readOpen(ar, tenantId, id, false);
            if (open == null || !java.util.Objects.equals(unitId, open.unitId())) throw new BusinessException("历史资金开口不存在或租户/单位不一致，请先对账");
            opens.put(id, open);
            if ((ar ? BizSourceTypeEnum.STOCK_OUT : BizSourceTypeEnum.STOCK_IN).equals(open.type())) {
                if (open.originId() == null) throw new BusinessException("核销原库存单关联缺失");
                headers.add(open.originId());
            }
        }
        for (Long id : headers) if ((ar ? cashLedgerMapper.lockCashStockOut(tenantId,id) : cashLedgerMapper.lockCashStockIn(tenantId,id)) == null)
            throw new BusinessException("核销原库存单不存在或租户不一致");
        for (OpenLock expected : opens.values()) {
            OpenLock actual = readOpen(ar, tenantId, expected.id(), true);
            if (!expected.equals(actual)) throw new BusinessException("核销开口关联已变更，请重试");
        }
        context.scopes.put(scope, java.util.Set.copyOf(ids));
    }

    public void requireCashFootprint(boolean ar, Long tenantId, Long cashId, java.util.Collection<Long> openIds) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            || !org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) throw new BusinessException("退款待办刷新缺少资金事务");
        java.util.Set<Long> held = footprints().scopes.get(new Scope(ar,tenantId,cashId));
        if (held == null || !held.containsAll(openIds)) throw new BusinessException("退款待办刷新前必须锁定源资金全部关联范围");
    }

    private OpenLock readOpen(boolean ar,Long tenantId,Long id,boolean lock) {
        if (ar) { ReceivableDO r = lock ? cashLedgerMapper.lockReceivableForCash(tenantId,id) : cashLedgerMapper.receivableForCash(tenantId,id);
            return r == null ? null : new OpenLock(r.getId(),r.getTenantId(),r.getUnitId(),r.getSourceType(),r.getSourceId()); }
        PayableDO r = lock ? cashLedgerMapper.lockPayableForCash(tenantId,id) : cashLedgerMapper.payableForCash(tenantId,id);
        return r == null ? null : new OpenLock(r.getId(),r.getTenantId(),r.getUnitId(),r.getSourceType(),r.getSourceId());
    }
    private record OpenLock(Long id,Long tenantId,Long unitId,BizSourceTypeEnum type,Long originId) { }
    private record Scope(boolean ar,Long tenantId,Long cashId) { }
    private static class Footprints implements org.springframework.transaction.support.TransactionSynchronization {
        final WriteOffHelper owner; final java.util.Map<Scope,java.util.Set<Long>> scopes=new java.util.HashMap<>();
        Footprints(WriteOffHelper owner){this.owner=owner;}
    }
    private Footprints footprints() {
        for (var synchronization : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())
            if (synchronization instanceof Footprints state && state.owner == this) return state;
        Footprints state=new Footprints(this); org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(state); return state;
    }

    /**
     * 回款/付款下可反核销的核销批次列表
     */
    public List<com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp> listSummaries(Long receiptId, Long paymentId) {
        var q = Wrappers.lambdaQuery(WriteOffItemDO.class)
            .eq(WriteOffItemDO::getDirection, WriteOffDirectionEnum.WRITE_OFF)
            .orderByDesc(WriteOffItemDO::getWriteOffId);
        if (receiptId != null) {
            q.eq(WriteOffItemDO::getReceiptId, receiptId);
        } else if (paymentId != null) {
            q.eq(WriteOffItemDO::getPaymentId, paymentId);
        } else {
            throw new BusinessException("请指定回款或付款单");
        }
        List<WriteOffItemDO> items = writeOffItemMapper.selectList(q);
        if (items.isEmpty()) return List.of();
        var masterIds = items.stream().map(WriteOffItemDO::getWriteOffId).distinct().toList();
        Map<Long, WriteOffDO> masters = new HashMap<>();
        for (int begin = 0; begin < masterIds.size(); begin += SUMMARY_QUERY_BATCH_SIZE) {
            // 使用带DataPermission的selectList，保持逐主键读取原有的可见范围。
            var batch = masterIds.subList(begin, Math.min(begin + SUMMARY_QUERY_BATCH_SIZE, masterIds.size()));
            for (var master : writeOffMapper.selectList(Wrappers.lambdaQuery(WriteOffDO.class)
                .select(WriteOffDO::getId, WriteOffDO::getWriteOffDate, WriteOffDO::getRemark)
                .in(WriteOffDO::getId, batch))) masters.put(master.getId(), master);
        }
        Set<Long> reversed = findReversedSummaryItems(items);
        java.util.LinkedHashMap<Long, com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp> map = new java.util.LinkedHashMap<>();
        for (WriteOffItemDO item : items) {
            com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp summary = map.computeIfAbsent(item.getWriteOffId(), id -> {
                com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp s = new com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp();
                s.setWriteOffId(id);
                s.setAmount(BigDecimal.ZERO);
                s.setReversed(false);
                WriteOffDO master = masters.get(id);
                if (master != null) {
                    s.setWriteOffDate(master.getWriteOffDate());
                    s.setReason(master.getRemark());
                }
                return s;
            });
            summary.setAmount(summary.getAmount().add(item.getAmount()));
            if (reversed.contains(item.getId())) summary.setReversed(true);
        }
        return map.values().stream().filter(s -> !Boolean.TRUE.equals(s.getReversed())).toList();
    }

    /** 只优化只读汇总；写入/反核销仍使用原有逐项锁与校验。 */
    private Set<Long> findReversedSummaryItems(List<WriteOffItemDO> items) {
        Map<String, Long> markerIds = new HashMap<>();
        for (var item : items) markerIds.put(reverseMarker(item.getId()), item.getId());
        var markers = new ArrayList<>(markerIds.keySet());
        Set<Long> reversed = new HashSet<>();
        for (int begin = 0; begin < markers.size(); begin += SUMMARY_QUERY_BATCH_SIZE) {
            var batch = markers.subList(begin, Math.min(begin + SUMMARY_QUERY_BATCH_SIZE, markers.size()));
            var rows = writeOffItemMapper.selectList(Wrappers.lambdaQuery(WriteOffItemDO.class)
                .select(WriteOffItemDO::getReason)
                .eq(WriteOffItemDO::getDirection, WriteOffDirectionEnum.REVERSE)
                .and(group -> batch.forEach(marker -> group.or().likeRight(WriteOffItemDO::getReason, marker))));
            for (var row : rows) {
                String reason = row.getReason();
                if (reason == null) continue;
                int delimiter = reason.indexOf('|');
                Long id = delimiter < 0 ? null : markerIds.get(reason.substring(0, delimiter + 1));
                if (id != null) reversed.add(id);
            }
        }
        return reversed;
    }

    private List<WriteOffItemDO> resolveOrigins(Long receiptId,
                                                Long paymentId,
                                                WriteOffOpenTypeEnum openType,
                                                WriteOffReverseReq req) {
        if (req.getWriteOffId() == null && CollUtil.isEmpty(req.getWriteOffItemIds())) {
            throw new BusinessException("请指定核销主表ID或核销明细ID");
        }
        List<WriteOffItemDO> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(req.getWriteOffItemIds())) {
            for (Long itemId : req.getWriteOffItemIds()) {
                WriteOffItemDO item = writeOffItemMapper.selectById(itemId);
                if (item == null) {
                    throw new BusinessException("核销明细不存在");
                }
                validateOrigin(item, receiptId, paymentId, openType);
                list.add(item);
            }
        } else {
            list = writeOffItemMapper.selectList(Wrappers.lambdaQuery(WriteOffItemDO.class)
                .eq(WriteOffItemDO::getWriteOffId, req.getWriteOffId())
                .eq(WriteOffItemDO::getDirection, WriteOffDirectionEnum.WRITE_OFF)
                .eq(WriteOffItemDO::getOpenType, openType)
                .eq(receiptId != null, WriteOffItemDO::getReceiptId, receiptId)
                .eq(paymentId != null, WriteOffItemDO::getPaymentId, paymentId));
            if (CollUtil.isEmpty(list)) {
                throw new BusinessException("无可反核销的明细");
            }
        }
        return list;
    }

    private void validateOrigin(WriteOffItemDO item, Long receiptId, Long paymentId, WriteOffOpenTypeEnum openType) {
        if (!WriteOffDirectionEnum.WRITE_OFF.equals(item.getDirection())) {
            throw new BusinessException("只能反核销正向核销明细");
        }
        if (!openType.equals(item.getOpenType())) {
            throw new BusinessException("核销开口类型不匹配");
        }
        if (receiptId != null && !ObjectUtil.equal(item.getReceiptId(), receiptId)) {
            throw new BusinessException("核销明细不属于当前回款单");
        }
        if (paymentId != null && !ObjectUtil.equal(item.getPaymentId(), paymentId)) {
            throw new BusinessException("核销明细不属于当前付款单");
        }
    }

    private void assertNotReversed(WriteOffItemDO origin) {
        String marker = reverseMarker(origin.getId());
        Long cnt = writeOffItemMapper.selectCount(Wrappers.lambdaQuery(WriteOffItemDO.class)
            .eq(WriteOffItemDO::getDirection, WriteOffDirectionEnum.REVERSE)
            .likeRight(WriteOffItemDO::getReason, marker));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("该核销明细已反核销");
        }
    }

    private String reverseMarker(Long originItemId) {
        return "REV_OF:" + originItemId + "|";
    }

    private String buildReverseReason(Long originItemId, String reason) {
        String base = reverseMarker(originItemId);
        return StrUtil.isBlank(reason) ? base : base + reason;
    }

    private WriteOffDO newWriteOff(Long tenantId, String remark) {
        WriteOffDO master = new WriteOffDO();
        master.setTenantId(tenantId);
        master.setDeptId(TenantFillHelper.currentDeptId());
        master.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_WRITE_OFF));
        master.setWriteOffDate(LocalDateTime.now());
        master.setRemark(remark);
        writeOffMapper.insert(master);
        return master;
    }

    private void insertItem(Long tenantId,
                            Long writeOffId,
                            Long receiptId,
                            Long paymentId,
                            WriteOffOpenTypeEnum openType,
                            Long openId,
                            WriteOffDirectionEnum direction,
                            BigDecimal amount,
                            String reason, Long cashSequence) {
        WriteOffItemDO item = new WriteOffItemDO();
        item.setTenantId(tenantId);
        item.setWriteOffId(writeOffId);
        item.setReceiptId(receiptId);
        item.setPaymentId(paymentId);
        item.setOpenType(openType);
        item.setOpenId(openId);
        item.setDirection(direction);
        item.setAmount(amount);
        item.setReason(reason);
        item.setCashSequence(cashSequence);
        writeOffItemMapper.insert(item);
    }

    private void applyAr(ReceivableDO ar, BigDecimal amount, boolean writeOff) {
        BigDecimal oldWritten=nz(ar.getWrittenOffAmount()),oldUnwritten=nz(ar.getUnwrittenAmount());
        if (writeOff) {
            ar.setWrittenOffAmount(nz(ar.getWrittenOffAmount()).add(amount));
            ar.setUnwrittenAmount(nz(ar.getUnwrittenAmount()).subtract(amount));
        } else {
            ar.setWrittenOffAmount(nz(ar.getWrittenOffAmount()).subtract(amount));
            ar.setUnwrittenAmount(nz(ar.getUnwrittenAmount()).add(amount));
        }
        ar.setDocStatus(ar.getUnwrittenAmount().compareTo(BigDecimal.ZERO) <= 0
            ? OpenItemStatusEnum.CLOSED
            : OpenItemStatusEnum.OPEN);
        if (cashLedgerMapper.applyCashReceivable(ar.getTenantId(),ar.getId(),ar.getWrittenOffAmount(),ar.getUnwrittenAmount(),ar.getDocStatus().getValue(),com.ls.aegis.common.context.UserContextHolder.getUserId(),oldWritten,oldUnwritten)!=1)
            throw new BusinessException("应收核销余额已变更，请重试");
        syncStockOutPayStatus(ar);
        if (!writeOff) {
            psiTodoCloseSupport.completeReverseIfCleared(ar);
        }
    }

    private void applyAp(PayableDO ap, BigDecimal amount, boolean writeOff) {
        BigDecimal oldWritten=nz(ap.getWrittenOffAmount()),oldUnwritten=nz(ap.getUnwrittenAmount());
        if (writeOff) {
            ap.setWrittenOffAmount(nz(ap.getWrittenOffAmount()).add(amount));
            ap.setUnwrittenAmount(nz(ap.getUnwrittenAmount()).subtract(amount));
        } else {
            ap.setWrittenOffAmount(nz(ap.getWrittenOffAmount()).subtract(amount));
            ap.setUnwrittenAmount(nz(ap.getUnwrittenAmount()).add(amount));
        }
        ap.setDocStatus(ap.getUnwrittenAmount().compareTo(BigDecimal.ZERO) <= 0
            ? OpenItemStatusEnum.CLOSED
            : OpenItemStatusEnum.OPEN);
        if (cashLedgerMapper.applyCashPayable(ap.getTenantId(),ap.getId(),ap.getWrittenOffAmount(),ap.getUnwrittenAmount(),ap.getDocStatus().getValue(),com.ls.aegis.common.context.UserContextHolder.getUserId(),oldWritten,oldUnwritten)!=1)
            throw new BusinessException("应付核销余额已变更，请重试");
        syncStockInPayStatus(ap);
        if (!writeOff) {
            psiTodoCloseSupport.completeReverseIfCleared(ap);
        }
    }

    /**
     * 退货调减开口后也需回写原出库支付状态（与核销同口径）。
     */
    public void syncPayStatusFromReceivable(ReceivableDO ar) {
        syncStockOutPayStatus(ar);
    }

    /**
     * 退货调减开口后也需回写原入库支付状态（与核销同口径）。
     */
    public void syncPayStatusFromPayable(PayableDO ap) {
        syncStockInPayStatus(ap);
    }

    private void syncStockOutPayStatus(ReceivableDO ar) {
        if (!BizSourceTypeEnum.STOCK_OUT.equals(ar.getSourceType()) || ar.getSourceId() == null) {
            return;
        }
        if (cashLedgerMapper.cashStockOutPayStatus(ar.getTenantId(),ar.getSourceId(),toPayStatus(ar.getTaxInclusiveAmount(),ar.getWrittenOffAmount()).getValue(),com.ls.aegis.common.context.UserContextHolder.getUserId())!=1)
            throw new BusinessException("核销原出库单不存在或已变更");
    }

    private void syncStockInPayStatus(PayableDO ap) {
        if (!BizSourceTypeEnum.STOCK_IN.equals(ap.getSourceType()) || ap.getSourceId() == null) {
            return;
        }
        if (cashLedgerMapper.cashStockInPayStatus(ap.getTenantId(),ap.getSourceId(),toPayStatus(ap.getTaxInclusiveAmount(),ap.getWrittenOffAmount()).getValue(),com.ls.aegis.common.context.UserContextHolder.getUserId())!=1)
            throw new BusinessException("核销原入库单不存在或已变更");
    }

    private PayStatusEnum toPayStatus(BigDecimal total, BigDecimal written) {
        BigDecimal t = nz(total);
        BigDecimal w = nz(written);
        if (w.compareTo(BigDecimal.ZERO) <= 0) {
            return PayStatusEnum.UNPAID;
        }
        if (w.compareTo(t) >= 0) {
            return PayStatusEnum.PAID;
        }
        return PayStatusEnum.PARTIAL;
    }

    private BigDecimal sum(List<WriteOffReq.Item> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (WriteOffReq.Item line : lines) {
            total = total.add(line.getAmount());
        }
        return total;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
