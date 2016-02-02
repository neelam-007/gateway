package com.ca.siteminder;

import com.l7tech.objectmodel.Goid;
import com.sun.istack.NotNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface SiteMinderAgentContextCacheManager {
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
     * @return existing cache or new cache
     */
    SiteMinderAgentContextCache createCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName,
                                         int resourceMaxEntries, int authenticationMaxEntries,
                                         int authorizationMaxEntries);

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
