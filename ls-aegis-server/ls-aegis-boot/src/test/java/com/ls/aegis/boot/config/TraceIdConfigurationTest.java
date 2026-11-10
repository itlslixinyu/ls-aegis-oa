package com.ls.aegis.boot.config;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.id.snowflake.AbstractClock;
import com.yomahub.tlog.id.snowflake.UniqueIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.trace.autoconfigure.TraceAutoConfiguration;
import top.continew.starter.trace.filter.TLogServletFilter;
import top.continew.starter.trace.handler.TraceIdGenerator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner()
        .withUserConfiguration(TraceIdConfiguration.class)
        .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class))
        .withPropertyValues("continew-starter.trace.enabled=true", "continew-starter.trace.trace-id-name=X-Trace-Id",
            "continew-starter.trace.tlog.pattern=[$spanId][$traceId]",
            "continew-starter.trace.tlog.enable-invoke-time-print=false", "continew-starter.trace.tlog.mdc-enable=false");

    @Test
    void actualAutoConfigurationInstallsOnlyCustomGenerator() {
        withContext(() -> context.run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(1, ctx.getBeansOfType(TLogIdGenerator.class).size());
            assertFalse(ctx.containsBean("tLogIdGenerator"));
            assertSame(ctx.getBean(TLogIdGenerator.class), TLogIdGeneratorLoader.getIdGenerator());
            assertTrue(ctx.getBean("tLogServletFilter", FilterRegistrationBean.class).getFilter() instanceof TLogServletFilter);
            assertUuid(ctx.getBean(TLogIdGenerator.class).generateTraceId());
        }));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void actualFilterSucceedsWhenOriginalSnowflakeRejectsSmallRollback(long rollback) throws Exception {
        Field clockField = field("clock");
        Field lastTime = field("lastTime");
        Field sequence = field("sequence");
        synchronized (UniqueIdGenerator.class) {
            Object previousClock = clockField.get(null);
            long previousTime = lastTime.getLong(null);
            long previousSequence = sequence.getLong(null);
            AtomicLong clock = new AtomicLong(System.currentTimeMillis());
            try {
                clockField.set(null, new AbstractClock() {
                    @Override public long millis() { return clock.get(); }
                });
                lastTime.setLong(null, clock.get());
                clock.addAndGet(-rollback);
                assertThrows(IllegalArgumentException.class, () -> new TraceIdGenerator().generateTraceId());
                withContext(() -> context.run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    TLogServletFilter filter = (TLogServletFilter) ctx.getBean("tLogServletFilter", FilterRegistrationBean.class).getFilter();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    assertDoesNotThrow(() -> filter.doFilter(new MockHttpServletRequest("GET", "/probe"), response, (req, res) -> {
                        String trace = TLogContext.getTraceId();
                        assertUuid(trace);
                        assertEquals(trace, MDC.get("tlogTraceId"));
                        assertEquals(trace, response.getHeader("X-Trace-Id"));
                    }));
                    assertUuid(response.getHeader("X-Trace-Id"));
                    assertNull(TLogContext.getTraceId());
                    assertNull(MDC.get("tlogTraceId"));
                }));
            } finally {
                clockField.set(null, previousClock);
                lastTime.setLong(null, previousTime);
                sequence.setLong(null, previousSequence);
            }
        }
    }

    @Test
    void incomingTraceRemainsUnchangedAndFailureCleansContext() {
        withContext(() -> context.run(ctx -> {
            TLogServletFilter filter = (TLogServletFilter) ctx.getBean("tLogServletFilter", FilterRegistrationBean.class).getFilter();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe");
            String trace = UUID.randomUUID().toString();
            request.addHeader("tlogTraceId", trace);
            MockHttpServletResponse response = new MockHttpServletResponse();
            RuntimeException failure = new RuntimeException("downstream failure");
            assertSame(failure, assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, (req, res) -> {
                assertEquals(trace, TLogContext.getTraceId());
                throw failure;
            })));
            assertEquals(trace, response.getHeader("X-Trace-Id"));
            assertNull(TLogContext.getTraceId());
            assertNull(MDC.get("tlogTraceId"));
        }));
    }

    @Test
    void concurrentGeneratorsProduceUniqueCompatibleUuidV4AcrossInstances() throws Exception {
        List<TLogIdGenerator> generators = List.of(
            new TraceIdConfiguration().clockIndependentTraceIdGenerator(),
            new TraceIdConfiguration().clockIndependentTraceIdGenerator());
        Set<String> ids = ConcurrentHashMap.newKeySet();
        var executor = Executors.newFixedThreadPool(12);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int thread = 0; thread < 12; thread++) {
                TLogIdGenerator generator = generators.get(thread % generators.size());
                tasks.add(executor.submit(() -> {
                    for (int i = 0; i < 5000; i++) {
                        String id = generator.generateTraceId();
                        assertUuid(id);
                        assertTrue(ids.add(id), "duplicate trace identity");
                    }
                }));
            }
            for (Future<?> task : tasks) task.get(30, TimeUnit.SECONDS);
            assertEquals(60000, ids.size());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Field field(String name) throws Exception {
        Field field = UniqueIdGenerator.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void assertUuid(String value) {
        assertNotNull(value);
        assertEquals(36, value.length());
        UUID uuid = UUID.fromString(value);
        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());
        assertEquals(uuid.toString(), value);
    }

    // TLog uses a JVM-global loader; never leave a test-specific generator installed.
    private static void withContext(Runnable action) {
        TLogIdGenerator previous = TLogIdGeneratorLoader.getIdGenerator();
        try { action.run(); } finally { TLogIdGeneratorLoader.setIdGenerator(previous); }
    }
}
