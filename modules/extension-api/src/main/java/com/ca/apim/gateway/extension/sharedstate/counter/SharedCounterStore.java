package com.ca.apim.gateway.extension.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;

import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * A SharedCounterStore provides thread safe and atomicity guarantees on operation of counters it holds
 * Currently, an update operation should consider update 5 long values inside counter object
 */
public interface SharedCounterStore {

    /**
     * Initialize the counter store
     */
    void init();

    /**
     * Get the detailed information of the given counter name.
     * If there are no counter mapping to the given counter name, return null
     *
     * @param name the counter name
     * @return the the detailed all time unit information of the counter
     */
    SharedCounterState query(String name);

    /**
     * Get the detailed information of the given counter name.
     * If there are no counter mapping to the given counter name, a new counter will be created.
     *
     * @param name the counter name
     * @return the the detailed all time unit information of the counter
     */
    SharedCounterState get(String name);

    /**
     * Get the current value associated of the specified counter
     * @param name the counter name
     * @param counterOperationProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @return the current value of interest
     * @throws NoSuchElementException if counter does not yet exist
     */
    long get(String name, Properties counterOperationProperties, CounterFieldOfInterest fieldOfInterest);

    /**
     * Atomically updates the specified counter value by the given delta
     * returning the previous value
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @return the previous value of interest
     * @throws NoSuchElementException if counter does not yet exist
     */
    long getAndUpdate(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);

    /**
     * Atomically updates the specified counter value with the given value within quota
     * returning the previous value
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
     * @return the previous value of interest
     * @throws NoSuchElementException if counter does not yet exist
     */
    long getAndUpdate(
            String name,
            Properties counterOperationsProperties,
            CounterFieldOfInterest fieldOfInterest,
            long timestamp,
            int delta,
            long limit) throws CounterLimitReachedException;

    /**
     * Atomically updates the specified counter value with the given value
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @throws NoSuchElementException if counter does not yet exist
     */
    void update(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);
    
    /**
     * Atomically updates the specified counter value with the given value within quota
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
     * @throws NoSuchElementException if counter does not yet exist
     */
    void update(
            String name,
            Properties counterOperationsProperties,
            CounterFieldOfInterest fieldOfInterest,
            long timestamp,
            int delta,
            long limit) throws CounterLimitReachedException;

    /**
     * Atomically updates the specified counter value with the given value
     * returning the updated value
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @return the updated value
     * @throws NoSuchElementException if counter does not yet exist
     */
    long updateAndGet(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);
    
    /**
     * Atomically updates the specified counter value with the given value within quota
     * returning the updated value
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
     * @return the updated value
     * @throws NoSuchElementException if counter does not yet exist
     */
    long updateAndGet(
            String name,
            Properties counterOperationsProperties,
            CounterFieldOfInterest fieldOfInterest,
            long timestamp,
            int delta,
            long limit) throws CounterLimitReachedException;

    /**
     * Atomically reset the counter to zero
     * @param name the counter name
     * @throws NoSuchElementException if counter does not yet exist
     */
    void reset(String name);
}
