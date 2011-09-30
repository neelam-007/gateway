package com.l7tech.server.sla;

import com.l7tech.objectmodel.ObjectModelException;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for managers that provides access to counters used at runtime by ServerThroughputQuota assertions.
 *
 * @author flascelles@layer7-tech.com
 */
public interface CounterManager {
    /**
     * Check if a counter exists or not.  If it does not exist, then optionally create a new counter with a given counter name.
     * @param counterName: used to check if the database has such counter whose name is counterName.
     * @param create true if counter should be created if it doesn't already exist.
     * @throws com.l7tech.objectmodel.ObjectModelException : thrown when data access errors occur.
     * @return true iff. the counter already existed when this method was called.
     */
    boolean checkOrCreateCounter(@NotNull String counterName, boolean create) throws ObjectModelException;

    /**
     * Increment the counter identified by objectId only if the resulting value of the counter for
     * the passed fieldOfInterest will not exceed the passed limit.
     *
     * @param counterName the name of the counter used for counter lookup
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest if incremented. if the limit is already reached, an exceptio is thrown
     * @throws com.l7tech.server.sla.CounterManager.LimitAlreadyReachedException if the limit was already reached
     */
    public long incrementOnlyWithinLimitAndReturnValue(String counterName,
                                                       long timestamp,
                                                       int fieldOfInterest,
                                                       long limit) throws CounterManager.LimitAlreadyReachedException;

    /**
     * Increment this counter, and return the specific value of interest
     * @param counterName the name of the counter used for counter lookup
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest
     */
    public long incrementAndReturnValue(String counterName, long timestamp, int fieldOfInterest);

    /**
     * get a current counter value without incrementing anything
     */ 
    public long getCounterValue(String counterName, int fieldOfInterest);

    /**
     * Get a snapshot of all current counter values without incrementing anything.
     *
     * @param counterName the counter name to query.  Required.
     * @return a CounterInfo describing the current state (in the database) of the specified counter, or null if information was not found.
     */
    public CounterInfo getCounterInfo(final @NotNull String counterName);

    /**
     * Decrement the counter.
     */
    public void decrement(String counterName);

    public class LimitAlreadyReachedException extends Exception {
        public LimitAlreadyReachedException(String msg) {
            super(msg);
        }
    }
}
