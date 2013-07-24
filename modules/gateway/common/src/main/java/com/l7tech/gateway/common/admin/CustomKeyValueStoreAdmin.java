package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.CUSTOM_KEY_VALUE_STORE;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing ${@link com.l7tech.policy.CustomKeyValueStore} instances
 * and related configuration on the Gateway.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=CUSTOM_KEY_VALUE_STORE)
@Administrative
public interface CustomKeyValueStoreAdmin {

    /**
     *  Finds all {@link CustomKeyValueStore} with specified key prefix.
     *
     * @param keyPrefix the key prefix to look up
     * @return a collection of {@link CustomKeyValueStore}. Never null but may be empty.
     * @throws FindException if there is an error
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull String keyPrefix) throws FindException;

    /**
     * Finds {@link CustomKeyValueStore} by its key, or returns null if not found.
     *
     * @param key the key to look up
     * @return the {@link CustomKeyValueStore}, or null if not found
     * @throws FindException if there is an error
     */
    @Nullable
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    CustomKeyValueStore findByUniqueKey(@NotNull String key) throws FindException;

    /**
     * Saves a new {@link CustomKeyValueStore} in the key value store.
     *
     * @param customKeyValue the {@link CustomKeyValueStore} to save
     * @throws SaveException if there is an error
     */
    @Secured(stereotype=SAVE)
    void saveCustomKeyValue(@NotNull CustomKeyValueStore customKeyValue) throws SaveException;

    /**
     * Updates an existing {@link CustomKeyValueStore} in the key value store.
     *
     * @param customKeyValue the {@link CustomKeyValueStore} to update
     * @throws UpdateException if there is an error
     */
    @Secured(stereotype=UPDATE)
    void updateCustomKeyValue(@NotNull CustomKeyValueStore customKeyValue) throws UpdateException;

    /**
     * Deletes {@link CustomKeyValueStore} with the specified key from the key value store.
     *
     * @param key the key to delete
     * @throws DeleteException if there is an error
     */
    @Secured(stereotype=DELETE_BY_UNIQUE_ATTRIBUTE)
    void deleteCustomKeyValue(@NotNull String key) throws DeleteException;
}