package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * Represents policy execution Failure configuration.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
final public class PolicyFailure implements FailureCondition {
    private static final String FAILURE_CONDITION_POLICY = "Policy Failure";

    private final int samplingWindow;
    private final int maxFailureCount;

    PolicyFailure (final int samplingWindow,
                  final int MaxFailureCount) {

        this.samplingWindow = samplingWindow;
        this.maxFailureCount = MaxFailureCount;
    }

    public int getSamplingWindow() {
        return samplingWindow;
    }

    public int getMaxFailureCount() {
        return maxFailureCount;
    }

    public String getType() {
        return FAILURE_CONDITION_POLICY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PolicyFailure that = (PolicyFailure) o;
        return this.samplingWindow == that.samplingWindow && this.maxFailureCount == that.maxFailureCount;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 29 * result + samplingWindow;
        result = 29 * result + maxFailureCount;
        return result;
    }
}
