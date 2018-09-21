package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class CounterTest {

    private static final String name = "localCounter";

    private Counter counter;
    private Instant startTime;

    @Before
    public void setUp() throws Exception {
        counter = new Counter(name);
        startTime = Instant.parse("2018-06-15T01:00:00.0Z");
    }

    @Test
    public void get_onNewCounter_returnsZeroForAllCounterFields() {
        assertCounterState(0, 0, 0, 0, 0);
    }

    @Test
    public void get_afterIncrementingCounterBy10_returns10ForAllCounterFields() {
        counter.updateCounter(startTime.toEpochMilli(), 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test
    public void updateCounter_givenCounterSetTo99_expectAllCounterFieldsSetTo100WhenUpdatedBy1UnderOneSecondLater() {
        Instant updateTime = startTime.truncatedTo(ChronoUnit.SECONDS);

        counter.updateCounter(updateTime.toEpochMilli(), 99);
        assertCounterState(99, 99, 99, 99, 99);
        assertEquals(updateTime.toEpochMilli(), counter.getLastUpdate());

        Instant updateTimePlusUnderOneSec = updateTime.plusSeconds(1).minusNanos(1);
        counter.updateCounter(updateTimePlusUnderOneSec.toEpochMilli(), 1);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTimePlusUnderOneSec.toEpochMilli(), counter.getLastUpdate());
    }

    @Test
    public void updateCounter_givenCounterSetTo100_expectSecondsFieldSetTo6AndAllOtherFieldsSetTo106WhenUpdatedBy6OneSecondLater() {
       
        Instant updateTime = startTime.truncatedTo(ChronoUnit.SECONDS);

        counter.updateCounter(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), counter.getLastUpdate());

        Instant updateTimePlusOneSec = updateTime.plusSeconds(1);
        counter.updateCounter(updateTimePlusOneSec.toEpochMilli(), 6);
        assertCounterState(6, 106, 106, 106, 106);
        assertEquals(updateTimePlusOneSec.toEpochMilli(), counter.getLastUpdate());
    }

    @Test
    public void updateCounter_givenCounterSetTo100_expectSecondsFieldSetToNegativeOneAndAllOtherFieldsSetTo99WhenDecrementedByOneOverOneSecondLater() {
        Instant updateTime = startTime.truncatedTo(ChronoUnit.SECONDS);

        counter.updateCounter(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), counter.getLastUpdate());

        Instant updateTimePlusOverOneSec = updateTime.plusSeconds(1).plusNanos(1);
        counter.updateCounter(updateTimePlusOverOneSec.toEpochMilli(), -1);
        assertCounterState(-1, 99, 99, 99, 99);
        assertEquals(updateTimePlusOverOneSec.toEpochMilli(), counter.getLastUpdate());
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetToZeroAndUpdateBy9WithLimit10_incrementsCounterTo9() throws CounterLimitReachedException {
        counter.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, startTime.toEpochMilli(), 9, 10);
        assertCounterState(9, 9, 9, 9, 9);
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetTo9AndUpdateBy1WithLimit10_incrementsCounterTo10() throws CounterLimitReachedException {
        counter.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, startTime.toEpochMilli(), 9, 10);
        assertCounterState(9, 9, 9, 9, 9);
        counter.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, startTime.toEpochMilli(), 1, 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test(expected = CounterLimitReachedException.class)
    public void updateWithoutExceedingLimit_givenCounterSetTo10AndUpdateBy1WithLimit10_throwsExceptionAndCounterRemainsAt10() throws CounterLimitReachedException {
        CounterLimitReachedException exception = null;
        counter.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, startTime.toEpochMilli(), 10, 10);
        assertCounterState(10, 10, 10, 10, 10);
        try {
            counter.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, startTime.toEpochMilli(), 1, 10);
        } catch (CounterLimitReachedException e) {
            exception = e;
        }

        assertCounterState(10, 10, 10, 10, 10);
        assert exception != null;
        throw exception;
    }

    @Test
    public void getName_givenCounter_returnsThatCountersName() {
        assertEquals(name, counter.getName());
    }

    @Test
    public void getLastUpdate_givenCounterUpdatedWithUnixTimestamp_returnsSameUpdateTimeAsUnixTimestamp() {
        long timestampIn = startTime.toEpochMilli();
        counter.updateCounter(timestampIn, 1);
        long timestampOut = counter.getLastUpdate();
        assertEquals(timestampIn, timestampOut);
    }

    @Test
    public void updateCounter_givenCounterSetToMaxLong_doesNotIncrementWhenIncrementedByOne() {
        long now = startTime.toEpochMilli();
        long deltaThatOverflows = 100;

        counter.updateCounter(now, Long.MAX_VALUE);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        counter.updateCounter(now, deltaThatOverflows);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void updateCounter_givenCounterSetToMinLong_doesNotDecrementWhenDecrementedByOne() {
        long now = startTime.toEpochMilli();
        long almostMinLong = Long.MIN_VALUE + 2L;
        long deltaThatOverflows = -100L;

        counter.updateCounter(now, almostMinLong);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);

        counter.updateCounter(now, deltaThatOverflows);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);
    }

    private void assertCounterState(long secCount, long minCount, long hourCount, long dayCount, long monthCount) {
        assertEquals(secCount, counter.getCounterField(CounterFieldOfInterest.SEC));
        assertEquals(minCount, counter.getCounterField(CounterFieldOfInterest.MIN));
        assertEquals(hourCount, counter.getCounterField(CounterFieldOfInterest.HOUR));
        assertEquals(dayCount, counter.getCounterField(CounterFieldOfInterest.DAY));
        assertEquals(monthCount, counter.getCounterField(CounterFieldOfInterest.MONTH));
    }

}