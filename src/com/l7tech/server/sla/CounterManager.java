package com.l7tech.server.sla;

/**
 * An interface for managers that provides access to counters used at runtime by ServerThroughputQuota assertions.
 *
 * @author flascelles@layer7-tech.com
 */
public interface CounterManager {

    /**
     * Increment the counter identified by counterId only if the resulting value of the counter for
     * the passed fieldOfInterest will not exceed the passed limit.
     *
     * @param counterId the id of the counter as provided by CounterIDManager
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest if incremented. if the limit is already reached, an exceptio is thrown
     * @throws com.l7tech.server.sla.CounterCache.LimitAlreadyReachedException if the limit was already reached
     */
    public long incrementOnlyWithinLimitAndReturnValue(long counterId,
                                                       long timestamp,
                                                       int fieldOfInterest,
                                                       long limit) throws CounterCache.LimitAlreadyReachedException;

    /**
     * Increment this counter, and return the specific value of interest
     * @param counterId the id of the counter as provided by CounterIDManager
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest
     */
    public long incrementAndReturnValue(long counterId, long timestamp, int fieldOfInterest);

    /**
     * Decrement the counter.
     */
    public void decrement(long counterId);

    public class LimitAlreadyReachedException extends Exception {
        public LimitAlreadyReachedException(String msg) {
            super(msg);
        }
    };
}
