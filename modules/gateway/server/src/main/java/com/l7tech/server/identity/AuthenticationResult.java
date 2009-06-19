package com.l7tech.server.identity;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.server.ServerConfig;
import com.l7tech.security.token.SecurityToken;
import com.whirlycott.cache.Cache;

import java.security.cert.X509Certificate;

/**
 * Created on successful authentication events.
 */
public final class AuthenticationResult {

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

    /**
     * Create an authentication result with given settings.
     *
     * <p>NOTE: The <code>authenticatedCert</code> is only set when the user
     * authenticated using the certificate. If the user simply has a certificate
     * the the value MUST be <code>null</code>.</p>
     *
     * @param user The authenticated user (required)
     * @param securityToken The authenticating token (required)
     * @param authenticatedCert The authenticating certificate (required if certWasSignedByStaleCA)
     * @param certWasSignedByStaleCA True if the authenticating certificate was possibly issued by for (our) prior CA cert
     */
    public AuthenticationResult( final User user,
                                 final SecurityToken securityToken,
                                 final X509Certificate authenticatedCert,
                                 final boolean certWasSignedByStaleCA ) {
        this(user, new SecurityToken[]{securityToken}, authenticatedCert, certWasSignedByStaleCA);            
    }

    /**
     * Create an authentication result with given settings.
     *
     * <p>NOTE: The <code>authenticatedCert</code> is only set when the user
     * authenticated using the certificate. If the user simply has a certificate
     * the the value MUST be <code>null</code>.</p>
     *
     * @param user The authenticated user (required)
     * @param securityTokens The authenticating token(s) (required)
     * @param authenticatedCert The authenticating certificate (required if certWasSignedByStaleCA)
     * @param certWasSignedByStaleCA True if the authenticating certificate was possibly issued by for (our) prior CA cert
     */
    public AuthenticationResult( final User user,
                                 final SecurityToken[] securityTokens,
                                 final X509Certificate authenticatedCert,
                                 final boolean certWasSignedByStaleCA ) {
        if (user == null) throw new IllegalArgumentException("user is required");
        if (securityTokens == null || securityTokens.length==0) throw new IllegalArgumentException("security token(s) are required");
        if (certWasSignedByStaleCA && authenticatedCert == null) throw new IllegalArgumentException("Must include the stale cert if there is one");

        this.user = user;
        this.securityTokens = securityTokens;
        this.certSignedByStaleCA = certWasSignedByStaleCA;
        this.authenticatedCert = authenticatedCert;
    }

    /**
     * Create an authentication result for the given user.
     *
     * @param user The authenticated user (required)
     * @param securityTokens The authenticating token(s) (required)
     */
    public AuthenticationResult( final User user,
                                 final SecurityToken[] securityTokens ) {
        this(user, securityTokens, null, false);
    }

    /**
     * Create an authentication result for the given user.
     *
     * @param user The authenticated user (required)
     * @param securityToken The authenticating token (required)
     */
    public AuthenticationResult( final User user,
                                 final SecurityToken securityToken ) {
        this(user, securityToken, null, false);
    }

    /**
     * Create an authentication result with fresh token.
     *
     * @param result The authentication result (required)
     * @param securityTokens The authenticating token(s) (required)
     */
    public AuthenticationResult( final AuthenticationResult result,
                                 final SecurityToken[] securityTokens ) {
        this(result.getUser(), securityTokens, result.getAuthenticatedCert(), result.isCertSignedByStaleCA());
    }

    /**
     * Get the authenticated User.
     *
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Was the certificate possibly signed for one of our previous CA certs?
     *
     * @return True if possibly signed by a prior CA.
     */
    public boolean isCertSignedByStaleCA() {
        return certSignedByStaleCA;
    }

    /**
     * Get the authenticating certificate.
     *
     * @return The certificate or null
     */
    public X509Certificate getAuthenticatedCert() {
        return authenticatedCert;
    }

    /**
     * Is this authentication result for the given token.
     *
     * @param token The token to check
     * @return True if the given token is not null and matches
     */
    public boolean matchesSecurityToken( final SecurityToken token ) {
        boolean matches = false;

        if ( token != null ) {
            for ( SecurityToken securityToken : securityTokens ) {
                if ( token == securityToken ) {
                    matches = true;
                    break;
                }
            }
        }

        return matches;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if ( !(obj instanceof AuthenticationResult) ) return false;

        AuthenticationResult authResult = (AuthenticationResult) obj;

        if ( !authResult.getUser().equals(this.user) ) return false;

        //i'm not sure if I need to compare the following, but are here just in case.
        if ( authResult.isCertSignedByStaleCA() != this.certSignedByStaleCA ) return false;
        X509Certificate authenticatedCert = authResult.getAuthenticatedCert();        
        if ( authenticatedCert != this.authenticatedCert ){
            if ( authenticatedCert == null || !authenticatedCert.equals(this.authenticatedCert) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + ( certSignedByStaleCA ? 1 : 0 );
        result = 31 * result + ( authenticatedCert != null ? authenticatedCert.hashCode() : 0 );
        return result;
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
        @Override
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

        @Override
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
        cache.store(
                new CacheKey(user.getProviderId(),  user.getId(), group.getProviderId(), group.getId()),
                System.currentTimeMillis() * (isMember ? 1 : -1));
    }

    public Boolean getCachedGroupMembership(Group group) {
        Cache cache = getCache();
        if (cache == null) return null; // fail fast if caching disabled
        Long when = (Long)cache.retrieve(
                new CacheKey(user.getProviderId(), user.getId(),
                             group.getProviderId(), group.getId()));
        if (when == null) return null; // missed
        long w = when;
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
    private final X509Certificate authenticatedCert;
    private final SecurityToken[] securityTokens; // should not be accessible
}

