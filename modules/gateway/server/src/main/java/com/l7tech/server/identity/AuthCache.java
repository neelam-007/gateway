package com.l7tech.server.identity;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeSource;
import com.whirlycott.cache.Cache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cached authentication.
 */
public final class AuthCache {
    private static final Logger logger = Logger.getLogger(AuthCache.class.getName());

    private static final int SUCCESS_CACHE_TUNER_INTERVAL = 59;
    private static final int FAILURE_CACHE_TUNER_INTERVAL = 61;

    public final static int SUCCESS_CACHE_SIZE = ConfigFactory.getIntProperty( ServerConfigParams.PARAM_AUTH_CACHE_SUCCESS_CACHE_SIZE, 200 );
    public final static int FAILURE_CACHE_SIZE = ConfigFactory.getIntProperty( ServerConfigParams.PARAM_AUTH_CACHE_FAILURE_CACHE_SIZE, 100 );

    private static final boolean AUTH_MUTEX_ENABLED = ConfigFactory.getBooleanProperty( "com.l7tech.server.identity.authCacheMutexEnabled", true );

    private final TimeSource timeSource;
    private final Cache successCache;
    private final Cache failureCache;
    private final boolean successCacheDisabled;
    private final boolean failureCacheDisabled;

    AuthCache() {
        this(   "AuthCache",
                new TimeSource(),
                SUCCESS_CACHE_SIZE,
                SUCCESS_CACHE_TUNER_INTERVAL,
                FAILURE_CACHE_SIZE,
                FAILURE_CACHE_TUNER_INTERVAL);
    }

    AuthCache(final String name,
              final TimeSource source,
              final int successCacheSize,
              final int successTunerInterval,
              final int failureCacheSize,
              final int failureTunerInterval) {
        timeSource = source;

        successCache = successCacheSize < 1 ? null :
                WhirlycacheFactory.createCache(name + ".success", successCacheSize, successTunerInterval, WhirlycacheFactory.POLICY_LFU);
        successCacheDisabled = (successCache == null);
        if(successCacheDisabled){
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,"Successful authentication caching has been disabled via configuration");
        }

        failureCache = failureCacheSize < 1 ? null :
                WhirlycacheFactory.createCache(name + ".failure", failureCacheSize, failureTunerInterval, WhirlycacheFactory.POLICY_LFU); 

        failureCacheDisabled = (failureCache == null);
        if(failureCacheDisabled){
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,"Failed authentication caching has been disabled via configuration");
        }
    }

    public static int getDefaultAuthSuccessCacheTime() {
        return ConfigFactory.getIntProperty(ServerConfigParams.PARAM_AUTH_CACHE_MAX_SUCCESS_TIME, 1000);
    }

    public static int getDefaultAuthFailureCacheTime() {
        return ConfigFactory.getIntProperty(ServerConfigParams.PARAM_AUTH_CACHE_MAX_FAILURE_TIME, 1000);
    }

    void dispose() {
        WhirlycacheFactory.shutdown(successCache);
        WhirlycacheFactory.shutdown(failureCache);    
    }

    private static class CacheKey {
        private int cachedHashcode = -1;
        private final Goid providerOid;
        private final LoginCredentials creds;

        private CacheKey(Goid providerOid, LoginCredentials creds) {
            this.providerOid = providerOid;
            this.creds = creds;
        }

        /** @noinspection RedundantIfStatement*/
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (providerOid != null ? !providerOid.equals(cacheKey.providerOid) : cacheKey.providerOid != null) return false;
            if (creds != null ? !creds.equals(cacheKey.creds) : cacheKey.creds != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            if (cachedHashcode == -1) {
                int result;
                result = (providerOid != null ? providerOid.hashCode() : 0);
                result = 31 * result + (creds != null ? creds.hashCode() : 0);
                cachedHashcode = result;
            }
            return cachedHashcode;
        }
    }

    private static class InstanceHolder {
        private static final AuthCache INSTANCE = new AuthCache();
    }

    public static AuthCache getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Gets a cached AuthenticationResult based on the supplied credentials, if one is available
     * and its timestamp has not expired, or null if the authentication failed.
     * <p/>
     * This uses the current default success and failure cache lifetimes.
     *
     * @param creds         the credentials presented in the request
     * @param idp           the IdentityProvider to use in case a re-authentication is required
     * @return the cached AuthenticationResult if one was available from cache, or null if not.
     * @throws AuthenticationException if the authentication could not be performed for some low-level reason
     */
    public AuthenticationResult getCachedAuthResult(LoginCredentials creds, IdentityProvider idp) throws AuthenticationException {
        return getCachedAuthResult(creds, idp, getDefaultAuthSuccessCacheTime(), getDefaultAuthFailureCacheTime());
    }

    /**
     * Gets a cached AuthenticationResult based on the supplied credentials, if one is available
     * and its timestamp has not expired, or null if the authentication failed.
     *
     * @param creds         the credentials presented in the request
     * @param idp           the IdentityProvider to use in case a re-authentication is required
     * @param maxSuccessAge the maximum age, in milliseconds, to cache successful authentications
     * @param maxFailAge    the maximum age, in milliseconds, to cache failed authentications
     * @return the cached AuthenticationResult if one was available from cache, or null if not.
     * @throws AuthenticationException if the authentication could not be performed for some low-level reason
     */
    public AuthenticationResult getCachedAuthResult(LoginCredentials creds, IdentityProvider idp,
                                                    int maxSuccessAge, int maxFailAge)
            throws AuthenticationException
    {
        String credString = creds.toString();
        final Goid providerOid = idp.getConfig().getGoid();
        final CacheKey ckey = new CacheKey(providerOid, creds);
        Object cached = getCacheEntry(ckey, credString, idp, maxSuccessAge, maxFailAge);
        if (cached instanceof AuthenticationResult) {
            return new AuthenticationResult((AuthenticationResult)cached, creds.getSecurityTokens());
        } else if (cached != null) {
            return null;
        }

        // There was a successCache miss before, so someone has to authenticate it.

        // We'll allow multiple simultaneous authentications for the same credentials only for internal password auth
        boolean isInternalPasswordAuth =
                idp instanceof InternalIdentityProvider &&
                (creds.getFormat() == CredentialFormat.CLEARTEXT ||
                 creds.getFormat() == CredentialFormat.BASIC ||
                 creds.getFormat() == CredentialFormat.DIGEST);

        if (AUTH_MUTEX_ENABLED && !successCacheDisabled && !isInternalPasswordAuth) {
            // Let's make sure only one thread does so on this SSG.
            // Lock username so we only auth it on one thread at a time
            String credsMutex = (Goid.toString(providerOid) + credString).intern();
            synchronized (credsMutex) {
                // Recheck successCache now that we have the username lock
                cached = getCacheEntry(ckey, credString, idp, maxSuccessAge, maxFailAge);
                if (cached instanceof AuthenticationResult) {
                    // Someone else got there first with a success
                    return new AuthenticationResult((AuthenticationResult)cached, creds.getSecurityTokens());
                } else if (cached != null) {
                    // Someone else got there first with a failure
                    return null;
                }
                return getAndCacheNewResult(creds, credString, ckey, idp);
            }
        }

        // Either AUTH_ONE_AT_A_TIME specifically or all caching in general is disabled.  Skip locking and Just Do It.
        return getAndCacheNewResult(creds, credString, ckey, idp);
    }

    // If caller wants only one thread at a time to authenticate any given username,
    // caller is responsible for ensuring that only one thread at a time calls this per username,
    private AuthenticationResult getAndCacheNewResult(LoginCredentials creds, String credString, CacheKey ckey, IdentityProvider idp)
            throws AuthenticationException
    {
        AuthenticationResult result = null;
        AuthenticationException thrown = null;
        try {
            result = ((AuthenticatingIdentityProvider)idp).authenticate(creds);
        } catch (AuthenticationException e) {
            thrown = e;
        }
        String which = null;

        // Skip if successCache is disabled

        if (!failureCacheDisabled && result == null) {
            which = "failed";
            failureCache.store(ckey, currentTimeMillis());
        }else if(!successCacheDisabled){
            which = "successful";
            successCache.store(ckey, result);
        }

        if(which != null){
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Caching {0} authentication for {1} on IdP \"{2}\"",
                           new String[]{which, credString, idp.getConfig().getName()});
        }

        if (thrown != null) {
            throw thrown;
        } else if ( result != null ) {
            return new AuthenticationResult(result, creds.getSecurityTokens());
        } else {
            return null;
        }
    }

    /**
     * Gets a cache entry
     * Fails fast by returning null when both the successCache and the failureCache are disabled.
     * In the case when one is disabled, it will proceed with the lookup and then return null when not found
     * , null will always be found for the cache entry which is disabled
     * @return Object either an AuthenticationResult on a success hit, or a Long on a failure hit, null when both caches
     * miss OR there was a hit but the cache values have expired and have not yet been cleaned from the cache
     */
    private Object getCacheEntry(CacheKey ckey, String credString, IdentityProvider idp, int maxSuccessAge, int maxFailAge) {

        if (successCacheDisabled && failureCacheDisabled) return null; // fail fast if successCache and failureCache is disabled        

        Long cachedFailureTime = null;
        AuthenticationResult cachedAuthResult = null;

        //Determine if the success cache has this key
        if(!successCacheDisabled){
            Object cachedObj = successCache.retrieve(ckey);
            if(cachedObj != null && cachedObj instanceof AuthenticationResult){
                cachedAuthResult = (AuthenticationResult)cachedObj;
            }
        }
        //it doesn't have it or it's disabled
        if(cachedAuthResult == null){
            //If no success cache and failure cache is enabled, check it
            if(!failureCacheDisabled){
                //check if it's a fail for these creds
                Object cachedObj = failureCache.retrieve(ckey);
                if(cachedObj != null && cachedObj instanceof Long){
                    cachedFailureTime = (Long)cachedObj;
                }else{
                    //as miss in failureCache also, return null
                    return null;
                }
            }else{
                //failure cache not enabled and success cache missed / disabled
                return null;
            }
        }

        String log;
        Object returnValue;
        long cacheAddedTime;
        long maxAge;
        if (cachedFailureTime != null) {
            cacheAddedTime = cachedFailureTime;
            log = "failure";
            maxAge = maxFailAge;
            returnValue = cachedFailureTime;
        } else {
            cacheAddedTime = cachedAuthResult.getTimestamp();
            log = "success";
            maxAge = maxSuccessAge;
            returnValue = cachedAuthResult;
        }

        if (cacheAddedTime + maxAge > currentTimeMillis()) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Using cached {0} for {1} on IdP \"{2}\"", new String[] {log, credString, idp.getConfig().getName()});
            return returnValue;
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cached {0} for {1} is stale on IdP \"{2}\"; will reauthenticate", new String[] {log, credString, idp.getConfig().getName()});
            }
            return null;
        }
    }

    /**
     * Get the current time in millis.
     */
    private long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }
}
