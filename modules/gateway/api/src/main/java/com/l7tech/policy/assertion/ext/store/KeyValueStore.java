package com.l7tech.policy.assertion.ext.store;

import java.util.Map;

/**
 * Provides methods to access a key value store.
 *
 * Usage:
 * =============================================================================
 * To save, update, delete key value pair from the key value store, the key must be
 * prefixed with a value that is unique to your Custom Assertion.
 * For example,
 *    keyValueStoreServices.contains("<prefix>"+"<key>");
 *    keyValueStoreServices.save("<prefix>"+"<key>", value);
 *    keyValueStoreServices.update("<prefix>"+"<key>", value);
 *    keyValueStoreServices.delete("<prefix>"+"<key>");
 *
 * To retrieve all key value pairs for your Custom Assertion, search using the prefix.
 * For example,
 *    keyValueStoreServices.findAllWithKeyPrefix("<prefix>");
 *
 * Is is the responsibility of the Custom Assertions developer to serialize and
 * de-serialize the object they want to store/retrieve from the key value store.
 */
public interface KeyValueStore {
    /**
     * Finds all key values with specified key prefix.
     *
     * @param keyPrefix the key prefix to look up
     * @return a map of key values. Never null but may be empty.
     * @throws KeyValueStoreException if there is an error
     */
    Map<String, byte[]> findAllWithKeyPrefix(String keyPrefix) throws KeyValueStoreException;

    /**
     * Finds the value for the specified key, or return null if not found.
     *
     * @param key the key to look up
     * @return the value, or null if not found
     * @throws KeyValueStoreException if there is an error
     */
    byte[] get(String key) throws KeyValueStoreException;

    /**
     * Checks whether the specified key is in the key value store.
     *
     * @param key the key to check
     * @return whether the specified key is in the key value store
     * @throws KeyValueStoreException if there is an error
     */
    boolean contains(String key) throws KeyValueStoreException;

    /**
     * Saves a new key value in the key value store.
     *
     * @param key the key to save
     * @param value the value to update
     * @throws KeyValueStoreException if the key already exist, or if there is an error
     */
    void save(String key, byte[] value) throws KeyValueStoreException;

    /**
     *  Updates an existing key value in the key value store.
     *
     * @param key the key to update
     * @param value the value to update
     * @throws KeyValueStoreException if the key is not found, or if there is an error
     */
    void update(String key, byte[] value) throws KeyValueStoreException;

    /**
     * Saves or updates the key value in the key value store
     *
     * @param key the key to save or update
     * @param value the value to save or update
     * @throws KeyValueStoreException if there is an error
     */
    void saveOrUpdate(String key, byte[] value) throws KeyValueStoreException;

    /**
     * Deletes the specified key value from the key value store. If the key is not
     * found, then the key is not deleted.
     *
     * @param key the key to delete
     * @throws KeyValueStoreException if there is an error
     */
    void delete(String key) throws KeyValueStoreException;
}