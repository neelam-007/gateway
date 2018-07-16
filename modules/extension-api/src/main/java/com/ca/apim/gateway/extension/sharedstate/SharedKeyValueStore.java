package com.ca.apim.gateway.extension.sharedstate;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A SharedKeyValueStore provides a way for the gateway to store values indexed by a key, and possibly sharing these information
 * across a cluster.
 * Also provides thread safe and atomicity guarantees on operations on the keys and values.
 */
public interface SharedKeyValueStore<K extends Serializable, V extends Serializable> {

    /**
     * Returns the value for the specified key, or null if the key value store does not contain this key.
     *
     * @param key Key to be searched
     * @return value associated with the specified key, or null if not present
     */
    V get(K key);

    /**
     * Clear all entries in the key value store.
     */
    void clear();

    /**
     * @return true if this key value store contains no key-value mappings, false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes the mapping for a key from this key value store if it is present.
     *
     * @param key the key to be removed
     * @return the removed value or null if the key value store does not contain this key
     */
    V remove(K key);

    /**
     * Removes the mapping for a key from this key value store if it is present. In a distributed environment,
     * expect this to be more performant than {@link SharedKeyValueStore#remove(Serializable)}.
     *
     * @param key the key to be deleted
     */
    void delete(K key);

    /**
     * Associates the specified value with the specified key in this key value store.
     * In a distributed environment this is slower than {@link SharedKeyValueStore#set(Serializable, Serializable)}
     *
     * @param key the key to be added
     * @param value the value associated with the key
     * @return old value of the entry
     */
    V put(K key, V value);

    /**
     * Associates the specified value with the specified key in this key value store.
     * In a distributed environment this is slower than {@link SharedKeyValueStore#set(Serializable, Serializable, long, TimeUnit)}
     * If this key is already present in the map, the previous value and the previous time to live will be overwritten
     *
     * @param key the key to be added
     * @param value the value associated with the key
     * @param ttl maximum time for this entry to stay in the map (0 means infinite, negative means map default)
     * @param timeUnit time unit for the TTL
     * @return old value of the entry
     * @throws NullPointerException if the specified key or value is null
     */
    V put(K key, V value, long ttl, TimeUnit timeUnit);

    /**
     * Associates the specified value with the key in the key-value store if and only if the condition function evaluates to true
     * The condition function takes the previous value associated with that key in the key-value store (or null if none exists) and produces a boolean
     *
     * @param key the key to be added
     * @param value the value associated with the key
     * @param condition a function operating on the previous value associated with this key
     * @param ttl maximum time for this entry to stay in the map (0 means infinite, negative means map default)
     * @param timeUnit time unit for the TTL
     * @return true if condition evaluated to true & the put operation was performed, false otherwise
     */
    boolean putIfCondition(K key, V value, Function<V, Boolean> condition, long ttl, TimeUnit timeUnit);

    /**
     * Associates the specified value with the specified key in this key value store.
     * Does not return the previous value.
     *
     * Note that in a distributed environment, this will generally be faster
     * than {@link SharedKeyValueStore#put(Serializable, Serializable)}.
     *
     * @param key the key to be set
     * @param value the value associated with the key
     */
    void set(K key, V value);

    /**
     * Associates the specified value with the specified key in this key value store.
     * Does not return the previous value.
     *
     * Note that in a distributed environment, this will generally be faster
     * than {@link SharedKeyValueStore#put(Serializable, Serializable, long, TimeUnit)}
     *
     * @param key the key to be set
     * @param value the value associated with the key
     * @param ttl maximum time for this entry to stay in the map (0 means infinite, negative means map default)
     * @param timeUnit time unit for the TTL
     */
    void set(K key, V value, long ttl, TimeUnit timeUnit);

    /**
     * Check if the store contains a mapping for the key specified.
     *
     * @param key key to be checked
     * @return true if this key value store contains a mapping for the specified key, false otherwise
     */
    boolean containsKey(K key);

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or null if there is no current mapping).
     *
     * @param key key to be computed
     * @param remappingFunction function that provides the value for the key
     * @return old value of the entry
     */
    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * If the specified key is not already associated with a value (or is mapped to null), attempts to compute its value using the given mapping function and enters it into this map unless null.
     *
     * @param key key to be computed
     * @param mappingFunction function that provides the value for the key
     * @return current value of the entry if present
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    /**
     * If the value for the specified key is present and non-null, attempts to compute a new mapping given the key and its current mapped value.

     * @param key key to be computed
     * @param remappingFunction function that provides the value for the key
     * @return old value of the entry, null if there was no value for the key
     */
    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * @return the map name as a non-null String.
     */
    String getName();

}
