package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * Represents Latency configuration.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
final public class Latency implements FailureCondition {
    private static final String FAILURE_CONDITION_LATENCY = "Latency";

    private final int samplingWindow;
    private final int maxFailureCount;
    private final int limit;

    public Latency(final int samplingWindow,
                   final int maxFailureCount,
                   final int limit) {
        this.samplingWindow = samplingWindow;
        this.maxFailureCount = maxFailureCount;
        this.limit = limit;
    }

    public int getSamplingWindow() {
        return samplingWindow;
    }

    int getLimit() {
        return limit;
    }

    public int getMaxFailureCount() {
        return maxFailureCount;
    }

    public String getType() {
        return FAILURE_CONDITION_LATENCY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Latency that = (Latency) o;
        return this.samplingWindow == that.samplingWindow &&
                this.maxFailureCount == that.maxFailureCount &&
                this.limit == that.limit;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 29 * result + samplingWindow;
        result = 29 * result + maxFailureCount;
        result = 29 * result + limit;
        return result;
    }
}
