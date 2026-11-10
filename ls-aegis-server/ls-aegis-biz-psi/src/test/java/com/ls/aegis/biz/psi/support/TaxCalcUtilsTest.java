package com.ls.aegis.biz.psi.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 价税计算单测
 */
class TaxCalcUtilsTest {
    @Test void negativePriceMustNotCreateNegativeReturnOrInventoryAmounts() {
        assertThrows(IllegalArgumentException.class, () -> TaxCalcUtils.calcByInclusivePrice(BigDecimal.ONE, new BigDecimal("-100"), BigDecimal.ZERO));
    }
    @Test void zeroPriceIsExplicitlyAllowedWithoutInventingNegativeStockCost() {
        var result = TaxCalcUtils.calcByInclusivePrice(BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("13"));
        assertEquals(new BigDecimal("0.00"), result.taxInclusiveAmount());
        assertEquals(new BigDecimal("0.00"), result.taxAmount());
    }

    @Test
    void calcByInclusivePrice_shouldSplitTaxCorrectly() {
        TaxCalcUtils.LineTaxResult r = TaxCalcUtils.calcByInclusivePrice(
            new BigDecimal("2"),
            new BigDecimal("113"),
            new BigDecimal("13"));
        assertEquals(new BigDecimal("200.00"), r.untaxAmount());
        assertEquals(new BigDecimal("26.00"), r.taxAmount());
        assertEquals(new BigDecimal("226.00"), r.taxInclusiveAmount());
        assertEquals(new BigDecimal("100.000000"), r.untaxPrice());
    }
}
