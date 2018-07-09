package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.Striped;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * LocalKeyValueStore provides an implementation of SharedKeyValueStore in memory. The data here will not be shared
 * amongst different gateway.
 */
public class LocalKeyValueStore<K extends Serializable, V extends Serializable> implements SharedKeyValueStore<K, V> {

    private final String name;
    private ConcurrentMap<K, V> kvMap;
    private DelayQueue<PendingExpireMs<K>> entriesToExpire;
    private Striped<Lock> striped = Striped.lock(Runtime.getRuntime().availableProcessors() * 4);

    LocalKeyValueStore(@NotNull final String name) {
        this.name = name;
        kvMap = new ConcurrentHashMap<>();
        entriesToExpire = new DelayQueue<>();
    }

    @Override
    public boolean isEmpty() {
        cleanup();
        return kvMap.isEmpty();
    }

    @Override
    public V get(K key) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))) {
            cleanup();
            return kvMap.get(key);
        }
    }

    @Override
    public V put(K key, V value) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))){
            cleanup();
            return kvMap.put(key, value);
        }
    }

    /**
     * Using any other entry insertion method will not change the associated time-to-live for a given entry.
     */
    @Override
    public V put(K key, V value, long ttl, TimeUnit timeUnit) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))){
            cleanup();
            V prev = kvMap.put(key, value);
            updatePendingExpires(key, ttl, timeUnit);
            return prev;
        }
    }

    @Override
    public boolean putIfCondition(K key, V value, Function<V, Boolean> condition, long ttl, TimeUnit timeUnit) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))) {
            cleanup();
            V prev = kvMap.get(key);
            if (condition.apply(prev)) {
                kvMap.put(key, value);
                updatePendingExpires(key, ttl, timeUnit);
                return true;
            }
            return false;
        }
    }

    @Override
    public void set(K key, V value) {
        put(key, value);
    }

    /**
     * Using any other entry insertion method will not change the associated time-to-live for a given entry.
     */
    @Override
    public void set(K key, V value, long ttl, TimeUnit timeUnit) {
        put(key, value, ttl, timeUnit);
    }

    @Override
    public boolean containsKey(K key) {
       try (CloseableLock closeableLock = new CloseableLock(striped.get(key))){
            cleanup();
            return kvMap.containsKey(key);
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))) {
            cleanup();
            return kvMap.compute(key, remappingFunction);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))){
            cleanup();
            return kvMap.computeIfAbsent(key, mappingFunction);
        }
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))) {
            cleanup();
            return kvMap.computeIfPresent(key, remappingFunction);
        }
    }

    @Override
    public V remove(K key) {
        try (CloseableLock closeableLock = new CloseableLock(striped.get(key))){
            cleanup();
            return kvMap.remove(key);
        }
    }

    @Override
    public void delete(K key) {
        remove(key);
    }

    @Override
    public void clear() {
        kvMap.clear();
        entriesToExpire.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Actually expires entries awaiting removal due to expiration from the store.
     * All methods accessing the internal map need to call this first to ensure the map state
     * reflects that expired entries have been removed.
     * <p>
     * Do to the use of DelayQueue, concurrent threads will typically increase cleanup throughput.
     */
    private void cleanup() {
        PendingExpireMs<K> expired = entriesToExpire.poll();
        while (expired != null) {
            kvMap.remove(expired.getKey());
            expired = entriesToExpire.poll();
        }
    }

    /**
     * Add a new pending expire for this key, and remove any outdated ones
     */
    private void updatePendingExpires(K key, long ttl, TimeUnit timeUnit) {
        PendingExpireMs<K> expirable = new PendingExpireMs<>(key, PendingExpireMs.UNIT.convert(ttl, timeUnit));
        entriesToExpire.remove(expirable); // NOSONAR don't keep the old expirable in the queue!
        if (ttl > 0) {
            entriesToExpire.put(expirable);
        }
    }

    private static class PendingExpireMs<K> implements Delayed {

        private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;

        private final K key;
        private long delay;

        private PendingExpireMs(@NotNull final K key, final long ttl) {
            this.key = key;
            this.delay = System.currentTimeMillis() + ttl;
        }

        @Override
        public long getDelay(@NotNull TimeUnit desiredUnit) {
            return desiredUnit.convert(delay - System.currentTimeMillis(), UNIT);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            if (o instanceof PendingExpireMs) {
                return Long.compare(delay, ((PendingExpireMs) o).getDelay());
            }
            return Long.compare(delay, o.getDelay(UNIT));
        }

        private K getKey() {
            return key;
        }

        public long getDelay() {
            return delay;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PendingExpireMs<?> that = (PendingExpireMs<?>) o;
            return Objects.equal(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key);
        }
    }

    /**
     * An autocloseable lock wrapper to allow locking/unlocking a lock as a try-with-resources statement
     */
    private class CloseableLock implements AutoCloseable {
        private Lock lock;

        CloseableLock(Lock lock) {
            this.lock = lock;
            lock.lock(); //NOSONAR this is intended to be unlocked by the close() method
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }
}
