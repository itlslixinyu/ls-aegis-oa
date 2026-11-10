package com.ls.aegis.boot.config;

import me.ahoo.cosid.machine.DefaultClockBackwardsSynchronizer;
import me.ahoo.cosid.machine.ClockBackwardsSynchronizer;
import me.ahoo.cosid.snowflake.ClockSyncSnowflakeId;
import me.ahoo.cosid.snowflake.MillisecondSnowflakeId;
import me.ahoo.cosid.snowflake.SnowflakeId;
import me.ahoo.cosid.snowflake.exception.ClockBackwardsException;
import me.ahoo.cosid.snowflake.exception.ClockTooManyBackwardsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises the installed CosID implementation, without changing any business generator. */
class CosIdClockSyncBoundaryTest {

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void existingDefaultSynchronizerRecoversSmallRollbackWithoutDuplicate(long rollback) {
        long now = System.currentTimeMillis();
        ControlledSnowflake actual = new ControlledSnowflake(now + rollback);
        long first = actual.generate();
        actual.nextTime.set(now);
        actual.returnRealClock = true;
        ClockSyncSnowflakeId synced = new ClockSyncSnowflakeId(actual, new DefaultClockBackwardsSynchronizer());
        long second = assertTimeoutPreemptively(Duration.ofSeconds(2), synced::generate);
        assertTrue(second > first);
        assertTrue(actual.getLastTimestamp() >= now + rollback);
    }

    @Test
    void largeRollbackRemainsRejectedWithoutEmittingAnotherId() {
        long future = System.currentTimeMillis() + 10000;
        ControlledSnowflake actual = new ControlledSnowflake(future);
        actual.generate();
        actual.nextTime.set(future - 10000);
        actual.returnRealClock = true;
        ClockSyncSnowflakeId synced = new ClockSyncSnowflakeId(actual, new DefaultClockBackwardsSynchronizer());
        assertThrows(ClockTooManyBackwardsException.class, synced::generate);
        assertEquals(future, actual.getLastTimestamp());
    }

    @Test
    void secondRollbackIsNotSilentlySwallowedOrTurnedIntoADuplicate() {
        ControlledSnowflake actual = new ControlledSnowflake(1700000000000L);
        long first = actual.generate();
        actual.nextTime.set(1699999999997L);
        AtomicInteger syncs = new AtomicInteger();
        // A second rollback after a sync is distinct from one small correction.
        ClockSyncSnowflakeId synced = new ClockSyncSnowflakeId(actual, countingSynchronizer(syncs));
        assertThrows(ClockBackwardsException.class, synced::generate);
        assertEquals(1, syncs.get());
        assertEquals(1700000000000L, actual.getLastTimestamp());
        actual.nextTime.set(1700000000000L);
        assertNotEquals(first, actual.generate());
    }

    @Test
    void unrelatedGeneratorFailuresAreNeverRetried() {
        SnowflakeId actual = mock(SnowflakeId.class);
        IllegalStateException failure = new IllegalStateException("machine lease lost");
        when(actual.generate()).thenThrow(failure);
        AtomicInteger syncs = new AtomicInteger();
        ClockSyncSnowflakeId synced = new ClockSyncSnowflakeId(actual, countingSynchronizer(syncs));
        assertSame(failure, assertThrows(IllegalStateException.class, synced::generate));
        verify(actual, times(1)).generate();
        assertEquals(0, syncs.get());
    }

    private static ClockBackwardsSynchronizer countingSynchronizer(AtomicInteger syncs) {
        return new ClockBackwardsSynchronizer() {
            @Override public void sync(long lastTimestamp) { syncs.incrementAndGet(); }
            @Override public void syncUninterruptibly(long lastTimestamp) { syncs.incrementAndGet(); }
        };
    }

    private static class ControlledSnowflake extends MillisecondSnowflakeId {
        private final AtomicLong nextTime;
        private boolean returnRealClock;

        ControlledSnowflake(long initialTime) {
            super(1);
            nextTime = new AtomicLong(initialTime);
        }

        @Override
        protected long getCurrentTime() {
            long requested = returnRealClock ? nextTime.getAndSet(Long.MIN_VALUE) : nextTime.get();
            return returnRealClock && requested == Long.MIN_VALUE ? System.currentTimeMillis() : requested;
        }
    }
}
