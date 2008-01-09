package com.l7tech.server.identity;

import com.l7tech.common.util.WhirlycacheFactory;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.server.ServerConfig;
import com.whirlycott.cache.Cache;

import java.security.cert.X509Certificate;

/**
 * Created on successful authentication events.  Semi-threadsafe (mutators are synchronized).
 */
public final class AuthenticationResult {
    public static final AuthenticationResult AUTHENTICATED_UNKNOWN_USER = new AuthenticationResult();

    private static class CacheHolder {
        private static final int CACHE_SIZE =
                ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_GROUP_MEMB_CACHE_SIZE, 1000, 15000);
        private static final Cache CACHE = CACHE_SIZE < 1
                                           ? null
                                           : WhirlycacheFactory.createCache("groupMemberships",
                                                                            CACHE_SIZE,
                                                                            71,
                                                                            WhirlycacheFactory.POLICY_LFU);
    }

    private AuthenticationResult() {
        user = null;
        certSignedByStaleCA = false;
    }

    public AuthenticationResult(User user, X509Certificate authenticatedCert, boolean certWasSignedByStaleCA) {
        this.user = user;
        if (user == null) throw new NullPointerException();
        this.certSignedByStaleCA = certWasSignedByStaleCA;
        this.authenticatedCert = authenticatedCert;
        if (certSignedByStaleCA && authenticatedCert == null) throw new IllegalArgumentException("Must include the stale cert if there is one");
    }

    public AuthenticationResult(User user) {
        this(user, null, false);
    }

    public User getUser() {
        if (user == null) throw new UnsupportedOperationException("Unknown authenticated user");
        return user;
    }

    public boolean isCertSignedByStaleCA() {
        return certSignedByStaleCA;
    }

    public synchronized X509Certificate getAuthenticatedCert() {
        return authenticatedCert;
    }

    public synchronized void setAuthenticatedCert(X509Certificate authenticatedCert) {
        this.authenticatedCert = authenticatedCert;
    }

    private static class CacheKey {
        private final long userProviderOid;
        private final String userId;
        private final long groupProviderOid;
        private final String groupId;
        private final int hashCode;

        public CacheKey(long userProviderOid, String userId, long groupProviderOid, String groupId) {
            if (userId == null || groupId == null) throw new NullPointerException();
            this.userProviderOid = userProviderOid;
            this.userId = userId;
            this.groupProviderOid = groupProviderOid;
            this.groupId = groupId;
            this.hashCode = makeHashCode();
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (groupProviderOid != cacheKey.groupProviderOid) return false;
            if (userProviderOid != cacheKey.userProviderOid) return false;
            if (!groupId.equals(cacheKey.groupId)) return false;
            if (!userId.equals(cacheKey.userId)) return false;

            return true;
        }

        public int hashCode() {
            return hashCode;
        }

        private int makeHashCode() {
            int result;
            result = (int)(userProviderOid ^ (userProviderOid >>> 32));
            result = 29 * result + userId.hashCode();
            result = 29 * result + (int)(groupProviderOid ^ (groupProviderOid >>> 32));
            result = 29 * result + groupId.hashCode();
            return result;
        }
    }

    private static Cache getCache() {
        return CacheHolder.CACHE;
    }

    public void setCachedGroupMembership(Group group, boolean isMember) {
        Cache cache = getCache();
        if (cache == null) return; // fail fast if caching disabled
        cache.store(new CacheKey(user.getProviderId(), user.getId(),
                                                group.getProviderId(), group.getId()),
                                   new Long(System.currentTimeMillis() * (isMember ? 1 : -1)));
    }

    public Boolean getCachedGroupMembership(Group group) {
        Cache cache = getCache();
        if (cache == null) return null; // fail fast if caching disabled
        Long when = (Long)cache.retrieve(
                new CacheKey(user.getProviderId(), user.getId(),
                             group.getProviderId(), group.getId()));
        if (when == null) return null; // missed
        long w = when.longValue();
        boolean success = w > 0;
        w = Math.abs(w);
        if (w < timestamp) return null; // ignore group membership checks cached before this authresult was created
        //noinspection UnnecessaryBoxing
        return Boolean.valueOf(success);
    }

    public long getTimestamp() {
        return timestamp;
    }

    private final User user;
    private final long timestamp = System.currentTimeMillis();
    private final boolean certSignedByStaleCA;

    private X509Certificate authenticatedCert;
}

