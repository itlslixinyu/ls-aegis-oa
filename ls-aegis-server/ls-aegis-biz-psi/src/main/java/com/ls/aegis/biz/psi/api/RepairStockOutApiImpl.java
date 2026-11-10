package com.ls.aegis.biz.psi.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockLineReq;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.service.StockOutService;
import com.ls.aegis.common.api.psi.RepairStockOutApi;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修结算出库：按仓生成 REPAIR 出库单并确认
 */
@Service
@RequiredArgsConstructor
public class RepairStockOutApiImpl implements RepairStockOutApi {

    private final StockOutService stockOutService;
    private final StockOutMapper stockOutMapper;
    private final com.ls.aegis.biz.psi.mapper.RepairStockOutInternalMapper repairInternalMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;
    private final com.ls.aegis.biz.psi.mapper.StockOutItemMapper stockOutItemMapper;
    private final com.ls.aegis.biz.psi.mapper.StockOutBatchMapper stockOutBatchMapper;
    private final com.ls.aegis.biz.psi.support.FifoStockHelper fifoStockHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal confirmRepairOut(Cmd cmd) {
        if (cmd == null || cmd.getOrderId() == null || cmd.getSettlementId() == null) {
            throw new BusinessException("维修出库必须指定工单和结算单");
        }
        String remarkPrefix = "工单[" + StrUtil.blankToDefault(cmd.getOrderNo(), String.valueOf(cmd.getOrderId())) + "]结算出库";
        Long tenantId = com.ls.aegis.biz.psi.support.TenantFillHelper.currentTenantId();
        List<StockOutDO> existing = repairHeads(tenantId, cmd.getOrderId(), remarkPrefix);
        List<StockOutDO> active = existing.stream().filter(out -> out.getDocStatus() != com.ls.aegis.biz.psi.enums.DocStatusEnum.VOID).toList();
        if (CollUtil.isEmpty(cmd.getItems())) {
            if (!active.isEmpty()) throw new BusinessException("无用料结算存在活动维修出库，请先对账");
            return BigDecimal.ZERO;
        }
        Long defaultWh = resolveDefaultWarehouse(tenantId);
        Map<Long, List<StockLineReq>> byWarehouse = new LinkedHashMap<>();
        for (Item item : cmd.getItems()) {
            if (item.getGoodsId() == null || item.getQuantity() == null
                || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("维修出库用料必须关联商品，且数量为正");
            }
            Long warehouseId = item.getWarehouseId() != null ? item.getWarehouseId() : defaultWh;
            if (warehouseId == null) {
                throw new BusinessException("未配置进销存仓库，无法维修出库");
            }
            WarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
            if (warehouse == null || !java.util.Objects.equals(tenantId, warehouse.getTenantId()))
                throw new BusinessException("维修出库仓库不存在或租户不一致");
            GoodsDO goods = goodsMapper.selectById(item.getGoodsId());
            if (goods == null || !java.util.Objects.equals(tenantId, goods.getTenantId())) {
                throw new BusinessException("商品不存在：" + StrUtil.blankToDefault(item.getGoodsName(), String.valueOf(item
                    .getGoodsId())));
            }
            StockLineReq line = new StockLineReq();
            line.setGoodsId(item.getGoodsId());
            line.setQuantity(item.getQuantity());
            BigDecimal price = item.getTaxInclusivePrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                price = ObjectUtil.defaultIfNull(goods.getSalePrice(), BigDecimal.ZERO);
            }
            line.setTaxInclusivePrice(price);
            line.setTaxRate(ObjectUtil.defaultIfNull(goods.getDefaultTaxRate(), new BigDecimal("13")));
            byWarehouse.computeIfAbsent(warehouseId, k -> new ArrayList<>()).add(line);
        }
        if (byWarehouse.isEmpty()) return BigDecimal.ZERO;
        if (!active.isEmpty()) {
            return validateExisting(active, byWarehouse, tenantId, cmd);
        }
        fifoStockHelper.lockAll(byWarehouse.entrySet().stream().flatMap(entry -> entry.getValue().stream()
            .map(line -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(tenantId, entry.getKey(), line.getGoodsId()))).toList());
        int seq = 0;
        for (Map.Entry<Long, List<StockLineReq>> e : byWarehouse.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            seq++;
            StockOutReq req = new StockOutReq();
            req.setOutType(StockOutTypeEnum.REPAIR);
            req.setOutDate(LocalDateTime.now());
            req.setWarehouseId(e.getKey());
            req.setUnitId(cmd.getCustomerId());
            req.setRemark(remarkPrefix + (byWarehouse.size() > 1 ? ("-" + seq) : ""));
            req.setItems(e.getValue());
            Long id = stockOutService.create(req);
            StockOutDO link = new StockOutDO();
            link.setId(id); link.setRepairOrderId(cmd.getOrderId()); link.setRepairSettlementId(cmd.getSettlementId());
            if (stockOutMapper.updateById(link) != 1) throw new BusinessException("维修出库结算关联保存失败");
            DocConfirmReq confirmReq = new DocConfirmReq();
            confirmReq.setIdempotentKey(cn.hutool.crypto.digest.DigestUtil.sha256Hex("repair-out:" + cmd.getOrderId() + ":"
                + ObjectUtil.defaultIfNull(cmd.getSettlementId(), 0L) + ":" + e.getKey()));
            stockOutService.confirm(id, confirmReq);
        }
        List<StockOutDO> confirmed = repairHeads(tenantId, cmd.getOrderId(), remarkPrefix).stream()
            .filter(out -> out.getDocStatus() != com.ls.aegis.biz.psi.enums.DocStatusEnum.VOID).toList();
        return validateExisting(confirmed, byWarehouse, tenantId, cmd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseRepairOut(Long orderId, String orderNo) {
        if (orderId == null || StrUtil.isBlank(orderNo)) throw new BusinessException("维修出库冲销必须指定工单");
        String prefix = "工单[" + orderNo + "]结算出库";
        Long tenantId = com.ls.aegis.biz.psi.support.TenantFillHelper.currentTenantId();
        List<StockOutDO> candidates = repairHeads(tenantId, orderId, prefix);
        Map<Long, List<com.ls.aegis.biz.psi.model.entity.StockOutItemDO>> itemsByOut = new LinkedHashMap<>();
        List<com.ls.aegis.biz.psi.support.StockMutationLock.Key> stockKeys = new ArrayList<>();
        // Lock all existing heads before any stock anchor, then acquire the whole multi-warehouse footprint.
        for (StockOutDO out : candidates) {
            if (com.ls.aegis.biz.psi.enums.DocStatusEnum.VOID.equals(out.getDocStatus())) continue;
            if (!com.ls.aegis.biz.psi.enums.DocStatusEnum.CONFIRMED.equals(out.getDocStatus()))
                throw new BusinessException("存在未确认维修出库草稿，请先核对后再冲销结算");
            var items = repairInternalMapper.selectItems(tenantId, out.getId());
            if (items.isEmpty()) throw new BusinessException("维修出库缺少明细，不能冲销");
            itemsByOut.put(out.getId(), items);
            for (var item : items) stockKeys.add(new com.ls.aegis.biz.psi.support.StockMutationLock.Key(tenantId, out.getWarehouseId(), item.getGoodsId()));
        }
        fifoStockHelper.lockAll(stockKeys);
        for (StockOutDO out : candidates) {
            var items = itemsByOut.get(out.getId());
            if (items == null) continue;
            for (var item : items) {
                var consumed = stockOutBatchMapper.selectByOutItemDesc(item.getId());
                BigDecimal quantity = consumed.stream().map(com.ls.aegis.biz.psi.model.entity.StockOutBatchDO::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cost = consumed.stream().map(com.ls.aegis.biz.psi.model.entity.StockOutBatchDO::getCostAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (quantity.compareTo(item.getQuantity()) != 0 || cost.compareTo(item.getCostAmount()) != 0) {
                    throw new BusinessException("维修出库批次与数量/成本不一致，请先对账");
                }
                var restores = consumed.stream().map(c -> new com.ls.aegis.biz.psi.support.FifoStockHelper.ConsumeLine(
                    c.getBatchId(), c.getQuantity(), c.getUntaxPrice(), c.getCostAmount())).toList();
                fifoStockHelper.restoreConsumes(tenantId, out.getWarehouseId(), item.getGoodsId(), restores,
                    com.ls.aegis.biz.psi.enums.BizSourceTypeEnum.REPAIR_REVERSE, out.getId(), out.getDocNo());
            }
            if (repairInternalMapper.markVoid(tenantId, out.getId()) != 1)
                throw new BusinessException("维修出库状态已变更，冲销失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assertNoActiveRepairOut(Long orderId, String orderNo) {
        if (orderId == null || StrUtil.isBlank(orderNo)) throw new BusinessException("撤销关闭必须指定工单");
        Long tenantId = com.ls.aegis.biz.psi.support.TenantFillHelper.currentTenantId();
        if (repairHeads(tenantId, orderId, "工单[" + orderNo + "]结算出库").stream()
                .anyMatch(out -> out.getDocStatus() != com.ls.aegis.biz.psi.enums.DocStatusEnum.VOID))
            throw new BusinessException("存在未冲销维修出库，不能撤销关闭工单");
    }

    private List<StockOutDO> repairHeads(Long tenantId, Long orderId, String prefix) {
        return repairInternalMapper.lockRepairHeads(tenantId, orderId, prefix).stream().map(out -> {
            if (!java.util.Objects.equals(tenantId, out.getTenantId())) throw new BusinessException("维修出库租户不一致");
            if (java.util.Objects.equals(orderId, out.getRepairOrderId())) return out;
            // Legacy compatibility is literal; order numbers containing '%'/'_' are never LIKE wildcards.
            if (out.getRepairOrderId() == null && out.getRemark() != null
                && out.getRemark().matches(java.util.regex.Pattern.quote(prefix) + "(?:-[1-9][0-9]*)?")) return out;
            throw new BusinessException("历史维修出库关联不明确，请先核对工单关联后再确认或冲销");
        }).sorted(java.util.Comparator.comparing(StockOutDO::getId)).toList();
    }

    private BigDecimal validateExisting(List<StockOutDO> active, Map<Long, List<StockLineReq>> expected, Long tenantId, Cmd cmd) {
        BigDecimal totalCost = BigDecimal.ZERO;
        if (active.size() != expected.size()) throw new BusinessException("维修出库关联不完整，不能跳过或重复确认");
        java.util.Set<Long> warehouses = new java.util.HashSet<>();
        for (StockOutDO out : active) {
            if (!java.util.Objects.equals(tenantId, out.getTenantId())
                    || !java.util.Objects.equals(cmd.getOrderId(), out.getRepairOrderId())
                    || !java.util.Objects.equals(cmd.getSettlementId(), out.getRepairSettlementId())
                    || out.getDocStatus() != com.ls.aegis.biz.psi.enums.DocStatusEnum.CONFIRMED || !warehouses.add(out.getWarehouseId())
                    || !expected.containsKey(out.getWarehouseId())) throw new BusinessException("维修出库状态或仓库关联不一致");
            Map<Long, BigDecimal> expectedQuantities = new java.util.TreeMap<>(), actualQuantities = new java.util.TreeMap<>();
            for (StockLineReq line : expected.get(out.getWarehouseId())) expectedQuantities.merge(line.getGoodsId(), line.getQuantity(), BigDecimal::add);
            BigDecimal headCost = BigDecimal.ZERO;
            for (var line : repairInternalMapper.selectItems(tenantId, out.getId())) {
                if (!java.util.Objects.equals(tenantId, line.getTenantId()) || !java.util.Objects.equals(out.getId(), line.getStockOutId())
                        || line.getGoodsId() == null || line.getQuantity() == null || line.getQuantity().signum() <= 0)
                    throw new BusinessException("维修出库明细关联或数量异常，请先对账");
                BigDecimal lineCost = checkedCost(line.getCostAmount());
                BigDecimal batchQty = BigDecimal.ZERO, batchCost = BigDecimal.ZERO;
                for (var batch : stockOutBatchMapper.selectByOutItemDesc(line.getId())) {
                    if (!java.util.Objects.equals(tenantId, batch.getTenantId()) || !java.util.Objects.equals(line.getId(), batch.getStockOutItemId())
                            || batch.getBatchId() == null || batch.getQuantity() == null || batch.getQuantity().signum() <= 0)
                        throw new BusinessException("维修出库FIFO批次关联或数量异常，请先对账");
                    batchQty = batchQty.add(batch.getQuantity());
                    batchCost = checkedCost(batchCost.add(checkedCost(batch.getCostAmount())));
                }
                if (batchQty.compareTo(line.getQuantity()) != 0 || batchCost.compareTo(lineCost) != 0)
                    throw new BusinessException("维修出库FIFO数量/成本与明细不一致，请先对账");
                actualQuantities.merge(line.getGoodsId(), line.getQuantity(), BigDecimal::add);
                headCost = checkedCost(headCost.add(lineCost));
            }
            if (headCost.compareTo(checkedCost(out.getCostAmount())) != 0)
                throw new BusinessException("维修出库头与FIFO成本不一致，请先对账");
            totalCost = checkedCost(totalCost.add(headCost));
            if (!expectedQuantities.keySet().equals(actualQuantities.keySet()) || expectedQuantities.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().compareTo(actualQuantities.get(entry.getKey())) != 0))
                throw new BusinessException("维修出库商品数量与工单不一致，请先对账");
        }
        return totalCost;
    }

    private BigDecimal checkedCost(BigDecimal cost) {
        if (cost == null || cost.signum() < 0 || cost.stripTrailingZeros().scale() > 2 || cost.precision() - cost.scale() > 10)
            throw new BusinessException("维修实际FIFO成本必须为非负数且最多10位整数、2位小数");
        return cost;
    }

    private Long resolveDefaultWarehouse(Long tenantId) {
        List<WarehouseDO> list = warehouseMapper.selectList(Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getTenantId, tenantId)
            .eq(WarehouseDO::getStatus, DisEnableStatusEnum.ENABLE)
            .orderByDesc(WarehouseDO::getIsDefault)
            .orderByAsc(WarehouseDO::getId)
            .last("LIMIT 1"));
        return CollUtil.isEmpty(list) ? null : list.get(0).getId();
    }
}
