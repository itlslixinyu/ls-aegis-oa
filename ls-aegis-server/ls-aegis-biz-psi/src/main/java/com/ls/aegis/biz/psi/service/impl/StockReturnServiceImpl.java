package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.enums.ReturnTypeEnum;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.mapper.StockInItemMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.StockOutBatchMapper;
import com.ls.aegis.biz.psi.mapper.StockOutItemMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.mapper.StockReturnItemMapper;
import com.ls.aegis.biz.psi.mapper.StockReturnMapper;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.entity.StockInDO;
import com.ls.aegis.biz.psi.model.entity.StockInItemDO;
import com.ls.aegis.biz.psi.model.entity.StockOutBatchDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.entity.StockOutItemDO;
import com.ls.aegis.biz.psi.model.entity.StockReturnDO;
import com.ls.aegis.biz.psi.model.entity.StockReturnItemDO;
import com.ls.aegis.biz.psi.model.query.StockReturnQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockReturnReq;
import com.ls.aegis.biz.psi.model.resp.StockReturnResp;
import com.ls.aegis.biz.psi.service.StockReturnService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.InvoiceStatusSupport;
import com.ls.aegis.biz.psi.support.PsiTodoHelper;
import com.ls.aegis.biz.psi.support.TaxCalcUtils;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 退货：销售原路恢复批次；采购从原入库批次扣减
 */
@Service
@RequiredArgsConstructor
public class StockReturnServiceImpl implements StockReturnService {

    private final StockReturnMapper stockReturnMapper;
    private final StockReturnItemMapper stockReturnItemMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final StockOutBatchMapper stockOutBatchMapper;
    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final PsiTodoHelper psiTodoHelper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;
    private final WriteOffHelper writeOffHelper;
    private final InvoiceStatusSupport invoiceStatusSupport;
    private final CashCompensationService cashCompensationService;
    private final com.ls.aegis.biz.psi.mapper.CashLedgerMapper cashLedgerMapper;

    @Override
    public PageResp<StockReturnResp> page(StockReturnQuery query, PageQuery pageQuery) {
        QueryWrapper<StockReturnDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockReturnDO> page = stockReturnMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        return PageResp.build(page, StockReturnResp.class);
    }

    @Override
    public StockReturnResp get(Long id) {
        StockReturnDO entity = require(id);
        StockReturnResp resp = BeanUtil.copyProperties(entity, StockReturnResp.class);
        List<StockReturnItemDO> items = stockReturnItemMapper.selectList(Wrappers.lambdaQuery(StockReturnItemDO.class)
            .eq(StockReturnItemDO::getReturnId, id)
            .orderByAsc(StockReturnItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockReturnResp.StockReturnItemResp.class));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockReturnReq req) {
        validate(req);
        Long tenantId = TenantFillHelper.currentTenantId();
        StockReturnDO entity = buildHeader(req, tenantId);
        stockReturnMapper.insert(entity);
        insertItems(tenantId, entity.getId(), req);
        refreshHeaderAmount(entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockReturnReq req, Long id) {
        StockReturnDO entity = requireLocked(id);
        assertDraft(entity);
        validate(req);
        entity.setReturnType(req.getReturnType());
        entity.setReturnDate(req.getReturnDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setOriginStockOutId(req.getOriginStockOutId());
        entity.setOriginStockInId(req.getOriginStockInId());
        entity.setRemark(req.getRemark());
        stockReturnMapper.updateById(entity);
        stockReturnItemMapper.delete(Wrappers.lambdaQuery(StockReturnItemDO.class).eq(StockReturnItemDO::getReturnId, id));
        insertItems(entity.getTenantId(), id, req);
        refreshHeaderAmount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockReturnDO entity = requireLocked(id);
        assertDraft(entity);
        stockReturnItemMapper.delete(Wrappers.lambdaQuery(StockReturnItemDO.class).eq(StockReturnItemDO::getReturnId, id));
        stockReturnMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String confirm(Long id, DocConfirmReq req) {
        StockReturnDO locked = stockReturnMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("退货单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-return-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(StockReturnDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        List<StockReturnItemDO> items = stockReturnItemMapper.selectList(Wrappers.lambdaQuery(StockReturnItemDO.class)
            .eq(StockReturnItemDO::getReturnId, locked.getId())
            .orderByAsc(StockReturnItemDO::getSort));
        // 锁原单使同一原单的并发退货串行；保存时的可退量不是确认时的有效快照。
        StockReturnReq validation = BeanUtil.copyProperties(locked, StockReturnReq.class);
        validation.setItems(BeanUtil.copyToList(items, StockReturnReq.StockReturnItemReq.class));
        validate(validation, true);
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        // 原单头→开口→库存锚点，与资金核销的原单头→开口顺序一致。
        if (ReturnTypeEnum.SALE_RETURN.equals(locked.getReturnType())) reduceReceivable(tenantId, locked);
        else reducePayable(tenantId, locked);
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        BigDecimal totalCost = BigDecimal.ZERO;

        if (ReturnTypeEnum.SALE_RETURN.equals(locked.getReturnType())) {
            for (StockReturnItemDO item : items) {
                List<FifoStockHelper.ConsumeLine> restores = buildSaleRestoreLines(item);
                BigDecimal cost = fifoStockHelper.restoreConsumes(tenantId, locked.getWarehouseId(), item
                    .getGoodsId(), restores, BizSourceTypeEnum.SALE_RETURN, locked.getId(), locked.getDocNo());
                item.setCostAmount(cost);
                stockReturnItemMapper.updateById(item);
                totalCost = totalCost.add(cost);
            }
            createTodos(tenantId, locked);
        } else {
            for (StockReturnItemDO item : items) {
                List<FifoStockHelper.ConsumeLine> consumes = fifoStockHelper.outboundFromSource(tenantId, locked
                    .getWarehouseId(), item.getGoodsId(), item.getQuantity(), BizSourceTypeEnum.STOCK_IN, locked
                        .getOriginStockInId(), BizSourceTypeEnum.PURCHASE_RETURN, locked.getId(), locked.getDocNo());
                BigDecimal cost = consumes.stream()
                    .map(FifoStockHelper.ConsumeLine::costAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setCostAmount(cost);
                stockReturnItemMapper.updateById(item);
                totalCost = totalCost.add(cost);
            }
            createTodos(tenantId, locked);
        }

        locked.setCostAmount(totalCost);
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockReturnMapper.updateById(locked);
        return locked.getDocNo();
    }

    private List<FifoStockHelper.ConsumeLine> buildSaleRestoreLines(StockReturnItemDO item) {
        StockOutItemDO outItem = stockOutItemMapper.selectById(item.getOriginItemId());
        if (outItem == null) {
            throw new BusinessException("原出库明细不存在");
        }
        BigDecimal returned = nz(stockReturnMapper.sumReturnedQty(item.getOriginItemId()));
        // 当前单尚未确认，sumReturned 不含本单；可退 = 原数量 - 已确认退货
        BigDecimal remainReturnable = outItem.getQuantity().subtract(returned);
        if (item.getQuantity().compareTo(remainReturnable) > 0) {
            throw new BusinessException("退货数量超过可退数量，商品ID=" + item.getGoodsId() + "，可退=" + remainReturnable);
        }
        List<StockOutBatchDO> consumes = stockOutBatchMapper.selectByOutItemDesc(item.getOriginItemId());
        BigDecimal skip = returned;
        BigDecimal need = item.getQuantity();
        List<FifoStockHelper.ConsumeLine> restores = new ArrayList<>();
        for (StockOutBatchDO c : consumes) {
            if (need.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal usable = c.getQuantity();
            if (skip.compareTo(BigDecimal.ZERO) > 0) {
                if (skip.compareTo(usable) >= 0) {
                    skip = skip.subtract(usable);
                    continue;
                }
                usable = usable.subtract(skip);
                skip = BigDecimal.ZERO;
            }
            BigDecimal take = usable.min(need);
            // 按累计已退数量的原始成本差额分摊，最后一次带回尾差，避免三次退1件丢1分钱。
            BigDecimal alreadyFromBatch = c.getQuantity().subtract(usable);
            BigDecimal beforeCost = c.getCostAmount().multiply(alreadyFromBatch).divide(c.getQuantity(), 2, RoundingMode.HALF_UP);
            BigDecimal afterCost = c.getCostAmount().multiply(alreadyFromBatch.add(take)).divide(c.getQuantity(), 2, RoundingMode.HALF_UP);
            BigDecimal cost = afterCost.subtract(beforeCost);
            restores.add(new FifoStockHelper.ConsumeLine(c.getBatchId(), take, c.getUntaxPrice(), cost));
            need = need.subtract(take);
        }
        if (need.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("原出库批次消耗明细不足，无法原路恢复");
        }
        return restores;
    }

    private void reduceReceivable(Long tenantId, StockReturnDO locked) {
        ReceivableDO ar = cashLedgerMapper.lockReturnReceivable(tenantId, locked.getOriginStockOutId());
        if (ar == null) {
            throw new BusinessException("原销售单缺少应收，请先对账，不能只恢复库存");
        }
        if (ar.getUnwrittenAmount().compareTo(locked.getTaxInclusiveAmount()) < 0) {
            throw new BusinessException("应收未核销余额不足，请先反核销后再退货。未核销=" + ar.getUnwrittenAmount());
        }
        ar.setUnwrittenAmount(ar.getUnwrittenAmount().subtract(locked.getTaxInclusiveAmount()));
        ar.setTaxInclusiveAmount(ar.getTaxInclusiveAmount().subtract(locked.getTaxInclusiveAmount()));
        if (ar.getUnwrittenAmount().compareTo(BigDecimal.ZERO) == 0) {
            ar.setDocStatus(OpenItemStatusEnum.CLOSED);
        }
        if (cashLedgerMapper.applyReturnReceivable(tenantId, ar.getId(), ar.getTaxInclusiveAmount(), ar.getUnwrittenAmount(),
            ar.getDocStatus().getValue(), UserContextHolder.getUserId()) != 1) throw new BusinessException("退货应收更新失败");
        writeOffHelper.syncPayStatusFromReceivable(ar);
    }

    private void reducePayable(Long tenantId, StockReturnDO locked) {
        PayableDO ap = cashLedgerMapper.lockReturnPayable(tenantId, locked.getOriginStockInId());
        if (ap == null) {
            throw new BusinessException("原采购单缺少应付，请先对账，不能只扣库存");
        }
        if (ap.getUnwrittenAmount().compareTo(locked.getTaxInclusiveAmount()) < 0) {
            throw new BusinessException("应付未核销余额不足，请先反核销后再退货。未核销=" + ap.getUnwrittenAmount());
        }
        ap.setUnwrittenAmount(ap.getUnwrittenAmount().subtract(locked.getTaxInclusiveAmount()));
        ap.setTaxInclusiveAmount(ap.getTaxInclusiveAmount().subtract(locked.getTaxInclusiveAmount()));
        if (ap.getUnwrittenAmount().compareTo(BigDecimal.ZERO) == 0) {
            ap.setDocStatus(OpenItemStatusEnum.CLOSED);
        }
        if (cashLedgerMapper.applyReturnPayable(tenantId, ap.getId(), ap.getTaxInclusiveAmount(), ap.getUnwrittenAmount(),
            ap.getDocStatus().getValue(), UserContextHolder.getUserId()) != 1) throw new BusinessException("退货应付更新失败");
        writeOffHelper.syncPayStatusFromPayable(ap);
    }

    private void createTodos(Long tenantId, StockReturnDO locked) {
        String bizType = locked.getReturnType() == null ? "STOCK_RETURN" : locked.getReturnType().getValue();
        boolean sale = ReturnTypeEnum.SALE_RETURN.equals(locked.getReturnType());
        BigDecimal linkedInvoice = BigDecimal.ZERO;
        if (sale && locked.getOriginStockOutId() != null) {
            linkedInvoice = invoiceStatusSupport.confirmedOutLinked(locked.getTenantId(),locked.getOriginStockOutId(), null);
        } else if (!sale && locked.getOriginStockInId() != null) {
            linkedInvoice = invoiceStatusSupport.confirmedInLinked(locked.getTenantId(),locked.getOriginStockInId(), null);
        }
        if (linkedInvoice.compareTo(BigDecimal.ZERO) > 0) {
            addTodo(tenantId, "RED_INVOICE", "待冲红：" + locked.getDocNo(), bizType, locked);
        }
        BigDecimal written = BigDecimal.ZERO;
        if (sale) {
            ReceivableDO ar = cashLedgerMapper.lockReturnReceivable(tenantId, locked.getOriginStockOutId());
            if (ar != null && ar.getWrittenOffAmount() != null) {
                written = ar.getWrittenOffAmount();
            }
        } else {
            PayableDO ap = cashLedgerMapper.lockReturnPayable(tenantId, locked.getOriginStockInId());
            if (ap != null && ap.getWrittenOffAmount() != null) {
                written = ap.getWrittenOffAmount();
            }
        }
        if (written.compareTo(BigDecimal.ZERO) > 0) {
            addTodo(tenantId, "REVERSE_WRITEOFF", "待反核销：" + locked.getDocNo(), bizType, locked);
        }
        Long openId;
        if (sale) {
            ReceivableDO ar = cashLedgerMapper.lockReturnReceivable(tenantId, locked.getOriginStockOutId());
            openId = ar == null ? null : ar.getId();
        } else {
            PayableDO ap = cashLedgerMapper.lockReturnPayable(tenantId, locked.getOriginStockInId());
            openId = ap == null ? null : ap.getId();
        }
        if (locked.getTaxInclusiveAmount().signum() > 0 && cashCompensationService.hasRefundableCredit(sale, openId)) {
            addTodo(tenantId, sale ? "REFUND" : "RECEIVE_REFUND",
                (sale ? "待核对退款：" : "待核对收退款：") + locked.getDocNo(), bizType, locked);
        }
    }

    private void addTodo(Long tenantId, String type, String title, String bizType, StockReturnDO locked) {
        psiTodoHelper.create(tenantId, type, title, bizType, locked.getId(), locked.getDocNo());
    }

    private void validate(StockReturnReq req) {
        validate(req, false);
    }

    private void validate(StockReturnReq req, boolean lockOrigin) {
        if (req.getReturnType() == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("退货类型和明细不能为空");
        }
        java.util.Set<Long> origins = new java.util.HashSet<>();
        for (StockReturnReq.StockReturnItemReq line : req.getItems()) {
            if (line == null || line.getOriginItemId() == null || line.getGoodsId() == null
                || line.getQuantity() == null || line.getQuantity().signum() <= 0
                || line.getTaxInclusivePrice() == null || line.getTaxInclusivePrice().signum() < 0
                || line.getTaxRate() == null || line.getTaxRate().signum() < 0) {
                throw new BusinessException("退货明细须关联原明细和商品，数量为正，单价和税率非负");
            }
            if (!origins.add(line.getOriginItemId())) {
                throw new BusinessException("同一原明细不可重复，请合并数量后录入");
            }
        }
        if (ReturnTypeEnum.SALE_RETURN.equals(req.getReturnType())) {
            if (req.getOriginStockOutId() == null) {
                throw new BusinessException("销售退货必须关联原出库单");
            }
            StockOutDO out = lockOrigin ? stockOutMapper.selectByIdForUpdate(req.getOriginStockOutId())
                : stockOutMapper.selectById(req.getOriginStockOutId());
            if (out == null || !DocStatusEnum.CONFIRMED.equals(out.getDocStatus())) {
                throw new BusinessException("原出库单不存在或未确认");
            }
            if (!ObjectUtil.equal(out.getWarehouseId(), req.getWarehouseId())) {
                throw new BusinessException("退货仓库必须与原出库仓库一致");
            }
            if (!com.ls.aegis.biz.psi.enums.StockOutTypeEnum.SALE.equals(out.getOutType())) {
                throw new BusinessException("销售退货仅支持销售出库；维修出库请走维修结算冲销");
            }
            if (!ObjectUtil.equal(out.getUnitId(), req.getUnitId())) {
                throw new BusinessException("退货客户必须与原出库单一致");
            }
            for (StockReturnReq.StockReturnItemReq line : req.getItems()) {
                StockOutItemDO outItem = stockOutItemMapper.selectById(line.getOriginItemId());
                if (outItem == null) {
                    throw new BusinessException("原出库明细不存在");
                }
                if (!ObjectUtil.equal(outItem.getStockOutId(), out.getId())
                    || !ObjectUtil.equal(outItem.getGoodsId(), line.getGoodsId())) {
                    throw new BusinessException("退货原明细、原出库单和商品不匹配");
                }
                BigDecimal returned = nz(stockReturnMapper.sumReturnedQty(line.getOriginItemId()));
                BigDecimal remain = outItem.getQuantity().subtract(returned);
                if (line.getQuantity().compareTo(remain) > 0) {
                    throw new BusinessException("销售退货数量超过可退数量，可退=" + remain);
                }
            }
        } else {
            if (req.getOriginStockInId() == null) {
                throw new BusinessException("采购退货必须关联原入库单");
            }
            StockInDO in = lockOrigin ? stockInMapper.selectByIdForUpdate(req.getOriginStockInId())
                : stockInMapper.selectById(req.getOriginStockInId());
            if (in == null || !DocStatusEnum.CONFIRMED.equals(in.getDocStatus())) {
                throw new BusinessException("原入库单不存在或未确认");
            }
            if (!ObjectUtil.equal(in.getWarehouseId(), req.getWarehouseId())) {
                throw new BusinessException("退货仓库必须与原入库仓库一致");
            }
            if (!com.ls.aegis.biz.psi.enums.StockInTypeEnum.PURCHASE.equals(in.getInType())) {
                throw new BusinessException("采购退货仅支持采购入库");
            }
            if (!ObjectUtil.equal(in.getUnitId(), req.getUnitId())) {
                throw new BusinessException("退货供应商必须与原入库单一致");
            }
            for (StockReturnReq.StockReturnItemReq line : req.getItems()) {
                StockInItemDO inItem = stockInItemMapper.selectById(line.getOriginItemId());
                if (inItem == null) {
                    throw new BusinessException("原入库明细不存在");
                }
                if (!ObjectUtil.equal(inItem.getStockInId(), in.getId())
                    || !ObjectUtil.equal(inItem.getGoodsId(), line.getGoodsId())) {
                    throw new BusinessException("退货原明细、原入库单和商品不匹配");
                }
                BigDecimal returned = nz(stockReturnMapper.sumReturnedQty(line.getOriginItemId()));
                BigDecimal remain = inItem.getQuantity().subtract(returned);
                if (line.getQuantity().compareTo(remain) > 0) {
                    throw new BusinessException("采购退货数量超过可退数量，可退=" + remain);
                }
            }
        }
    }

    private StockReturnDO buildHeader(StockReturnReq req, Long tenantId) {
        StockReturnDO entity = new StockReturnDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_RETURN));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setReturnType(req.getReturnType());
        entity.setReturnDate(req.getReturnDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setOriginStockOutId(req.getOriginStockOutId());
        entity.setOriginStockInId(req.getOriginStockInId());
        entity.setTaxInclusiveAmount(BigDecimal.ZERO);
        entity.setUntaxAmount(BigDecimal.ZERO);
        entity.setTaxAmount(BigDecimal.ZERO);
        entity.setCostAmount(BigDecimal.ZERO);
        entity.setRemark(req.getRemark());
        return entity;
    }

    private void insertItems(Long tenantId, Long returnId, StockReturnReq req) {
        int sort = 1;
        for (StockReturnReq.StockReturnItemReq line : req.getItems()) {
            TaxCalcUtils.LineTaxResult tax = TaxCalcUtils.calcByInclusivePrice(line.getQuantity(), line
                .getTaxInclusivePrice(), line.getTaxRate());
            StockReturnItemDO item = new StockReturnItemDO();
            item.setTenantId(tenantId);
            item.setReturnId(returnId);
            item.setOriginItemId(line.getOriginItemId());
            item.setGoodsId(line.getGoodsId());
            item.setQuantity(line.getQuantity());
            item.setTaxInclusivePrice(line.getTaxInclusivePrice());
            item.setTaxRate(line.getTaxRate());
            item.setUntaxPrice(tax.untaxPrice());
            item.setUntaxAmount(tax.untaxAmount());
            item.setTaxAmount(tax.taxAmount());
            item.setTaxInclusiveAmount(tax.taxInclusiveAmount());
            item.setCostAmount(BigDecimal.ZERO);
            item.setSort(sort++);
            stockReturnItemMapper.insert(item);
        }
    }

    private void refreshHeaderAmount(Long id) {
        List<StockReturnItemDO> items = stockReturnItemMapper.selectList(Wrappers.lambdaQuery(StockReturnItemDO.class)
            .eq(StockReturnItemDO::getReturnId, id));
        BigDecimal incl = BigDecimal.ZERO;
        BigDecimal untax = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (StockReturnItemDO item : items) {
            incl = incl.add(item.getTaxInclusiveAmount());
            untax = untax.add(item.getUntaxAmount());
            tax = tax.add(item.getTaxAmount());
        }
        StockReturnDO upd = new StockReturnDO();
        upd.setId(id);
        upd.setTaxInclusiveAmount(incl);
        upd.setUntaxAmount(untax);
        upd.setTaxAmount(tax);
        stockReturnMapper.updateById(upd);
    }

    private StockReturnDO requireLocked(Long id) {
        StockReturnDO entity = stockReturnMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockReturnDO require(Long id) {
        StockReturnDO entity = stockReturnMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("退货单不存在");
        }
        return entity;
    }

    private void assertDraft(StockReturnDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
