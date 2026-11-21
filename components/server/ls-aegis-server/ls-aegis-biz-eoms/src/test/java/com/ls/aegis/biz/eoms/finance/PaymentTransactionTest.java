package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.mapper.finance.*;
import com.ls.aegis.biz.eoms.service.impl.finance.PaymentServiceImpl;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real MyBatis SQL + Spring transactions over a private in-memory database; no live connection. */
class PaymentTransactionTest {
    JdbcTemplate db;
    PaymentServiceImpl service;
    FinanceEventMapper events;
    PaymentMapper payments;
    ReceivableMapper receivables;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        db=new JdbcTemplate(ds);
        String audit="create_by varchar(64),create_time timestamp,update_by varchar(64),update_time timestamp,remark varchar(500),del_flag char(1) default '0'";
        String scope=",tenant_id bigint default 0,dept_id bigint default 0,create_user bigint default 0";
        db.execute("create table psi_unit(id bigint primary key,unit_code varchar(32),unit_name varchar(100),deleted int default 0)");
        db.execute("create table eoms_repair_order(id bigint primary key,order_no varchar(32))");
        db.execute("create table eoms_receivable(id bigint primary key,customer_id bigint,repair_order_id bigint,receivable_no varchar(32),settlement_id bigint,amount decimal(12,2),received_amount decimal(12,2),arrears_amount decimal(12,2),status varchar(20),"+audit+scope+")");
        db.execute("create table eoms_payment(id bigint auto_increment primary key,customer_id bigint,receivable_id bigint,payment_no varchar(32),batch_id varchar(64),amount decimal(12,2),payment_method varchar(20),payment_time timestamp,status varchar(20) default 'ACTIVE',reversed_time timestamp,reversed_by varchar(64),"+audit+scope+")");
        db.execute("create table eoms_income(id bigint auto_increment primary key,income_no varchar(32) unique,repair_order_id bigint,customer_id bigint,amount decimal(12,2),income_type varchar(20),income_date date,payment_id bigint unique,reversal_of_id bigint unique,"+audit+")");
        db.execute("create table eoms_finance_event(id bigint auto_increment primary key,aggregate_type varchar(20),aggregate_id bigint,action varchar(20),related_id bigint,amount decimal(12,2),operator varchar(64),reason varchar(500),create_time timestamp,unique(aggregate_type,aggregate_id,action))");
        var c=new MybatisConfiguration();c.getTypeAliasRegistry().registerAliases("com.ls.aegis.biz.eoms.domain");
        c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));
        for(String name:List.of("EomsDataScopeMapper","PaymentMapper","IncomeMapper","ReceivableMapper","FinanceEventMapper"))
            try(InputStream in=Files.newInputStream(Path.of("src/main/resources/mapper/eoms/"+name+".xml"))){new XMLMapperBuilder(in,c,name,c.getSqlFragments()).parse();}
        var session=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c));
        payments=session.getMapper(PaymentMapper.class);events=session.getMapper(FinanceEventMapper.class);
        var target=spy(new PaymentServiceImpl());
        doAnswer(inv->payments.selectPaymentById(inv.getArgument(0))).when(target).selectPaymentById(anyLong());
        ReflectionTestUtils.setField(target,"paymentMapper",payments);
        receivables=session.getMapper(ReceivableMapper.class);ReflectionTestUtils.setField(target,"receivableMapper",receivables);
        ReflectionTestUtils.setField(target,"incomeMapper",session.getMapper(IncomeMapper.class));
        ReflectionTestUtils.setField(target,"financeEventMapper",events);
        var numbers=mock(EomsNoGenerator.class);var counter=new AtomicInteger();when(numbers.generateIncomeNo()).thenAnswer(inv->"TEST-REV-"+counter.incrementAndGet());
        ReflectionTestUtils.setField(target,"eomsNoGenerator",numbers);
        var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource()));
        service=(PaymentServiceImpl)factory.getProxy();
        db.update("insert into eoms_receivable(id,customer_id,repair_order_id,amount,received_amount,arrears_amount,status) values(20,40,50,100,37.50,62.50,'PARTIAL')");
        db.update("insert into eoms_payment(id,customer_id,receivable_id,amount,status) values(1,40,20,37.50,'ACTIVE')");
        db.update("insert into eoms_income(id,income_no,repair_order_id,customer_id,amount,income_type,payment_id) values(30,'TEST-ORIGINAL',50,40,37.50,'REPAIR',1)");
    }
    BigDecimal amount(String column){return db.queryForObject("select "+column+" from eoms_receivable where id=20",BigDecimal.class);}
    int rows(String table){return db.queryForObject("select count(*) from "+table,Integer.class);}
    @Test void committedReversalBalancesLedgerAndRetainsReceiptAndOriginalIncome() {
        assertEquals(1,service.deletePaymentById(1L));assertEquals(0,service.deletePaymentById(1L));
        assertEquals("REVERSED",db.queryForObject("select status from eoms_payment where id=1",String.class));
        assertEquals("UNPAID",db.queryForObject("select status from eoms_receivable where id=20",String.class));
        assertEquals(0,amount("received_amount").compareTo(BigDecimal.ZERO));assertEquals(0,amount("arrears_amount").compareTo(new BigDecimal("100")));
        assertEquals(2,rows("eoms_income"));assertEquals(0,db.queryForObject("select sum(amount) from eoms_income",BigDecimal.class).compareTo(BigDecimal.ZERO));
        assertEquals(1,rows("eoms_finance_event"));assertEquals(1,rows("eoms_payment"));
    }
    @Test void failureAtLastAuditWriteRollsBackIncomeReceivableAndReceiptTogether() {
        // A real unique-key violation at the final write, after income+receipt updates.
        db.update("insert into eoms_finance_event(aggregate_type,aggregate_id,action) values('PAYMENT',1,'REVERSE')");
        assertThrows(DataIntegrityViolationException.class,()->service.deletePaymentById(1L));
        assertEquals("ACTIVE",db.queryForObject("select status from eoms_payment where id=1",String.class));
        assertEquals(1,rows("eoms_income"));assertEquals(0,amount("received_amount").compareTo(new BigDecimal("37.50")));
        assertEquals(0,amount("arrears_amount").compareTo(new BigDecimal("62.50")));
    }
    @Test void mixedLegacyBatchRollsBackEarlierValidCompensation() {
        db.update("insert into eoms_payment(id,customer_id,receivable_id,amount,status) values(2,40,20,10,'ACTIVE')");
        assertThrows(RuntimeException.class,()->service.deletePaymentByIds(new Long[]{1L,2L}));
        assertEquals(1,rows("eoms_income"));assertEquals(0,rows("eoms_finance_event"));
        assertEquals(2,db.queryForObject("select count(*) from eoms_payment where status='ACTIVE'",Integer.class));
        assertEquals(0,amount("received_amount").compareTo(new BigDecimal("37.50")));
    }
    @Test void concurrentDuplicateReversalProducesOneCompensationAndNoDoubleReceivableRollback() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Integer> action=()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));return service.deletePaymentById(1L);};
            Future<Integer> a=pool.submit(action),b=pool.submit(action);assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            assertEquals(1,a.get(15,TimeUnit.SECONDS)+b.get(15,TimeUnit.SECONDS));
            assertEquals(2,rows("eoms_income"));assertEquals(1,rows("eoms_finance_event"));assertEquals(0,amount("received_amount").compareTo(BigDecimal.ZERO));
        } finally {pool.shutdownNow();}
    }
    @Test void legacyZeroUnpaidIsNotCollectableAndZeroPaidCanBeVoidedWithoutAllowingPositivePaid() {
        db.update("update eoms_receivable set amount=0,received_amount=0,arrears_amount=0,status='UNPAID' where id=20");
        var q=new com.ls.aegis.biz.eoms.domain.Receivable();q.setCustomerId(40L);assertTrue(receivables.selectUnsettledByCustomerId(q).isEmpty());
        db.update("update eoms_receivable set status='PAID' where id=20");assertEquals(1,receivables.voidReceivable(20L,"TEST"));
        db.update("update eoms_receivable set status='PAID',amount=100 where id=20");assertEquals(0,receivables.voidReceivable(20L,"TEST"));
    }

    @Test void zeroPaymentRemainsRejectedWithoutCreatingIncomeOrPayment(){
        int oldPayments=rows("eoms_payment"),oldIncome=rows("eoms_income");var p=new com.ls.aegis.biz.eoms.domain.Payment();p.setReceivableId(20L);p.setAmount(BigDecimal.ZERO);
        assertThrows(RuntimeException.class,()->service.insertPayment(p));assertEquals(oldPayments,rows("eoms_payment"));assertEquals(oldIncome,rows("eoms_income"));
    }

}
