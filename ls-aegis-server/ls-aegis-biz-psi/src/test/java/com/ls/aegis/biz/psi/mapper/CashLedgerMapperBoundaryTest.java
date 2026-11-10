package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real SQL plus actual MyBatis-Plus tenant/data-permission plugins. All tables are isolated H2 fixtures. */
public class CashLedgerMapperBoundaryTest {
    private JdbcTemplate jdbc;
    private CashLedgerMapper mapper;
    private org.springframework.transaction.support.TransactionTemplate tx;
    private VisibleBankProbe probe;
    private final AtomicLong currentTenant = new AtomicLong(999), currentDept = new AtomicLong(999);

    public interface VisibleBankProbe {
        @Select("select count(*) from psi_bank_statement b where b.tenant_id=1 and b.match_status='MATCHED' and b.deleted=0")
        long visibleMatchedRows();
    }

    @BeforeEach void setup() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(ds);
        tx = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        for (String table : List.of("psi_receivable", "psi_payable")) {
            jdbc.execute("create table " + table + "(id bigint primary key,tenant_id bigint,dept_id bigint,source_type varchar(30),source_id bigint,tax_inclusive_amount decimal(18,2),unwritten_amount decimal(18,2),written_off_amount decimal(18,2),doc_status varchar(20),update_user bigint,update_time timestamp,deleted bigint default 0)");
            String type = table.equals("psi_receivable") ? "STOCK_OUT" : "STOCK_IN";
            jdbc.update("insert into " + table + "(id,tenant_id,dept_id,source_type,source_id,tax_inclusive_amount,unwritten_amount,written_off_amount,doc_status,deleted) values(301,1,10,?,700,100,70,30,'OPEN',0),(302,1,20,?,701,100,70,30,'OPEN',0),(303,2,20,?,700,100,70,30,'OPEN',0),(304,1,20,'OTHER',700,100,70,30,'OPEN',0),(305,1,20,?,700,100,70,30,'OPEN',1)",type,type,type,type);
        }
        for (String table : List.of("psi_payment", "psi_receipt")) {
            jdbc.execute("create table " + table + "(id bigint primary key,tenant_id bigint,dept_id bigint,amount decimal(18,2),refund_source_id bigint,refund_return_id bigint,source_sequence bigint,void_sequence bigint,doc_status varchar(20),deleted bigint default 0)");
        }
        jdbc.execute("create table psi_stock_return(id bigint primary key,tenant_id bigint,dept_id bigint,origin_stock_out_id bigint,origin_stock_in_id bigint,deleted bigint default 0)");
        jdbc.execute("create table psi_bank_statement(id bigint primary key,tenant_id bigint,dept_id bigint,match_biz_type varchar(20),match_biz_id bigint,match_status varchar(20),deleted bigint default 0)");
        jdbc.execute("create table psi_todo(id bigint primary key,tenant_id bigint,dept_id bigint,biz_id bigint,todo_type varchar(30),done boolean,update_time timestamp,update_user bigint,deleted bigint default 0)");
        jdbc.update("insert into psi_stock_return values(100,1,10,700,800,0),(101,1,20,700,800,0),(102,1,20,701,801,0),(200,2,20,700,800,0),(103,1,20,700,800,1)");
        for (String table : List.of("psi_payment", "psi_receipt")) {
            jdbc.update("insert into " + table + " values(1,1,10,10,50,100,3,null,'CONFIRMED',0),(2,1,20,20,50,100,4,null,'CONFIRMED',0),(3,2,20,900,50,100,5,null,'CONFIRMED',0),(4,1,20,30,50,100,6,7,'VOID',0),(5,1,20,80,50,100,8,null,'CONFIRMED',1),(6,1,20,7,50,101,9,null,'CONFIRMED',0),(7,1,20,11,51,100,10,null,'CONFIRMED',0),(8,1,20,13,50,102,11,null,'CONFIRMED',0),(9,2,20,90,50,200,12,null,'CONFIRMED',0),(10,1,20,15,50,103,13,null,'CONFIRMED',0)");
        }
        jdbc.update("insert into psi_bank_statement values(1,1,10,'RECEIPT',50,'MATCHED',0),(2,1,20,'RECEIPT',50,'MATCHED',0),(3,2,20,'RECEIPT',50,'MATCHED',0),(4,1,20,'PAYMENT',50,'MATCHED',0),(5,1,20,'RECEIPT',51,'MATCHED',0),(6,1,20,'RECEIPT',50,'UNMATCHED',0),(7,1,20,'RECEIPT',50,'MATCHED',1)");
        jdbc.update("insert into psi_todo(id,tenant_id,dept_id,biz_id,todo_type,done,deleted) values(1,1,10,100,'REFUND',false,0),(2,1,20,100,'REFUND',false,0),(3,2,20,100,'REFUND',false,0),(4,1,20,101,'REFUND',false,0),(5,1,20,100,'RECEIVE_REFUND',false,0),(6,1,20,100,'REFUND',false,1)");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        MybatisPlusInterceptor plugins = new MybatisPlusInterceptor();
        plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new LongValue(currentTenant.get()); }
        }));
        plugins.addInnerInterceptor(new DataPermissionInterceptor(new MultiDataPermissionHandler() {
            @Override public Expression getSqlSegment(Table table, Expression where, String statementId) {
                String qualifier = table.getAlias() == null ? table.getName() : table.getAlias().getName();
                return new EqualsTo(new Column(qualifier + ".dept_id"), new LongValue(currentDept.get()));
            }
        }));
        config.addInterceptor(plugins);
        config.addMapper(CashLedgerMapper.class);
        config.addMapper(VisibleBankProbe.class);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(ds); factory.setConfiguration(config);
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        mapper = session.getMapper(CashLedgerMapper.class); probe = session.getMapper(VisibleBankProbe.class);
    }
    @AfterEach void cleanup() { jdbc.execute("SHUTDOWN"); }

    @Test void pluginsAreActuallyActiveButAuthorizedInternalQueryCountsAllDepartments() {
        assertEquals(0, probe.visibleMatchedRows(), "wrong current tenant must hide normal mapper rows");
        currentTenant.set(1); assertEquals(0, probe.visibleMatchedRows(), "wrong current department must hide normal mapper rows");
        currentDept.set(10); assertEquals(1, probe.visibleMatchedRows());
        currentTenant.set(999); currentDept.set(999);
        assertEquals(2, mapper.bankMatches(1L, "RECEIPT", 50L));
        assertEquals(1, mapper.bankMatches(2L, "RECEIPT", 50L));
        assertEquals(1, mapper.bankMatches(1L, "PAYMENT", 50L));
        assertEquals(1, mapper.bankMatches(1L, "RECEIPT", 51L));
        assertEquals(0, mapper.bankMatches(1L, "RECEIPT", 999L));
        InterceptorIgnore ignore = CashLedgerMapper.class.getAnnotation(InterceptorIgnore.class);
        assertNotNull(ignore); assertEquals("true", ignore.tenantLine()); assertEquals("true", ignore.dataPermission());
    }

    @Test void refundSumsKeepTenantReturnStateAndDeletionBoundariesInBothDirections() {
        // 10+20 from two departments, plus 11 from another source belonging to the same return: all reduce its cap.
        for (BigDecimal amount : List.of(mapper.sumRefundPayments(1L,100L),mapper.sumRefundReceipts(1L,100L))) money("41", amount);
        money("900", mapper.sumRefundPayments(2L,100L)); money("900", mapper.sumRefundReceipts(2L,100L));
        money("7", mapper.sumRefundPayments(1L,101L)); money("7", mapper.sumRefundReceipts(1L,101L));
        money("0", mapper.sumRefundPayments(999L,100L)); money("0", mapper.sumRefundReceipts(1L,999L));
    }

    @Test void historiesUseSourceOriginAndTenantJoinAndMapBothCausalSequences() {
        for (List<CashLedgerMapper.RefundHistory> history : List.of(mapper.refundPayments(1L,50L,700L),mapper.refundReceipts(1L,50L,800L))) {
            assertEquals(Set.of(1L,2L,4L,6L), history.stream().map(CashLedgerMapper.RefundHistory::getId).collect(java.util.stream.Collectors.toSet()));
            CashLedgerMapper.RefundHistory reversed = history.stream().filter(r -> r.getId().equals(4L)).findFirst().orElseThrow();
            assertEquals(6L,reversed.getSourceSequence()); assertEquals(7L,reversed.getVoidSequence());
            assertEquals(100L,reversed.getRefundReturnId()); assertEquals("VOID",reversed.getDocStatus()); money("30",reversed.getAmount());
        }
        assertEquals(List.of(7L),mapper.refundPayments(1L,51L,700L).stream().map(CashLedgerMapper.RefundHistory::getId).toList());
        assertEquals(List.of(9L),mapper.refundReceipts(2L,50L,800L).stream().map(CashLedgerMapper.RefundHistory::getId).toList());
        assertTrue(mapper.refundPayments(1L,50L,999L).isEmpty());
    }

    @Test void invariantSourceReadsRequireExactTenantIdAndLiveRow() {
        assertNotNull(mapper.receiptForInvariant(1L,1L)); assertNotNull(mapper.paymentForInvariant(2L,3L));
        assertNull(mapper.receiptForInvariant(2L,1L)); assertNull(mapper.paymentForInvariant(1L,3L));
        assertNull(mapper.receiptForInvariant(1L,5L)); assertNull(mapper.paymentForInvariant(1L,999L));
    }

    @Test void todoUpdateAndReopenReachBothDepartmentsOnlyForExactTenantReturnAndType() {
        assertEquals(2,mapper.setRefundTodo(1L,100L,"REFUND",true,88L));
        assertEquals(List.of(1L,2L),jdbc.queryForList("select id from psi_todo where done=true order by id",Long.class));
        assertEquals(2,jdbc.queryForObject("select count(*) from psi_todo where update_user=88 and update_time is not null",Integer.class));
        assertEquals(2,mapper.setRefundTodo(1L,100L,"REFUND",false,89L));
        assertEquals(0,jdbc.queryForObject("select count(*) from psi_todo where done=true",Integer.class));
        assertEquals(0,mapper.setRefundTodo(999L,100L,"REFUND",true,99L));
        assertEquals(0,mapper.setRefundTodo(1L,999L,"REFUND",true,99L));
        assertNull(jdbc.queryForObject("select update_user from psi_todo where id=3",Long.class));
        assertNull(jdbc.queryForObject("select update_user from psi_todo where id=4",Long.class));
        assertNull(jdbc.queryForObject("select update_user from psi_todo where id=5",Long.class));
        assertNull(jdbc.queryForObject("select update_user from psi_todo where id=6",Long.class));
    }
    @Test void returnOpenLocksFindExactTenantOriginTypeAndLiveRowAcrossDepartmentScope() {
        tx.executeWithoutResult(status -> {
            assertEquals(301L,mapper.lockReturnReceivable(1L,700L).getId());
            assertEquals(302L,mapper.lockReturnReceivable(1L,701L).getId());
            assertEquals(303L,mapper.lockReturnReceivable(2L,700L).getId());
            assertEquals(301L,mapper.lockReturnPayable(1L,700L).getId());
            assertEquals(302L,mapper.lockReturnPayable(1L,701L).getId());
            assertEquals(303L,mapper.lockReturnPayable(2L,700L).getId());
            assertNull(mapper.lockReturnReceivable(999L,700L));
            assertNull(mapper.lockReturnPayable(1L,999L));
        });
    }

    @Test void returnOpenUpdatesAffectOnlyAuthorizedTenantAndExactLiveId() {
        for (boolean receivable : new boolean[]{true,false}) {
            String table = receivable ? "psi_receivable" : "psi_payable";
            tx.executeWithoutResult(status -> {
                assertEquals(1,applyOpen(receivable,1L,302L,"90","60",88L));
                assertEquals(0,applyOpen(receivable,2L,302L,"0","0",99L));
                assertEquals(0,applyOpen(receivable,1L,305L,"0","0",99L));
                assertEquals(0,applyOpen(receivable,1L,999L,"0","0",99L));
            });
            money("90",jdbc.queryForObject("select tax_inclusive_amount from " + table + " where id=302",BigDecimal.class));
            money("60",jdbc.queryForObject("select unwritten_amount from " + table + " where id=302",BigDecimal.class));
            money("30",jdbc.queryForObject("select written_off_amount from " + table + " where id=302",BigDecimal.class));
            assertEquals(88L,jdbc.queryForObject("select update_user from " + table + " where id=302",Long.class));
            assertNotNull(jdbc.queryForObject("select update_time from " + table + " where id=302",java.sql.Timestamp.class));
            assertEquals(4,jdbc.queryForObject("select count(*) from " + table + " where tax_inclusive_amount=100 and update_user is null",Integer.class));
        }
    }

    @Test void returnOpenLockSerializesRealTransactionsUntilCommitForBothDirections() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for (boolean receivable : new boolean[]{true,false}) {
                java.util.concurrent.CountDownLatch held = new java.util.concurrent.CountDownLatch(1), release = new java.util.concurrent.CountDownLatch(1), competing = new java.util.concurrent.CountDownLatch(1);
                try {
                    java.util.concurrent.Future<?> first = pool.submit(() -> tx.executeWithoutResult(status -> {
                        money("100",lockAmount(receivable)); held.countDown();
                        try { if (!release.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("test lock timed out"); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                        assertEquals(1,applyOpen(receivable,1L,301L,"80","50",88L));
                    }));
                    assertTrue(held.await(5,java.util.concurrent.TimeUnit.SECONDS));
                    java.util.concurrent.Future<BigDecimal> second = pool.submit(() -> tx.execute(status -> { competing.countDown(); return lockAmount(receivable); }));
                    assertTrue(competing.await(5,java.util.concurrent.TimeUnit.SECONDS));
                    assertThrows(java.util.concurrent.TimeoutException.class,() -> second.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
                    release.countDown(); first.get(5,java.util.concurrent.TimeUnit.SECONDS);
                    money("80",second.get(5,java.util.concurrent.TimeUnit.SECONDS));
                } finally { release.countDown(); }
            }
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS)); }
    }
    private BigDecimal lockAmount(boolean receivable) {
        return receivable ? mapper.lockReturnReceivable(1L,700L).getTaxInclusiveAmount() : mapper.lockReturnPayable(1L,700L).getTaxInclusiveAmount();
    }
    private int applyOpen(boolean receivable,Long tenant,Long id,String amount,String unwritten,Long actor) {
        return receivable ? mapper.applyReturnReceivable(tenant,id,new BigDecimal(amount),new BigDecimal(unwritten),"OPEN",actor)
            : mapper.applyReturnPayable(tenant,id,new BigDecimal(amount),new BigDecimal(unwritten),"OPEN",actor);
    }

    private static void money(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
}
