package com.l7tech.server.sla;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import org.springframework.transaction.annotation.Transactional;

/**
 * An interface for managers that provides access to counters used at runtime by ServerThroughputQuota assertions.
 *
 * @author flascelles@layer7-tech.com
 */
public interface CounterManager {
    /**
     * Check if a counter exists or not.  If it does not exist, then create a new counter with a given counter name.
     * @param counterName: used to check if the database has such counter whose name is counterName.
     * @throws com.l7tech.objectmodel.ObjectModelException : thrown when data access errors occur.
     */
    void checkOrCreateCounter(String counterName) throws ObjectModelException;

    @Transactional(readOnly=true)
    String[] getAllCounterNames() throws FindException;

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
     * Decrement the counter.
     */
    public void decrement(String counterName);

    public class LimitAlreadyReachedException extends Exception {
        public LimitAlreadyReachedException(String msg) {
            super(msg);
        }
    }
}
