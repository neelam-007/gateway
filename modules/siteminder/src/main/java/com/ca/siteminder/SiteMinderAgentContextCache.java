package com.ca.siteminder;

import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SiteMinderAgentContextCache holds the resource and session caches for a SiteMinder agent
 */
public class SiteMinderAgentContextCache {

    public enum AgentContextSubCacheType {

        AGENT_CACHE_RESOURCE("resource"),
        AGENT_CACHE_AUTHENTICATION("authentication"),
        AGENT_CACHE_AUTHORIZATION("authorization"),
        AGENT_CACHE_ACO("aco");

        private String name;

        AgentContextSubCacheType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static class AgentContextSubCache {

        private AgentContextSubCacheType cacheType;
        private final Cache cache;
        private final int maxSize;
        private final long maxAge;

        public AgentContextSubCache(@Nullable Cache cache, AgentContextSubCacheType cacheType, int maxSize, long maxAge) {
            this.cache = cache;
            this.cacheType = cacheType;
            this.maxSize = maxSize;
            this.maxAge = maxAge;
        }

        public AgentContextSubCacheType getCacheType() {
            return cacheType;
        }

        public Cache getCache() {
            return cache;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public long getMaxAge() {
            return maxAge;
        }
    }

    private final Map<AgentContextSubCacheType, AgentContextSubCache> subCacheMap = new HashMap<>();

    public SiteMinderAgentContextCache(@NotNull final List<AgentContextSubCache> subCaches) {
        for (AgentContextSubCache subCache : subCaches) {
            subCacheMap.put(subCache.getCacheType(), subCache);
        }
    }

    public AgentContextSubCache getSubCache(AgentContextSubCacheType type) {
        return subCacheMap.get(type);
    }

    public static class ResourceCacheKey {
        private final String resource;
        private final String action;
        private final String serverName;

        public ResourceCacheKey(@NotNull String resource, @NotNull String action, @NotNull String serverName) {
            this.resource = resource;
            this.action = action;
            this.serverName = serverName;
        }

        public String getResource() {
            return resource;
        }

        public String getAction() {
            return action;
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

            return true;
        }

        @Override
        public int hashCode() {
            int result = resource.hashCode();
            result = 31 * result + action.hashCode() + serverName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ResourceCacheKey{" +
                    "resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    ", serverName=" + serverName + '\'' +
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
