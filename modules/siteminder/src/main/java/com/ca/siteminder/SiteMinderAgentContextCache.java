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

    public static class ResourceCacheKey {
        private final String resource;
        private final String action;
        private final String userIp;
        private final String serverName;

        public ResourceCacheKey(@NotNull String resource, @NotNull String action,
                                @NotNull String userIp, @NotNull String serverName) {
            this.resource = resource;
            this.action = action;
            this.userIp = userIp;
            this.serverName = serverName;
        }

        public String getResource() {
            return resource;
        }

        public String getAction() {
            return action;
        }

        public String getUserIp() {
            return userIp;
        }

        public String getServerName() {
            return serverName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceCacheKey that = (ResourceCacheKey) o;

            if (!action.equals(that.action)) return false;
            if (!resource.equals(that.resource)) return false;
            if (!serverName.equals(that.serverName)) return false;
            if (!userIp.equals(that.userIp)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = resource.hashCode();
            result = 31 * result + action.hashCode();
            result = 31 * result + userIp.hashCode();
            result = 31 * result + serverName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ResourceCacheKey{" +
                    "resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    ", userIp='" + userIp + '\'' +
                    ", serverName='" + serverName + '\'' +
                    '}';
        }
    }

    public static class SessionCacheKey {

        public enum AuthType { AUTHENTICATION, AUTHORIZATION }

        private final String sessionId;
        private final String realmOid;
        private final String resource;
        private final String action;
        private final AuthType authType;

        public SessionCacheKey(@NotNull String sessionId, @NotNull String realmOid,
                               @NotNull String resource, @NotNull String action, @NotNull AuthType authType) {
            this.sessionId = sessionId;
            this.realmOid = realmOid;
            this.resource = resource;
            this.action = action;
            this.authType = authType;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getRealmOid() {
            return realmOid;
        }

        public String getResource() {
            return resource;
        }

        public String getAction() {
            return action;
        }

        public AuthType getAuthType() {
            return authType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SessionCacheKey that = (SessionCacheKey) o;

            if (!action.equals(that.action)) return false;
            if (!realmOid.equals(that.realmOid)) return false;
            if (!resource.equals(that.resource)) return false;
            if (!sessionId.equals(that.sessionId)) return false;
            if (!authType.equals(that.authType)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = sessionId.hashCode();
            result = 31 * result + (realmOid.hashCode());
            result = 31 * result + (resource.hashCode());
            result = 31 * result + (action.hashCode());
            result = 31 * result + (authType.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "SessionCacheKey{" +
                    "sessionId='" + sessionId + '\'' +
                    ", realmOid='" + realmOid + '\'' +
                    ", resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    ", authType=" + authType +
                    '}';
        }
    }
}
