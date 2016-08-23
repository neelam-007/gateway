package com.l7tech.policy.assertion;

/**
 * Created by fonia01 on 2016-08-03.
 * Class created to contain assertion metrics
 * current holds start and end times in MS
 */
public final class AssertionMetrics {
    private final long startTimeMs;
    private final long endTimeMs;
    
    public AssertionMetrics(final long startTimeMs, final long endTimeMs) {
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public long getLatencyMs() {
        return endTimeMs - startTimeMs;
    }
}
