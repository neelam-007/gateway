package com.ca.siteminder;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Goid;
import com.sun.istack.NotNull;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SiteMinderAgentContextCacheManager is a singleton class that manages all SiteMinderAgentContextCache.  It utilizes
 * the ConcurrentSkipListMap.
 */
public class SiteMinderAgentContextCacheManager {

    public static final String AGENT_CACHE_PREFIX = "agent.";
    public static final String AGENT_USE_AGENT_CACHE_SUFFIX = ".useAgentCache";
    public static final String AGENT_RESOURCE_CACHE_SIZE_SUFFIX = ".resourceCache.size";
    public static final String AGENT_RESOURCE_CACHE_MAX_AGE_SUFFIX = ".resourceCache.maxAge";
    public static final String AGENT_AUTHENTICATION_CACHE_SIZE_SUFFIX = ".authenticationCache.size";
    public static final String AGENT_AUTHENTICATION_CACHE_MAX_AGE_SUFFIX = ".authenticationCache.maxAge";
    public static final String AGENT_AUTHORIZATION_CACHE_SIZE_SUFFIX = ".authorizationCache.size";
    public static final String AGENT_AUTHORIZATION_CACHE_MAX_AGE_SUFFIX = ".authorizationCache.maxAge";

    public static final String SM_USE_GLOBAL_CACHE_CLUSTER_PROP = "sm.cache.global";
    public static final String SM_USE_GLOBAL_CACHE_PROP = ClusterProperty.asServerConfigPropertyName(SM_USE_GLOBAL_CACHE_CLUSTER_PROP);
    public static final String SM_USE_GLOBAL_CACHE_DESC = "Whether to use a global cache for all Siteminder agents.  true to use global cache; false to use per agent. Requires gateway restart.";
    public static final boolean SM_USE_GLOBAL_CACHE_DEFAULT = true;

    public static final String SM_RESOURCE_CACHE_SIZE_CLUSTER_PROP = "sm.resourceCache.size";
    public static final String SM_RESOURCE_CACHE_SIZE_PROP = ClusterProperty.asServerConfigPropertyName(SM_RESOURCE_CACHE_SIZE_CLUSTER_PROP);
    public static final String SM_RESOURCE_CACHE_SIZE_DESC = "The number of entries to cache in the Resource Cache, 0 for no caching (Integer). Requires gateway restart.";
    public static final int SM_RESOURCE_CACHE_SIZE_DEFAULT = 10;

    public static final String SM_RESOURCE_CACHE_MAX_AGE_CLUSTER_PROP = "sm.resourceCache.maxAge";
    public static final String SM_RESOURCE_CACHE_MAX_AGE_PROP = ClusterProperty.asServerConfigPropertyName(SM_RESOURCE_CACHE_MAX_AGE_CLUSTER_PROP);
    public static final String SM_RESOURCE_CACHE_MAX_AGE_DESC = "Maximum age of entries in the Resource Cache (Milliseconds). Requires gateway restart.";
    public static final long SM_RESOURCE_CACHE_MAX_AGE_DEFAULT = 300000; // milliseconds

    public static final String SM_AUTHENTICATION_CACHE_SIZE_CLUSTER_PROP = "sm.authenticationCache.size";
    public static final String SM_AUTHENTICATION_CACHE_SIZE_PROP = ClusterProperty.asServerConfigPropertyName(SM_AUTHENTICATION_CACHE_SIZE_CLUSTER_PROP);
    public static final String SM_AUTHENTICATION_CACHE_SIZE_DESC = "The number of entries to cache in the Authentication Cache, 0 for no caching (Integer). Requires gateway restart.";
    public static final int SM_AUTHENTICATION_CACHE_SIZE_DEFAULT = 10;

    public static final String SM_AUTHENTICATION_CACHE_MAX_AGE_CLUSTER_PROP = "sm.authenticationCache.maxAge";
    public static final String SM_AUTHENTICATION_CACHE_MAX_AGE_PROP = ClusterProperty.asServerConfigPropertyName(SM_AUTHENTICATION_CACHE_MAX_AGE_CLUSTER_PROP);
    public static final String SM_AUTHENTICATION_CACHE_MAX_AGE_DESC = "Maximum age of entries in the Authentication Cache (Milliseconds). Requires gateway restart.";
    public static final long SM_AUTHENTICATION_CACHE_MAX_AGE_DEFAULT = 3600000; // milliseconds

    public static final String SM_AUTHORIZATION_CACHE_SIZE_CLUSTER_PROP = "sm.authorizationCache.size";
    public static final String SM_AUTHORIZATION_CACHE_SIZE_PROP = ClusterProperty.asServerConfigPropertyName(SM_AUTHORIZATION_CACHE_SIZE_CLUSTER_PROP);
    public static final String SM_AUTHORIZATION_CACHE_SIZE_DESC = "The number of entries to cache in the Authorization Cache, 0 for no caching (Integer). Requires gateway restart.";
    public static final int SM_AUTHORIZATION_CACHE_SIZE_DEFAULT = 10;

    public static final String SM_AUTHORIZATION_CACHE_MAX_AGE_CLUSTER_PROP = "sm.authorizationCache.maxAge";
    public static final String SM_AUTHORIZATION_CACHE_MAX_AGE_PROP = ClusterProperty.asServerConfigPropertyName(SM_AUTHORIZATION_CACHE_MAX_AGE_CLUSTER_PROP);
    public static final String SM_AUTHORIZATION_CACHE_MAX_AGE_DESC = "Maximum age of entries in the Authorization Cache (Milliseconds). Requires gateway restart.";
    public static final long SM_AUTHORIZATION_CACHE_MAX_AGE_DEFAULT = 3600000; // milliseconds

    private static final String GLOBAL_CACHE_AGENT_NAME = "";
    private static final Logger LOGGER = Logger.getLogger(SiteMinderAgentContextCacheManager.class.getName());

    private final ConcurrentSkipListMap<Key, SiteMinderAgentContextCache> cacheMap;

    // Default cache values, initialize
    private int resourceMaxEntries = SM_RESOURCE_CACHE_SIZE_DEFAULT;
    private long resourceMaxAge = SM_RESOURCE_CACHE_MAX_AGE_DEFAULT;
    private int authenticationMaxEntries = SM_AUTHENTICATION_CACHE_SIZE_DEFAULT;
    private long authenticationMaxAge = SM_AUTHENTICATION_CACHE_MAX_AGE_DEFAULT;
    private int authorizationMaxEntries = SM_AUTHORIZATION_CACHE_SIZE_DEFAULT;
    private long authorizationMaxAge = SM_AUTHORIZATION_CACHE_MAX_AGE_DEFAULT;
    private boolean isGlobal = true;

    public SiteMinderAgentContextCacheManager() {
        cacheMap = new ConcurrentSkipListMap<>();
    }

    public void setDefaultCacheSettings(int resMaxEntries, long resMaxAge,
                                        int authnMaxEntries, long authnMaxAge,
                                        int authzMaxEntries, long authzMaxAge,
                                        boolean global) {
        resourceMaxEntries = resMaxEntries;
        resourceMaxAge = resMaxAge;
        authenticationMaxEntries = authnMaxEntries;
        authenticationMaxAge = authnMaxAge;
        authorizationMaxEntries = authzMaxEntries;
        authorizationMaxAge = authzMaxAge;
        isGlobal = global;

        LOGGER.log(Level.FINE, "Changing default cache settings resource max entries {0} max age {1}, authentication max entries {2} max age {3}, authorization max entries {4} max age{5}, global cache {global}",
                new Object[]{resMaxEntries, resMaxAge, authnMaxEntries, authnMaxAge, authzMaxEntries, authzMaxAge, global});
    }

    /**
     * Add a new cache if it does not exist.  If any of the cache settings are null, the default will be used
     *
     * @param key key
     * @param resMaxEntries the maximum number of entries allowed in the resource cache
     * @param resMaxAge the maximum age of an entry in milliseconds for the resource cache
     * @param authnMaxEntries the maximum number of entries allowed in the authentication cache
     * @param authnMaxAge the maximum age of an entry in milliseconds for the authentication cache
     * @param authzMaxEntries the maximum number of entries allowed in the authorization cache
     * @param authzMaxAge the maximum age of an entry in milliseconds for the authorization cache
     */
    private void addCache(@NotNull Key key,
                          Integer resMaxEntries, Long resMaxAge,
                          Integer authnMaxEntries, Long authnMaxAge,
                          Integer authzMaxEntries, Long authzMaxAge) {

        int resourceMaxEntries = (resMaxEntries == null) ? this.resourceMaxEntries : resMaxEntries;
        long resourceMaxAge = (resMaxAge == null) ? this.resourceMaxAge : resMaxAge;
        int authenticationMaxEntries = (authnMaxEntries == null) ? this.authenticationMaxEntries : authnMaxEntries;
        long authenticationMaxAge = (authnMaxAge == null) ? this.authenticationMaxAge : authnMaxAge;
        int authorizationMaxEntries = (authzMaxEntries == null) ? this.authorizationMaxEntries : authzMaxEntries;
        long authorizationMaxAge = (authzMaxAge == null) ? this.authorizationMaxAge : authzMaxAge;

        SiteMinderAgentContextCache newCache = new SiteMinderAgentContextCache(key.toString(),
                resourceMaxEntries, resourceMaxAge, authenticationMaxEntries, authenticationMaxAge, authorizationMaxEntries, authorizationMaxAge);

        SiteMinderAgentContextCache previous = cacheMap.putIfAbsent(key, newCache);

        if (previous == null) {
            LOGGER.log(Level.FINE,
                    "Initialized new cache: {0}, resourceCache: size {1} maxAge {2}, authnCache: size {3}  maxAge {4}, authzCache: size {5} maxAge {6}",
                    new Object[]{key, resourceMaxEntries, resourceMaxAge, authenticationMaxEntries, authenticationMaxAge, authorizationMaxEntries, authorizationMaxAge});
        }
    }

    /**
     * Is use global cache
     * @return true if is configured to use global cache; false otherwise
     */
    public boolean isUseGlobalCache() {
        return isGlobal;
    }

    /**
     * Get the global cache key
     * @return the global cache key
     */
    private Key getGlobalCacheKey() {
        return new Key(Goid.DEFAULT_GOID, GLOBAL_CACHE_AGENT_NAME);
    }

    /**
     * Get the global cache
     * @return global cache
     */
    public SiteMinderAgentContextCache getGlobalCache() {
        LOGGER.log(Level.FINE, "Getting global cache...");
        return getCache(getGlobalCacheKey(), null, null, null, null, null, null);
    }

    /**
     * Get cache associated with the Goid and smAgentName
     * @param smConfigGoid siteminder config goid
     * @param smAgentName agent name
     * @return cache if exists; null otherwise
     */
    public SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName) {
        return getCache(new Key(smConfigGoid, smAgentName));
    }

    /**
     * Get cache.  Cache is created if it does not exist.
     * @param smConfigGoid siteminder config goid
     * @param smAgentName agent name
     * @param resourceMaxEntries the maximum number of entries allowed in the resource cache
     * @param resourceMaxAge the maximum age of an entry in milliseconds for the resource cache
     * @param authnMaxEntries the maximum number of entries allowed in the authentication cache
     * @param authnMaxAge the maximum age of an entry in milliseconds for the authentication cache
     * @param authzMaxEntries the maximum number of entries allowed in the authorization cache
     * @param authzMaxAge the maximum age of an entry in milliseconds for the authorization cache
     * @return existing cache or new cache
     */
    public SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName,
                                                Integer resourceMaxEntries, Long resourceMaxAge,
                                                Integer authnMaxEntries, Long authnMaxAge,
                                                Integer authzMaxEntries, Long authzMaxAge) {
        Key key = new Key(smConfigGoid, smAgentName);
        return getCache(key, resourceMaxEntries, resourceMaxAge, authnMaxEntries, authnMaxAge, authzMaxEntries, authzMaxAge);
    }

    /**
     * Get cache.  Cache is created if it does not exist.
     * @param key key
     * @param resourceMaxEntries the maximum number of entries allowed in the resource cache
     * @param resourceMaxAge the maximum age of an entry in milliseconds for the resource cache
     * @param authnMaxEntries the maximum number of entries allowed in the authentication cache
     * @param authnMaxAge the maximum age of an entry in milliseconds for the authentication cache
     * @param authzMaxEntries the maximum number of entries allowed in the authorization cache
     * @param authzMaxAge the maximum age of an entry in milliseconds for the authorization cache
     * @return existing cache or new cache
     */
    private SiteMinderAgentContextCache getCache(@NotNull Key key,
                                                Integer resourceMaxEntries, Long resourceMaxAge,
                                                Integer authnMaxEntries, Long authnMaxAge,
                                                Integer authzMaxEntries, Long authzMaxAge) {
        SiteMinderAgentContextCache cache = getCache(key);

        if (cache != null) {
            return cache;
        }

        addCache(key, resourceMaxEntries, resourceMaxAge, authnMaxEntries, authnMaxAge, authzMaxEntries, authzMaxAge);

        // recursive call, should return a cache if no removal is done between add/get
        return getCache(key, resourceMaxEntries, resourceMaxAge, authnMaxEntries, authnMaxAge, authzMaxEntries, authzMaxAge);
    }

    // TODO: call this when SiteMinderConfiguration is updated
    /**
     * Remove the caches associated with the specified Goid.
     * Note: this operation is not atomic and might not remove all entries associated to the Goid if the entries are
     *       added concurrently.
     * @param smConfigGoid siteminder config Goid
     */
    public void removeCaches(@NotNull Goid smConfigGoid) {
        LOGGER.log(Level.FINE, "Removing caches for Goid {0}", smConfigGoid);

        // Look for the first key to be removed
        Key keyToRemove = cacheMap.higherKey(new Key(smConfigGoid, ""));

        while (keyToRemove != null && smConfigGoid.equals(keyToRemove.getConfigGoid())) {
            cacheMap.remove(keyToRemove);
            keyToRemove = cacheMap.higherKey(keyToRemove);
        }
    }

    /**
     * Get cache
     * @param key key to cache
     * @return cache if exists
     */
    private SiteMinderAgentContextCache getCache(@NotNull Key key) {
        LOGGER.log(Level.FINE, "Getting cache {0}", key);
        return cacheMap.get(key);
    }

    /**
     * Remove all caches
     * Note: this operation is not atomic and might not remove all entries if new entries are
     *       added by other thread at the same time.
     */
    public void removeAllCache() {
        cacheMap.clear();
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
         * @param o
         * @return
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
