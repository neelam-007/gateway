/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.X509Config;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.server.identity.PersistentIdentityProvider;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateExpiredException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Federated Identity Provider allows authorization of {@link User}s and {@link Group}s
 * whose identities are managed by trusted third-party authorities.  It supports federated credentials
 * that have been provided using both X509 and SAML.
 *
 * @see FederatedIdentityProviderConfig
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProvider extends PersistentIdentityProvider {
    public FederatedIdentityProvider( IdentityProviderConfig config ) {
        if ( !(config instanceof FederatedIdentityProviderConfig) )
            throw new IllegalArgumentException("Config must be an instance of FederatedIdentityProviderConfig");
        this.config = (FederatedIdentityProviderConfig)config;
        this.userManager = new FederatedUserManager(this);
        this.groupManager = new FederatedGroupManager(this);
        this.trustedCertManager = (TrustedCertManager)Locator.getDefault().lookup(TrustedCertManager.class);
        this.clientCertManager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);

        long[] certOids = this.config.getTrustedCertOids();
        for ( int i = 0; i < certOids.length; i++ ) {
            long oid = certOids[i];
            TrustedCert cert = null;
            try {
                cert = trustedCertManager.findByPrimaryKey(oid);
                if (cert == null) {
                    logger.severe("FederatedIdentityProvider '" + config.getName() +
                                  "' refers to TrustedCert " + oid + ", which no longer exists");
                    continue;
                }
                CertificateExpiry exp = CertUtils.checkValidity(cert.getCertificate());
                if (exp.getDays() <= CertificateExpiry.FINE_DAYS) logWillExpire( cert, exp );
                trustedCerts.put(cert.getSubjectDn(), cert);
            } catch ( FindException e ) {
                throw new RuntimeException("Couldn't retrieve a cert from the TrustedCertManager - cannot proceed", e);
            } catch ( CertificateNotYetValidException e ) {
                logInvalidCert( cert, e );
            } catch ( IOException e ) {
                logInvalidCert( cert, e );
            } catch ( CertificateExpiredException e ) {
                logInvalidCert( cert, e );
            } catch ( CertificateException e ) {
                logInvalidCert( cert, e );
            }
        }
    }

    private void logInvalidCert( TrustedCert cert, Exception e ) {
        final String msg = "Trusted cert for " + cert.getSubjectDn() +
                           " is not valid or corrupted. Identities asserted by the corresponding authority " +
                           "will not be authorized.";
        if ( e == null ) {
            logger.log( Level.SEVERE, msg);
        } else {
            logger.log( Level.SEVERE, msg, e );
        }
    }

    private void logWillExpire( TrustedCert cert, CertificateExpiry e ) {
        final String msg = "Trusted cert for " + cert.getSubjectDn() +
                           " will expire in approximately " + e.getDays() + " days.";
        logger.log( e.getSeverity(), msg );
    }


    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException, IOException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT ) {
            if ( !config.isX509Supported() )
                throw new BadCredentialsException("This identity provider is not configured to support X.509 credentials");

            final X509Config x509Config = config.getX509Config();

            X509Certificate requestCert = (X509Certificate)pc.getPayload();
            String subjectDn = requestCert.getSubjectDN().getName();
            String issuerDn = requestCert.getIssuerDN().getName();

            if ( !trustedCerts.isEmpty() ) {
                // There could be no trusted certs--this means that specific client certs
                // are trusted no matter what
                TrustedCert trustedCert = (TrustedCert)trustedCerts.get(issuerDn);
                if ( trustedCert == null ) throw new BadCredentialsException("Signer is not trusted");
                if ( !trustedCert.isTrustedForSigningClientCerts() )
                    throw new BadCredentialsException("The trusted certificate with DN '" +
                                                      trustedCert.getSubjectDn() +
                                                      " is not trusted for signing client certificates");

                try {
                    CertificateExpiry exp = CertUtils.checkValidity(trustedCert.getCertificate());
                    if (exp.getDays() <= CertificateExpiry.FINE_DAYS)
                        logWillExpire(trustedCert, exp);
                } catch ( CertificateException e ) {
                    logInvalidCert(trustedCert, e);
                    throw new AuthenticationException(e.getMessage(), e);
                }

                try {
                    // Check that cert was signed by CA key
                    requestCert.verify(trustedCert.getCertificate().getPublicKey());
                } catch ( CertificateException e ) {
                    logger.log( Level.WARNING, e.getMessage(), e );
                    throw new BadCredentialsException(e.getMessage(), e);
                } catch ( NoSuchAlgorithmException e ) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new BadCredentialsException(e.getMessage(), e);
                } catch ( InvalidKeyException e ) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new BadCredentialsException(e.getMessage(), e);
                } catch ( NoSuchProviderException e ) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new BadCredentialsException(e.getMessage(), e);
                } catch ( SignatureException e ) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new BadCredentialsException(e.getMessage(), e);
                }
            }

            FederatedUser u = userManager.findBySubjectDN(subjectDn);
            if (u == null && trustedCerts.isEmpty())
                throw new BadCredentialsException("No Federated User with DN = '" +
                                                  subjectDn + "' could be found and virtual groups" +
                                                  " are not permitted without trusted certs");

            X509Certificate storedCert = (X509Certificate)clientCertManager.getUserCert(u);
            if ( storedCert == null ) {
                // This is OK as long as it was signed by a trusted cert
                if ( trustedCerts.isEmpty() ) {
                    // No trusted certs means the request cert must match a previously stored client cert
                    throw new BadCredentialsException("User " + u + " has no client certificate stored, " +
                                                      "and this Federated Identity Provider has no CA certs " +
                                                      "that are trusted");
                }
            } else if ( !storedCert.equals(requestCert) ) {
                    throw new BadCredentialsException("Request certificate for user " + u +
                                                      " does not match previously stored certificate");
            }

            final Class csa = pc.getCredentialSourceAssertion();

            if ( ( x509Config.isWssBinarySecurityToken() && csa == RequestWssX509Cert.class ) ||
                 ( x509Config.isSslClientCert() && csa == HttpClientCert.class )) {
                if ( u == null ) {
                    // Make a fake user so that a VirtualGroup can still resolve it
                    u = new FederatedUser();
                    u.setSubjectDn(subjectDn);
                }
                return u;
            }

            throw new BadCredentialsException("Federated IDP " + config.getName() + "(" + config.getOid() +
                                              ") is not configured to trust certificates found with " +
                                              csa );
        } else if ( pc.getFormat() == CredentialFormat.SAML ) {
            // TODO
            if ( !config.isSamlSupported() ) throw new AuthenticationException("This identity provider is not configured to support SAML credentials");
        } else {
            throw new BadCredentialsException("Can't authenticate without SAML or X.509 certificate credentials");
        }
        return null;
    }

    /**
     * Meaningless - no passwords in FIP anyway
     */
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public void test() {
        // TODO
    }

    private final FederatedIdentityProviderConfig config;
    private final FederatedUserManager userManager;
    private final FederatedGroupManager groupManager;
    private final TrustedCertManager trustedCertManager;
    private final ClientCertManager clientCertManager;

    private transient final Map trustedCerts = new HashMap();

    private final Logger logger = Logger.getLogger(getClass().getName());
}
