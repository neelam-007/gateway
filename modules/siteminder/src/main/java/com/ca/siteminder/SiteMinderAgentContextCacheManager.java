package com.ca.siteminder;

import com.l7tech.objectmodel.Goid;
import com.sun.istack.NotNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface SiteMinderAgentContextCacheManager {
    void setDefaultCacheSettings(int resMaxEntries, long resMaxAge,
                                 int authnMaxEntries, long authnMaxAge,
                                 int authzMaxEntries, long authzMaxAge,
                                 boolean global);

    /**
     * Is use global cache
     * @return true if is configured to use global cache; false otherwise
     */
    boolean isUseGlobalCache();

    /**
     * Get the global cache
     * @return global cache
     */
    SiteMinderAgentContextCache getGlobalCache();

    /**
     * Get cache associated with the Goid and smAgentName
     * @param smConfigGoid siteminder config goid
     * @param smAgentName agent name
     * @return cache if exists; null otherwise
     */
    SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName);

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
    SiteMinderAgentContextCache getCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName,
                                         Integer resourceMaxEntries, Long resourceMaxAge,
                                         Integer authnMaxEntries, Long authnMaxAge,
                                         Integer authzMaxEntries, Long authzMaxAge);

    /**
     * Remove the caches associated with the specified Goid.
     * Note: this operation is not atomic and might not remove all entries associated to the Goid if the entries are
     *       added concurrently.
     * @param smConfigGoid siteminder config Goid
     */
    void removeCaches(@NotNull Goid smConfigGoid);

    /**
     * Remove all caches
     * Note: this operation is not atomic and might not remove all entries if new entries are
     *       added by other thread at the same time.
     */
    void removeAllCache();
}
