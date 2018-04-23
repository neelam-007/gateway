package com.ca.siteminder;

import com.ca.siteminder.SiteMinderAgentContextCache.AgentContextSubCache;
import com.ca.siteminder.SiteMinderAgentContextCache.AgentContextSubCacheType;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.objectmodel.Goid;
import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SiteMinderAgentContextCacheManager is a singleton class that manages all SiteMinderAgentContextCache.  It utilizes
 * the ConcurrentSkipListMap.
 */
public class SiteMinderAgentContextCacheManagerImpl implements SiteMinderAgentContextCacheManager {
    private static final Logger LOGGER = Logger.getLogger(SiteMinderAgentContextCacheManagerImpl.class.getName());

    private final Lock lock = new ReentrantLock();
    private final ConcurrentSkipListMap<Key, SiteMinderAgentContextCache> agentCacheMap;

    public SiteMinderAgentContextCacheManagerImpl() {
        agentCacheMap = new ConcurrentSkipListMap<>();
    }

    @Override
    public SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName) {
        return agentCacheMap.get(new Key(smConfigGoid, smAgentName));
    }

    @Override
    public SiteMinderAgentContextCache createCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName, @NotNull List<AgentContextSubCache> subCaches) {
        Key key = new Key(smConfigGoid, smAgentName);
        String primaryKey = key.getFullKeyName();

        SiteMinderAgentContextCache agentContextCache = null;

        try {
            lock.lock();

            // cache may have already been created
            agentContextCache = agentCacheMap.get(key);

            // if not, create it and add to agentCacheMap
            if (null == agentContextCache) {
                List<AgentContextSubCache> newSubCaches = new ArrayList<>();

                for (AgentContextSubCache subCache : subCaches) {
                    newSubCaches.add(new AgentContextSubCache(
                            (subCache.getMaxSize() > 0) ? createWhirlycache(getSubCacheKeyString(primaryKey, subCache.getCacheType()),
                                    subCache.getMaxSize(), 59, WhirlycacheFactory.POLICY_LRU) : null,
                            subCache.getCacheType(),
                            subCache.getMaxSize(),
                            subCache.getMaxAge()));
                }

                agentContextCache = new SiteMinderAgentContextCache(newSubCaches);
                agentCacheMap.put(key, agentContextCache);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Initialized new cache: {0}, {1}; {2}; {3}; {4}", new Object[] { key,
                            getSubCacheDetailsString(agentContextCache, AgentContextSubCacheType.AGENT_CACHE_RESOURCE),
                            getSubCacheDetailsString(agentContextCache, AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION),
                            getSubCacheDetailsString(agentContextCache, AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION),
                            getSubCacheDetailsString(agentContextCache, AgentContextSubCacheType.AGENT_CACHE_ACO)
                    });
                }
            }
        } finally {
            lock.unlock();
        }

        return agentContextCache;
    }

    private String getSubCacheKeyString(String key, AgentContextSubCacheType cacheType) {
        return key + "." + cacheType.getName();
    }

    private String getSubCacheDetailsString(SiteMinderAgentContextCache agentContextCache, AgentContextSubCacheType subCacheType) {
        AgentContextSubCache subCache = agentContextCache.getSubCache(subCacheType);
        if (subCache != null) {
            return String.format("%sCache: size %d, max age %d",
                    subCacheType.getName(), subCache.getMaxSize(), subCache.getMaxAge());
        } else {
            return subCacheType.getName() + "Cache: size 0, max age 0";
        }
    }

    private Cache createWhirlycache(String name, int size, int tunerInterval, String maintenancePolicy) {
        return WhirlycacheFactory.createCache(name, size, tunerInterval, maintenancePolicy);
    }

    @Override
    public void removeCaches(@NotNull Goid smConfigGoid) {
        ArrayList<SiteMinderAgentContextCache> agentContextCacheList = null;

        try {
            lock.lock();

            // Look for the first key to be removed
            Key keyToRemove = agentCacheMap.higherKey(new Key(smConfigGoid, ""));

            if (null == keyToRemove) {
                // no entries, or all already removed
                return;
            }

            LOGGER.log(Level.FINE, "Removing caches for Goid {0}", smConfigGoid);

            agentContextCacheList = new ArrayList<>();

            // remove all agent context caches from the agentCacheMap and add them to a list
            while (keyToRemove != null && smConfigGoid.equals(keyToRemove.getConfigGoid())) {
                agentContextCacheList.add(agentCacheMap.get(keyToRemove));
                agentCacheMap.remove(keyToRemove);
                keyToRemove = agentCacheMap.higherKey(keyToRemove);
            }
        } finally {
            lock.unlock();
        }

        // for each removed SiteMinderAgentContextCache, shutdown its Whirlycache instances
        for (SiteMinderAgentContextCache agentContextCache : agentContextCacheList) {
            shutdownCaches(agentContextCache);
        }
    }

    @Override
    public void removeAllCaches() {
        ArrayList<SiteMinderAgentContextCache> agentContextCacheList = null;

        try {
            lock.lock();

            agentContextCacheList = new ArrayList<>();

            // remove all agent context caches from the agentCacheMap and add them to a list
            for (Key key : agentCacheMap.keySet()) {
                agentContextCacheList.add(agentCacheMap.get(key));
                agentCacheMap.remove(key);
            }
        } finally {
            lock.unlock();
        }

        // for each removed SiteMinderAgentContextCache, shutdown its Whirlycache instances
        for (SiteMinderAgentContextCache agentContextCache : agentContextCacheList) {
            shutdownCaches(agentContextCache);
        }
    }

    /**
     * Shut down each of the Whirlycache instances for the specified SiteMinderAgentContextCache
     *
     * @param agentContextCache the SiteMinderAgentContextCache of whose Whirlycaches to shut down
     */
    private void shutdownCaches(@NotNull SiteMinderAgentContextCache agentContextCache) {
        for (AgentContextSubCacheType subCacheType : AgentContextSubCacheType.values()) {
            AgentContextSubCache subCache = agentContextCache.getSubCache(subCacheType);
            if (subCache != null && subCache.getCache() != null) {
                WhirlycacheFactory.shutdown(subCache.getCache());
            }
        }
    }

    /**
     * Key to the cache
     */
    private class Key implements Comparable<Key> {
        private final Goid configGoid;
        private final String agentName;

        Key(@NotNull Goid smConfigGoid, @NotNull String smAgentName) {
            configGoid = smConfigGoid;
            agentName = smAgentName;
        }

        public Goid getConfigGoid() {
            return configGoid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return configGoid.equals(key.configGoid) && agentName.equals(key.agentName);

        }

        @Override
        public int hashCode() {
            int result = configGoid.hashCode();
            result = 31 * result + (agentName.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "configGoid=" + configGoid +
                    ", agentName='" + agentName + '\'' +
                    '}';
        }

        public String getFirstLevelKeyName() {
            return "Key{configGoid=" + configGoid + "}";
        }

        public String getFullKeyName() {
            return this.toString();
        }

        /**
         * Compares two Keys.  The comparison is based on the results of the compareTo in the following order:
         *   1.  Goid of the config
         *   2.  Agent Name
         */
        @Override
        public int compareTo(@NotNull Key o) {
            int result = configGoid.compareTo(o.configGoid);

            if (result == 0) {
                result = agentName.compareTo(o.agentName);
            }

            return result;
        }
    }
}
