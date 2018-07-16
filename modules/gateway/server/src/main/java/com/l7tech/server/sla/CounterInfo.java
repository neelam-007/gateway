package com.l7tech.server.sla;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;
import com.l7tech.util.CollectionUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * Can be used to hold a snapshot of the current state of a counter.
 */
public final class CounterInfo implements Serializable, SharedCounterState {
    private final String name;
    private final Map<CounterFieldOfInterest, Long> counterState;
    private final Date lastUpdate;

    /**
     * @deprecated Please use the other constructor.
     */
    @Deprecated
    public CounterInfo(String name, long sec, long min, long hr, long day, long mnt, Date lastUpdate) {
        this(
                name,
                new EnumMap<>(
                        CollectionUtils.MapBuilder.<CounterFieldOfInterest, Long>builder()
                                .put(CounterFieldOfInterest.SEC, sec)
                                .put(CounterFieldOfInterest.MIN, min)
                                .put(CounterFieldOfInterest.HOUR, hr)
                                .put(CounterFieldOfInterest.DAY, day)
                                .put(CounterFieldOfInterest.MONTH, mnt)
                                .unmodifiableMap()
                ),
                lastUpdate
        );
    }

    public CounterInfo(String name, Map<CounterFieldOfInterest, Long> counterState, Date lastUpdate) {
        this.name = name;
        this.counterState = counterState;
        this.lastUpdate = lastUpdate;
    }

    public String getName() {
        return name;
    }

    public long getCount(CounterFieldOfInterest field) {
        return counterState.get(field);
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
