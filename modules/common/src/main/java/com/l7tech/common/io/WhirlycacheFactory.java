/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.whirlycott.cache.policy.FIFOMaintenancePolicy;
import com.whirlycott.cache.policy.LRUMaintenancePolicy;
import com.whirlycott.cache.policy.LFUMaintenancePolicy;
import com.whirlycott.cache.*;
import com.whirlycott.cache.impl.ConcurrentHashMapImpl;

/**
 * Creates a whirlycache.
 */
public class WhirlycacheFactory {

    //- PUBLIC

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
     * @param tunerInterval  seconds in between runs of the tuner process.  Don't run too frequently, but keep in mind that only
     *                       the tuner will remove items from the cache if it exceeds its limit.
     * @param replacementPolicy  the replacement policy.  Must be one of {@link #POLICY_FIFO}, {@link #POLICY_LRU}, or {@link #POLICY_LFU}.
     * @return a new Cache instance.  Never null.
     */
    public static Cache createCache(String name, int size, int tunerInterval, String replacementPolicy) {
        try {
            CacheMaintenancePolicy policy = (CacheMaintenancePolicy)Class.forName(replacementPolicy).newInstance();
            return createCache(name, size, tunerInterval, policy);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Invalid replacement policy classname: " + replacementPolicy, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Invalid replacement policy classname: " + replacementPolicy, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid replacement policy classname: " + replacementPolicy, e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid replacement policy classname: " + replacementPolicy, e);
        }
    }

    /**
     * Create a new whirlycache instance with the specified settings and a custom maintenance policy.
     * The returned cache does its own synchronization, and also owns its own cleanup thread that takes care of
     * running the tuner periodically.
     *
     * @param name    the unique name of the new cache instance.  Must be unique and not null.
     * @param size    the maximum number of items to keep in the cache.  The cache may be allowed to exceed this number
     *                in between runs of the tuner.
     * @param tunerInterval  seconds in between runs of the tuner process.  Don't run too frequently, but keep in mind that only
     *                       the tuner will remove items from the cache if it exceeds its limit.
     * @param maintenancePolicy   a custom maintenance policy for this cache.  Must not be null.
     * @return a new Cache instance.  Never null.
     */
    public static Cache createCache(String name, int size, int tunerInterval, CacheMaintenancePolicy maintenancePolicy) {
        CacheConfiguration cc = new CacheConfiguration();
        cc.setMaxSize(size);
        cc.setName(name);
        cc.setBackend(ConcurrentHashMapImpl.class.getName()); // not needed or used: we specify it manually instead, justbelow
        cc.setPolicy(POLICY_LRU); // not needed or used: we specify it manually instead, justbelow
        cc.setTunerSleepTime(tunerInterval);

        final ManagedCache managedCache = new ConcurrentHashMapImpl();
        maintenancePolicy.setCache(managedCache);
        maintenancePolicy.setConfiguration(cc);

        Cache cache = null;
        synchronized(cacheMap) {
            if (!shutdown) {
                if ( cacheMap.containsKey(name) ) logger.warning("Duplicate cache name used '" + name + "'.");

                CacheDecorator cacheDecorator = new CacheDecorator(managedCache, cc, new CacheMaintenancePolicy[] { maintenancePolicy });
                cache = cacheDecorator;
                cacheMap.put(name, cacheDecorator);
            }
        }

        if (cache == null) {
            // if we are shutting down we can't create new CacheDecorator's so return the dummy cache
            cache = NullCache;
        }  else {
            // work around for bug 3053 - logging of invalid characters when cache not used
            Object dummy = new Object();
            cache.store(dummy, dummy);
            cache.remove(dummy);
        }

        return cache;
    }

    /**
     * Shutdown the given cache instance an remove from the cache map.
     *
     * @param cache the cache to shutdown
     * @retrurn true if the cache was shutdown
     */
    public static boolean shutdown( final Cache cache ) {
        boolean shutdownCache = false;

        synchronized(cacheMap) {
            if (!shutdown) {
                for (Map.Entry<String,CacheDecorator> entry : cacheMap.entrySet()) {
                    String name = entry.getKey();
                    CacheDecorator cacheDecorator = entry.getValue();

                    if ( cacheDecorator == cache ) {
                        if (logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO, "Shutting down cache ''{0}''.", name);

                        shutdownCache = true;
                        cacheMap.remove(name);
                        cacheDecorator.shutdown();
                        break;
                    }
                }
            }
        }

        return shutdownCache;
    }

    /**
     * Shutdown cache tuning thread.
     */
    public static void shutdown() {
        synchronized(cacheMap) {
            shutdown = true;
            for (Map.Entry<String,CacheDecorator> entry : cacheMap.entrySet()) {
                String name = entry.getKey();
                CacheDecorator cacheDecorator = entry.getValue();
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Shutting down cache ''{0}''.", name);
                cacheDecorator.shutdown();
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WhirlycacheFactory.class.getName());

    private static final Cache NullCache = new Cache() {
        public void clear() {}
        public Object remove(Cacheable key) { return null; }
        public Object remove(Object key) { return null; }
        public Object retrieve(Cacheable key) { return null; }
        public Object retrieve(Object key) { return null; }
        public int size() { return 0; }
        public void store(Cacheable key, Object value) {}
        public void store(Cacheable key, Object value, long expireTime) {}
        public void store(Object key, Object value) {}
        public void store(Object key, Object value, long expireTime) {}
    };

    private static final Map<String,CacheDecorator> cacheMap = Collections.synchronizedMap(new HashMap<String,CacheDecorator>());
    private static boolean shutdown = false;
}
