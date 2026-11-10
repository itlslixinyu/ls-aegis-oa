package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockFlowTypeEnum;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.mapper.StockFlowMapper;
import com.ls.aegis.biz.psi.model.entity.StockBatchDO;
import com.ls.aegis.biz.psi.model.entity.StockFlowDO;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FIFO 库存扣减 / 入库建批 / 原路恢复 / 调拨 / 流水
 */
@Component
@RequiredArgsConstructor
public class FifoStockHelper {

    private static final int AMOUNT_SCALE = 2;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private final StockBatchMapper stockBatchMapper;
    private final StockFlowMapper stockFlowMapper;
    private final DocNoGenerator docNoGenerator;
    private final StockMutationLock stockMutationLock;

    /**
     * 入库建批并记流水
     */
    public StockBatchDO inbound(Long tenantId,
                                Long warehouseId,
                                Long goodsId,
                                BigDecimal quantity,
                                BigDecimal untaxPrice,
                                BigDecimal untaxAmount,
                                LocalDateTime inDate,
                                BizSourceTypeEnum sourceType,
                                Long sourceId,
                                String sourceNo) {
        return createBatch(tenantId, warehouseId, goodsId, quantity, untaxPrice, untaxAmount, inDate, inDate, sourceType, sourceId, sourceNo, StockFlowTypeEnum.IN);
    }

    /**
     * 调入建批：保留原入库日与成本
     */
    public StockBatchDO transferInbound(Long tenantId,
                                        Long warehouseId,
                                        Long goodsId,
                                        BigDecimal quantity,
                                        BigDecimal untaxPrice,
                                        BigDecimal costAmount,
                                        LocalDateTime inDate,
                                        LocalDateTime originInDate,
                                        Long sourceId,
                                        String sourceNo) {
        return createBatch(tenantId, warehouseId, goodsId, quantity, untaxPrice, costAmount, inDate, originInDate, BizSourceTypeEnum.TRANSFER, sourceId, sourceNo, StockFlowTypeEnum.TRANSFER_IN);
    }

    /**
     * FIFO 出库扣减
     */
    public List<ConsumeLine> outbound(Long tenantId,
                                      Long warehouseId,
                                      Long goodsId,
                                      BigDecimal needQty,
                                      BizSourceTypeEnum sourceType,
                                      Long sourceId,
                                      String sourceNo) {
        return outbound(tenantId, warehouseId, goodsId, needQty, sourceType, sourceId, sourceNo, StockFlowTypeEnum.OUT, null);
    }

    /**
     * 调出：FIFO 扣减，流水类型 TRANSFER_OUT，并带回原批次日期
     */
    public List<ConsumeLine> transferOutbound(Long tenantId,
                                              Long warehouseId,
                                              Long goodsId,
                                              BigDecimal needQty,
                                              Long sourceId,
                                              String sourceNo) {
        return outbound(tenantId, warehouseId, goodsId, needQty, BizSourceTypeEnum.TRANSFER, sourceId, sourceNo, StockFlowTypeEnum.TRANSFER_OUT, null);
    }

    /**
     * 盘亏：FIFO 扣减，流水类型 LOSS
     */
    public List<ConsumeLine> lossOutbound(Long tenantId,
                                          Long warehouseId,
                                          Long goodsId,
                                          BigDecimal needQty,
                                          BizSourceTypeEnum sourceType,
                                          Long sourceId,
                                          String sourceNo) {
        return outbound(tenantId, warehouseId, goodsId, needQty, sourceType, sourceId, sourceNo, StockFlowTypeEnum.LOSS, null);
    }

    /**
     * 盘盈：新建批次，流水类型 PROFIT
     */
    public StockBatchDO profitInbound(Long tenantId,
                                      Long warehouseId,
                                      Long goodsId,
                                      BigDecimal quantity,
                                      BigDecimal untaxPrice,
                                      BigDecimal untaxAmount,
                                      LocalDateTime inDate,
                                      BizSourceTypeEnum sourceType,
                                      Long sourceId,
                                      String sourceNo) {
        return createBatch(tenantId, warehouseId, goodsId, quantity, untaxPrice, untaxAmount, inDate, inDate, sourceType, sourceId, sourceNo, StockFlowTypeEnum.PROFIT);
    }

    /**
     * 调整盘亏：FIFO 扣减，流水类型 ADJUST
     */
    public List<ConsumeLine> adjustOutbound(Long tenantId,
                                            Long warehouseId,
                                            Long goodsId,
                                            BigDecimal needQty,
                                            Long sourceId,
                                            String sourceNo) {
        return outbound(tenantId, warehouseId, goodsId, needQty, BizSourceTypeEnum.STOCK_ADJUST, sourceId, sourceNo, StockFlowTypeEnum.ADJUST, null);
    }

    /**
     * 调整盘盈：新建批次，流水类型 ADJUST
     */
    public StockBatchDO adjustInbound(Long tenantId,
                                      Long warehouseId,
                                      Long goodsId,
                                      BigDecimal quantity,
                                      BigDecimal untaxPrice,
                                      BigDecimal untaxAmount,
                                      LocalDateTime inDate,
                                      Long sourceId,
                                      String sourceNo) {
        return createBatch(tenantId, warehouseId, goodsId, quantity, untaxPrice, untaxAmount, inDate, inDate, BizSourceTypeEnum.STOCK_ADJUST, sourceId, sourceNo, StockFlowTypeEnum.ADJUST);
    }

    /**
     * 采购退货：仅从指定来源入库单生成的批次中扣减
     */
    public List<ConsumeLine> outboundFromSource(Long tenantId,
                                                Long warehouseId,
                                                Long goodsId,
                                                BigDecimal needQty,
                                                BizSourceTypeEnum sourceTypeFilter,
                                                Long sourceIdFilter,
                                                BizSourceTypeEnum bizSourceType,
                                                Long bizId,
                                                String bizNo) {
        return outbound(tenantId, warehouseId, goodsId, needQty, bizSourceType, bizId, bizNo, StockFlowTypeEnum.OUT, new SourceFilter(sourceTypeFilter, sourceIdFilter));
    }

    /**
     * 销售退货原路恢复：按消耗明细加回批次，不新建批次
     */
    public BigDecimal restoreConsumes(Long tenantId,
                                      Long warehouseId,
                                      Long goodsId,
                                      List<ConsumeLine> restores,
                                      BizSourceTypeEnum sourceType,
                                      Long sourceId,
                                      String sourceNo) {
        if (restores == null || restores.isEmpty()) {
            throw new BusinessException("恢复明细不能为空");
        }
        StockBalance before = currentBalance(tenantId, warehouseId, goodsId);
        BigDecimal beforeQty = before.quantity();
        BigDecimal beforeAmt = before.amount();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        for (ConsumeLine line : restores) {
            StockBatchDO batch = stockBatchMapper.selectByIdForUpdate(tenantId, line.batchId());
            if (batch == null) {
                throw new BusinessException("原批次不存在，batchId=" + line.batchId());
            }
            if (!java.util.Objects.equals(batch.getWarehouseId(), warehouseId)
                || !java.util.Objects.equals(batch.getGoodsId(), goodsId)
                || line.quantity() == null || line.quantity().signum() <= 0
                || line.costAmount() == null || line.costAmount().signum() < 0
                || batch.getRemainQuantity().add(line.quantity()).compareTo(batch.getTotalQuantity()) > 0
                || batch.getRemainAmount().add(line.costAmount()).compareTo(batch.getTotalAmount()) > 0) {
                throw new BusinessException("原路恢复批次的商品/仓库/数量/成本不一致或超过原批次");
            }
            batch.setRemainQuantity(batch.getRemainQuantity().add(line.quantity()));
            batch.setRemainAmount(batch.getRemainAmount().add(line.costAmount()));
            stockBatchMapper.updateById(batch);
            totalQty = totalQty.add(line.quantity());
            totalAmt = totalAmt.add(line.costAmount());
        }
        writeFlow(tenantId, warehouseId, goodsId, StockFlowTypeEnum.IN, beforeQty, totalQty, beforeAmt, totalAmt, sourceType, sourceId, sourceNo);
        return totalAmt;
    }

    private List<ConsumeLine> outbound(Long tenantId,
                                       Long warehouseId,
                                       Long goodsId,
                                       BigDecimal needQty,
                                       BizSourceTypeEnum sourceType,
                                       Long sourceId,
                                       String sourceNo,
                                       StockFlowTypeEnum flowType,
                                       SourceFilter sourceFilter) {
        if (needQty == null || needQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("出库数量必须大于 0");
        }
        StockBalance before = currentBalance(tenantId, warehouseId, goodsId);
        BigDecimal beforeQty = before.quantity();
        BigDecimal beforeAmt = before.amount();
        List<StockBatchDO> batches;
        if (sourceFilter == null) {
            batches = stockBatchMapper.selectAvailableForUpdate(tenantId, warehouseId, goodsId);
        } else {
            batches = stockBatchMapper.selectAvailableBySourceForUpdate(tenantId, warehouseId, goodsId, sourceFilter
                .sourceType()
                .getValue(), sourceFilter.sourceId());
        }
        if (batches == null) {
            batches = List.of();
        } else {
            sortFifo(batches);
        }
        BigDecimal available = batches.stream().map(StockBatchDO::getRemainQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (available.compareTo(needQty) < 0) {
            throw new BusinessException("库存不足，商品ID=" + goodsId + "，可用=" + available + "，需求=" + needQty);
        }

        BigDecimal remainNeed = needQty;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ConsumeLine> lines = new ArrayList<>();
        for (StockBatchDO batch : batches) {
            if (remainNeed.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal take = batch.getRemainQuantity().min(remainNeed);
            BigDecimal cost;
            if (take.compareTo(batch.getRemainQuantity()) == 0) {
                cost = batch.getRemainAmount();
            } else {
                cost = take.multiply(batch.getUntaxPrice()).setScale(AMOUNT_SCALE, ROUND);
            }
            batch.setRemainQuantity(batch.getRemainQuantity().subtract(take));
            batch.setRemainAmount(batch.getRemainAmount().subtract(cost));
            if (batch.getRemainQuantity().compareTo(BigDecimal.ZERO) == 0) {
                batch.setRemainAmount(BigDecimal.ZERO);
            }
            stockBatchMapper.updateById(batch);
            lines.add(new ConsumeLine(batch.getId(), take, batch.getUntaxPrice(), cost, batch.getInDate(), batch
                .getOriginInDate()));
            totalCost = totalCost.add(cost);
            remainNeed = remainNeed.subtract(take);
        }
        if (remainNeed.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("库存不足（并发占用），商品ID=" + goodsId);
        }
        writeFlow(tenantId, warehouseId, goodsId, flowType, beforeQty, needQty.negate(), beforeAmt, totalCost
            .negate(), sourceType, sourceId, sourceNo);
        return lines;
    }

    private StockBatchDO createBatch(Long tenantId,
                                     Long warehouseId,
                                     Long goodsId,
                                     BigDecimal quantity,
                                     BigDecimal untaxPrice,
                                     BigDecimal amount,
                                     LocalDateTime inDate,
                                     LocalDateTime originInDate,
                                     BizSourceTypeEnum sourceType,
                                     Long sourceId,
                                     String sourceNo,
                                     StockFlowTypeEnum flowType) {
        if (quantity == null || quantity.signum() <= 0 || untaxPrice == null || untaxPrice.signum() < 0
            || amount == null || amount.signum() < 0) {
            throw new BusinessException("入库数量必须为正数，单价与成本不能为负数");
        }
        StockBalance before = currentBalance(tenantId, warehouseId, goodsId);
        BigDecimal beforeQty = before.quantity();
        BigDecimal beforeAmt = before.amount();

        StockBatchDO batch = new StockBatchDO();
        batch.setTenantId(tenantId);
        batch.setWarehouseId(warehouseId);
        batch.setGoodsId(goodsId);
        batch.setBatchNo(docNoGenerator.next(tenantId, "PC"));
        batch.setSourceType(sourceType);
        batch.setSourceId(sourceId);
        batch.setSourceNo(sourceNo);
        batch.setInDate(inDate);
        batch.setOriginInDate(originInDate);
        batch.setTotalQuantity(quantity);
        batch.setRemainQuantity(quantity);
        batch.setUntaxPrice(untaxPrice);
        batch.setTotalAmount(amount);
        batch.setRemainAmount(amount);
        stockBatchMapper.insert(batch);

        writeFlow(tenantId, warehouseId, goodsId, flowType, beforeQty, quantity, beforeAmt, amount, sourceType, sourceId, sourceNo);
        return batch;
    }

    private void writeFlow(Long tenantId,
                           Long warehouseId,
                           Long goodsId,
                           StockFlowTypeEnum flowType,
                           BigDecimal beforeQty,
                           BigDecimal changeQty,
                           BigDecimal beforeAmt,
                           BigDecimal changeAmt,
                           BizSourceTypeEnum sourceType,
                           Long sourceId,
                           String sourceNo) {
        StockFlowDO flow = new StockFlowDO();
        flow.setTenantId(tenantId);
        flow.setDeptId(TenantFillHelper.currentDeptId());
        flow.setWarehouseId(warehouseId);
        flow.setGoodsId(goodsId);
        flow.setFlowType(flowType);
        flow.setBeforeQty(beforeQty);
        flow.setChangeQty(changeQty);
        flow.setAfterQty(beforeQty.add(changeQty));
        flow.setBeforeAmount(beforeAmt);
        flow.setChangeAmount(changeAmt);
        flow.setAfterAmount(beforeAmt.add(changeAmt));
        flow.setSourceType(sourceType);
        flow.setSourceId(sourceId);
        flow.setSourceNo(sourceNo);
        flow.setConfirmUser(UserContextHolder.getUserId());
        flow.setConfirmTime(LocalDateTime.now());
        stockFlowMapper.insert(flow);
    }

    /** Pre-lock the complete document footprint before the first mutation (including both transfer warehouses). */
    public void lockAll(java.util.Collection<StockMutationLock.Key> keys) {
        stockMutationLock.lockAll(keys);
    }

    /** A locking read, deliberately avoiding plain SUM under an older REPEATABLE READ snapshot. */
    public StockBalance currentBalance(Long tenantId, Long warehouseId, Long goodsId) {
        stockMutationLock.lock(tenantId, warehouseId, goodsId);
        List<StockBatchDO> batches = stockBatchMapper.selectAllForUpdate(tenantId, warehouseId, goodsId);
        BigDecimal quantity = BigDecimal.ZERO, amount = BigDecimal.ZERO;
        for (StockBatchDO batch : batches) {
            quantity = quantity.add(nz(batch.getRemainQuantity()));
            amount = amount.add(nz(batch.getRemainAmount()));
        }
        return new StockBalance(quantity, amount);
    }

    public record StockBalance(BigDecimal quantity, BigDecimal amount) {}

    /** 显式盘盈纠正回到原批次。约束原租户/仓库/商品/原价和缺口，保留调整关联与流水。 */
    public BigDecimal restoreAdjustmentBatch(Long tenantId, Long warehouseId, Long goodsId, Long batchId,
                                            BigDecimal quantity, BigDecimal price, Long adjustId, String adjustNo) {
        stockMutationLock.lock(tenantId, warehouseId, goodsId);
        StockBatchDO batch = stockBatchMapper.selectByIdForUpdate(tenantId, batchId);
        if (batch == null || !java.util.Objects.equals(batch.getTenantId(), tenantId)
            || !java.util.Objects.equals(batch.getWarehouseId(), warehouseId)
            || !java.util.Objects.equals(batch.getGoodsId(), goodsId) || price == null
            || price.compareTo(batch.getUntaxPrice()) != 0 || quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("原批次盘盈纠正须同租户/仓库/商品，且使用原批次单价和正数量");
        }
        BigDecimal missing = batch.getTotalQuantity().subtract(batch.getRemainQuantity());
        BigDecimal missingCost = batch.getTotalAmount().subtract(batch.getRemainAmount());
        if (quantity.compareTo(missing) > 0 || missing.signum() <= 0 || missingCost.signum() < 0) {
            throw new BusinessException("纠正数量或成本超过原批次缺口");
        }
        BigDecimal cost = quantity.compareTo(missing) == 0 ? missingCost
            : missingCost.multiply(quantity).divide(missing, 2, RoundingMode.HALF_UP);
        return restoreConsumes(tenantId, warehouseId, goodsId, List.of(new ConsumeLine(batchId, quantity, price, cost)),
            BizSourceTypeEnum.STOCK_ADJUST, adjustId, adjustNo);
    }

    private static void sortFifo(List<StockBatchDO> batches) {
        batches.sort(Comparator.comparing(StockBatchDO::getInDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(StockBatchDO::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private record SourceFilter(BizSourceTypeEnum sourceType, Long sourceId) {
    }

    /**
     * FIFO 消耗行（含原批次日期，供调拨继承）
     */
    public record ConsumeLine(Long batchId,
                              BigDecimal quantity,
                              BigDecimal untaxPrice,
                              BigDecimal costAmount,
                              LocalDateTime inDate,
                              LocalDateTime originInDate) {

        public ConsumeLine(Long batchId, BigDecimal quantity, BigDecimal untaxPrice, BigDecimal costAmount) {
            this(batchId, quantity, untaxPrice, costAmount, null, null);
        }
    }
}
