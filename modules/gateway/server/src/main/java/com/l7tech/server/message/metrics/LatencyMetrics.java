package com.l7tech.server.message.metrics;

/**
 * Class created to contain metrics
 * currently holds start and end times in MS
 * TODO finalize the structure and name of this class before release
 */
public final class LatencyMetrics {
    private final long startTimeMs;
    private final long endTimeMs;
    
    public LatencyMetrics(final long startTimeMs, final long endTimeMs) {
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
