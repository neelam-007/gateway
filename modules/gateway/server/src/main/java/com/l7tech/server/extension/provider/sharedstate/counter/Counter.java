package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.google.common.math.LongMath;
import com.l7tech.server.sla.CounterInfo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest.*;

class Counter {

    private String name;
    private long lastUpdate;
    private final EnumMap<CounterFieldOfInterest, Long> map = new EnumMap<>(CounterFieldOfInterest.class);

    private static final Collection<CounterFieldOfInterest> FIELDS_OF_INTEREST = Collections.unmodifiableList(
            Stream.of(CounterFieldOfInterest.values())
                    .filter(field -> field != CounterFieldOfInterest.NONE)
                    .collect(Collectors.toList())
    );

    private static final Logger LOGGER = Logger.getLogger(Counter.class.getName());

    Counter(){
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            map.put(field, 0L);
        }
        this.lastUpdate = Clock.systemUTC().millis();
    }

    Counter(String name){
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    long getLastUpdate() {
        return lastUpdate;
    }

    void setLastUpdate(long updateTime) {
        this.lastUpdate = updateTime;
    }

    long getCounterField(CounterFieldOfInterest field){
        return map.get(field);
    }

    void setCounterField(CounterFieldOfInterest field, long value){
        map.put(field, value);
    }

    private boolean limitExceeded(CounterFieldOfInterest field, long delta, long limit) {
        long valueToCheck = getCounterField(field);
        return valueToCheck+delta>limit;
    }

    /**
     * This method updates each field in the counter based upon the delta value and then updates the timestamp
     * @param timestamp the time when this method was called
     * @param delta the amount to increment the counter by
     */
    void updateCounter(long timestamp, long delta){
        resetExpiredFields(timestamp);
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            updateField(field, delta);
        }
        this.lastUpdate = timestamp;
    }

    /**
     * This method updates each field in the counter based upon the delta value and then updates the timestamp
     * @param timestamp the time when this method was called
     * @param delta the amount to increment the counter by
     * @param limit the limit for this update
     */
    void updateCounterWithoutExceedingLimit(CounterFieldOfInterest fieldOfInterest, long timestamp, long delta, long limit) throws CounterLimitReachedException {
        resetExpiredFields(timestamp);
        if(limitExceeded(fieldOfInterest, delta, limit)) throw new CounterLimitReachedException("Limit reached for counter " + name);
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            updateField(field, delta);
        }
        this.lastUpdate = timestamp;
    }

    /**
     * This method updates the field based on the new delta value and checks for any overflow that could occur
     * @param field the field that is being updated
     * @param delta the amount to increment of decrement the field by
     */
    private void updateField(CounterFieldOfInterest field, long delta) {
        long currentCounterValue = getCounterField(field);
        boolean deltaIsPositive = delta >= 0;
        if (deltaIsPositive) {
            try {
                setCounterField(field, LongMath.checkedAdd(currentCounterValue, delta));
            } catch (ArithmeticException e) {
                logOverflowError(field);
            }
        } else {
            long positiveDelta = -delta;
            try {
                setCounterField(field, LongMath.checkedSubtract(currentCounterValue, positiveDelta));
            } catch (ArithmeticException e) {
                logOverflowError(field);
            }
        }
    }

    /**
     * Logs whenever there is an overflow
     * @param field the field in which contains this max value
     */
    private void logOverflowError(CounterFieldOfInterest field) {
        LOGGER.log(Level.SEVERE,
                "Counter {0} has reached the boundary value for the {1} field. Counter value will not change any further.",
                new Object[]{name, field.getName()}
        );
    }

    private ZonedDateTime getZonedTime(long epochTime){
        Instant instant = Instant.ofEpochMilli(epochTime);
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Resets all the fields that have expired bases on the current timestamp
     * @param timestamp the timestamp of which to compare the last updated time with
     */
    private void resetExpiredFields(long timestamp){
        if(!doDateTimesMatchDownToSeconds(getZonedTime(timestamp))){
            setCounterField(SEC, 0L);
        }
        if(!doDateTimesMatchDownToMinutes(getZonedTime(timestamp))){
            setCounterField(MIN, 0L);
        }
        if(!doDateTimesMatchDownToHours(getZonedTime(timestamp))) {
            setCounterField(HOUR, 0L);
        }
        if(!doDateTimesMatchDownToDays(getZonedTime(timestamp))) {
            setCounterField(DAY, 0L);
        }
        if(!doDateTimesMatchDownToMonths(getZonedTime(timestamp))) {
            setCounterField(MONTH, 0L);
        }
    }

    /**
     * Check that now matches last update time down to the second, so check year, month, day, hour, minute, seconds
     * match last update time.
     * Units lower than seconds are ignored.
     * @return true if the date and time match down to the second.
     */
    private boolean doDateTimesMatchDownToSeconds(ZonedDateTime now) {
        return getZonedTime(lastUpdate).truncatedTo(ChronoUnit.SECONDS).equals(now.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Check that now matches last update time down to the minute, so check year, month, day, hour, minute.
     * Seconds and lower units are ignored.
     * @return true if the date and time match down to the minute
     */
    private boolean doDateTimesMatchDownToMinutes(ZonedDateTime now) {
        return getZonedTime(lastUpdate).truncatedTo(ChronoUnit.MINUTES).equals(now.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * Check that now matches last update time down to the hour, so check year, month, day, hour
     * Minutes and lower units are ignored.
     * @return true if the date and time match down to the hour
     */
    private boolean doDateTimesMatchDownToHours(ZonedDateTime now) {
        return getZonedTime(lastUpdate).truncatedTo(ChronoUnit.HOURS).equals(now.truncatedTo(ChronoUnit.HOURS));
    }

    /**
     * Check that now matches last update time down to the day of year (implicitly checks both month and day).
     * The time of day is ignored.
     * @return true if the date and time match down to the day
     */
    private boolean doDateTimesMatchDownToDays(ZonedDateTime now) {
        return getZonedTime(lastUpdate).truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS));
    }

    /**
     * Check that now matches last update time down to the month of year, so check year, and month of year.
     * Day of the month and time of day is ignored.
     * @return true if the date and time match down to the month
     */
    private boolean doDateTimesMatchDownToMonths(ZonedDateTime now) {
        // Java's truncatedTo() only works for time units DAYS and smaller... so just check year and month explicitly
        return getZonedTime(lastUpdate).getYear() == now.getYear() && getZonedTime(lastUpdate).getMonthValue() == now.getMonthValue();
    }

    Map<CounterFieldOfInterest, Long> toMap(){
        return new EnumMap<>(map);
    }

    SharedCounterState getState(){
        return new CounterInfo(name, toMap(), new Date(lastUpdate));
    }

    @Override
    public String toString() {
        return "Counter{" +
                "name='" + name + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", map=" + map +
                '}';
    }
}
