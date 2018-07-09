package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.MapPartitionLostListener;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * HazelcastKeyValueStore is a wrapper class that provides the implementation of the SharedKeyValueStore utilizing
 * the Hazelcast IMap data structure
 *
 * @param <K>
 * @param <V>
 */
public class HazelcastKeyValueStore<K extends Serializable, V extends Serializable>
        implements SharedKeyValueStore<K, V> {

    private final IMap<K, V> hcMap;

    HazelcastKeyValueStore(IMap<K, V> hcMap) {
        this.hcMap = hcMap;
    }

    @Override
    public V get(K key) {
        return hcMap.get(key);
    }

    @Override
    public void clear() {
        hcMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return hcMap.isEmpty();
    }

    @Override
    public V remove(K key) {
        return hcMap.remove(key);
    }

    @Override
    public void delete(K key) {
        hcMap.delete(key);
    }

    @Override
    public V put(K key, V value) {
        return hcMap.put(key, value);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit timeUnit) {
        return hcMap.put(key, value, ttl, timeUnit);
    }

    @Override
    public boolean putIfCondition(K key, V value, Function<V, Boolean> condition, long ttl, TimeUnit timeUnit) {
        hcMap.lock(key);
        try {
            V prevValue = hcMap.get(key);
            if (condition.apply(prevValue)) {
                hcMap.put(key, value, ttl, timeUnit);
                return true;
            }
            return false;
        } finally {
            hcMap.unlock(key);
        }
    }

    @Override
    public void set(K key, V value) {
        hcMap.set(key, value);
    }

    @Override
    public void set(K key, V value, long ttl, TimeUnit timeUnit) {
        hcMap.set(key, value, ttl, timeUnit);
    }

    @Override
    public boolean containsKey(K key) {
        return hcMap.containsKey(key);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return hcMap.compute(key, remappingFunction);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return hcMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return hcMap.computeIfPresent(key, remappingFunction);
    }

    @Override
    public String getName() {
        return hcMap.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HazelcastKeyValueStore)) {
            return false;
        }

        HazelcastKeyValueStore<?, ?> that = (HazelcastKeyValueStore<?, ?>) o;
        return Objects.equals(hcMap, that.hcMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hcMap);
    }

    /**
     * See {@link IMap#addPartitionLostListener(MapPartitionLostListener)}.
     */
    String addPartitionLostListener(MapPartitionLostListener listener) {
        return hcMap.addPartitionLostListener(listener);
    }

    /**
     * See {@link IMap#removePartitionLostListener(String)}.
     */
    void removePartitionLostListener(String partitionListenerId) {
        hcMap.removePartitionLostListener(partitionListenerId);
    }
}
