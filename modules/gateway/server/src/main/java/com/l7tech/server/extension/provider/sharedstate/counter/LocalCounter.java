package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.google.common.math.LongMath;
import com.l7tech.server.sla.CounterInfo;
import com.l7tech.util.CollectionUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LocalCounter provides a local in memory counter to perform increase/decrease/reset operations.
 * Users of this class are responsible for ensuring thread safety.
 */
final class LocalCounter {

    private static final Logger LOGGER = Logger.getLogger(LocalCounter.class.getName());

    private static final Collection<CounterFieldOfInterest> FIELDS_OF_INTEREST = Collections.unmodifiableList(
            Stream.of(CounterFieldOfInterest.values())
                    .filter(field -> field != CounterFieldOfInterest.NONE)
                    .collect(Collectors.toList())
    );

    private static final ZoneId ZONE_ID = ZoneOffset.UTC;

    private final String name;
    private final Counter counter;
    private ZonedDateTime lastUpdateTime;
    private final EnumMap<CounterFieldOfInterest, Function<ZonedDateTime, Boolean>> dateTimeMatchesDownToFieldCheckers;

    LocalCounter(String name) {
        this.counter = new Counter();
        this.name = name;
        lastUpdateTime = ZonedDateTime.now(ZONE_ID);

        Map<CounterFieldOfInterest, Function<ZonedDateTime, Boolean>> checkerMap = new HashMap<>(); // NOSONAR converted to EnumMap below..
        checkerMap.put(CounterFieldOfInterest.SEC, this::doDateTimesMatchDownToSeconds);
        checkerMap.put(CounterFieldOfInterest.MIN, this::doDateTimesMatchDownToMinutes);
        checkerMap.put(CounterFieldOfInterest.HOUR, this::doDateTimesMatchDownToHours);
        checkerMap.put(CounterFieldOfInterest.DAY, this::doDateTimesMatchDownToDays);
        checkerMap.put(CounterFieldOfInterest.MONTH, this::doDateTimesMatchDownToMonths);
        dateTimeMatchesDownToFieldCheckers = new EnumMap<>(checkerMap);
    }

    public SharedCounterState get() {
        CollectionUtils.MapBuilder<CounterFieldOfInterest, Long> counterState = CollectionUtils.MapBuilder.builder();
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            counterState.put(field, get(field));
        }
        return new CounterInfo(
                name,
                new EnumMap<>(counterState.unmodifiableMap()),
                Date.from(lastUpdateTime.toInstant())
        );
    }

    public long get(CounterFieldOfInterest fieldOfInterest) {
        return counter.get(fieldOfInterest);
    }

    public void updateBy(long updateTimestamp, long delta) {
        ZonedDateTime zonedUpdateTime = convertUnixTimestampToUtcZonedDateTime(updateTimestamp);
        resetFieldsThatHaveExpired(zonedUpdateTime);
        updateAllFields(zonedUpdateTime, delta);
    }

    public void updateWithoutExceedingLimit(long updateTimestamp, long delta, CounterFieldOfInterest field, long limit) throws CounterLimitReachedException {
        ZonedDateTime zonedUpdateTime = convertUnixTimestampToUtcZonedDateTime(updateTimestamp);
        resetFieldsThatHaveExpired(zonedUpdateTime);
        long proposedNewCountForFieldOfInterest = get(field) + delta;
        if (proposedNewCountForFieldOfInterest > limit) {
            throw new CounterLimitReachedException("Limit reached for counter " + name);
        }
        updateAllFields(zonedUpdateTime, delta);
    }

    public long getLastUpdateTime() {
        return lastUpdateTime.toInstant().toEpochMilli();
    }

    private void setLastUpdateTime(ZonedDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Reset the counter fields that have expired based on comparing the last update time and the new update time.
     * @param updateTime the new update time
     */
    private void resetFieldsThatHaveExpired(ZonedDateTime updateTime) {
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            Function<ZonedDateTime, Boolean> dateTimeMatchesDownToThisField = dateTimeMatchesDownToFieldCheckers.get(field);
            boolean dateTimeMismatched = !dateTimeMatchesDownToThisField.apply(updateTime);
            if (dateTimeMismatched) {
                counter.resetField(field);
            }
        }
    }

    private void updateAllFields(ZonedDateTime updateTimestamp, long delta) {
        for (CounterFieldOfInterest field : FIELDS_OF_INTEREST) {
            counter.updateField(field, delta);
        }
        setLastUpdateTime(updateTimestamp);
    }

    public String getName() {
        return name;
    }

    private ZonedDateTime convertUnixTimestampToUtcZonedDateTime(long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE_ID);
    }

    /**
     * Check that now matches last update time down to the second, so check year, month, day, hour, minute, seconds
     * match last update time.
     * Units lower than seconds are ignored.
     * @return true if the date and time match down to the second.
     */
    private boolean doDateTimesMatchDownToSeconds(ZonedDateTime now) {
        return lastUpdateTime.truncatedTo(ChronoUnit.SECONDS).equals(now.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Check that now matches last update time down to the minute, so check year, month, day, hour, minute.
     * Seconds and lower units are ignored.
     * @return true if the date and time match down to the minute
     */
    private boolean doDateTimesMatchDownToMinutes(ZonedDateTime now) {
        return lastUpdateTime.truncatedTo(ChronoUnit.MINUTES).equals(now.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * Check that now matches last update time down to the hour, so check year, month, day, hour
     * Minutes and lower units are ignored.
     * @return true if the date and time match down to the hour
     */
    private boolean doDateTimesMatchDownToHours(ZonedDateTime now) {
        return lastUpdateTime.truncatedTo(ChronoUnit.HOURS).equals(now.truncatedTo(ChronoUnit.HOURS));
    }

    /**
     * Check that now matches last update time down to the day of year (implicitly checks both month and day).
     * The time of day is ignored.
     * @return true if the date and time match down to the day
     */
    private boolean doDateTimesMatchDownToDays(ZonedDateTime now) {
        return lastUpdateTime.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS));
    }

    /**
     * Check that now matches last update time down to the month of year, so check year, and month of year.
     * Day of the month and time of day is ignored.
     * @return true if the date and time match down to the month
     */
    private boolean doDateTimesMatchDownToMonths(ZonedDateTime now) {
        // Java's truncatedTo() only works for time units DAYS and smaller... so just check year and month explicitly
        return lastUpdateTime.getYear() == now.getYear() && lastUpdateTime.getMonthValue() == now.getMonthValue();
    }

    private final class Counter {

        private final long[] counters;

        private Counter() {
            counters = new long[6];
        }

        private long get(CounterFieldOfInterest fieldOfInterest) {
            return counters[fieldOfInterest.ordinal()];
        }

        /**
         *  Updates the counter field by delta, with overflow checking. Updates that would overflow do not succeed
         *  and the counter retains its current value. Such updates are logged to notify users.
         */
        private void updateField(CounterFieldOfInterest field, long delta) {
            long currentCounterValue = get(field);
            boolean deltaIsPositive = delta >= 0;
            if (deltaIsPositive) {
                try {
                    setField(field, LongMath.checkedAdd(currentCounterValue, delta));
                } catch (ArithmeticException e) {
                    logOverflowError(CounterLimit.MAX, field);
                }
            } else {
                long positiveDelta = -delta;
                try {
                    setField(field, LongMath.checkedSubtract(currentCounterValue, positiveDelta));
                } catch (ArithmeticException e) {
                    logOverflowError(CounterLimit.MIN, field);
                }
            }
        }

        private void resetField(CounterFieldOfInterest field) {
            counters[field.ordinal()] = 0;
        }

        private void setField(CounterFieldOfInterest field, long newCounterValue) {
            counters[field.ordinal()] = newCounterValue;
        }

        private void logOverflowError(CounterLimit limitReached, CounterFieldOfInterest field) {
            LOGGER.log(Level.SEVERE,
                    "Counter {0} has reached the {1} counter value for the {2} field. Counter value will not change any further.",
                    new Object[]{name, limitReached.getName(), field.getName(), limitReached.getRollOverValue()}
            );
        }
    }

    private enum CounterLimit {
        MAX("maximum", Long.MAX_VALUE, Long.MIN_VALUE),
        MIN("minimum", Long.MIN_VALUE, Long.MAX_VALUE);

        private final String name;
        private final long limit;
        private final long rollOverValue;

        CounterLimit(String name, long limit, long rollOverValue) {
            this.name = name;
            this.limit = limit;
            this.rollOverValue = rollOverValue;
        }

        public String getName() {
            return name;
        }

        public long getLimit() {
            return limit;
        }

        public long getRollOverValue() {
            return rollOverValue;
        }
    }

}
