package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.service.impl.finance.PaymentServiceImpl;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairSettlementServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompensationSqlTest {
    MybatisConfiguration config() throws Exception {
        var c=new MybatisConfiguration();
        c.getTypeAliasRegistry().registerAliases("com.ls.aegis.biz.eoms.domain");
        for(String file:List.of("EomsDataScopeMapper","IncomeMapper","PaymentMapper","CostMapper","ReceivableMapper","RepairSettlementMapper","RepairOrderMapper","RepairApplyMapper","RepairMaterialMapper","FinanceEventMapper"))
            try(InputStream in=Files.newInputStream(Path.of("src/main/resources/mapper/eoms/"+file+".xml"))) {
                new XMLMapperBuilder(in,c,file,c.getSqlFragments()).parse();
            }
        return c;
    }
    BoundSql sql(MybatisConfiguration c,String folder,String mapper,String method,Object p) {
        return c.getMappedStatement("com.ls.aegis.biz.eoms.mapper."+folder+"."+mapper+"."+method).getBoundSql(p);
    }
    String compact(BoundSql s) {return s.getSql().replaceAll("\\s+"," ").trim();}
    @Test void defaultReceiptListExcludesReversalsWhileAuditFilterIsExplicit() throws Exception {
        var c=config();var q=new Payment();
        assertTrue(compact(sql(c,"finance","PaymentMapper","selectPaymentList",q)).contains("p.status='ACTIVE'"));
        q.setStatus("REVERSED");
        BoundSql audit=sql(c,"finance","PaymentMapper","selectPaymentList",q);
        assertTrue(audit.getParameterMappings().stream().anyMatch(p->p.getProperty().equals("status")));
        q.setStatus("ALL");assertFalse(compact(sql(c,"finance","PaymentMapper","selectPaymentList",q)).contains("p.status="));
    }
    @Test void receivableRollbackStatusIsComputedBeforeMysqlLeftToRightAssignments() throws Exception {
        String query=compact(sql(config(),"finance","ReceivableMapper","rollbackReceivableOnPaymentDelete",Map.of("id",1L,"amount",BigDecimal.TEN,"updateBy","test")));
        assertTrue(query.indexOf("set status")<query.indexOf("received_amount = received_amount -"));
        assertTrue(query.contains("received_amount = ? then 'UNPAID'"));
        assertTrue(query.contains("status in ('UNPAID','PARTIAL','PAID')"));
    }
    @Test void voidReceivableCannotReceiveMoneyAndRetainsHistoricalFaceAmount() throws Exception {
        var c=config();
        String collect=compact(sql(c,"finance","ReceivableMapper","updateReceivableOnPayment",Map.of("id",1L,"amount",BigDecimal.TEN,"updateBy","test","newStatus","PAID")));
        assertTrue(collect.contains("status in ('UNPAID','PARTIAL')"));
        String cancel=compact(sql(c,"finance","ReceivableMapper","voidReceivable",Map.of("id",1L,"operator","test")));
        assertTrue(cancel.contains("status='VOID', arrears_amount=0"));
        assertTrue(cancel.contains("received_amount=0"));
        assertFalse(cancel.contains("set amount="));assertFalse(cancel.contains("del_flag='2'"));
    }
    @Test void aggregateLocksAreRealForUpdateAndSettlementRedoExcludesReversed() throws Exception {
        var c=config();
        for(String[] method:new String[][]{{"finance","PaymentMapper","lockPayment"},{"finance","ReceivableMapper","lockReceivable"},{"finance","CostMapper","lockByOrder"},{"repair","RepairSettlementMapper","lockSettlement"},{"repair","RepairOrderMapper","lockOrder"},{"repair","RepairApplyMapper","lockApply"},{"repair","RepairMaterialMapper","lockByOrder"}})
            assertTrue(compact(sql(c,method[0],method[1],method[2],Map.of("id",1L))).endsWith("for update"));
        assertTrue(compact(sql(c,"repair","RepairSettlementMapper","selectSettlementByOrderId",Map.of("repairOrderId",1L))).contains("s.status != 'REVERSED'"));
    }
    @Test void incomeAndCostPersistExactSourceAndReversalLinkages() throws Exception {
        var c=config();Income income=new Income();income.setIncomeNo("TEST");income.setPaymentId(1L);income.setReversalOfId(2L);
        BoundSql insert=sql(c,"finance","IncomeMapper","insertIncome",income);
        assertTrue(insert.getSql().contains("payment_id"));assertTrue(insert.getSql().contains("reversal_of_id"));
        assertTrue(insert.getParameterMappings().stream().anyMatch(p->p.getProperty().equals("paymentId")));
        Cost cost=new Cost();cost.setSettlementId(3L);cost.setReversalOfId(4L);
        insert=sql(c,"finance","CostMapper","insertCost",cost);
        assertTrue(insert.getSql().contains("settlement_id"));assertTrue(insert.getSql().contains("reversal_of_id"));
        assertTrue(insert.getParameterMappings().stream().anyMatch(p->p.getProperty().equals("reversalOfId")));
    }
    @Test void confirmedHeaderCrudIsGuardedAtSqlLayerAndReversalIsCasWithoutSoftDelete() throws Exception {
        var c=config();RepairSettlement s=new RepairSettlement();s.setId(1L);s.setRemark("TEST");
        assertTrue(compact(sql(c,"repair","RepairSettlementMapper","updateRepairSettlement",s)).contains("status='DRAFT'"));
        assertTrue(compact(sql(c,"repair","RepairSettlementMapper","deleteRepairSettlementByIds",Map.of("array",new Long[]{1L,2L}))).contains("status='DRAFT'"));
        String reverse=compact(sql(c,"repair","RepairSettlementMapper","reverseSettlement",Map.of("id",1L,"operator","test","remark","test")));
        assertTrue(reverse.contains("status='CONFIRMED'"));assertTrue(reverse.contains("set status='REVERSED'"));assertFalse(reverse.contains("del_flag='2'"));
    }
    @Test void compensationEntriesDeclareRollbackForEveryException() throws Exception {
        for(java.lang.reflect.Method method:List.of(PaymentServiceImpl.class.getMethod("deletePaymentByIds",Long[].class),
                PaymentServiceImpl.class.getMethod("insertPayment",Payment.class),RepairSettlementServiceImpl.class.getMethod("reverseSettlement",Long.class,String.class))) {
            Transactional annotation=method.getAnnotation(Transactional.class);
            assertNotNull(annotation);assertTrue(List.of(annotation.rollbackFor()).contains(Exception.class));
        }
    }
}
