/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.common.util.WhirlycacheFactory;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.whirlycott.cache.Cache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cached authentication.
 */
public final class AuthCache {
    private static final Logger logger = Logger.getLogger(AuthCache.class.getName());

    private final Cache cache;

    private AuthCache() {
        String name = "AuthCache_unified";
        int succSize = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_AUTH_CACHE_SUCCESS_CACHE_SIZE, 200);
        int failSize = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_AUTH_CACHE_FAILURE_CACHE_SIZE, 100);
        int size = succSize + failSize;
        int tunerInterval = 59;
        cache = size < 1 ? null :
                WhirlycacheFactory.createCache(name, size, tunerInterval, WhirlycacheFactory.POLICY_LFU);
    }

    private static class CacheKey {
        private int cachedHashcode = -1;
        private final long providerOid;
        private final LoginCredentials creds;

        public CacheKey(long providerOid, LoginCredentials creds) {
            this.providerOid = providerOid;
            this.creds = creds;
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CacheKey cacheKey = (CacheKey)o;

            if (providerOid != cacheKey.providerOid) return false;
            if (creds != null ? !creds.equals(cacheKey.creds) : cacheKey.creds != null) return false;

            return true;
        }

        public int hashCode() {
            if (cachedHashcode == -1) {
                int result;
                result = (int)(providerOid ^ (providerOid >>> 32));
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
        final long providerOid = idp.getConfig().getOid();
        final CacheKey ckey = new CacheKey(providerOid, creds);
        Object cached = getCacheEntry(ckey, credString, idp, maxSuccessAge, maxFailAge);
        if (cached instanceof AuthenticationResult) {
            return (AuthenticationResult)cached;
        } else if (cached != null) {
            return null;
        }

        // There was a cache miss before, so someone has to authenticate it.

        // We'll allow multiple simultaneous authentications for the same credentials only for internal password auth
        boolean concurrentOk = idp instanceof InternalIdentityProvider &&
                creds.getFormat() == CredentialFormat.CLEARTEXT;

        if (cache != null && concurrentOk) {
            // Let's make sure only one thread does so on this SSG.
            // Lock username so we only auth it on one thread at a time
            String credsMutex = (Long.toString(providerOid) + credString).intern();
            synchronized (credsMutex) {
                // Recheck cache now that we have the username lock
                cached = getCacheEntry(ckey, credString, idp, maxSuccessAge, maxFailAge);
                if (cached instanceof AuthenticationResult) {
                    // Someone else got there first with a success
                    return (AuthenticationResult)cached;
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
            result = idp.authenticate(creds);
        } catch (AuthenticationException e) {
            thrown = e;
        }
        String which;

        // Skip if cache is disabled
        if (cache != null) {
            if (result == null) {
                which = "failed";
                cache.store(ckey, new Long(System.currentTimeMillis()));
            } else {
                which = "successful";
                cache.store(ckey, result);
            }

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Caching {0} authentication for {1} on IdP \"{2}\"",
                           new String[]{which, credString, idp.getConfig().getName()});
        }

        if (thrown != null) {
            throw thrown;
        } else {
            return result;
        }
    }

    /**
     * Gets a cache entry
     */
    private Object getCacheEntry(CacheKey ckey, String credString, IdentityProvider idp, int maxSuccessAge, int maxFailAge) {
        if (cache == null) return null; // fail fast if cache is disabled
        Long cachedFailureTime = null;
        AuthenticationResult cachedAuthResult = null;
        Object cachedObj = cache.retrieve(ckey);
        if (cachedObj instanceof Long) {
            cachedFailureTime = (Long)cachedObj;
        } else if (cachedObj instanceof AuthenticationResult) {
            cachedAuthResult = (AuthenticationResult)cachedObj;
        }
        if (cachedAuthResult == null && cachedFailureTime == null) return null;

        String log;
        Object returnValue;
        long cacheAddedTime;
        long maxAge;
        if (cachedFailureTime != null) {
            cacheAddedTime = cachedFailureTime.longValue();
            log = "failure";
            maxAge = maxFailAge;
            returnValue = cachedFailureTime;
        } else {
            cacheAddedTime = cachedAuthResult.getTimestamp();
            log = "success";
            maxAge = maxSuccessAge;
            returnValue = cachedAuthResult;
        }

        if (cacheAddedTime + maxAge > System.currentTimeMillis()) {
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
}
