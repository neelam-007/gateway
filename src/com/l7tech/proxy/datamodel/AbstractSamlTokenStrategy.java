/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public SecurityToken getOrCreate()
            throws OperationCanceledException, GeneralSecurityException, IOException, ClientCertificateException,
            KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException
    {
        synchronized (lock) {
            removeIfExpired();
            if (cachedAssertion != null)
                return cachedAssertion;

        }
        SamlAssertion newone = acquireSamlAssertion();
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
     * Flush cached assertion if it has expired (or will expire soon).
     */
    private void removeIfExpired() {
        synchronized (lock) {
            if (cachedAssertion != null && cachedAssertion.isExpiringSoon(SAML_PREEXPIRE_SEC)) {
                log.log(Level.INFO, "Our SAML Holder-of-key assertion has expired or will do so within the next " +
                                                                SAML_PREEXPIRE_SEC + " seconds.  Will throw it away and get a new one.");
                cachedAssertion = null;
            }
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
    protected abstract SamlAssertion acquireSamlAssertion()
            throws OperationCanceledException, GeneralSecurityException,
            KeyStoreCorruptException, BadCredentialsException, IOException;
}
