package com.ca.apim.gateway.extension.sharedstate;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * A SharedCounterStore provides thread safe and atomicity guarantees on operation of counters it holds
 */
public interface SharedCounterStore<K extends Serializable> {
    /**
     * Initialize the counter with the initialValue only if it does not exist
     * @param key the key to the counter
     * @param initialValue the initial value
     */
    void init(K key, Long initialValue);

    /**
     * Get the current value associated of the specified counter
     * @param key the key to the counter
     * @return the current value
     * @throws NoSuchElementException if counter does not yet exist
     */
    Long get(K key);

    /**
     * Atomically increments the specified counter value by one
     * @param key the key to the counter
     * @return the previous value
     * @throws NoSuchElementException if counter does not yet exist
     */
    Long getAndIncrement(K key);

    /**
     * Atomically increments the specified counter value by one
     * @param key the key to the counter
     * @return the updated value
     * @throws NoSuchElementException if counter does not yet exist
     */
    Long incrementAndGet(K key);

    /**
     * Atomically updates the specified counter value with the results of applying the given function,
     * returning the previous value
     * @param key the key to the counter
     * @param function a side-effect-free function
     * @return the previous value
     * @throws NoSuchElementException if counter does not yet exist
     */
    Long getAndUpdate(K key, LongUnaryOperator function);

    /**
     * Atomically updates the specified counter value with the results of applying the given function,
     * returning the updated value
     * @param key the key to the counter
     * @param function a side-effect-free function
     * @return the updated value
     * @throws NoSuchElementException if counter does not yet exist
     */
    Long updateAndGet(K key, LongUnaryOperator function);

    /**
     * Removes the specified counter
     * @param key the key to the counter
     */
    void remove(K key);
}
