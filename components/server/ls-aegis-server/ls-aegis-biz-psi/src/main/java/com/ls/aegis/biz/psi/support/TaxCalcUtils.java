package com.ls.aegis.biz.psi.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价税计算（含税单价口径，以后端为准）
 */
public final class TaxCalcUtils {

    private static final int AMOUNT_SCALE = 2;
    private static final int PRICE_SCALE = 6;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private TaxCalcUtils() {
    }

    /**
     * 按含税单价、数量、税率计算行价税。
     *
     * @param quantity            数量
     * @param taxInclusivePrice   含税单价
     * @param taxRatePercent      税率百分比，如 13 表示 13%
     * @return 行价税结果
     */
    public static LineTaxResult calcByInclusivePrice(BigDecimal quantity,
                                                    BigDecimal taxInclusivePrice,
                                                    BigDecimal taxRatePercent) {
        if (quantity == null || taxInclusivePrice == null || taxRatePercent == null) {
            throw new IllegalArgumentException("数量、含税单价、税率不能为空");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        if (taxInclusivePrice.signum() < 0) {
            throw new IllegalArgumentException("含税单价不能为负数");
        }
        if (taxRatePercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("税率不能为负数");
        }
        BigDecimal taxInclusiveAmount = quantity.multiply(taxInclusivePrice).setScale(AMOUNT_SCALE, ROUND);
        BigDecimal divisor = BigDecimal.ONE.add(taxRatePercent.divide(BigDecimal.valueOf(100), 8, ROUND));
        BigDecimal untaxAmount = taxInclusiveAmount.divide(divisor, AMOUNT_SCALE, ROUND);
        BigDecimal taxAmount = taxInclusiveAmount.subtract(untaxAmount);
        BigDecimal untaxPrice = untaxAmount.divide(quantity, PRICE_SCALE, ROUND);
        return new LineTaxResult(untaxPrice, untaxAmount, taxAmount, taxInclusiveAmount);
    }

    /**
     * 行价税结果
     *
     * @param untaxPrice           未税单价
     * @param untaxAmount          未税金额
     * @param taxAmount            税额
     * @param taxInclusiveAmount   含税金额
     */
    public record LineTaxResult(BigDecimal untaxPrice,
                                BigDecimal untaxAmount,
                                BigDecimal taxAmount,
                                BigDecimal taxInclusiveAmount) {
    }
}
