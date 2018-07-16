package com.ca.apim.gateway.extension.sharedstate.counter;

import java.util.Date;

/**
 * SharedCounterState provides an immutable snapshot of current counter state
 */
public interface SharedCounterState {
    /**
     * Get the counter name
     * @return the counter name
     */
    public String getName();

    /**
     * Get the count in the last time interval as the provided time field.
     * @return the counter value for the time field
     */
    public long getCount(CounterFieldOfInterest field);

    /**
     * Get the last update time for the counter
     * @return the last update timestamp
     */
    public Date getLastUpdate();
}
