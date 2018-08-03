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

    private Counter underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new Counter(name);
    }

    @Test
    public void get_onNewCounter_returnsZeroForAllCounterFields() {
        assertCounterState(0, 0, 0, 0, 0);
    }

    @Test
    public void get_afterIncrementingCounterBy10_returns10ForAllCounterFields() {
        underTest.updateCounter(Instant.now().toEpochMilli(), 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test
    public void updateCounter_givenCounterSetTo99_expectAllCounterFieldsSetTo100WhenUpdatedBy1UnderOneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateCounter(updateTime.toEpochMilli(), 99);
        assertCounterState(99, 99, 99, 99, 99);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdate());

        Instant updateTimePlusUnderOneSec = updateTime.plusSeconds(1).minusNanos(1);
        underTest.updateCounter(updateTimePlusUnderOneSec.toEpochMilli(), 1);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTimePlusUnderOneSec.toEpochMilli(), underTest.getLastUpdate());
    }

    @Test
    public void updateCounter_givenCounterSetTo100_expectSecondsFieldSetTo6AndAllOtherFieldsSetTo106WhenUpdatedBy6OneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateCounter(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdate());

        Instant updateTimePlusOneSec = updateTime.plusSeconds(1);
        underTest.updateCounter(updateTimePlusOneSec.toEpochMilli(), 6);
        assertCounterState(6, 106, 106, 106, 106);
        assertEquals(updateTimePlusOneSec.toEpochMilli(), underTest.getLastUpdate());
    }

    @Test
    public void updateCounter_givenCounterSetTo100_expectSecondsFieldSetToNegativeOneAndAllOtherFieldsSetTo99WhenDecrementedByOneOverOneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateCounter(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdate());

        Instant updateTimePlusOverOneSec = updateTime.plusSeconds(1).plusNanos(1);
        underTest.updateCounter(updateTimePlusOverOneSec.toEpochMilli(), -1);
        assertCounterState(-1, 99, 99, 99, 99);
        assertEquals(updateTimePlusOverOneSec.toEpochMilli(), underTest.getLastUpdate());
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetToZeroAndUpdateBy9WithLimit10_incrementsCounterTo9() throws CounterLimitReachedException {
        underTest.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, System.currentTimeMillis(), 9, 10);
        assertCounterState(9, 9, 9, 9, 9);
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetTo9AndUpdateBy1WithLimit10_incrementsCounterTo10() throws CounterLimitReachedException {
        underTest.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, System.currentTimeMillis(), 9, 10);
        assertCounterState(9, 9, 9, 9, 9);
        underTest.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, System.currentTimeMillis(), 1, 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test(expected = CounterLimitReachedException.class)
    public void updateWithoutExceedingLimit_givenCounterSetTo10AndUpdateBy1WithLimit10_throwsExceptionAndCounterRemainsAt10() throws CounterLimitReachedException {
        CounterLimitReachedException exception = null;
        underTest.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, System.currentTimeMillis(), 10, 10);
        assertCounterState(10, 10, 10, 10, 10);
        try {
            underTest.updateCounterWithoutExceedingLimit(CounterFieldOfInterest.SEC, System.currentTimeMillis(), 1, 10);
        } catch (CounterLimitReachedException e) {
            exception = e;
        }

        assertCounterState(10, 10, 10, 10, 10);
        assert exception != null;
        throw exception;
    }

    @Test
    public void getName_givenCounter_returnsThatCountersName() {
        assertEquals(name, underTest.getName());
    }

    @Test
    public void getLastUpdate_givenCounterUpdatedWithUnixTimestamp_returnsSameUpdateTimeAsUnixTimestamp() {
        long timestampIn = System.currentTimeMillis();
        underTest.updateCounter(timestampIn, 1);
        long timestampOut = underTest.getLastUpdate();
        assertEquals(timestampIn, timestampOut);
    }

    @Test
    public void updateCounter_givenCounterSetToMaxLong_doesNotIncrementWhenIncrementedByOne() {
        long now = Instant.now().toEpochMilli();
        long deltaThatOverflows = 100;

        underTest.updateCounter(now, Long.MAX_VALUE);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        underTest.updateCounter(now, deltaThatOverflows);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void updateCounter_givenCounterSetToMinLong_doesNotDecrementWhenDecrementedByOne() {
        long now = Instant.now().toEpochMilli();
        long almostMinLong = Long.MIN_VALUE + 2L;
        long deltaThatOverflows = -100L;

        underTest.updateCounter(now, almostMinLong);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);

        underTest.updateCounter(now, deltaThatOverflows);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);
    }

    private void assertCounterState(long secCount, long minCount, long hourCount, long dayCount, long monthCount) {
        assertEquals(secCount, underTest.getCounterField(CounterFieldOfInterest.SEC));
        assertEquals(minCount, underTest.getCounterField(CounterFieldOfInterest.MIN));
        assertEquals(hourCount, underTest.getCounterField(CounterFieldOfInterest.HOUR));
        assertEquals(dayCount, underTest.getCounterField(CounterFieldOfInterest.DAY));
        assertEquals(monthCount, underTest.getCounterField(CounterFieldOfInterest.MONTH));
    }

}