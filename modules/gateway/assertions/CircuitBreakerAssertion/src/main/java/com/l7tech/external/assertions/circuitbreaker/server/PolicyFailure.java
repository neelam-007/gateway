package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * Represents policy execution Failure configuration.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
final public class PolicyFailure implements FailureCondition {
    private final long samplingWindow;
    private final long maxFailureCount;
    public static final String FAILURE_CONDITION_POLICY = "POLICY_FAILURE";

    public PolicyFailure(final long samplingWindow,
                         final long MaxFailureCount) {

        this.samplingWindow = samplingWindow;
        this.maxFailureCount = MaxFailureCount;
    }

    public long getSamplingWindow() {
        return samplingWindow;
    }

    public long getMaxFailureCount() {
        return maxFailureCount;
    }

    public String getType() {
        return this.FAILURE_CONDITION_POLICY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PolicyFailure that = (PolicyFailure)o;
        if(this.samplingWindow != that.samplingWindow)     return false;
        if(this.maxFailureCount != that.maxFailureCount)     return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 29 * result + new Long(samplingWindow).intValue();
        result = 29 * result + new Long(maxFailureCount).intValue();
        return result;
    }
}
