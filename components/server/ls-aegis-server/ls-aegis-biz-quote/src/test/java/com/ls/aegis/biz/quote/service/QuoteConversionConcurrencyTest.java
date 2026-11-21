package com.ls.aegis.biz.quote.service;

import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.mapper.QuoteMapper;
import com.ls.aegis.biz.quote.mapper.QuoteDetailMapper;
import com.ls.aegis.biz.quote.port.QuoteToRepairBridge;
import com.ls.aegis.biz.quote.service.impl.QuoteServiceImpl;
import com.ls.aegis.starter.support.docno.DocNoService;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.core.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real mapper XML and two independent database transactions; no in-use datasource. */
public class QuoteConversionConcurrencyTest {
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private QuoteServiceImpl service;
    private AtomicInteger calls;
    private CountDownLatch entered;
    private CountDownLatch release;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        QuoteTestActor.open();
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table eoms_quote(id bigint primary key, quote_no varchar(50), customer_id bigint, repair_order_id bigint, status varchar(20), del_flag char(1), update_by varchar(50), update_time timestamp, tenant_id bigint default 1, create_user bigint default 100, dept_id bigint default 10)");
        jdbc.execute("create table synthetic_orders(id bigint primary key, quote_id bigint)");
        jdbc.update("insert into eoms_quote(id,quote_no,customer_id,status,del_flag) values (1,'TEST-QUOTE',2,'DRAFT','0')");
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        // H2 omits MySQL SYSDATE(): adapt only the fixture clock, retaining actual locking/update SQL.
        String mapperXml = new ClassPathResource("mapper/quote/QuoteMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        bean.setMapperLocations(new ByteArrayResource(mapperXml.replace("sysdate()", "CURRENT_TIMESTAMP").getBytes(StandardCharsets.UTF_8)));
        SqlSessionFactory factory = bean.getObject();
        QuoteMapper mapper = new SqlSessionTemplate(factory).getMapper(QuoteMapper.class);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        calls = new AtomicInteger();
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        ObjectProvider<QuoteToRepairBridge> bridge = mock(ObjectProvider.class);
        when(bridge.getIfAvailable()).thenReturn((quoteId, customerId, quoteNo, address, remark, operator) -> {
            int call = calls.incrementAndGet();
            jdbc.update("insert into synthetic_orders(id,quote_id) values (?,?)", 100L + call, quoteId);
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test bridge timed out");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return 100L + call;
        });
        service = new QuoteServiceImpl(mapper, mock(QuoteDetailMapper.class), mock(ObjectProvider.class), bridge, mock(DocNoService.class));
    }

    @AfterEach void cleanup() throws InterruptedException {
        if (release != null) release.countDown();
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "test threads must terminate");
        if (jdbc != null) jdbc.execute("SHUTDOWN");
        QuoteTestActor.close();
    }

    private <T> Future<T> submit(Callable<T> work) {
        return pool.submit(() -> { QuoteTestActor.open(); try { return work.call(); } finally { QuoteTestActor.close(); } });
    }
    private Future<?> submit(Runnable work) {
        return submit(() -> { work.run(); return null; });
    }

    @Test void concurrentConversionsCreateOneOrderAndRetryReturnsSameId() throws Exception {
        Future<Long> first = submit(() -> tx.execute(status -> service.convertToOrder(1L, "test", null)));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        CountDownLatch secondStarted = new CountDownLatch(1);
        Future<Long> second = submit(() -> tx.execute(status -> { secondStarted.countDown(); return service.convertToOrder(1L, "test", null); }));
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        try {
            assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS), "second transaction must wait for quotation lock");
        } finally { release.countDown(); }
        assertEquals(101L, first.get(5, TimeUnit.SECONDS));
        assertEquals(101L, second.get(5, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertEquals(1, jdbc.queryForObject("select count(*) from synthetic_orders", Integer.class));
        assertEquals("CONFIRM", jdbc.queryForObject("select status from eoms_quote where id=1", String.class));
        assertEquals(101L, jdbc.queryForObject("select repair_order_id from eoms_quote where id=1", Long.class));
    }

    @Test void concurrentUpdateCannotOverwriteConvertedTerminalStatus() throws Exception {
        assertConcurrentMutationRejected(() -> { Quote patch = new Quote(); patch.setId(1L); patch.setStatus("CANCEL"); service.updateQuote(patch); });
        assertEquals("CONFIRM", jdbc.queryForObject("select status from eoms_quote where id=1", String.class));
    }

    @Test void concurrentDeleteCannotRemoveConvertedQuote() throws Exception {
        assertConcurrentMutationRejected(() -> service.deleteQuoteById(1L));
        assertEquals("0", jdbc.queryForObject("select del_flag from eoms_quote where id=1", String.class));
    }

    @Test void bridgeFailureRollsBackOrderAndAllowsSafeRetry() {
        release.countDown();
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            service.convertToOrder(1L, null, null);
            throw new IllegalStateException("synthetic downstream failure");
        }));
        assertEquals(0, jdbc.queryForObject("select count(*) from synthetic_orders", Integer.class));
        assertEquals("DRAFT", jdbc.queryForObject("select status from eoms_quote where id=1", String.class));
        Long retried = tx.execute(status -> service.convertToOrder(1L, null, null));
        assertEquals(102L, retried.longValue());
        assertEquals(1, jdbc.queryForObject("select count(*) from synthetic_orders", Integer.class));
    }

    private void assertConcurrentMutationRejected(Runnable mutation) throws Exception {
        Future<Long> first = submit(() -> tx.execute(status -> service.convertToOrder(1L, null, null)));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        CountDownLatch secondStarted = new CountDownLatch(1);
        Future<?> second = submit(() -> tx.executeWithoutResult(status -> { secondStarted.countDown(); mutation.run(); }));
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        try { assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS)); }
        finally { release.countDown(); }
        assertEquals(101L, first.get(5, TimeUnit.SECONDS));
        ExecutionException failure = assertThrows(ExecutionException.class, () -> second.get(5, TimeUnit.SECONDS));
        assertInstanceOf(BusinessException.class, failure.getCause());
    }
}
