package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * Represents Latency Failure configuration.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
final public class LatencyFailure implements FailureCondition {
    private final long samplingWindow;
    private final long maxFailureCount;
    private final long limit;
    public static final String FAILURE_CONDITION_LATENCY = "LATENCY_FAILURE";

    public LatencyFailure(final long samplingWindow,
                         final long maxFailureCount,
                         final long limit) {

        this.samplingWindow = samplingWindow;
        this.maxFailureCount = maxFailureCount;
        this.limit = limit;
    }

    public long getSamplingWindow() {
        return samplingWindow;
    }

    public long getLimit() {
        return limit;
    }

    public long getMaxFailureCount() {
        return maxFailureCount;
    }

    public String getType() {
        return this.FAILURE_CONDITION_LATENCY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LatencyFailure that = (LatencyFailure)o;
        if(this.samplingWindow != that.samplingWindow) return false;
        if(this.maxFailureCount != that.maxFailureCount) return false;
        if(this.limit != that.limit) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 29 * result + new Long(samplingWindow).intValue();
        result = 29 * result + new Long(maxFailureCount).intValue();
        result = 29 * result + new Long(limit).intValue();
        return result;
    }
}
