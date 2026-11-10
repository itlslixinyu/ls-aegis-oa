package com.ls.aegis.biz.quote.support;

import com.ls.aegis.biz.quote.constant.QuoteConstants;
import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.domain.QuoteDetail;
import com.ls.aegis.compat.common.core.text.Convert;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 报价金额汇总：材料/服务小计、税前、税额、含税总价、大写。
 */
public final class QuoteAmountCalculator {

    private QuoteAmountCalculator() {
    }

    public static void applyTotals(Quote quote, List<QuoteDetail> details) {
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal service = BigDecimal.ZERO;
        if (details != null) {
            for (QuoteDetail line : details) {
                if (line == null || line.getAmount() == null) {
                    continue;
                }
                if (QuoteConstants.LINE_TYPE_MATERIAL.equals(line.getLineType())) {
                    material = material.add(line.getAmount());
                } else {
                    service = service.add(line.getAmount());
                }
            }
        }
        material = material.setScale(2, RoundingMode.HALF_UP);
        service = service.setScale(2, RoundingMode.HALF_UP);
        quote.setMaterialSubtotal(material);
        quote.setServiceSubtotal(service);

        BigDecimal preTax = material.add(service).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = quote.getTaxRate() == null ? new BigDecimal("4.00") : quote.getTaxRate();
        quote.setTaxRate(rate);
        BigDecimal taxAmount = preTax.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = preTax.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        if (material.precision() > 12 || service.precision() > 12 || total.precision() > 12) {
            throw new BusinessException("报价合计超出允许范围");
        }
        quote.setTotalAmount(total);
    }

    public static BigDecimal preTaxTotal(Quote quote) {
        BigDecimal material = nullToZero(quote.getMaterialSubtotal());
        BigDecimal service = nullToZero(quote.getServiceSubtotal());
        return material.add(service).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal taxAmount(Quote quote) {
        BigDecimal preTax = preTaxTotal(quote);
        BigDecimal rate = quote.getTaxRate() == null ? new BigDecimal("4.00") : quote.getTaxRate();
        return preTax.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static String amountUpper(Quote quote) {
        BigDecimal total = quote.getTotalAmount() == null ? BigDecimal.ZERO : quote.getTotalAmount();
        return Convert.digitUppercase(total.doubleValue());
    }

    private static BigDecimal nullToZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
