package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;

import org.jfree.data.time.Month;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocalCounterStoreTest {

    private static final String COUNTER_NAME = "testCounter";
    private static final String COUNTER_NAME_NOEXIST = "invalidCounter";

    private LocalCounterStore underTest;

    private Properties counterOperationProperties;

    private Instant clock;

    @Before
    public void setup() {
        counterOperationProperties = new Properties();
        underTest = new LocalCounterStore();
        underTest.init();

        clock = Instant.now().truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * when init counter with value, expect to get the init value
     * - expect all field is set to 0
     */
    @Test
    public void testInit() {
        assertCounterState(0, 0, 0, 0, 0);
    }

    @Test
    public void testGet() {
        underTest.update(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, Clock.systemUTC().millis(), 2);
        assertCounterState(2, 2, 2, 2, 2);
    }

    @Test
    public void testGetNonExistentCounter() {
       assertEquals(null, underTest.query(COUNTER_NAME_NOEXIST));
    }

    /**
     * when get and increment is called, expect
     * - return previous value
     * - next get would return previous value + 1
     * - expect the counter snapshot is most updated
     */
    @Test
    public void testGetAndIncrement() {
        assertEquals(0, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1));
        assertEquals(1, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1));
        assertCounterState(2, 2, 2, 2, 2);
    }

    /**
     * Test increment the counter on different field of interest of Second
     * - expected second field updated correctly
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnSec() {
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1);
        assertCounterState(1, 1, 1, 1, 1);

        // Test the increment outside the same second
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.plusSeconds(1).plusNanos(1).toEpochMilli(), 1);
        assertCounterState(1, 2, 2, 2, 2);
    }

    /**
     * Test increment the counter on different field of interest of Second with Quota
     * - expected second field updated correctly with in Quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnSecWithQuota() throws CounterLimitReachedException {
        int limit = 5;
        int increaseValue = 1;

        for (int round = 0; round < 5; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plusNanos(1);
        }
        assertCounterState(5, 5, 5, 5, 5);

        clock = clock.plusSeconds(1);
        for (int round = 0; round < 5; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plusNanos(1);
        }
        assertCounterState(5, 10, 10, 10, 10);
    }

    /**
     * Test increment the counter on different field of interest of Second with Quota
     * - expected second field updated correctly with in Quota
     * - the counter snapshot is updated correctly
     * - exception thrown when quota get hit
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementOnSecWithQuotaReached() throws CounterLimitReachedException {
        int limit = 5;
        int increaseValue = 1;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 5; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plusNanos(1);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertCounterState(5, 5, 5, 5, 5);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * Test increment the counter on different field of interest of Minute
     * - expected minute field updated correctly within quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnMin() {
        int increaseValue = 1;
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 1);

        // Test the increment outside the same minute
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.plusSeconds(60).plusNanos(1).toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 2, 2, 2);
    }

    /**
     * Test increment the counter on different field of interest of Minute with quota
     * - expected minute field updated correctly within quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnMinWithQuota() throws CounterLimitReachedException {
        int limit = 60;
        int increaseValue = 1;
        for (int round = 0; round < 60; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plusSeconds(1).plusNanos(1);
        }
        assertCounterState(1, 60, 60, 60, 60);

        // Test the increment outside the same minute
        clock = clock.plusSeconds(60);
        for (int round = 0; round < 60; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plusSeconds(1).plusNanos(1);
        }
        assertCounterState(1, 60, 120, 120, 120);
    }

    /**
     * Test increment the counter on different field of interest of Minute with quota
     * - expected minute field updated correctly within quota
     * - the counter snapshot is updated correctly
     * - exception thrown when quota get hit
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementOnMinWithQuotaReached() throws CounterLimitReachedException {
        int limit = 60;
        int increaseValue = 1;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 60; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plusNanos(900 * 1000 * 1000);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertCounterState(1, 60, 60, 60, 60);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * Test increment the counter on different field of interest of Hour
     * - expected hour field updated correctly
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnHour() {
        int increaseValue = 1;
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 1);

        // Test the increment outside the same hour
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.plus(1, ChronoUnit.HOURS).plusNanos(1).toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 2, 2);
    }

    /**
     * Test increment the counter on different field of interest of Hour with quota
     * - expected hour field updated correctly within quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnHourWithQuota() throws CounterLimitReachedException {
        int limit = 60;
        int increaseValue = 1;
        for (int round = 0; round < 60; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.MINUTES);
        }
        assertCounterState(1, 1, 60, 60, 60);

        // Test the increment outside the same hour
        clock.plus(1, ChronoUnit.HOURS);
        for (int round = 0; round < 60; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.MINUTES);
        }
        assertCounterState(1, 1, 60, 120, 120);
    }

    /**
     * Test increment the counter on different field of interest of Hour with quota
     * - expected hour field updated correctly within quota
     * - the counter snapshot is updated correctly
     * - exception thrown when quota get hit
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementOnHourWithQuotaReached() throws CounterLimitReachedException {
        int limit = 60;
        int increaseValue = 1;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 60; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plusSeconds(50);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }

        assertCounterState(1, 1, 60, 60, 60);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.HOUR, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * Test increment the counter on different field of interest of Day
     * - expected day field updated correctly
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnDay() {
        int increaseValue = 1;
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 1);

        // Test the increment outside the same Day
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.plus(1, ChronoUnit.DAYS).toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 2);
    }

    /**
     * Test increment the counter on different field of interest of Day with quota
     * - expected day field updated correctly within quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnDayWithQuota() throws CounterLimitReachedException {
        int limit = 24;
        int increaseValue = 1;
        // increment for 24 rounds, simulate a day
        for (int round = 0; round < 24; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.HOURS);
        }
        assertCounterState(1, 1, 1, 24, 24);

        // Test the increment outside the same hour
        for (int round = 0; round < 24; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.HOURS);
        }
        assertCounterState(1, 1, 1, 24, 48);
    }

    /**
     * Test increment the counter on different field of interest of Day with quota
     * - expected day field updated correctly within quota
     * - the counter snapshot is updated correctly
     * - exception thrown when quota get hit
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementOnDayWithQuotaReached() throws CounterLimitReachedException {
        int limit = 24;
        int increaseValue = 1;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 24; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plus(30, ChronoUnit.MINUTES);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertCounterState(1, 1, 2, 24, 24);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.DAY, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * Test increment the counter on different field of interest of Month
     * - expected month field updated correctly
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnMonth() {
        int increaseValue = 1;

        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 1);

        // Test the increment outside the same DAY. May 28 is only 3 days away from June, cannot do a week :P
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.plus(1, ChronoUnit.DAYS).toEpochMilli(), increaseValue);
        assertCounterState(1, 1, 1, 1, 2);

        // Test the increment outside the same month
        long nextMonth = ZonedDateTime.ofInstant(clock, ZoneOffset.UTC).plusMonths(1).toInstant().toEpochMilli();
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, nextMonth, increaseValue);
        assertCounterState(1, 1, 1, 1, 1);
    }

    /**
     * Test increment the counter on different field of interest of Month with Quota
     * - expected month field updated correctly within quota
     * - the counter snapshot is updated correctly
     */
    @Test
    public void testIncrementOnMonthWithQuota() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 1;
        // Make the to start of 30 day month, date 2018/6/1 00:00:00";
        clock = ZonedDateTime.of(LocalDateTime.of(2018, 6, 1, 0, 0, 0), ZoneOffset.UTC).toInstant();
        for (int round = 0; round < 30; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.DAYS);
        }
        assertCounterState(1, 1, 1, 1, 30);

        // Test the increment outside the same Month
        // Set clock to date 2018/7/1 00:00:00
        clock = ZonedDateTime.of(LocalDateTime.of(2018, 7, 1, 0, 0, 0), ZoneOffset.UTC).toInstant();
        for (int round = 0; round < 30; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plus(1, ChronoUnit.DAYS);
        }
        assertCounterState(1, 1, 1, 1, 30);
    }

    /**
     * Test increment the counter on different field of interest of Month with Quota
     * - expected month field updated correctly within quota
     * - the counter snapshot is updated correctly
     * - exception thrown when quota get hit
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementOnMonthWithQuotaReached() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 1;
        clock = ZonedDateTime.now(ZoneOffset.UTC).withMonth(Month.JANUARY).with(TemporalAdjusters.firstDayOfMonth()).toInstant().truncatedTo(ChronoUnit.DAYS);

        CounterLimitReachedException exception = null;
        // increment for 30 rounds, simulate less than a month
        for (int round = 0; round < 30; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plus(18, ChronoUnit.HOURS);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }

        }
        assertCounterState(1, 1, 1, 2, 30);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MONTH, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * when increment and get is called, expect
     * - return new value
     */
    @Test
    public void testIncrementAndGet() {
        assertEquals(0, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1));
        assertCounterState(1, 1, 1, 1, 1);

        underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1);
        underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1);
        underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), -3);
        assertEquals(0, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 1));
        assertCounterState(1, 1, 1, 1, 1);
    }

    /**
     * when increment and get is called with quota defined, expect
     * - return new value with in quota
     */
    @Test
    public void testIncrementAndGetWithQuota() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 1;
        for (int round = 0; round < 30; round++) {
            assertEquals(round, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit));
            clock = clock.plusSeconds(1);
        }
    }

    /**
     * when increment and get is called with quota defined, expect
     * - return new value with in quota
     * - exception thrown when quota is reached
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testIncrementAndGetWithQuotaReached() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 1;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 30; round++) {
            try {
                assertEquals(round, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit));
                clock = clock.plusSeconds(1);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertNull(exception);

        // failed here
        underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * When get and update is called, expect
     * - return previous value
     * - next get would return the new value
     */
    @Test
    public void testUpdateAndGet() {
        assertEquals((10), underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, Instant.now().toEpochMilli(), 10));
        assertCounterState(10, 10, 10, 10, 10);
    }

    /**
     * When get and update is called with quota, expect
     * - return correct updated value with quota
     */
    @Test
    public void testUpdateAndGetWithQuota() throws CounterLimitReachedException {
        int limit = 20;
        int increaseValue = 3;

        for (int round = 0; round < 5; round++) {
            underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
            clock = clock.plusSeconds(1);
        }
        assertCounterState(3, 15, 15, 15, 15);
    }

    /**
     * When get and update is called with quota, expect
     * - return correct updated value with quota
     * - exception thrown when quota reached
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testUpdateAndGetWithQuotaReached() throws CounterLimitReachedException {
        int limit = 20;
        int increaseValue = 3;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 6; round++) {
            try {
                underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
                clock = clock.plusSeconds(1);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertCounterState(3, 18, 18, 18, 18);
        assertNull(exception);

        // failed here
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * When get and update is called, expect
     * - return previous value
     * - next get would return the new value with the correct increment
     */
    @Test
    public void testGetAndUpdate() {
        assertEquals(0, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 4));
        assertEquals(4, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 6));
        assertCounterState(10, 10, 10, 10, 10);
    }

    /**
     * When get and update is called with quota, expect
     * - return previous value with quota
     * - next get would return the new value with the correct increment
     */
    @Test
    public void testGetAndUpdateWithQuota() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 3;
        for (int round = 0; round < 10; round++) {
            assertEquals(round * increaseValue, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit));
            clock = clock.plusSeconds(1);
        }
    }

    /**
     * When get and update is called with quota, expect
     * - return previous value with quota
     * - next get would return the new value with the correct increment
     * - Exception thrown when quota reached
     */
    @Test(expected = CounterLimitReachedException.class)
    public void testGetAndUpdateWithQuotaReached() throws CounterLimitReachedException {
        int limit = 30;
        int increaseValue = 3;

        CounterLimitReachedException exception = null;
        for (int round = 0; round < 10; round++) {
            try {
                assertEquals(round * increaseValue, underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit));
                clock = clock.plusSeconds(1);
            } catch (CounterLimitReachedException ex) {
                exception = ex;
            }
        }
        assertNull(exception);

        // fails on the next update
        underTest.getAndUpdate(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.MIN, clock.toEpochMilli(), increaseValue, limit);
    }

    /**
     * When decrement is called, expect
     * - return the correct value
     */
    @Test
    public void testDecrement() {
        assertEquals(10, underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, Instant.now().toEpochMilli(), 10));
        assertEquals(5, underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, Instant.now().toEpochMilli(), -5));
        assertCounterState(5, 5, 5, 5, 5);
    }

    /**
     * When Rest is called, expect
     * - all counter value back to 0
     */
    @Test
    public void testRest() {
        assertEquals(10, underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), 10));
        assertEquals(5, underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, clock.toEpochMilli(), -5));
        underTest.reset(COUNTER_NAME);
        assertCounterState(0,0, 0, 0, 0);
    }

    /**
     * When Rest is called, expect
     * - all counter value back to 0
     */
    @Test
    public void testIncrementThreadSafe() {
        underTest.updateAndGet(COUNTER_NAME, counterOperationProperties, CounterFieldOfInterest.SEC, Instant.now().toEpochMilli(), 1);
    }

    private void assertCounterState(long secCount, long minCount, long hourCount, long dayCount, long monthCount) {
        SharedCounterState counterState = underTest.get(COUNTER_NAME);
        assertEquals(secCount, counterState.getCount(CounterFieldOfInterest.SEC));
        assertEquals(minCount, counterState.getCount(CounterFieldOfInterest.MIN));
        assertEquals(hourCount, counterState.getCount(CounterFieldOfInterest.HOUR));
        assertEquals(dayCount, counterState.getCount(CounterFieldOfInterest.DAY));
        assertEquals(monthCount, counterState.getCount(CounterFieldOfInterest.MONTH));
    }

}