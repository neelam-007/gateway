/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;

/**
 * Shared code used by various SAML token strategies.
 */
public abstract class AbstractSamlTokenStrategy extends AbstractTokenStrategy {
    private static final Logger log = Logger.getLogger(AbstractSamlTokenStrategy.class.getName());
    private static final int SAML_PREEXPIRE_SEC = 30;

    private final Object lock;
    private SamlAssertion cachedAssertion = null;

    /**
     * Create a token strategy.
     *
     * @param tokenType the type of SAML token we are to manage.
     * @param lock the object whose lock to use to protect our internal state, or null to use this token strategy
     *        instance itself as the lock object.  Normally an Ssg instance.
     */
    public AbstractSamlTokenStrategy(SecurityTokenType tokenType, Object lock) {
        super(tokenType);
        if (lock == null) lock = this;
        this.lock = lock;
    }

    public SecurityToken getOrCreate(Ssg ssg)
            throws OperationCanceledException, GeneralSecurityException, IOException, ClientCertificateException,
            KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException, HttpChallengeRequiredException {
        synchronized (lock) {
            removeIfExpired();
            if (cachedAssertion != null)
                return cachedAssertion;

        }
        SamlAssertion newone = acquireSamlAssertion(ssg);
        if (newone != null)
            checkForWackyIssueInstant(newone.getIssueInstant());
        synchronized (lock) {
            return cachedAssertion = newone;
        }
    }

    public SecurityToken getIfPresent() {
        synchronized (lock) {
            removeIfExpired();
            return cachedAssertion;
        }
    }

    /**
     * Check for clock skew (Bug #1473) and issue a warning if necessary.  This assumes that IssueInstant is right now
     * (ie that old tokens wont be reused by the token service).  If the issueInstant is off by more than 30 seconds
     * from the local clock, a warning is sent to the logger.
     *
     * @param issueInstant the issue instant of the token to check.  If null, no action will be taken.
     */
    protected void checkForWackyIssueInstant(Calendar issueInstant) {
        if (issueInstant == null)
            return;

        Calendar tooLate = Calendar.getInstance();
        tooLate.add(Calendar.SECOND, 30);
        Calendar tooEarly = Calendar.getInstance();
        tooEarly.add(Calendar.SECOND, -30);

        if (issueInstant.after(tooLate) || issueInstant.before(tooEarly)) {
            Calendar now = Calendar.getInstance();
            long diff = (issueInstant.getTimeInMillis() - now.getTimeInMillis()) / 1000;
            tokenClockSkewWarning(diff);
        }
    }

    /** Override to change the warning when clock skew is detected.  THis method just calls logger.warning. */
    protected void tokenClockSkewWarning(long diff) {
        log.warning("Token server clock skew is over 30 seconds (" + diff + ") -- the resulting tokens may be considered stale by the target service");
    }
    
    /**
     * Flush cached assertion if it has expired (or will expire soon).
     */
    private void removeIfExpired() {
        synchronized (lock) {
            if (cachedAssertion != null && cachedAssertion.isExpiringSoon(SAML_PREEXPIRE_SEC)) {
                log.log(Level.INFO, "SAML assertion has expired or will do so within the next " +
                                    SAML_PREEXPIRE_SEC + " seconds.  Will throw it away and get a new one.");
                cachedAssertion = null;
            }
        }
    }

    public void clearCachedToken() {
        synchronized (lock) {
            cachedAssertion = null;
        }
    }

    public void onTokenRejected() {
        synchronized (lock) {
            cachedAssertion = null;
        }
    }

    /**
     * Actually acquire a new SAML assertion from the source.
     * <p>
     * The lock monitor must <em>not</em> be held when this method is called.
     *
     * @return the newly obtained SAML assertion.  Never null.
     * @throws OperationCanceledException  for the usual reasons
     * @throws GeneralSecurityException "
     * @throws KeyStoreCorruptException "
     * @throws BadCredentialsException "
     * @throws IOException "
     */
    protected abstract SamlAssertion acquireSamlAssertion(Ssg ssg)
            throws OperationCanceledException, GeneralSecurityException, ClientCertificateException, 
            KeyStoreCorruptException, BadCredentialsException, IOException, HttpChallengeRequiredException;
}
