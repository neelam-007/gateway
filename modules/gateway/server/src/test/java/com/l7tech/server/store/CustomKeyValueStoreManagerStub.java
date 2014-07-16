package com.l7tech.server.store;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreListener;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
    public <L extends KeyValueStoreListener> L getListener(Class<L> lClass) {
        if (KeyValueStoreChangeEventListener.class.equals(lClass)) {
            //noinspection unchecked
            return (L) listener;
        }

        return null;
    }

    @Override
    public Class<? extends PersistentEntityImp> getImpClass() {
        return CustomKeyValueStore.class;
    }

    private class KeyValueStoreChangeEventListenerStub extends KeyValueStoreChangeEventListener {
        private final Map<String, Set<Callback>> callbacks = new ConcurrentHashMap<>();

        @Override
        public void add(Callback callback) {
            String keyPrefix = callback.getKeyPrefix();
            Set<Callback> keyPrefixCallbacks = callbacks.get(keyPrefix);
            if (keyPrefixCallbacks == null) {
                keyPrefixCallbacks = new CopyOnWriteArraySet<>();
                callbacks.put(keyPrefix, keyPrefixCallbacks);
            }
            keyPrefixCallbacks.add(callback);
        }

        @Override
        public void remove(Callback callback) {
            String keyPrefix = callback.getKeyPrefix();
            Set<Callback> keyPrefixCallbacks = callbacks.get(keyPrefix);
            if (keyPrefixCallbacks != null) {
                keyPrefixCallbacks.remove(callback);
            }
        }
    }
}