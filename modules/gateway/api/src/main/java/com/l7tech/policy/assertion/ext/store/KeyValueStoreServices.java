package com.l7tech.policy.assertion.ext.store;

/**
 * Service for getting key value stores.
 */
public interface KeyValueStoreServices {

    static final String INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME = "internalTransactional";

    /**
     * Gets the default key value store. The default key value store is the internalTransactional store.
     *
     * @return the default key value store
     * @throws KeyValueStoreException if the default key value store is not found
     */
    KeyValueStore getKeyValueStore() throws KeyValueStoreException;

    /**
     * Gets the key value store with the specified name.
     *
     * @param name the name of the key value store
     * @return the key value store with the specified name
     * @throws KeyValueStoreException if the key value store with the specified name is not found
     */
    KeyValueStore getKeyValueStore(String name) throws KeyValueStoreException;
}