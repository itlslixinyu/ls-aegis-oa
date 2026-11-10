package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationTest {
    private InvoiceReq valid(){var r=new InvoiceReq();r.setTaxInclusiveAmount(new BigDecimal("113"));r.setUntaxAmount(new BigDecimal("100"));r.setTaxAmount(new BigDecimal("13"));return r;}
    private InvoiceReq.Link link(long id,String amount){var l=new InvoiceReq.Link();l.setStockDocId(id);l.setAmount(new BigDecimal(amount));return l;}
    @Test void taxNumbersMayBeAbsentWithoutSqlNullOrInventedIdentity(){var r=valid();InvoiceValidation.validate(r);assertEquals("",r.getBuyerTaxNo());assertEquals("",r.getSellerTaxNo());}
    @Test void inconsistentTaxCannotReachPersistence(){var r=valid();r.setTaxInclusiveAmount(new BigDecimal("100"));assertThrows(BusinessException.class,()->InvoiceValidation.validate(r));}
    @Test void negativeZeroOrExcessPrecisionBlueAmountsFail(){for(String amount:List.of("0","-1","1.001","10000000000000000")){var r=valid();r.setTaxInclusiveAmount(new BigDecimal(amount));assertThrows(BusinessException.class,()->InvoiceValidation.validate(r));}}
    @Test void sameSourceCannotBeSplitIntoRepeatedLinksToBypassItsCap(){var r=valid();r.setLinks(List.of(link(1,"60"),link(1,"53")));assertThrows(BusinessException.class,()->InvoiceValidation.validate(r));}
    @Test void negativeAllocationCannotHideAnotherSourcesOverAllocation(){var r=valid();r.setLinks(List.of(link(1,"120"),link(2,"-7")));assertThrows(BusinessException.class,()->InvoiceValidation.validate(r));}
    @Test void lineTotalsMustMatchHeader(){var r=valid();var line=new InvoiceReq.Item();line.setTaxInclusiveAmount(BigDecimal.TEN);line.setUntaxAmount(BigDecimal.TEN);line.setTaxAmount(BigDecimal.ZERO);r.setItems(List.of(line));assertThrows(BusinessException.class,()->InvoiceValidation.validate(r));}
    @Test void normalRoundingToleranceRemainsAccepted(){var r=valid();r.setTaxInclusiveAmount(new BigDecimal("113.01"));assertDoesNotThrow(()->InvoiceValidation.validate(r));}
}
