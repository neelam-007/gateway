package com.ca.siteminder;

import com.ca.siteminder.SiteMinderAgentContextCache.AgentContextSubCache;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
     * Creates Agent Context cache with all the sub-cache details.
     * @param smConfigGoid SiteMinder config goid
     * @param smAgentName Agent name
     * @param subCaches list of Sub-cache
     * @return existing or new cache
     */
    SiteMinderAgentContextCache createCache(@NotNull Goid smConfigGoid, @NotNull String smAgentName,
                                            @NotNull List<AgentContextSubCache> subCaches);

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
    void removeAllCaches();
}
