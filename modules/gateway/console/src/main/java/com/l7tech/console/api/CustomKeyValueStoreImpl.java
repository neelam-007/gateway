package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreListener;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link com.l7tech.policy.assertion.ext.store.KeyValueStore} interface.
 */
public class CustomKeyValueStoreImpl implements KeyValueStore {

    @Override
    public Map<String, byte[]> findAllWithKeyPrefix(String keyPrefix) throws KeyValueStoreException {
        try {
            Collection<CustomKeyValueStore> customKeyValues = Registry.getDefault().getCustomKeyValueStoreAdmin().findByKeyPrefix(keyPrefix);
            Map<String, byte[]> result = new HashMap<>(customKeyValues.size());
            for (CustomKeyValueStore customKeyValue : customKeyValues) {
                result.put(customKeyValue.getName(), customKeyValue.getValue());
            }
            return result;
        } catch (FindException e) {
            throw new KeyValueStoreException("Unable to find all: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public byte[] get(String key) throws KeyValueStoreException {
        try {
            CustomKeyValueStore customKeyValue = Registry.getDefault().getCustomKeyValueStoreAdmin().findByUniqueKey(key);
            if (customKeyValue != null) {
                return customKeyValue.getValue();
            }
            return null;
        } catch (FindException e) {
            throw new KeyValueStoreException("Unable to get: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public boolean contains(String key) throws KeyValueStoreException {
        try {
            return Registry.getDefault().getCustomKeyValueStoreAdmin().findByUniqueKey(key) != null;
        } catch (FindException e) {
            throw new KeyValueStoreException("Unable to check if key exists: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void save(String key, byte[] value) throws KeyValueStoreException {
        try {
            if (this.contains(key)) {
                throw new KeyValueStoreException("Unable to save. The key already exists: " + key);
            }

            CustomKeyValueStore customKeyValue = new CustomKeyValueStore();
            customKeyValue.setName(key);
            customKeyValue.setValue(value);
            Registry.getDefault().getCustomKeyValueStoreAdmin().saveCustomKeyValue(customKeyValue);
        } catch (SaveException e) {
            throw new KeyValueStoreException("Unable to save: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void update(String key, byte[] value) throws KeyValueStoreException {
        try {
            CustomKeyValueStore customKeyValue = Registry.getDefault().getCustomKeyValueStoreAdmin().findByUniqueKey(key);
            if (customKeyValue == null) {
                throw new KeyValueStoreException("Unable to update. The key does not exist: " + key);
            }

            customKeyValue.setValue(value);
            Registry.getDefault().getCustomKeyValueStoreAdmin().updateCustomKeyValue(customKeyValue);
        } catch (FindException | UpdateException e) {
            throw new KeyValueStoreException("Unable to update: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void saveOrUpdate(String key, byte[] value) throws KeyValueStoreException {
        if (!this.contains(key)) {
            this.save(key, value);
        } else {
            this.update(key, value);
        }
    }

    @Override
    public void delete(String key) throws KeyValueStoreException {
        try {
            Registry.getDefault().getCustomKeyValueStoreAdmin().deleteCustomKeyValue(key);
        } catch (DeleteException e) {
            throw new KeyValueStoreException("Unable to delete: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public <L extends KeyValueStoreListener> L getListener(Class<L> lClass) {
        // Key value store listener not supported in the SSM.
        //
        return null;
    }
}