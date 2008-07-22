package com.l7tech.util;

/**
 * Generic resolver interface.
 *
 * @author Steve Jones
 */
public interface Resolver<K,V> {

    /**
     * Resolve a value for the given key.
     *
     * @param key The key to resolve
     * @return the value or null if not found
     */
    V resolve(K key);
}
