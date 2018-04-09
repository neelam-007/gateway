package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedCounterStore;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

/**
 * LocalCounterStore provides a local in-memory version of the SharedCounterStore.  The data here will NOT be shared
 * amongst different gateway in a cluster.
 */
public class LocalCounterStore<K extends Serializable> implements SharedCounterStore<K> {
    private final ConcurrentMap<K, AtomicLong> counterMap;

    public LocalCounterStore() {
        counterMap = new ConcurrentHashMap<>();
    }

    @Override
    public void init(K key, Long initialValue) {
        counterMap.computeIfAbsent(key, k -> new AtomicLong(initialValue));
    }

    @Override
    public Long get(K key) {
        return getCounter(key).longValue();
    }

    @Override
    public Long getAndIncrement(K key) {
        return getCounter(key).getAndIncrement();
    }

    @Override
    public Long incrementAndGet(K key) {
        return getCounter(key).incrementAndGet();
    }

    @Override
    public Long getAndUpdate(K key, LongUnaryOperator function) {
        return getCounter(key).getAndUpdate(function);
    }

    @Override
    public Long updateAndGet(K key, LongUnaryOperator function) {
        return getCounter(key).updateAndGet(function);
    }

    @Override
    public void remove(K key) {
        counterMap.remove(key);
    }

    private AtomicLong getCounter(K key) {
        AtomicLong ret = counterMap.get(key);

        if (ret == null) {
            throw new NoSuchElementException("There is no counter associated to the key " + key);
        }

        return ret;
    }
}
