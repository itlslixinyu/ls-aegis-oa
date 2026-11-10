package com.ls.aegis.biz.psi.support;

import cn.idev.excel.EasyExcel;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.model.req.StockLineImportRowReq;
import com.ls.aegis.biz.psi.model.resp.ImportGoodsMatch;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import top.continew.starter.core.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StockLineImportSupportTest {
    org.springframework.context.support.GenericApplicationContext context;
    jakarta.validation.ValidatorFactory validators;
    @org.junit.jupiter.api.BeforeEach void initializeValidation() {
        validators=jakarta.validation.Validation.buildDefaultValidatorFactory();
        context=new org.springframework.context.support.GenericApplicationContext();
        context.registerBean(jakarta.validation.Validator.class,validators::getValidator);
        context.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(context);
    }
    @org.junit.jupiter.api.AfterEach void cleanupValidation() {
        context.close();validators.close();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(null);
    }
    GoodsMapper mapper=mock(GoodsMapper.class);
    MockMultipartFile excel(List<StockLineImportRowReq> rows) {
        var out=new ByteArrayOutputStream();
        EasyExcel.write(out,StockLineImportRowReq.class).sheet("导入数据").doWrite(rows);
        return new MockMultipartFile("file","synthetic.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
    }
    StockLineImportRowReq row(String code,String qty,String price,String tax) {
        var r=new StockLineImportRowReq();r.setGoodsCode(code);r.setQuantity(qty);r.setTaxInclusivePrice(price);r.setTaxRate(tax);return r;
    }
    void matches() {
        when(mapper.selectImportGoods(eq(7L),anyList())).thenAnswer(call->{
            List<String> codes=call.getArgument(1);
            assertTrue(codes.size()<=200);
            return codes.stream().filter(c->!c.equals("MISSING")).map(c->{var g=new ImportGoodsMatch();g.setImportCode(c);g.setId(10L);g.setGoodsCode(c.toUpperCase());g.setGoodsName("商品");g.setDefaultTaxRate(new BigDecimal("9"));return g;}).toList();
        });
    }
    @Test void fourHundredOneRowsUseThreeQueriesAndKeepOrderDecimalsAndDatabaseMatch() {
        matches();var rows=IntStream.range(0,401).mapToObj(i->row(" code"+i+" ","1,234.5678",i==0?"2.50":null,i==0?"13":null)).toList();
        var result=StockLineImportSupport.parse(excel(rows),mapper,7L);
        assertEquals(401,result.getValidRows());assertEquals(401,result.getTotalRows());
        assertEquals("CODE0",result.getItems().get(0).getGoodsCode());assertEquals("CODE400",result.getItems().get(400).getGoodsCode());
        assertEquals(new BigDecimal("1234.5678"),result.getItems().get(0).getQuantity());assertEquals(new BigDecimal("2.50"),result.getItems().get(0).getTaxInclusivePrice());assertEquals(new BigDecimal("13"),result.getItems().get(0).getTaxRate());assertEquals(new BigDecimal("9"),result.getItems().get(1).getTaxRate());assertEquals(BigDecimal.ZERO,result.getItems().get(1).getTaxInclusivePrice());
        verify(mapper,times(3)).selectImportGoods(eq(7L),anyList());verifyNoMoreInteractions(mapper);
    }
    @Test void duplicateMissingInvalidAndBlankRowsRemainRejected() {
        matches();
        for(List<StockLineImportRowReq> rows:List.of(List.of(row("A","1",null,null),row(" A ","2",null,null)),List.of(row("MISSING","1",null,null)),List.of(row("A","0",null,null)),List.of(row("A","-1",null,null)),List.of(row("A","invalid",null,null)),List.of(row("A","1","-1",null)),List.of(row("A","1",null,"-1")),List.of(row("A",null,null,null)),List.of(row(null,"1",null,null)))) {
            assertThrows(BusinessException.class,()->StockLineImportSupport.parse(excel(rows),mapper,7L));
        }
        assertThrows(BusinessException.class,()->StockLineImportSupport.parse(excel(List.of()),mapper,7L));
    }
    @Test void missingDefaultTaxUsesThirteenAndEmptyOrMalformedFilesFail() {
        var g=new ImportGoodsMatch();g.setImportCode("A");g.setId(1L);g.setGoodsCode("A");
        when(mapper.selectImportGoods(7L,List.of("A"))).thenReturn(List.of(g));
        assertEquals(new BigDecimal("13"),StockLineImportSupport.parse(excel(List.of(row("A","1",null,null))),mapper,7L).getItems().get(0).getTaxRate());
        assertThrows(BusinessException.class,()->StockLineImportSupport.parse(null,mapper,7L));
        assertThrows(BusinessException.class,()->StockLineImportSupport.parse(new MockMultipartFile("file",new byte[]{1,2,3}),mapper,7L));
    }
}
