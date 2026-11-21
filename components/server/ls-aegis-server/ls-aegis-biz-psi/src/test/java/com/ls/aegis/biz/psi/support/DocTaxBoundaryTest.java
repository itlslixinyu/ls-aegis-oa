package com.ls.aegis.biz.psi.support;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.service.impl.*;
import com.ls.aegis.biz.psi.enums.*;
import top.continew.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DocTaxBoundaryTest {
 static StockLineReq line(String price){var r=new StockLineReq();r.setGoodsId(1L);r.setQuantity(BigDecimal.ONE);r.setTaxInclusivePrice(new BigDecimal(price));r.setTaxRate(BigDecimal.ZERO);return r;}
 @ParameterizedTest @ValueSource(strings={"-0.01","-0.001","-0.05","0.005","0.001"})
 void negativeOrFractionalCentHeaderNeverBecomesAnOpenItem(String amount){var h=DocTaxHelper.recalc(List.of(line("0")));assertThrows(BusinessException.class,()->DocTaxHelper.applyManualDiff(h,new BigDecimal(amount)));}
 @ParameterizedTest @ValueSource(strings={"0.95","1.00","1.05","1.000"})
 void allowedTailDiffsConserveAmounts(String amount){var h=DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(List.of(line("1"))),new BigDecimal(amount));assertEquals(0,h.taxInclusiveAmount().compareTo(h.untaxAmount().add(h.taxAmount()).add(h.taxDiffAmount())));}
 @ParameterizedTest @ValueSource(strings={"0.94","1.06"})
 void tailDiffOutsideLimitStillRejected(String amount){assertThrows(BusinessException.class,()->DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(List.of(line("1"))),new BigDecimal(amount)));}
 @Test void zeroPriceAndZeroHeaderRemainValid(){var h=DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(List.of(line("0"))),BigDecimal.ZERO);assertEquals(0,h.taxInclusiveAmount().signum());assertEquals(0,h.taxDiffAmount().signum());}
 @Test void omittedManualAmountReturnsCalculatedHeader(){var h=DocTaxHelper.recalc(List.of(line("1")));assertSame(h,DocTaxHelper.applyManualDiff(h,null));}
 @Test void nullEmptyAndNullElementHaveBusinessErrors(){assertThrows(BusinessException.class,()->DocTaxHelper.recalc(null));assertThrows(BusinessException.class,()->DocTaxHelper.recalc(List.of()));assertThrows(BusinessException.class,()->DocTaxHelper.recalc(Arrays.asList(line("1"),null)));}
 @Test void repeatedGoodsWithDifferentPricesRemainSeparateLines(){var h=DocTaxHelper.recalc(List.of(line("1"),line("2")));assertEquals(2,h.lines().size());assertEquals(new BigDecimal("3.00"),h.taxInclusiveAmount());assertEquals(1,h.lines().get(0).sort());assertEquals(2,h.lines().get(1).sort());}
 @ParameterizedTest @ValueSource(strings={"-0.01","0.005"})
 void realStockInAndOutCreateRejectBeforeAnyHeaderOrItemWrite(String amount){
  var im=mock(StockInMapper.class);var ii=mock(StockInItemMapper.class);var in=new StockInServiceImpl(im,ii,null,null,null,null,null,null,null);var ir=new StockInReq();ir.setInType(StockInTypeEnum.OTHER);ir.setItems(List.of(line("0")));ir.setTaxInclusiveAmount(new BigDecimal(amount));assertThrows(BusinessException.class,()->in.create(ir));verifyNoInteractions(im,ii);
  var om=mock(StockOutMapper.class);var oi=mock(StockOutItemMapper.class);var out=new StockOutServiceImpl(om,oi,null,null,null,null,null,null,null);var or=new StockOutReq();or.setOutType(StockOutTypeEnum.OTHER);or.setItems(List.of(line("0")));or.setTaxInclusiveAmount(new BigDecimal(amount));assertThrows(BusinessException.class,()->out.create(or));verifyNoInteractions(om,oi);
 }
}
