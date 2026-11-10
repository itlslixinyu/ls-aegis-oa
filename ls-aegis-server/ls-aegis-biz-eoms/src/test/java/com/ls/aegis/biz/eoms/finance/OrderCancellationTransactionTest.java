package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.mapper.finance.*;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairOrderServiceImpl;
import com.ls.aegis.biz.eoms.service.impl.finance.IncomeServiceImpl;
import com.ls.aegis.common.api.psi.RepairStockOutApi;
import com.ls.aegis.common.context.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Production XML and annotated services, two real independent Spring/H2 transactions. */
public class OrderCancellationTransactionTest {
    JdbcTemplate db;TransactionTemplate tx;RepairOrderMapper mapper;RepairOrderServiceImpl orders;IncomeServiceImpl incomes;
    final RepairStockOutApi stock=mock(RepairStockOutApi.class);
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");db=new JdbcTemplate(ds);
        db.execute("create table sys_user(id bigint primary key,tenant_id bigint,status int,deleted int)");db.execute("create table sys_role(id bigint primary key,code varchar(40),deleted int)");db.execute("create table sys_user_role(user_id bigint,role_id bigint)");
        db.update("insert into sys_user values(5,0,1,0),(6,0,2,0),(7,0,1,0),(8,0,1,1),(9,7,1,0)");db.update("insert into sys_role values(1,'eoms_engineer',0),(2,'finance',0)");db.update("insert into sys_user_role values(5,1),(6,1),(7,2),(8,1),(9,1)");
        db.execute("create table eoms_repair_order(id bigint primary key,tenant_id bigint,status varchar(30),order_no varchar(64),remark varchar(500),del_flag char default '0',update_by varchar(64),update_time timestamp,engineer_id bigint,dept_id bigint,create_user bigint,start_time timestamp,accept_time timestamp,finish_time timestamp,sign_in_longitude varchar(64),sign_in_latitude varchar(64),sign_in_address varchar(500),sign_out_longitude varchar(64),sign_out_latitude varchar(64),sign_out_address varchar(500))");
        db.execute("create table eoms_repair_settlement(id bigint primary key,repair_order_id bigint,tenant_id bigint,status varchar(30),del_flag char default '0')");
        db.execute("create table eoms_receivable(id bigint primary key,repair_order_id bigint,tenant_id bigint,status varchar(30),received_amount decimal(12,2),del_flag char default '0')");
        db.execute("create table eoms_payment(id bigint primary key,receivable_id bigint,status varchar(30),del_flag char default '0')");
        String audit="create_by varchar(64),create_time timestamp,update_by varchar(64),update_time timestamp,remark varchar(500),del_flag char default '0'";
        db.execute("create table eoms_income(id bigint auto_increment primary key,income_no varchar(64),repair_order_id bigint,customer_id bigint,amount decimal(12,2),income_type varchar(30),income_date timestamp,payment_id bigint,reversal_of_id bigint,"+audit+")");
        db.execute("create table eoms_cost(id bigint primary key,repair_order_id bigint,amount decimal(12,2),del_flag char default '0')");
        db.execute("create table eoms_finance_event(id bigint auto_increment primary key,aggregate_type varchar(30),aggregate_id bigint,action varchar(30),related_id bigint,amount decimal(12,2),operator varchar(64),reason varchar(500),create_time timestamp,unique(aggregate_type,aggregate_id,action))");
        db.update("insert into eoms_repair_order(id,tenant_id,status,order_no,remark) values(10,0,'WAIT_SETTLEMENT','TEST-ORDER','ITLS-TEST-R30 original')");
        db.update("insert into eoms_repair_settlement(id,repair_order_id,tenant_id,status) values(1,10,0,'REVERSED')");
        db.update("insert into eoms_receivable(id,repair_order_id,tenant_id,status,received_amount) values(20,10,0,'VOID',0)");
        var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.getTypeAliasRegistry().registerAliases("com.ls.aegis.biz.eoms.domain");c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));
        for(String name:List.of("EomsDataScopeMapper","RepairOrderMapper","RepairSettlementMapper","FinanceEventMapper","IncomeMapper")){ // Only adapt MySQL's clock expression to H2; all guards, joins and row locking are production XML.
            String xml=Files.readString(Path.of("src/main/resources/mapper/eoms/"+name+".xml")).replace("sysdate()","CURRENT_TIMESTAMP");
            try(InputStream in=new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))){new XMLMapperBuilder(in,c,name,c.getSqlFragments()).parse();}
        }
        var sql=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c));mapper=sql.getMapper(RepairOrderMapper.class);
        var target=spy(new RepairOrderServiceImpl());doAnswer(inv->visible()).when(target).selectRepairOrderById(10L);
        ReflectionTestUtils.setField(target,"repairOrderMapper",mapper);ReflectionTestUtils.setField(target,"cancelSettlementMapper",sql.getMapper(RepairSettlementMapper.class));ReflectionTestUtils.setField(target,"cancelStockApi",stock);ReflectionTestUtils.setField(target,"cancelEventMapper",sql.getMapper(FinanceEventMapper.class));
        var manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);orders=proxy(target,manager);
        var income=new IncomeServiceImpl();ReflectionTestUtils.setField(income,"incomeMapper",sql.getMapper(IncomeMapper.class));ReflectionTestUtils.setField(income,"repairOrderMapper",mapper);ReflectionTestUtils.setField(income,"repairOrderService",orders);incomes=proxy(income,manager);identity();
    }
    <T>T proxy(T target,DataSourceTransactionManager manager){var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (T)factory.getProxy();}
    RepairOrder visible(){return db.queryForObject("select * from eoms_repair_order where id=10",new BeanPropertyRowMapper<>(RepairOrder.class));}
    void identity(){var u=new UserContext();u.setId(99L);u.setTenantId(0L);UserContextHolder.setContext(u,false);}
    @AfterEach void clear(){UserContextHolder.clearContext();db.execute("SHUTDOWN");}
    int count(String table){return db.queryForObject("select count(*) from "+table,Integer.class);}
    @Test void cancellationPreservesHistoryAndOriginalRemarkWithExactlyOneAudit(){assertEquals(1,orders.cancelAfterReversal(10L,"synthetic closure"));assertEquals(0,orders.cancelAfterReversal(10L,"retry"));assertEquals("CLOSED",visible().getStatus());assertTrue(visible().getRemark().startsWith("ITLS-TEST-R30 original\n"));assertTrue(visible().getRemark().contains("synthetic closure"));assertEquals(1,count("eoms_finance_event"));assertEquals(1,count("eoms_repair_settlement"));assertEquals(1,count("eoms_receivable"));}
    @ParameterizedTest @ValueSource(strings={"nohistory","draft","confirmed","processing","tenant","receivable","payment","income","cost","psi"}) void unresolvedBusinessCannotBeClosed(String branch){
        switch(branch){case "nohistory"->db.update("delete from eoms_repair_settlement");case "draft","confirmed"->db.update("update eoms_repair_settlement set status=?",branch.toUpperCase());case "processing"->db.update("update eoms_repair_order set status='PROCESSING'");case "tenant"->db.update("update eoms_repair_settlement set tenant_id=7");case "receivable"->db.update("update eoms_receivable set status='UNPAID'");case "payment"->db.update("insert into eoms_payment(id,receivable_id,status) values(1,20,'ACTIVE')");case "income"->db.update("insert into eoms_income(repair_order_id,amount) values(10,1)");case "cost"->db.update("insert into eoms_cost(id,repair_order_id,amount) values(1,10,1)");case "psi"->doThrow(new IllegalStateException("active stock out")).when(stock).assertNoActiveRepairOut(10L,"TEST-ORDER");}
        assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L,"must reject"));assertNotEquals("CLOSED",visible().getStatus());assertEquals(0,count("eoms_finance_event"));
    }
    @Test void failingAuditRollsBackTheOrderCas(){db.update("insert into eoms_finance_event(aggregate_type,aggregate_id,action) values('ORDER',10,'CANCEL_CLOSE')");assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L,"audit must persist"));assertEquals("WAIT_SETTLEMENT",visible().getStatus());}
    @Test void unrelatedTenantAndOrderLedgerNeverAffectThisExactOrder(){db.update("insert into eoms_repair_order(id,tenant_id,status) values(11,7,'WAIT_SETTLEMENT')");db.update("insert into eoms_income(repair_order_id,amount) values(11,100)");db.update("insert into eoms_cost(id,repair_order_id,amount) values(1,11,100)");assertEquals(1,orders.cancelAfterReversal(10L,"isolated"));}
    @Test void simultaneousCancellationIsIdempotentWithoutDeadlock()throws Exception{var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);try{List<Future<Integer>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{identity();try{start.await();return orders.cancelAfterReversal(10L,"concurrent");}finally{UserContextHolder.clearContext();}}));start.countDown();assertEquals(1,jobs.get(0).get(10,TimeUnit.SECONDS)+jobs.get(1).get(10,TimeUnit.SECONDS));assertEquals(1,count("eoms_finance_event"));}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}}
    @Test void draftCommittedWhileCancelWaitsForOrderForcesRetryInsteadOfClosing()throws Exception{raceAgainstOrderWriter(()->db.update("insert into eoms_repair_settlement(id,repair_order_id,tenant_id,status) values(2,10,0,'DRAFT')"));assertEquals(2,count("eoms_repair_settlement"));}
    @Test void manualIncomeCommittedWhileCancelWaitsCannotBeMissed()throws Exception{raceAgainstOrderWriter(()->incomes.insertIncome(income()));assertEquals(1,count("eoms_income"));}
    void raceAgainstOrderWriter(Runnable write)throws Exception{
        var held=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var writer=pool.submit(()->{identity();try{tx.execute(s->{mapper.lockOrder(10L);held.countDown();try{assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}write.run();return null;});}finally{UserContextHolder.clearContext();}});assertTrue(held.await(5,TimeUnit.SECONDS));
            var cancel=pool.submit(()->{identity();try{return assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L,"race"));}finally{UserContextHolder.clearContext();}});
            // Writer holds order; cancellation may lock history but cannot pass its order gate.
            release.countDown();writer.get(10,TimeUnit.SECONDS);cancel.get(10,TimeUnit.SECONDS);assertEquals("WAIT_SETTLEMENT",visible().getStatus());assertEquals(0,count("eoms_finance_event"));
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    Income income(){var i=new Income();i.setRepairOrderId(10L);i.setCustomerId(1L);i.setIncomeNo("TEST-INCOME");i.setAmount(BigDecimal.ONE);return i;}
    @Test void manualIncomeAfterCancellationIsRejectedWithoutLedgerWrite(){orders.cancelAfterReversal(10L,"closed");assertThrows(RuntimeException.class,()->incomes.insertIncome(income()));assertEquals(0,count("eoms_income"));}
    @Test void concurrentIncomeWaitsForCancellingOrderThenRejectsItsClosedState()throws Exception{
        var held=new CountDownLatch(1);var release=new CountDownLatch(1);var attempted=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        doAnswer(inv->{held.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return null;}).when(stock).assertNoActiveRepairOut(10L,"TEST-ORDER");
        try{var cancel=pool.submit(()->{identity();try{return orders.cancelAfterReversal(10L,"wins order gate");}finally{UserContextHolder.clearContext();}});assertTrue(held.await(5,TimeUnit.SECONDS));
            var income=pool.submit(()->{identity();try{attempted.countDown();return assertThrows(RuntimeException.class,()->incomes.insertIncome(income()));}finally{UserContextHolder.clearContext();}});
            assertTrue(attempted.await(5,TimeUnit.SECONDS));release.countDown();assertEquals(1,cancel.get(10,TimeUnit.SECONDS));income.get(10,TimeUnit.SECONDS);assertEquals("CLOSED",visible().getStatus());assertEquals(0,count("eoms_income"));
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void balancedOriginalAndReversalIncomeAndCostsRemainAsAuditAfterClosing(){db.update("insert into eoms_income(repair_order_id,amount) values(10,100),(10,-100)");db.update("insert into eoms_cost(id,repair_order_id,amount) values(1,10,50),(2,10,-50)");assertEquals(1,orders.cancelAfterReversal(10L,"all compensated"));assertEquals(2,count("eoms_income"));assertEquals(2,count("eoms_cost"));}
    @Test void missingReasonIsRejectedAndFullLegacyRemarkSurvivesWithAuditedReason(){assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L," "));db.update("update eoms_repair_order set remark=?","x".repeat(500));assertEquals(1,orders.cancelAfterReversal(10L,"full cancellation reason"));assertEquals("x".repeat(500),visible().getRemark());assertEquals("full cancellation reason",db.queryForObject("select reason from eoms_finance_event",String.class));assertEquals(0,orders.cancelAfterReversal(10L,"retry"));assertEquals(1,count("eoms_finance_event"));}
    @Test void manualIncomeAndCostUpdatesCannotMigratePreviouslyUnlinkedRowsIntoAnOrder(){
        var i=income();i.setId(123L);assertThrows(RuntimeException.class,()->incomes.updateIncome(i));
        var c=new Cost();c.setId(123L);c.setRepairOrderId(10L);assertThrows(RuntimeException.class,()->new com.ls.aegis.biz.eoms.service.impl.finance.CostServiceImpl().updateCost(c));assertEquals(0,count("eoms_income"));
    }

    @Test void invisibleOrderCannotReachGlobalGuardsOrWrites()throws Exception{
        var target=(RepairOrderServiceImpl)((org.springframework.aop.framework.Advised)orders).getTargetSource().getTarget();doReturn(null).when(target).selectRepairOrderById(10L);
        var rejected=assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L,"unauthorized"));assertTrue(rejected.getMessage().contains("无权"));assertEquals(0,count("eoms_finance_event"));verifyNoInteractions(stock);
    }
    @Test void actorTenantMustMatchAuthorizedOrderBeforeAnyGlobalGuard(){var u=new UserContext();u.setId(99L);u.setTenantId(7L);UserContextHolder.setContext(u,false);var rejected=assertThrows(RuntimeException.class,()->orders.cancelAfterReversal(10L,"wrong tenant"));assertTrue(rejected.getMessage().contains("租户"));verifyNoInteractions(stock);assertEquals(0,count("eoms_finance_event"));}

    @Test void allPcStateControllerActionsRejectAnInvisibleOrderWithoutWriting()throws Exception{
        var target=(RepairOrderServiceImpl)((org.springframework.aop.framework.Advised)orders).getTargetSource().getTarget();doReturn(null).when(target).selectRepairOrderById(10L);
        var controller=new com.ls.aegis.biz.eoms.controller.repair.RepairOrderController();ReflectionTestUtils.setField(controller,"repairOrderService",orders);
        assertThrows(RuntimeException.class,()->orders.assignEngineer(10L,5L,"other department"));
        assertThrows(RuntimeException.class,()->controller.accept(10L,"other department"));assertThrows(RuntimeException.class,()->controller.start(10L,"other department"));
        assertThrows(RuntimeException.class,()->controller.submit(10L,"other department"));assertThrows(RuntimeException.class,()->controller.close(10L,"other department"));
        assertEquals("WAIT_SETTLEMENT",visible().getStatus());assertEquals(0,count("eoms_finance_event"));verifyNoInteractions(stock);
    }
    @Test void simultaneousStartsUseCurrentLockedStateAndOnlyOneSucceeds()throws Exception{
        db.update("update eoms_repair_order set status='ACCEPTED'");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{List<Future<Boolean>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{identity();try{start.await();try{orders.startProcess(10L,"concurrent start");return true;}catch(com.ls.aegis.compat.common.exception.ServiceException rejected){return false;}}finally{UserContextHolder.clearContext();}}));start.countDown();int success=0;for(var job:jobs)if(job.get(10,TimeUnit.SECONDS))success++;assertEquals(1,success);assertEquals("PROCESSING",visible().getStatus());}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }

    @ParameterizedTest @ValueSource(longs={6,7,8,9,999}) void assignmentRejectsDisabledNonEngineerDeletedForeignTenantAndMissingUsers(long target){db.update("update eoms_repair_order set status='WAIT_ACCEPT'");assertThrows(RuntimeException.class,()->orders.assignEngineer(10L,target,"invalid engineer"));assertEquals("WAIT_ACCEPT",visible().getStatus());assertNull(visible().getEngineerId());}
    @Test void assignmentToAnEnabledEngineerUsesExactUserKeyAndSucceeds(){db.update("update eoms_repair_order set status='WAIT_ACCEPT'");assertEquals(1,orders.assignEngineer(10L,5L,"valid engineer"));assertEquals("ACCEPTED",visible().getStatus());assertEquals(5L,visible().getEngineerId());}

}
