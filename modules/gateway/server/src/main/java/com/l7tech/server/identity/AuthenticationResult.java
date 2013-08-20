package com.l7tech.server.identity;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ConfigFactory;
import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on successful authentication events.
 */
public final class AuthenticationResult {

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
        this( user, securityTokens, authenticatedCert, certWasSignedByStaleCA, System.currentTimeMillis(), getCache() );
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

    public void setCachedGroupMembership(Group group, boolean isMember) {
        groupCache.put(
                new CacheKey( user.getProviderId(), user.getId(), group.getProviderId(), group.getId() ),
                System.currentTimeMillis() * (isMember ? 1L : -1L) );
    }

    public Boolean getCachedGroupMembership(Group group) {
        Long when = groupCache.get(
                new CacheKey(user.getProviderId(), user.getId(),
                             group.getProviderId(), group.getId()));
        if (when == null) return null; // missed
        long w = when;
        boolean success = w > 0L;
        w = Math.abs(w);
        if (w < timestamp)
            return null; // ignore group membership checks cached before this authresult was created

        return success;
    }

    long getTimestamp() {
        return timestamp;
    }

    /**
     * Create an authentication result with fresh token.
     *
     * @param result The authentication result (required)
     * @param securityTokens The authenticating token(s) (required)
     */
    AuthenticationResult( final AuthenticationResult result,
                          final SecurityToken[] securityTokens ) {
        this(result.getUser(), securityTokens, result.getAuthenticatedCert(), result.isCertSignedByStaleCA(), result.getTimestamp(), getConsistentCache());
    }

    private AuthenticationResult( final User user,
                                  final SecurityToken[] securityTokens,
                                  final X509Certificate authenticatedCert,
                                  final boolean certWasSignedByStaleCA,
                                  final long timestamp,
                                  final GroupCache groupCache ) {
        if (user == null) throw new IllegalArgumentException("user is required");
        if (securityTokens == null || securityTokens.length==0) throw new IllegalArgumentException("security token(s) are required");
        if (certWasSignedByStaleCA && authenticatedCert == null) throw new IllegalArgumentException("Must include the stale cert if there is one");

        this.user = user;
        this.securityTokens = securityTokens;
        this.certSignedByStaleCA = certWasSignedByStaleCA;
        this.authenticatedCert = authenticatedCert;
        this.timestamp = timestamp;
        this.groupCache = groupCache;
    }

    private static GroupCache getCache() {
        return CacheHolder.CACHE;
    }

    private static GroupCache getConsistentCache() {
        return new ConsistentGroupCache();
    }

    private static class CacheKey {
        private static final Set<String> IGNORE_USER_ID = CollectionUtils.set("-1", Goid.toString( GoidEntity.DEFAULT_GOID ));

        private final Goid userProviderOid;
        private final String userId;
        private final Goid groupProviderOid;
        private final String groupId;
        private final int hashCode;

        private CacheKey(Goid userProviderOid, String userId, Goid groupProviderOid, String groupId) {
            if (userId == null || groupId == null) throw new NullPointerException();
            this.userProviderOid = userProviderOid;
            this.userId = userId;
            this.groupProviderOid = groupProviderOid;
            this.groupId = groupId;
            this.hashCode = makeHashCode();
        }

        private boolean isValid() {
            return !userId.isEmpty() && !IGNORE_USER_ID.contains( userId );
        }

        /** @noinspection RedundantIfStatement*/
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (groupProviderOid!=null?!groupProviderOid.equals(cacheKey.groupProviderOid):cacheKey.groupProviderOid!=null) return false;
            if (userProviderOid!=null?!userProviderOid.equals(cacheKey.userProviderOid):cacheKey.userProviderOid!=null) return false;
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
            result = (userProviderOid!=null?userProviderOid.hashCode():0);
            result = 29 * result + userId.hashCode();
            result = 29 * result + (groupProviderOid!=null?groupProviderOid.hashCode():0);
            result = 29 * result + groupId.hashCode();
            return result;
        }
    }

    private interface GroupCache {
        void put( CacheKey key, Long value );
        Long get( CacheKey key );
    }

    private static class CacheHolder {
        private static final GroupCache CACHE = new GroupCache() {
            private final int cacheSize =
                    ConfigFactory.getIntProperty( ServerConfigParams.PARAM_AUTH_CACHE_GROUP_MEMB_CACHE_SIZE, 1000 );
            @Nullable
            private final Cache membershipCache = cacheSize < 1
                                               ? null
                                               : WhirlycacheFactory.createCache("groupMemberships",
                    cacheSize,
                                                                                71,
                                                                                WhirlycacheFactory.POLICY_LFU);

            @Override
            public void put( final CacheKey key, final Long value ) {
                if ( membershipCache != null && key.isValid() ) {
                    membershipCache.store( key, value );
                }
            }

            @Override
            public Long get( final CacheKey key ) {
                Long expiryAndFlag = null;
                if ( membershipCache != null ) {
                    expiryAndFlag = (Long) membershipCache.retrieve( key );
                }
                return expiryAndFlag;
            }
        };
    }

    /**
     * A consistent GroupCache that will hold a reference to the underlying
     * group membership information for the lifetime of the encapsulating
     * AuthenticationResult (which should be appropriately scoped)
     *
     * This wrapping provides a consistent view of group membership for the
     * lifetime of a request.
     */
    private static final class ConsistentGroupCache implements GroupCache {
        private final GroupCache cache = getCache();
        private final Map<CacheKey,Long> consistentCache = Collections.synchronizedMap( new HashMap<CacheKey,Long>() );

        @Override
        public void put( final CacheKey key, final Long value ) {
            // Ok to store invalid keys since this cache is scoped to a single AuthenticationResult
            consistentCache.put( key, value );
            cache.put( key, value );
        }

        @Override
        public Long get( final CacheKey key ) {
            Long value = consistentCache.get( key );
            if ( value == null ) {
                value = cache.get( key );
                if ( value != null ) {
                    consistentCache.put( key, value );
                }
            }
            return value;
        }
    }

    private final GroupCache groupCache;
    private final User user;
    private final long timestamp;
    private final boolean certSignedByStaleCA;
    private final X509Certificate authenticatedCert;
    private final SecurityToken[] securityTokens; // should not be accessible

}

