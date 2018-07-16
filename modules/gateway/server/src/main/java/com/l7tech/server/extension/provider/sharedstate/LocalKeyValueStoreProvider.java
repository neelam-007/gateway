package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalKeyValueStoreProvider provides an in-memory implementation of the SharedKeyValueStoreProvider.
 * Note: This data provider only works with a single node as data will not be shared between
 *       the cluster.
 */
public class LocalKeyValueStoreProvider implements SharedKeyValueStoreProvider {
    private final ConcurrentMap<String, SharedKeyValueStore> kvStores;

    public LocalKeyValueStoreProvider() {
        kvStores = new ConcurrentHashMap<>();
    }

    /**
     * @see SharedKeyValueStoreProvider#getKeyValueStore(String, Configuration)
     */
    @Override
    public SharedKeyValueStore getKeyValueStore(String name, Configuration config) {
        return kvStores.computeIfAbsent(name, key -> new LocalKeyValueStore(name));
    }
}