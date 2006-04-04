/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.AuthenticationResult;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.common.util.Background;
import org.apache.commons.collections.LRUMap;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * @author alex
 */
public final class AuthCache {
    public static final int SUCCESS_CACHE_TIME = Integer.getInteger(AuthCache.class.getName() + ".maxSuccessTime", 60000).intValue();
    public static final int FAILURE_CACHE_TIME = Integer.getInteger(AuthCache.class.getName() + ".maxFailureTime", 30000).intValue();

    private static final Logger logger = Logger.getLogger(AuthCache.class.getName());

    private static final int DELETER_MAX_AGE = 60 * 60 * 1000;
    private static final int DELETER_DELAY = 30 * 1000;
    private static final int DELETER_PERIOD = 10 * 60 * 1000;

    private final LRUMap successCache = new LRUMap(Integer.getInteger(AuthCache.class.getName() + ".successCacheSize", 2000).intValue());
    private final LRUMap failureCache = new LRUMap(Integer.getInteger(AuthCache.class.getName() + ".failureCacheSize", 200).intValue());
    private final ReadWriteLock rwlock = new ReaderPreferenceReadWriteLock();

    private AuthCache() {
        Background.schedule(new TimerTask() {
            public void run() {
                Set tbd = new HashSet();
                try {
                    logger.finest("Looking for stale cache entries");
                    findStaleEntries(tbd, successCache);
                    findStaleEntries(tbd, failureCache);

                    int num = 0;
                    rwlock.writeLock().acquire();
                    for (Iterator i = tbd.iterator(); i.hasNext();) {
                        Object creds = i.next();
                        failureCache.remove(creds);
                        successCache.remove(creds);
                        num++;
                    }
                    rwlock.writeLock().release();
                    logger.log(Level.FINE, "Deleted {0} stale cache entries", new Object[] {new Integer(num)});
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.WARNING, "Interrupted waiting for cache lock");
                }

            }
        }, DELETER_DELAY, DELETER_PERIOD);
    }

    private void findStaleEntries(Set tbd, LRUMap cache) throws InterruptedException {
        rwlock.readLock().acquire();
        for (Iterator i = cache.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            LoginCredentials creds = (LoginCredentials)entry.getKey();
            Object result = entry.getValue();
            long time = -1;
            if (result instanceof Long) {
                time = ((Long)result).longValue();
            } else if (result instanceof AuthenticationResult) {
                AuthenticationResult authenticationResult = (AuthenticationResult)result;
                time = authenticationResult.getTimestamp();
            } else {
                logger.warning("Found an unexpected entry in cache: " + result);
            }
            if (time < 0 || (time + DELETER_MAX_AGE < System.currentTimeMillis())) {
                tbd.add(creds);
            }
        }
        rwlock.readLock().release();
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
        final long now = System.currentTimeMillis();
        String credString = creds.toString();
        try {
            Object cached = getCacheEntry(creds, credString, now, maxSuccessAge, maxFailAge);
            if (cached instanceof AuthenticationResult) {
                return (AuthenticationResult)cached;
            } else if (cached != null) {
                return null;
            }

            // There was a cache miss before, so someone has to authenticate it.
            // Let's make sure only one thread does so.
            String mutex = (Long.toString(idp.getConfig().getOid()) + credString).intern();
            synchronized(mutex) {
                cached = getCacheEntry(creds, credString, now, maxSuccessAge, maxFailAge);
                if (cached instanceof AuthenticationResult) {
                    // Someone else got there first with a success
                    return (AuthenticationResult)cached;
                } else if (cached != null) {
                    // Someone else got there first with a failure
                    return null;
                }

                AuthenticationResult result = null;
                AuthenticationException thrown = null;
                try {
                    result = idp.authenticate(creds);
                } catch (AuthenticationException e) {
                    thrown = e;
                }
                rwlock.writeLock().acquire();
                String which;
                final Long lnow = new Long(now);
                if (result == null) {
                    which = "failed";
                    failureCache.put(creds, lnow);
                    successCache.remove(creds);
                } else {
                    which = "successful";
                    successCache.put(creds, result);
                    failureCache.remove(creds);
                }
                rwlock.writeLock().release();
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Caching " + which + " authentication for " + creds);

                if (thrown != null) {
                    throw thrown;
                } else {
                    return result;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Interrupted waiting for cache lock", e);
        }
    }

    private Object getCacheEntry(LoginCredentials creds,
                                 String credString,
                                 long now,
                                 int maxSuccessAge,
                                 int maxFailAge)
            throws InterruptedException
    {
        rwlock.readLock().acquire();
        Long cachedFailureTime = (Long)failureCache.get(creds);
        AuthenticationResult cachedAuthResult = (AuthenticationResult)successCache.get(creds);
        rwlock.readLock().release();
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

        if (cacheAddedTime + maxAge > now) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Using cached {0} for {1}", new String[] {log, credString});
            return returnValue;
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cached {0} for {1} is stale; will reauthenticate", new String[] {log, credString});
            }
            return null;
        }
    }
}
