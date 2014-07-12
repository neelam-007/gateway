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
     * Ensure that a counter with the specified name exists in the database and is cached on this node.
     *
     * @param counterName: used to check if the database has such counter whose name is counterName.
     * @throws com.l7tech.objectmodel.ObjectModelException : thrown when data access errors occur.
     */
    void ensureCounterExists(@NotNull String counterName) throws ObjectModelException;

    /**
     * Increment the counter identified by objectId only if the resulting value of the counter for
     * the passed fieldOfInterest will not exceed the passed limit.
     * <p/>
     * If synchronous is false, this method may return without throwing LimitAlreadyReachedException even
     * though the DB increment will later be rejected (if other threads took our place).
     *
     * @param synchronous true if the increment should be done synchronously, in a new transaction.
     *                    false if it is OK to schedule the DB increment to occur later in a batch.
     * @param readSynchronous true if the ready should be done synchronously, false if use caching
     * @param counterName the name of the counter used for counter lookup
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @param incrementValue the value to increment the counter by
     * @return the counter value of interest if incremented. if the limit is already reached, an exceptio is thrown
     * @throws com.l7tech.server.sla.CounterManager.LimitAlreadyReachedException if the limit was already reached
     */
    public long incrementOnlyWithinLimitAndReturnValue(boolean synchronous,
                                                       boolean readSynchronous,
                                                       String counterName,
                                                       long timestamp,
                                                       int fieldOfInterest,
                                                       long limit,
                                                       int incrementValue) throws CounterManager.LimitAlreadyReachedException;

    /**
     * Increment this counter by X value, and return the specific value of interest
     * <p/>
     * If synchronous is false, this method may return without throwing LimitAlreadyReachedException even
     * though the DB increment will later be rejected (if other threads took our place).
     *
     * @param synchronous true if the increment should be done synchronously, in a new transaction.
     *                    false if it is OK to schedule the DB increment to occur later in a batch.
     * @param readSynchronous true if the ready should be done synchronously, false if use caching
     * @param counterName the name of the counter used for counter lookup
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @param value the value to increment the counter by
     * @return the counter value of interest
     */
    public long incrementAndReturnValue(boolean synchronous, boolean readSynchronous, String counterName, long timestamp, int fieldOfInterest, int value);

    /**
     * get a current counter value without incrementing anything
     *
     * @param readSynchronous true if the ready should be done synchronously, false if use caching
     */
    public long getCounterValue(boolean readSynchronous, String counterName, int fieldOfInterest);

    /**
     * Get a snapshot of all current counter values without incrementing anything.
     *
     * @param counterName the counter name to query.  Required.
     * @return a CounterInfo describing the current state (in the database) of the specified counter, or null if information was not found.
     */
    public CounterInfo getCounterInfo(final @NotNull String counterName);

    /**
     * Decrement the counter.
     * @param synchronous true if the decrement should be done synchronously, in a new transaction.
     *                    false if it is OK to schedule the DB decrement to occur later in a batch.
     * @param counterName the name of the counter used for counter lookup
     * @param decrementValue the value to decrement by
     * @parma timestamp the time for which this decrement should be recorded at
     */
    public void decrement(boolean synchronous, String counterName, int decrementValue, long timestamp);

    /**
     * Reset the counter synchronously.
     *
     * @param counterName the name of the counter used for counter lookup
     */
    public void reset(String counterName);

    public class LimitAlreadyReachedException extends Exception {
        public LimitAlreadyReachedException(String msg) {
            super(msg);
        }
    }
}
