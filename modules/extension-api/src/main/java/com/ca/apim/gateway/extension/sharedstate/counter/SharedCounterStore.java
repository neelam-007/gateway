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
     * If the counter does not exist it will be NOT be created
     * @param name the counter name
     * @return the the detailed all time unit information of the counter
     */
    SharedCounterState query(String name);

    /**
     * Get the detailed information of the given counter name.
     * If the counter does not exist it will be created
     * @param name the counter name
     * @return the the detailed all time unit information of the counter
     */
    SharedCounterState get(String name);

    /**
     * Get the current value associated of the specified counter.
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @return the current value of interest
     */
    long get(String name, Properties counterOperationProperties, CounterFieldOfInterest fieldOfInterest);

    /**
     * Atomically updates the specified counter value by the given delta
     * returning the previous value
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @return the previous value of interest
     */
    long getAndUpdate(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);

    /**
     * Atomically updates the specified counter value with the given value within quota
     * returning the previous value
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
     * @return the previous value of interest
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
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     */
    void update(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);
    
    /**
     * Atomically updates the specified counter value with the given value within quota
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
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
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @return the updated value
     */
    long updateAndGet(String name, Properties counterOperationsProperties, CounterFieldOfInterest fieldOfInterest, long timestamp, int delta);
    
    /**
     * Atomically updates the specified counter value with the given value within quota
     * returning the updated value
     * If the counter does not exist it will be created
     * @param name the counter name
     * @param counterOperationsProperties sync or async configuration get passed in
     * @param fieldOfInterest the key of time unit of the counter
     * @param timestamp the passed in timestamp of current operation time
     * @param delta the change value
     * @param limit the quota limit for the counter
     * @return the updated value
     */
    long updateAndGet(
            String name,
            Properties counterOperationsProperties,
            CounterFieldOfInterest fieldOfInterest,
            long timestamp,
            int delta,
            long limit) throws CounterLimitReachedException;

    /**
     * Atomically reset the counter to zero if it exists
     * If the counter does not exist, it may or may not be created by implementors.
     * @param name the counter name
     */
    void reset(String name);
}
