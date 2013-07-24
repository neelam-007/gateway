package com.l7tech.server.store;

import com.l7tech.gateway.common.admin.CustomKeyValueStoreAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.*;

/**
 */
public class CustomKeyValueStoreAdminImpl implements CustomKeyValueStoreAdmin {
    @Inject
    private CustomKeyValueStoreManager customKeyValueStoreManager;

    @NotNull
    @Override
    public Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull String keyPrefix) throws FindException {
        return customKeyValueStoreManager.findByKeyPrefix(keyPrefix);
    }

    @Nullable
    @Override
    public CustomKeyValueStore findByUniqueKey(@NotNull String key) throws FindException {
        return customKeyValueStoreManager.findByUniqueName(key);
    }

    @Override
    public void saveCustomKeyValue(@NotNull CustomKeyValueStore customKeyValue) throws SaveException {
        customKeyValueStoreManager.save(customKeyValue);
    }

    @Override
    public void updateCustomKeyValue(@NotNull CustomKeyValueStore customKeyValue) throws UpdateException {
        customKeyValueStoreManager.update(customKeyValue);
    }

    @Override
    public void deleteCustomKeyValue(@NotNull String key) throws DeleteException {
        customKeyValueStoreManager.deleteByKey(key);
    }
}