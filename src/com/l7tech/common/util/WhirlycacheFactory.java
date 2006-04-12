/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import com.whirlycott.cache.policy.FIFOMaintenancePolicy;
import com.whirlycott.cache.policy.LRUMaintenancePolicy;
import com.whirlycott.cache.policy.LFUMaintenancePolicy;
import com.whirlycott.cache.*;
import com.whirlycott.cache.impl.ConcurrentHashMapImpl;

/**
 * Creates a whirlycache
 */
public class WhirlycacheFactory {
    public static final String POLICY_FIFO = FIFOMaintenancePolicy.class.getName();
    public static final String POLICY_LRU  = LRUMaintenancePolicy.class.getName();
    public static final String POLICY_LFU  = LFUMaintenancePolicy.class.getName();

    /**
     * Create a new whirlycache instance with the specified settings.  The returned cache does its own
     * synchronization, and also owns its own cleanup thread that takes care of running the tuner periodically.
     *
     * @param name    the unique name of the new cache instance.  Must be unique and not null.
     * @param size    the maximum number of items to keep in the cache.  The cache may be allowed to exceed this number
     *                in between runs of the tuner.
     * @param replacementPolicy  the replacement policy.  Must be one of {@link #POLICY_FIFO}, {@link #POLICY_LRU}, or {@link #POLICY_LFU}.
     * @param tunerInterval  minutes in between runs of the tuner process.  Don't run too frequently, but keep in mind that only
     *                       the tuner will remove items from the cache if it exceeds its limit.
     * @return a new Cache instance.  Never null.
     */
    public static Cache createCache(String name, int size, String replacementPolicy, int tunerInterval) {
        CacheConfiguration cc = new CacheConfiguration();
        cc.setMaxSize(size);
        cc.setName(name);
        cc.setBackend(ConcurrentHashMapImpl.class.getName());
        cc.setPolicy(replacementPolicy);
        cc.setTunerSleepTime(tunerInterval);

        final ManagedCache managedCache = new ConcurrentHashMapImpl();
        final CacheMaintenancePolicy policy = new LFUMaintenancePolicy();
        policy.setCache(managedCache);
        policy.setConfiguration(cc);

        return new CacheDecorator(managedCache, cc, new CacheMaintenancePolicy[] { policy });
    }
}
