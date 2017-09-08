package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * Maintains a history of events where a failure condition was detected by the Circuit Breaker Assertion.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
final public class CircuitConfig {
    private final String trackerId;
    private final int recoveryPeriod;
    private final FailureCondition failureCondition;

    CircuitConfig(final String trackerID, final int recoveryPeriod,
                  final FailureCondition failureCondition) {

        this.trackerId = trackerID;
        this.recoveryPeriod = recoveryPeriod;
        this.failureCondition = failureCondition;
    }

    String getTrackerId() {
        return trackerId;
    }

    int getRecoveryPeriod() {
        return recoveryPeriod;
    }

    FailureCondition getFailureCondition() {
        return failureCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CircuitConfig that = (CircuitConfig) o;
        return (trackerId != null ? trackerId.equals(that.trackerId) : that.trackerId == null) &&
                this.recoveryPeriod == that.recoveryPeriod &&
                this.failureCondition.equals(that.failureCondition);
    }

    @Override
    public int hashCode() {
        int result;
        result = (trackerId != null ? trackerId.hashCode() : 0);
        result = 29 * result + recoveryPeriod;
        result = 29 * result + failureCondition.hashCode();
        return result;
    }
}
