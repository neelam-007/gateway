package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.Configuration;
import com.ca.apim.gateway.extension.sharedstate.SharedCounterProvider;
import com.ca.apim.gateway.extension.sharedstate.SharedCounterStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalCounterProvider provides an in-memory implementation of the SharedCounterProvider.
 * Note: This data provider only works with a single node as data will not be shared between
 *       the cluster.
 */
public class LocalCounterProvider implements SharedCounterProvider {
    private final ConcurrentMap<String, SharedCounterStore> counterStores;

    public LocalCounterProvider() {
        counterStores = new ConcurrentHashMap<>();
    }

    /**
     * @see SharedCounterProvider#getCounterStore(String, Configuration)
     */
    @Override
    public SharedCounterStore getCounterStore(String name, Configuration config) {
        return counterStores.computeIfAbsent(name, key -> new LocalCounterStore());
    }
}
