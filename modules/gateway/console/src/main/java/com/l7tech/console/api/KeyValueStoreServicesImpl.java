package com.l7tech.console.api;

import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;

/**
 * Implementation of {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreServices} interface.
 */
public class KeyValueStoreServicesImpl implements KeyValueStoreServices {

    private static final CustomKeyValueStoreImpl customKeyValueStore = new CustomKeyValueStoreImpl();

    @Override
    public KeyValueStore getKeyValueStore() throws KeyValueStoreException {
        return this.getKeyValueStore(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
    }

    @Override
    public KeyValueStore getKeyValueStore(String name) throws KeyValueStoreException {
        if (!KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME.equals(name)) {
            throw new KeyValueStoreException("No key value store available with name: " + name);
        }
        return customKeyValueStore;
    }
}