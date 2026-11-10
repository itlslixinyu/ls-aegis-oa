package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.model.req.StockLineReq;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 单据明细价税汇总
 */
public final class DocTaxHelper {

    private static final BigDecimal DIFF_LIMIT = new BigDecimal("0.05");
    private static final int AMOUNT_SCALE = 2;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private DocTaxHelper() {
    }

    /**
     * 重算明细并汇总表头
     */
    public static HeaderResult recalc(List<StockLineReq> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("单据明细不能为空");
        }
        BigDecimal sumInclusive = BigDecimal.ZERO;
        BigDecimal sumUntax = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        List<LineResult> lines = new ArrayList<>();
        int sort = 1;
        for (StockLineReq item : items) {
            if (item == null || item.getGoodsId() == null) {
                throw new BusinessException("明细商品不能为空");
            }
            TaxCalcUtils.LineTaxResult tax = TaxCalcUtils.calcByInclusivePrice(item.getQuantity(), item
                .getTaxInclusivePrice(), item.getTaxRate());
            lines.add(new LineResult(item.getGoodsId(), item.getQuantity(), item.getTaxInclusivePrice(), item
                .getTaxRate(), tax.untaxPrice(), tax.untaxAmount(), tax.taxAmount(), tax.taxInclusiveAmount(), sort++));
            sumInclusive = sumInclusive.add(tax.taxInclusiveAmount());
            sumUntax = sumUntax.add(tax.untaxAmount());
            sumTax = sumTax.add(tax.taxAmount());
        }
        return new HeaderResult(sumInclusive.setScale(AMOUNT_SCALE, ROUND), sumUntax.setScale(AMOUNT_SCALE, ROUND),
            sumTax.setScale(AMOUNT_SCALE, ROUND), BigDecimal.ZERO, lines);
    }

    /**
     * 校验手工表头尾差（可选）
     */
    public static HeaderResult applyManualDiff(HeaderResult calc, BigDecimal manualInclusive) {
        if (manualInclusive == null) {
            return calc;
        }
        // A tail difference must not turn a zero-price document into a negative AR/AP.
        if (manualInclusive.signum() < 0) {
            throw new BusinessException("表头含税金额不能为负数");
        }
        try {
            manualInclusive = manualInclusive.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException fractionalCent) {
            // Rounding header and difference separately can violate header = untax + tax + diff.
            throw new BusinessException("表头含税金额最多保留两位小数");
        }
        BigDecimal diff = manualInclusive.subtract(calc.taxInclusiveAmount()).abs();
        if (diff.compareTo(DIFF_LIMIT) > 0) {
            throw new BusinessException("表头含税金额与明细汇总差额超过 0.05，请检查");
        }
        BigDecimal taxDiff = manualInclusive.subtract(calc.taxInclusiveAmount()).setScale(AMOUNT_SCALE, ROUND);
        return new HeaderResult(manualInclusive.setScale(AMOUNT_SCALE, ROUND), calc.untaxAmount(), calc.taxAmount(),
            taxDiff, calc.lines());
    }

    public record LineResult(Long goodsId,
                             BigDecimal quantity,
                             BigDecimal taxInclusivePrice,
                             BigDecimal taxRate,
                             BigDecimal untaxPrice,
                             BigDecimal untaxAmount,
                             BigDecimal taxAmount,
                             BigDecimal taxInclusiveAmount,
                             Integer sort) {
    }

    public record HeaderResult(BigDecimal taxInclusiveAmount,
                               BigDecimal untaxAmount,
                               BigDecimal taxAmount,
                               BigDecimal taxDiffAmount,
                               List<LineResult> lines) {
    }
}
