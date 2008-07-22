package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.policy.assertion.Assertion;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Abstract base class for server assertions that cache security tokens.</p>
 *
 * <p>Presumably the expense of getting an new credential is greater than
 * the cache management overhead (e.g. credential fetched over the network)</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public abstract class AbstractServerCachedSecurityTokenAssertion extends AbstractServerAssertion implements ServerAssertion {

    //- PROTECTED

    /**
     * Construct an AbstractServerCachedSecurityTokenAssertion that uses the
     * given cacheKey.
     *
     * @param cacheKey the unique key used to identify the token.
     */
    protected AbstractServerCachedSecurityTokenAssertion(Assertion assertion, String cacheKey) {
        super(assertion);
        this.cacheKey = cacheKey;
    }

    /**
     * Get a security token from the cache.
     *
     * @param cache the cache from which to retrieve the token.
     * @return the cached Security token or null.
     */
    protected SecurityToken getCachedSecurityToken(PolicyContextCache cache) {
        SecurityToken securityToken = null;

        if(cache!=null) { // then check for cached version
            try {
                SecurityToken tokenFromCache = (SecurityToken) cache.get(cacheKey);
                if(tokenFromCache!=null) {
                    securityToken = tokenFromCache;
                }
            }
            catch(ClassCastException cce) {
                logger.log(Level.SEVERE, "Expected SecurityToken in cache", cce);
            }
        }

        return securityToken;
    }

    /**
     * Put a security token into the cache with the given expiry.
     *
     * @param cache the cache to use.
     * @param securityToken the token to be cached.
     * @param expiry the expiry time for the token (use 0 for never expires)
     */
    protected void setCachedSecurityToken(PolicyContextCache cache, SecurityToken securityToken, long expiry) {
        if(cache!=null) {
            cache.put(cacheKey, securityToken, new PolicyContextCache.Info(expiry));
        }
    }

    /**
     * Calculate the appropriate expiry time for the given saml assertion.
     *
     * @param samlAssertion the assertion
     * @return the expiry time in millis
     */
    protected long getSamlAssertionExpiry(SamlAssertion samlAssertion) {
        long expiry = 0;
        if(samlAssertion.getExpires()!=null) { // then use it less a bit (to allow for clock skew)
            expiry = samlAssertion.getExpires().getTimeInMillis() - EXPIRY_PRE_EXPIRE;
        }
        else expiry = System.currentTimeMillis() + EXPIRY_MAX_AGE;
        return expiry;
    }

    /**
     * Calculate the appropriate expiry time for the given username token.
     *
     * @param usernameToken the token
     * @return the expiry time in millis
     */
    protected long getUsernameTokenExpiry(UsernameToken usernameToken) {
        return getDefaultExpiry();
    }

    /**
     * Get the default expiry time.
     *
     * @return the expiry time in millis.
     */
    protected long getDefaultExpiry() {
        return System.currentTimeMillis() + EXPIRY_MAX_AGE;
    }

    /**
     * Add the cache invalidation routing result listener to the context.
     *
     * @param pec the PolicyEnforcementContext
     */
    protected void addCacheInvalidator(final PolicyEnforcementContext pec) {
        pec.addRoutingResultListener(new RoutingResultListener() {
            public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                return false;
            }

            public void routed(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                if(status!=HttpConstants.STATUS_OK) {
                    clearCache(pec);
                }
            }

            public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
                clearCache(pec);
            }

            private void clearCache(PolicyEnforcementContext context) {
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Running cache invalidation");
                }

                PolicyContextCache cache = context.getCache();
                if(cache!=null) {
                    if(logger.isLoggable(Level.INFO)) {
                        logger.info("Clearing cached SecurityToken");
                    }
                    cache.put(cacheKey, null, new PolicyContextCache.Info(1));
                }
            }
        });
    }

    //- PRIVATE

    /**
     *
     */
    private static final Logger logger = Logger.getLogger(AbstractServerCachedSecurityTokenAssertion.class.getName());

    /**
     *
     */
    private static final long EXPIRY_PRE_EXPIRE = 30000L; // 30 seconds
    private static final long EXPIRY_MAX_AGE = 300000L; // 5 minutes


    private final String cacheKey;
}
