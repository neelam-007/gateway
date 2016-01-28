package com.ca.siteminder;


import com.l7tech.common.io.WhirlycacheFactory;

import com.l7tech.objectmodel.Goid;
import com.sun.istack.NotNull;
import com.whirlycott.cache.Cache;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SiteMinderAgentContextCacheManager is a singleton class that manages all SiteMinderAgentContextCache.  It utilizes
 * the ConcurrentSkipListMap.
 */
public class SiteMinderAgentContextCacheManagerImpl implements SiteMinderAgentContextCacheManager {
    private static final Logger LOGGER = Logger.getLogger(SiteMinderAgentContextCacheManagerImpl.class.getName());

    private final ConcurrentSkipListMap<Key, SiteMinderAgentContextCache> agentCacheMap;

    public SiteMinderAgentContextCacheManagerImpl() {
        agentCacheMap = new ConcurrentSkipListMap<>();
    }

    @Override
    public SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName) {
        return agentCacheMap.get(new Key(smConfigGoid, smAgentName));
    }

    @Override
    public SiteMinderAgentContextCache createCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName,
                                                int resourceMaxEntries, int sessionMaxEntries) {
        Key key = new Key(smConfigGoid, smAgentName);
        Cache resourceCache = createWhirlycache(key.toString() + ".resource",
                resourceMaxEntries, 59, WhirlycacheFactory.POLICY_LRU);
        Cache sessionCache = createWhirlycache(key.toString() + ".session",
                sessionMaxEntries, 59, WhirlycacheFactory.POLICY_LRU);

        SiteMinderAgentContextCache newCache = new SiteMinderAgentContextCache(resourceCache, sessionCache);

        agentCacheMap.put(key, newCache);

        LOGGER.log(Level.FINE,
                "Initialized new cache: {0}, resourceCache: size {1} , sessionCache: size {2}",
                new Object[] {key, resourceMaxEntries, sessionMaxEntries});
        return newCache;
    }

    private Cache createWhirlycache(String name, int size, int tunerInterval, String maintenancePolicy) {
        return WhirlycacheFactory.createCache(name, size, tunerInterval, maintenancePolicy);
    }

    // TODO: call this when SiteMinderConfiguration is updated
    @Override
    public void removeCaches(@NotNull Goid smConfigGoid) {
        LOGGER.log(Level.FINE, "Removing caches for Goid {0}", smConfigGoid);

        // Look for the first key to be removed
        Key keyToRemove = agentCacheMap.higherKey(new Key(smConfigGoid, ""));

        while (keyToRemove != null && smConfigGoid.equals(keyToRemove.getConfigGoid())) {
            agentCacheMap.remove(keyToRemove);
            keyToRemove = agentCacheMap.higherKey(keyToRemove);
        }
    }

    @Override
    public void removeAllCache() {
        agentCacheMap.clear();
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

            if (configGoid != null ? !configGoid.equals(key.configGoid) : key.configGoid != null) return false;
            return !(agentName != null ? !agentName.equals(key.agentName) : key.agentName != null);

        }

        @Override
        public int hashCode() {
            int result = configGoid != null ? configGoid.hashCode() : 0;
            result = 31 * result + (agentName != null ? agentName.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "configGoid=" + configGoid +
                    ", agentName='" + agentName + '\'' +
                    '}';
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
