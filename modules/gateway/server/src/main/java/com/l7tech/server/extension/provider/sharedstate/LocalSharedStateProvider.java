package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalSharedStateProvider provides an in-memory implementation of the SharedStateProvider.
 * Note: This data provider only works with a single node as data will not be shared between
 *       the cluster.
 */
public class LocalSharedStateProvider implements SharedStateProvider {
    private final ConcurrentMap<String, SharedKeyValueStore> kvStores;

    public LocalSharedStateProvider() {
        kvStores = new ConcurrentHashMap<>();
    }

    /**
     * @see SharedStateProvider#getKeyValueStore(String, Configuration)
     */
    @Override
    public SharedKeyValueStore getKeyValueStore(String name, Configuration config) {
        return kvStores.computeIfAbsent(name, key -> new LocalKeyValueStore());
    }
}
