package com.ls.aegis.biz.psi.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FIFO 成本扣减口径单测（纯计算，不连库）
 */
class FifoCostCalcTest {

    @Test
    void shouldConsumeAcrossBatchesWithRemainAmountOnLastTake() {
        List<Batch> batches = new ArrayList<>();
        batches.add(new Batch(new BigDecimal("10"), new BigDecimal("10.00"), new BigDecimal("100.00")));
        batches.add(new Batch(new BigDecimal("5"), new BigDecimal("12.00"), new BigDecimal("60.00")));

        BigDecimal need = new BigDecimal("12");
        BigDecimal remainNeed = need;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Batch batch : batches) {
            if (remainNeed.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal take = batch.remainQty.min(remainNeed);
            BigDecimal cost;
            if (take.compareTo(batch.remainQty) == 0) {
                cost = batch.remainAmt;
            } else {
                cost = take.multiply(batch.untaxPrice).setScale(2, RoundingMode.HALF_UP);
            }
            batch.remainQty = batch.remainQty.subtract(take);
            batch.remainAmt = batch.remainAmt.subtract(cost);
            totalCost = totalCost.add(cost);
            remainNeed = remainNeed.subtract(take);
        }
        assertEquals(0, remainNeed.compareTo(BigDecimal.ZERO));
        // 10*10 + 2*12 = 100 + 24 = 124
        assertEquals(new BigDecimal("124.00"), totalCost);
        assertEquals(new BigDecimal("3"), batches.get(1).remainQty);
        assertEquals(new BigDecimal("36.00"), batches.get(1).remainAmt);
    }

    private static final class Batch {
        private BigDecimal remainQty;
        private final BigDecimal untaxPrice;
        private BigDecimal remainAmt;

        private Batch(BigDecimal remainQty, BigDecimal untaxPrice, BigDecimal remainAmt) {
            this.remainQty = remainQty;
            this.untaxPrice = untaxPrice;
            this.remainAmt = remainAmt;
        }
    }
}
