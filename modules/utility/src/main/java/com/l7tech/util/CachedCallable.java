package com.l7tech.util;

import java.util.concurrent.Callable;

/**
 * A Callable wrapper that services requests to a wrapped callable from a cache for up to a specified number of milliseconds.
 * <p/>
 * It is safe to use this wrapper from multiple threads.  The wrapped callable does NOT itself need to be
 * threadsafe, as only one thread at a time will ever invoke it: other callers waiting for up-to-date
 * information will wait for this to complete.
 */
public class CachedCallable<T> implements Callable<T> {
    static TimeSource timeSource = new TimeSource();
    private final long updateIntervalMillis;
    private final Callable<T> refiller;
    private volatile T cachedValue = null;
    private volatile long lastUpdated = 0;

    public CachedCallable(long updateIntervalMillis, Callable<T> refiller) {
        this.updateIntervalMillis = updateIntervalMillis;
        this.refiller = refiller;
    }

    /**
     * Get the value, invoking the refiller if necessary.
     *
     * @return a value, either cached or newly obtained from the refiller.
     *         May be null only if the refiller returned null.
     * @throws Exception if an exception is thrown by the refiller.
     */
    @Override
    public T call() throws Exception {
        T ret = cachedValue;
        if (cachedValue == null || isExpired()) {
            synchronized (this) {
                ret = cachedValue;
                if (ret == null || isExpired()) {
                    ret = refiller.call();
                    cachedValue = ret;
                    lastUpdated = currentTimeMillis();
                }
            }
        }
        return ret;
    }

    protected boolean isExpired() {
        return (currentTimeMillis() - lastUpdated) > updateIntervalMillis;
    }

    protected long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }
}
