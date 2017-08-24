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

    public CircuitConfig(final String trackerID, final int recoveryPeriod,
                         final FailureCondition failureCondition) {

        this.trackerId = trackerID;
        this.recoveryPeriod = recoveryPeriod;
        this.failureCondition = failureCondition;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getRecoveryPeriod() {
        return recoveryPeriod;
    }

    public FailureCondition getFailureCondition() {
        return failureCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CircuitConfig that = (CircuitConfig)o;
        if (trackerId != null ? !trackerId.equals(that.trackerId) : that.trackerId != null) return false;
        if(this.recoveryPeriod != that.recoveryPeriod)     return false;
        if( !this.failureCondition.equals(that.failureCondition)) return false;

        return true;
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
