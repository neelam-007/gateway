package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * LocalKeyValueStore provides an implementation of SharedKeyValueStore in memory.  The data here will not be shared
 * amongst different gateway.
 */
public class LocalKeyValueStore<K extends Serializable, V extends Serializable> implements SharedKeyValueStore<K, V> {
    private ConcurrentMap<K, V> kvMap;

    LocalKeyValueStore() {
        kvMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isEmpty() {
        return kvMap.isEmpty();
    }

    @Override
    public V get(K key) {
        return kvMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        return kvMap.put(key, value);
    }

    @Override
    public boolean containsKey(Serializable key) {
        return kvMap.containsKey(key);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return kvMap.compute(key, remappingFunction);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return kvMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return kvMap.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V remove(K key) {
        return kvMap.remove(key);
    }

    @Override
    public void clear() {
        kvMap.clear();
    }
}
