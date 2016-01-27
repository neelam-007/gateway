package com.ca.siteminder;

import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;

/**
 * SiteMinderAgentContextCache holds the resource and session caches for a SiteMinder agent
 */
public class SiteMinderAgentContextCache {

    private final Cache resourceCache;
    private final Cache sessionCache;

    public SiteMinderAgentContextCache(@NotNull Cache resourceCache, @NotNull Cache sessionCache) {
        this.resourceCache = resourceCache;
        this.sessionCache = sessionCache;
    }

    /**
     * Get the resource cache
     * @return the resource cache
     */
    public Cache getResourceCache() {
        return resourceCache;
    }

    /**
     * Get the session cache
     * @return the session cache
     */
    public Cache getSessionCache() {
        return sessionCache;
    }

}
