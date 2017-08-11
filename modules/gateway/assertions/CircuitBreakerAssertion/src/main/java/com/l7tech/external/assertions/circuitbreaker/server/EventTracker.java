package com.l7tech.external.assertions.circuitbreaker.server;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Maintains a history of events where a failure condition was detected by the Circuit Breaker Assertion.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public class EventTracker {

    private final ConcurrentSkipListSet<Long> eventTimestamps;

    public EventTracker() {
        eventTimestamps = new ConcurrentSkipListSet<>();
    }

    void recordEvent(final long timestamp) {
        eventTimestamps.add(timestamp);
    }

    long getCountSinceTimestamp(final long timestamp) {
        return eventTimestamps.tailSet(timestamp).size();
    }

    void clearTimestampBefore(final long timestamp) {
        eventTimestamps.removeAll(eventTimestamps.headSet(timestamp));
    }
}
