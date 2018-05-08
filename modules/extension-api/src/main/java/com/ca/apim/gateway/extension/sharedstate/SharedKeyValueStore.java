package com.ca.apim.gateway.extension.sharedstate;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A SharedKeyValueStore provides thread safe and atomicity guarantees on operations on the store
 */
public interface SharedKeyValueStore<K extends Serializable, V extends Serializable> {

    /**
     * Returns the value for the specified key, or null if the key value store does not contain this key
     * @param key
     * @return
     */
    V get(K key);

    /**
     * Clear all entries in the key value store
     */
    void clear();

    /**
     * Returns true if this key value store contains no key-value mappings
     * @return
     */
    boolean isEmpty();

    /**
     * Removes the mapping for a key from this key value store if it is present
     * @param key
     * @return the removed value or null if the key value store does not contain this key
     */
    V remove(K key);

    /**
     * Associates the specified value with the specified key in this key value store
     * @param key
     * @param value
     * @return
     */
    V put(K key, V value);

    /**
     * Returns true if this key value store contains a mapping for the specified key
     * @param key
     * @return
     */
    boolean containsKey(K key);

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or null if there is no current mapping)
     * @param key
     * @param remappingFunction
     * @return
     */
    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * If the specified key is not already associated with a value (or is mapped to null), attempts to compute its value using the given mapping function and enters it into this map unless null.
     * @param key
     * @param mappingFunction
     * @return
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    /**
     * If the value for the specified key is present and non-null, attempts to compute a new mapping given the key and its current mapped value.
     * @param key
     * @param remappingFunction
     * @return
     */
    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);
}
