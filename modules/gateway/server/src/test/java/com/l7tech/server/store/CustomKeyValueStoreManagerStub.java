package com.l7tech.server.store;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomKeyValueStoreManagerStub extends EntityManagerStub<CustomKeyValueStore, EntityHeader> implements CustomKeyValueStoreManager {

    private final KeyValueStoreChangeEventListener listener = new KeyValueStoreChangeEventListenerStub();

    public CustomKeyValueStoreManagerStub(CustomKeyValueStore... customKeyValueStoresIn) {
        super(customKeyValueStoresIn);
    }

    @NotNull
    @Override
    public Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull String keyPrefix) throws FindException {
        Collection<CustomKeyValueStore> got = findAll();
        List<CustomKeyValueStore> ret = new ArrayList<>();
        for (CustomKeyValueStore keyValue : got) {
            if (keyValue.getName().startsWith(keyPrefix)) {
                ret.add(keyValue);
            }
        }
        return ret;
    }

    @Override
    public void deleteByKey(@NotNull String key) throws DeleteException {
        try {
            Collection<CustomKeyValueStore> got = findAll();
            for (CustomKeyValueStore keyValue : got) {
                if (keyValue.getName().equals(key)) {
                    entities.remove(keyValue.getGoid());
                }
            }
        } catch (FindException e) {
            throw new DeleteException("Unable to delete using key: " + key, e);
        }
    }

    @Override
    public <T> T getListener(Class<T> lClass) {
        if (KeyValueStoreChangeEventListener.class.equals(lClass)) {
            //noinspection unchecked
            return (T) listener;
        }

        return null;
    }

    @Override
    public Class<? extends PersistentEntityImp> getImpClass() {
        return CustomKeyValueStore.class;
    }

    private class KeyValueStoreChangeEventListenerStub extends KeyValueStoreChangeEventListener {
        private final Map<String, Set<EventCallback>> eventCallbacks = new ConcurrentHashMap<>();

        @Override
        public void add(String keyPrefix, EventCallback eventCallback) {
            Set<EventCallback> keyPrefixEventCallbacks = eventCallbacks.get(keyPrefix);
            if (keyPrefixEventCallbacks == null) {
                keyPrefixEventCallbacks = new HashSet<>();
                eventCallbacks.put(keyPrefix, keyPrefixEventCallbacks);
            }

            keyPrefixEventCallbacks.add(eventCallback);
        }

        @Override
        public void remove(String keyPrefix, EventCallback eventCallback) {
            Set<EventCallback> keyPrefixListeners = eventCallbacks.get(keyPrefix);
            if (keyPrefixListeners != null) {
                keyPrefixListeners.remove(eventCallback);
            }
        }
    }
}