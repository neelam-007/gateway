package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for {@link com.l7tech.policy.CustomKeyValueStore}.
 */
public interface CustomKeyValueStoreManager extends GoidEntityManager<CustomKeyValueStore,EntityHeader> {

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
}