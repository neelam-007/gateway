package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterProvider;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalCounterProvider provides an in-memory implementation of the SharedCounterProvider.
 * Note: This data provider only works with a single node as data will not be shared between
 *       the cluster.
 */
public class LocalCounterProvider implements SharedCounterProvider {

    public static final String KEY = "local";

    private final ConcurrentMap<String, SharedCounterStore> counterStores;

    public LocalCounterProvider() {
        counterStores = new ConcurrentHashMap<>();
    }

    /**
     * @see SharedCounterProvider#getCounterStore(String)
     */
    @Override
    public SharedCounterStore getCounterStore(String name) {
        return counterStores.computeIfAbsent(name, key -> new LocalCounterStore());
    }

    @Override
    public String getName() {
        return KEY;
    }
}
