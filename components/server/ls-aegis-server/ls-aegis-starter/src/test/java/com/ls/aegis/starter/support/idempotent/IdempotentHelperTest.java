package com.ls.aegis.starter.support.idempotent;

import cn.hutool.extra.spring.SpringUtil;
import java.sql.Connection;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.cache.redisson.util.RedisLockUtils;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring JDBC transactions and synchronization; Redis I/O is a deterministic in-memory test boundary. */
@ExtendWith(OutputCaptureExtension.class)
class IdempotentHelperTest {
    private static GenericApplicationContext context;
    private DriverManagerDataSource ds;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private State state;
    private Boundary boundary;
    private final IdempotentHelper helper = new IdempotentHelper();

    @BeforeAll static void initializeRedisClassBoundary() {
        context = new GenericApplicationContext();
        context.registerBean(RedissonClient.class, () -> mock(RedissonClient.class));
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }
    @AfterAll static void closeContext() { context.close(); new SpringUtil().setApplicationContext(null); }
    @BeforeEach void setup() {
        ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table synthetic_ledger(id bigint primary key)");
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        state = new State();
        boundary = new Boundary(state);
    }
    @AfterEach void cleanup() {
        boundary.close();
        jdbc.execute("SHUTDOWN");
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty(), "no lock ownership resource may leak");
    }

    @Test void cacheOnlyAfterPhysicalCommitAndLockOnlyReleasedAfterCompletion() {
        state.onWrite = () -> {
            assertTrue(state.anyHeld(), "lock covers cache publication");
            try (Connection c = ds.getConnection(); var s = c.createStatement(); var r = s.executeQuery("select count(*) from synthetic_ledger")) {
                assertTrue(r.next()); assertEquals(1, r.getInt(1), "other connection must observe committed data before cache publication");
            } catch (Exception e) { throw new AssertionError(e); }
        };
        String result = tx.execute(status -> {
            String value = helper.execute(1L, "confirm", "test-key", () -> { jdbc.update("insert into synthetic_ledger values(1)"); return "ok"; });
            assertTrue(state.cache.isEmpty()); assertTrue(state.anyHeld());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) { assertTrue(state.cache.isEmpty()); assertTrue(state.anyHeld()); }
                @Override public void afterCommit() { assertEquals(1, state.writes.get()); assertTrue(state.anyHeld()); }
                @Override public void afterCompletion(int status) { assertFalse(state.anyHeld()); }
            });
            return value;
        });
        assertEquals("ok", result);
        assertEquals(1, state.writes.get()); assertEquals(1, state.unlocks.get());
        assertEquals(1, state.watchdogs.get()); assertEquals(0, state.fixedLeases.get());
    }

    @Test void rollbackPublishesNothingAndSameKeyCanBeRetried() {
        tx.executeWithoutResult(status -> {
            helper.execute(1L, "confirm", "test-key", () -> { jdbc.update("insert into synthetic_ledger values(1)"); return "rolled-back"; });
            status.setRollbackOnly();
            assertTrue(state.anyHeld()); assertTrue(state.cache.isEmpty());
        });
        assertEquals(0, rowCount()); assertTrue(state.cache.isEmpty()); assertFalse(state.anyHeld());
        String retried = tx.execute(status -> helper.execute(1L, "confirm", "test-key", () -> { jdbc.update("insert into synthetic_ledger values(1)"); return "retry"; }));
        assertEquals("retry", retried); assertEquals(1, rowCount()); assertEquals(1, state.writes.get());
    }

    @Test void actionExceptionReleasesImmediatelyAndDoesNotRegisterSuccess() {
        assertThrows(IllegalArgumentException.class, () -> tx.executeWithoutResult(status -> {
            try {
                helper.execute(1L, "confirm", "test-key", () -> { jdbc.update("insert into synthetic_ledger values(1)"); throw new IllegalArgumentException("synthetic action failure"); });
            } catch (IllegalArgumentException e) {
                assertFalse(state.anyHeld(), "release before caller transaction completion");
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
                throw e;
            }
        }));
        assertEquals(0, rowCount()); assertEquals(0, state.writes.get()); assertEquals(1, state.unlocks.get());
    }

    @Test void withoutTransactionKeepsImmediateCacheAndReplayBehavior() {
        AtomicInteger actions = new AtomicInteger();
        assertEquals("ok", helper.execute(1L, "confirm", "test-key", () -> { actions.incrementAndGet(); return "ok"; }));
        assertEquals(1, state.writes.get()); assertFalse(state.anyHeld());
        assertEquals("ok", helper.execute(1L, "confirm", "test-key", () -> fail("cached result must replay")));
        assertEquals(1, actions.get()); assertEquals(1, state.fixedLeases.get()); assertEquals(0, state.watchdogs.get());
    }

    @Test void sameTransactionReplaysPendingResultAndRejectsDuplicateRunOnce() {
        AtomicInteger actions = new AtomicInteger();
        tx.executeWithoutResult(status -> {
            assertEquals("first", helper.execute(1L, "confirm", "test-key", () -> { actions.incrementAndGet(); return "first"; }));
            assertEquals("first", helper.execute(1L, "confirm", "test-key", () -> fail("pending result must replay")));
            assertEquals(42, helper.runOnce("test:", 1L, "once", "once-key", Duration.ofMinutes(1), () -> 42).intValue());
            assertThrows(BusinessException.class, () -> helper.runOnce("test:", 1L, "once", "once-key", Duration.ofMinutes(1), () -> fail("must reject")));
            assertTrue(state.cache.isEmpty());
        });
        assertEquals(1, actions.get()); assertEquals(2, state.writes.get());
    }

    @Test void concurrentSameKeyCannotExecuteBeforeFirstTransactionCompletes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch pending = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        AtomicInteger actions = new AtomicInteger();
        try {
            Future<String> first = pool.submit(() -> {
                try (Boundary b = new Boundary(state)) {
                    return tx.execute(status -> {
                        String result = helper.execute(1L, "confirm", "test-key", () -> { actions.incrementAndGet(); return "ok"; });
                        pending.countDown();
                        try { if (!complete.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout"); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                        return result;
                    });
                }
            });
            assertTrue(pending.await(5, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> {
                try (Boundary b = new Boundary(state)) {
                    assertThrows(BusinessException.class, () -> tx.execute(status -> helper.execute(1L, "confirm", "test-key", () -> { actions.incrementAndGet(); return "duplicate"; })));
                }
            });
            second.get(5, TimeUnit.SECONDS);
            assertEquals(1, actions.get()); assertTrue(state.cache.isEmpty()); assertTrue(state.anyHeld());
            complete.countDown();
            assertEquals("ok", first.get(5, TimeUnit.SECONDS));
            assertEquals("ok", helper.execute(1L, "confirm", "test-key", () -> fail("must replay after commit")));
        } finally {
            complete.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void committedCacheFailureDoesNotMisreportRollbackOrLeakRawKey(CapturedOutput output) {
        state.failWrite = true;
        String result = tx.execute(status -> helper.execute(1L, "confirm", "PRIVATE-RAW-KEY", () -> { jdbc.update("insert into synthetic_ledger values(1)"); return "committed"; }));
        assertEquals("committed", result); assertEquals(1, rowCount());
        assertTrue(state.cache.isEmpty()); assertFalse(state.anyHeld());
        assertTrue(output.getAll().contains("Committed idempotent result cache write failed"));
        assertFalse(output.getAll().contains("PRIVATE-RAW-KEY"));
    }

    @Test void requiresNewUsesItsOwnSynchronizationAndOuterRollbackDoesNotEraseCommittedInnerResult() {
        TransactionTemplate inner = new TransactionTemplate(new DataSourceTransactionManager(ds));
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            helper.execute(1L, "outer", "test-key", () -> "outer");
            inner.executeWithoutResult(innerStatus -> helper.execute(1L, "inner", "test-key", () -> "inner"));
            assertEquals(1, state.writes.get());
            assertTrue(state.cache.containsValue("inner"));
            assertTrue(state.anyHeld(), "outer lock resumes until outer completion");
            assertEquals("outer", helper.execute(1L, "outer", "test-key", () -> fail("outer pending value must resume")));
            status.setRollbackOnly();
        });
        assertEquals(1, state.cache.size()); assertFalse(state.cache.containsValue("outer")); assertFalse(state.anyHeld());
    }

    @Test void sameKeyInRequiresNewOrNestedActionCannotReenterUncommittedOperation() {
        TransactionTemplate inner = new TransactionTemplate(new DataSourceTransactionManager(ds));
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicInteger actions = new AtomicInteger();
        tx.executeWithoutResult(status -> {
            helper.execute(1L, "confirm", "test-key", () -> {
                actions.incrementAndGet();
                assertThrows(BusinessException.class, () -> helper.execute(1L, "confirm", "test-key", () -> fail("nested duplicate must not execute")));
                return "outer";
            });
            assertThrows(BusinessException.class, () -> inner.execute(innerStatus -> helper.execute(1L, "confirm", "test-key", () -> fail("suspended transaction still owns key"))));
            assertEquals("outer", helper.execute(1L, "confirm", "test-key", () -> fail("outer pending result must resume")));
        });
        assertEquals(1, actions.get()); assertEquals(1, state.writes.get()); assertFalse(state.anyHeld());
    }

    @Test void completionReleaseFailureCannotChangeCommittedOutcomeOrLeakOwnership(CapturedOutput output) {
        state.failClose = true;
        String result = tx.execute(status -> helper.execute(1L, "confirm", "PRIVATE-RAW-KEY", () -> { jdbc.update("insert into synthetic_ledger values(1)"); return "committed"; }));
        assertEquals("committed", result); assertEquals(1, rowCount()); assertEquals(1, state.writes.get());
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
        assertTrue(output.getAll().contains("Idempotent lock release failed after transaction completion"));
        assertFalse(output.getAll().contains("PRIVATE-RAW-KEY"));
    }

    private int rowCount() { return jdbc.queryForObject("select count(*) from synthetic_ledger", Integer.class); }

    private static final class State {
        final Map<String, String> cache = new ConcurrentHashMap<>();
        final Map<String, AtomicBoolean> held = new ConcurrentHashMap<>();
        final AtomicInteger writes = new AtomicInteger(), unlocks = new AtomicInteger(), watchdogs = new AtomicInteger(), fixedLeases = new AtomicInteger();
        volatile boolean failWrite, failClose;
        volatile Runnable onWrite = () -> {};
        boolean anyHeld() { return held.values().stream().anyMatch(AtomicBoolean::get); }
        RedisLockUtils lock(String key) {
            AtomicBoolean flag = held.computeIfAbsent(key, k -> new AtomicBoolean());
            boolean acquired = flag.compareAndSet(false, true);
            RedisLockUtils lock = mock(RedisLockUtils.class);
            when(lock.isLocked()).thenReturn(acquired);
            doAnswer(inv -> { if (acquired) { flag.set(false); unlocks.incrementAndGet(); if (failClose) throw new IllegalStateException("PRIVATE-RAW-KEY"); } return null; }).when(lock).close();
            return lock;
        }
    }
    private static final class Boundary implements AutoCloseable {
        final MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class);
        final MockedStatic<RedisLockUtils> locks = mockStatic(RedisLockUtils.class);
        Boundary(State state) {
            redis.when(() -> RedisUtils.<String>get(anyString())).thenAnswer(inv -> state.cache.get(inv.<String>getArgument(0)));
            redis.when(() -> RedisUtils.set(anyString(), anyString(), any(Duration.class))).thenAnswer(inv -> {
                if (state.failWrite) throw new IllegalStateException("PRIVATE-RAW-KEY");
                state.onWrite.run(); state.cache.put(inv.getArgument(0), inv.getArgument(1)); state.writes.incrementAndGet(); return null;
            });
            locks.when(() -> RedisLockUtils.tryLockWithWatchdog(anyString())).thenAnswer(inv -> { state.watchdogs.incrementAndGet(); return state.lock(inv.getArgument(0)); });
            locks.when(() -> RedisLockUtils.tryLock(anyString())).thenAnswer(inv -> { state.fixedLeases.incrementAndGet(); return state.lock(inv.getArgument(0)); });
        }
        @Override public void close() { locks.close(); redis.close(); }
    }
}
