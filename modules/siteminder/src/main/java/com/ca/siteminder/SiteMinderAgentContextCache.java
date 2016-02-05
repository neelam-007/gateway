package com.ca.siteminder;

import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;

/**
 * SiteMinderAgentContextCache holds the resource and session caches for a SiteMinder agent
 */
public class SiteMinderAgentContextCache {

    private final Cache resourceCache;
    private final Cache authenticationCache;
    private final Cache authorizationCache;

    private final long resourceCacheMaxAge;
    private final long authorizationCacheMaxAge;
    private final long authenticationCacheMaxAge;

    public SiteMinderAgentContextCache(@NotNull Cache resourceCache, long resourceCacheMaxAge, @NotNull Cache authenticationCache,
                                       long authenticationCacheMaxAge, @NotNull Cache authorizationCache, long authorizationCacheMaxAge) {
        this.resourceCache = resourceCache;
        this.resourceCacheMaxAge = resourceCacheMaxAge;
        this.authenticationCache = authenticationCache;
        this.authenticationCacheMaxAge = authenticationCacheMaxAge;
        this.authorizationCache = authorizationCache;
        this.authorizationCacheMaxAge = authorizationCacheMaxAge;
    }

    /**
     * Get the resource cache
     * @return the resource cache
     */
    public Cache getResourceCache() {
        return resourceCache;
    }

    /**
     * Get the authentication cache
     * @return the authentication cache
     */
    public Cache getAuthenticationCache() {
        return authenticationCache;
    }

    /**
     * Get the authorization cache
     * @return the authorization cache
     */
    public Cache getAuthorizationCache() {
        return authorizationCache;
    }

    public long getResourceCacheMaxAge() {
        return resourceCacheMaxAge;
    }

    public long getAuthorizationCacheMaxAge() {
        return authorizationCacheMaxAge;
    }

    public long getAuthenticationCacheMaxAge() {
        return authenticationCacheMaxAge;
    }

    public static class ResourceCacheKey {
        private final String resource;
        private final String action;

        public ResourceCacheKey(@NotNull String resource, @NotNull String action) {
            this.resource = resource;
            this.action = action;
        }

        public String getResource() {
            return resource;
        }

        public String getAction() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceCacheKey that = (ResourceCacheKey) o;

            if (!action.equals(that.action)) return false;
            if (!resource.equals(that.resource)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = resource.hashCode();
            result = 31 * result + action.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ResourceCacheKey{" +
                    "resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

    public static class AuthenticationCacheKey {

        private final String sessionId;
        private final String realmOid;

        public AuthenticationCacheKey(@NotNull String sessionId, @NotNull String realmOid) {
            this.sessionId = sessionId;
            this.realmOid = realmOid;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getRealmOid() {
            return realmOid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AuthenticationCacheKey that = (AuthenticationCacheKey) o;

            if (!sessionId.equals(that.sessionId)) return false;
            if (!realmOid.equals(that.realmOid)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = sessionId.hashCode();
            result = 31 * result + (realmOid.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "AuthenticationCacheKey{" +
                    "sessionId='" + sessionId + '\'' +
                    ", realmOid='" + realmOid + '\'' +
                    '}';
        }
    }

    public static class AuthorizationCacheKey {

        private final String sessionId;
        private final String resource;
        private final String action;

        public AuthorizationCacheKey(@NotNull String sessionId, @NotNull String resource, @NotNull String action) {
            this.sessionId = sessionId;
            this.resource = resource;
            this.action = action;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getResource() {
            return resource;
        }

        public String getAction() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AuthorizationCacheKey that = (AuthorizationCacheKey) o;

            if (!action.equals(that.action)) return false;
            if (!resource.equals(that.resource)) return false;
            if (!sessionId.equals(that.sessionId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = sessionId.hashCode();
            result = 31 * result + (resource.hashCode());
            result = 31 * result + (action.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "AuthorizationCacheKey{" +
                    "sessionId='" + sessionId + '\'' +
                    ", resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    '}';
        }
    }
}
