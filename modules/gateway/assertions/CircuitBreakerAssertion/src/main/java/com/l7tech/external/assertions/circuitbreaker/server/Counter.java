package com.l7tech.external.assertions.circuitbreaker.server;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Maintains a history of events where a failure condition was detected by the Circuit Breaker Assertion.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class Counter {

    private final ConcurrentLinkedDeque<Long> failureTimestamps;

    Counter() {
        failureTimestamps = new ConcurrentLinkedDeque<>();
    }

    void recordFailure(final long timestamp) {
        failureTimestamps.add(timestamp);
    }

    long getCountSinceTimestamp(final long timestamp) {
        return failureTimestamps.stream().filter(failureTimestamp -> failureTimestamp >= timestamp).count();
    }

    void reset() {
        failureTimestamps.clear();
    }
}
