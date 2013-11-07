package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for {@link com.l7tech.policy.CustomKeyValueStore}.
 */
public interface CustomKeyValueStoreManager extends EntityManager<CustomKeyValueStore,EntityHeader> {

    /**
     * Finds all {@link CustomKeyValueStore} with specified key prefix.
     *
     * @param keyPrefix the key prefix to look up
     * @return a collection of {@link CustomKeyValueStore}. Never null but may be empty.
     * @throws FindException if there is an error
     */
    @NotNull
    Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull String keyPrefix) throws FindException;

    /**
     * Deletes {@link CustomKeyValueStore} with the specified key from the key value store.
     *
     * @param key the key to delete
     * @throws DeleteException if there is an error
     */
    void deleteByKey(@NotNull String key) throws DeleteException;

    /**
     * Adds a {@link KeyValueStoreChangeEventListener}.<p>
     * The {@link KeyValueStoreChangeEventListener} will receive a list of {@link KeyValueStoreChangeEventListener.Event Events}
     * when key values with give key prefix have been modified.
     *
     * @param keyPrefix the key prefix
     * @param listener the listener to be notified
     */
    void addListener(String keyPrefix, KeyValueStoreChangeEventListener listener);

    /**
     * Removes a {@link KeyValueStoreChangeEventListener} that has been added with the specified key prefix.
     *
     * @param keyPrefix the key prefix
     * @param listener the listener to remove
     */
    void removeListener(String keyPrefix, KeyValueStoreChangeEventListener listener);
}