package com.ca.siteminder;

import com.sun.istack.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SiteMinderAgentContextCache holds the resource, authentication, and authorization cache for a siteminder agent
 */
public class SiteMinderAgentContextCache {
    private static final Logger LOGGER = Logger.getLogger(SiteMinderAgentContextCache.class.getName());

    public static final String RESOURCE_CACHE_NAME = "isProtected";
    public static final String AUTHENTICATION_CACHE_NAME = "isAuthN";
    public static final String AUTHORIZATION_CACHE_NAME = "isAuthZ";

    private final SiteMinderContextCache resourceCache;
    private final SiteMinderContextCache authenticationCache;
    private final SiteMinderContextCache authorizationCache;

    /**
     * Constructor
     *
     * @param name the name
     * @param resourceMaxEntries the maximum number of entries allowed in the resource cache
     * @param resourceMaxAge the maximum age of an entry in milliseconds for the resource cache
     * @param authnMaxEntries the maximum number of entries allowed in the authentication cache
     * @param authnMaxAge the maximum age of an entry in milliseconds for the authentication cache
     * @param authzMaxEntries the maximum number of entries allowed in the authorization cache
     * @param authzMaxAge the maximum age of an entry in milliseconds for the authorization cache
     */
    public SiteMinderAgentContextCache(@NotNull String name,
                                       int resourceMaxEntries, long resourceMaxAge,
                                       int authnMaxEntries, long authnMaxAge,
                                       int authzMaxEntries, long authzMaxAge) {
        LOGGER.log(Level.FINE, "Creating SiteMinder Agent Cache for {0}", name);
        resourceCache = new SiteMinderContextCache(name + "." + RESOURCE_CACHE_NAME, resourceMaxEntries, resourceMaxAge);
        authenticationCache = new SiteMinderContextCache(name + "." + AUTHENTICATION_CACHE_NAME, authnMaxEntries, authnMaxAge);
        authorizationCache = new SiteMinderContextCache(name + "." + AUTHORIZATION_CACHE_NAME, authzMaxEntries, authzMaxAge);
    }

    /**
     * Get the resource cache
     * @return the resource cache
     */
    public SiteMinderContextCache getResourceCache() {
        return resourceCache;
    }

    /**
     * Get the authentication cache
     * @return the authentication cache
     */
    public SiteMinderContextCache getAuthenticationCache() {
        return authenticationCache;
    }

    /**
     * Get the authorization cache
     * @return the authorization cache
     */
    public SiteMinderContextCache getAuthorizationCache() {
        return authorizationCache;
    }
}
