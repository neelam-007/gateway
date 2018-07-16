package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class LocalCounterTest {

    private static final String name = "localCounter";

    private LocalCounter underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new LocalCounter(name);
    }

    @Test
    public void get_onNewCounter_returnsZeroForAllCounterFields() {
        assertCounterState(0, 0, 0, 0, 0);
    }

    @Test
    public void get_afterIncrementingCounterBy10_returns10ForAllCounterFields() {
        underTest.updateBy(Instant.now().toEpochMilli(), 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test
    public void updateBy_givenCounterSetTo99_expectAllCounterFieldsSetTo100WhenUpdatedBy1UnderOneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateBy(updateTime.toEpochMilli(), 99);
        assertCounterState(99, 99, 99, 99, 99);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdateTime());

        Instant updateTimePlusUnderOneSec = updateTime.plusSeconds(1).minusNanos(1);
        underTest.updateBy(updateTimePlusUnderOneSec.toEpochMilli(), 1);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTimePlusUnderOneSec.toEpochMilli(), underTest.getLastUpdateTime());
    }

    @Test
    public void updateBy_givenCounterSetTo100_expectSecondsFieldSetTo6AndAllOtherFieldsSetTo106WhenUpdatedBy6OneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateBy(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdateTime());

        Instant updateTimePlusOneSec = updateTime.plusSeconds(1);
        underTest.updateBy(updateTimePlusOneSec.toEpochMilli(), 6);
        assertCounterState(6, 106, 106, 106, 106);
        assertEquals(updateTimePlusOneSec.toEpochMilli(), underTest.getLastUpdateTime());
    }

    @Test
    public void updateBy_givenCounterSetTo100_expectSecondsFieldSetToNegativeOneAndAllOtherFieldsSetTo99WhenDecrementedByOneOverOneSecondLater() {
        Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        underTest.updateBy(updateTime.toEpochMilli(), 100);
        assertCounterState(100, 100, 100, 100, 100);
        assertEquals(updateTime.toEpochMilli(), underTest.getLastUpdateTime());

        Instant updateTimePlusOverOneSec = updateTime.plusSeconds(1).plusNanos(1);
        underTest.updateBy(updateTimePlusOverOneSec.toEpochMilli(), -1);
        assertCounterState(-1, 99, 99, 99, 99);
        assertEquals(updateTimePlusOverOneSec.toEpochMilli(), underTest.getLastUpdateTime());
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetToZeroAndUpdateBy9WithLimit10_incrementsCounterTo9() throws CounterLimitReachedException {
        underTest.updateWithoutExceedingLimit(System.currentTimeMillis(), 9, CounterFieldOfInterest.SEC, 10);
        assertCounterState(9, 9, 9, 9, 9);
    }

    @Test
    public void updateWithoutExceedingLimit_givenCounterSetTo9AndUpdateBy1WithLimit10_incrementsCounterTo10() throws CounterLimitReachedException {
        underTest.updateWithoutExceedingLimit(System.currentTimeMillis(), 9, CounterFieldOfInterest.SEC, 10);
        assertCounterState(9, 9, 9, 9, 9);
        underTest.updateWithoutExceedingLimit(System.currentTimeMillis(), 1, CounterFieldOfInterest.SEC, 10);
        assertCounterState(10, 10, 10, 10, 10);
    }

    @Test(expected = CounterLimitReachedException.class)
    public void updateWithoutExceedingLimit_givenCounterSetTo10AndUpdateBy1WithLimit10_throwsExceptionAndCounterRemainsAt10() throws CounterLimitReachedException {
        CounterLimitReachedException exception = null;
        underTest.updateWithoutExceedingLimit(System.currentTimeMillis(), 10, CounterFieldOfInterest.SEC, 10);
        assertCounterState(10, 10, 10, 10, 10);
        try {
            underTest.updateWithoutExceedingLimit(System.currentTimeMillis(), 1, CounterFieldOfInterest.SEC, 10);
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
    public void getLastUpdateTime_givenCounterUpdatedWithUnixTimestamp_returnsSameUpdateTimeAsUnixTimestamp() {
        long timestampIn = System.currentTimeMillis();
        underTest.updateBy(timestampIn, 1);
        long timestampOut = underTest.getLastUpdateTime();
        assertEquals(timestampIn, timestampOut);
    }

    @Test
    public void updateBy_givenCounterSetToMaxLong_doesNotIncrementWhenIncrementedByOne() {
        long now = Instant.now().toEpochMilli();
        long deltaThatOverflows = 100;

        underTest.updateBy(now, Long.MAX_VALUE);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        underTest.updateBy(now, deltaThatOverflows);
        assertCounterState(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void updateBy_givenCounterSetToMinLong_doesNotDecrementWhenDecrementedByOne() {
        long now = Instant.now().toEpochMilli();
        long almostMinLong = Long.MIN_VALUE + 2L;
        long deltaThatOverflows = -100L;

        underTest.updateBy(now, almostMinLong);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);

        underTest.updateBy(now, deltaThatOverflows);
        assertCounterState(almostMinLong, almostMinLong, almostMinLong, almostMinLong, almostMinLong);
    }

    private void assertCounterState(long secCount, long minCount, long hourCount, long dayCount, long monthCount) {
        assertEquals(secCount, underTest.get(CounterFieldOfInterest.SEC));
        assertEquals(minCount, underTest.get(CounterFieldOfInterest.MIN));
        assertEquals(hourCount, underTest.get(CounterFieldOfInterest.HOUR));
        assertEquals(dayCount, underTest.get(CounterFieldOfInterest.DAY));
        assertEquals(monthCount, underTest.get(CounterFieldOfInterest.MONTH));
    }

}