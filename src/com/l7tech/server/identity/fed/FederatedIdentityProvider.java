/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.KeystoreUtils;
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
import java.util.HashSet;
import java.util.Set;
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
        this.providerConfig = (FederatedIdentityProviderConfig)config;
        this.userManager = new FederatedUserManager(this);
        this.groupManager = new FederatedGroupManager(this);
        this.trustedCertManager = (TrustedCertManager)Locator.getDefault().lookup(TrustedCertManager.class);
        this.clientCertManager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);

        long[] certOids = providerConfig.getTrustedCertOids();
        for ( int i = 0; i < certOids.length; i++ ) {
            Long oid = new Long(certOids[i]);
            certOidSet.add(oid);
        }
    }

    public IdentityProviderConfig getConfig() {
        return providerConfig;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException, IOException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT )
            return authorizeX509(pc);
        else if ( pc.getFormat() == CredentialFormat.SAML ) {
            return authorizeSaml(pc);
        } else {
            throw new BadCredentialsException("Can't authenticate without SAML or X.509 certificate credentials");
        }
    }

    private User authorizeSaml( LoginCredentials pc ) throws AuthenticationException {
        if ( !providerConfig.isSamlSupported() )
            throw new AuthenticationException("This identity provider is not configured to support SAML credentials");
        // TODO
        throw new AuthenticationException("SAML authorization is not yet implemented");
    }

    private User authorizeX509( LoginCredentials pc ) throws IOException, AuthenticationException, FindException {
        if ( !providerConfig.isX509Supported() )
            throw new BadCredentialsException("This identity provider is not configured to support X.509 credentials");

        final X509Config x509Config = providerConfig.getX509Config();

        X509Certificate requestCert = (X509Certificate)pc.getPayload();
        String subjectDn = requestCert.getSubjectDN().getName();

        if ( !certOidSet.isEmpty() ) {
            // There could be no trusted certs--this means that specific client certs
            // are trusted no matter who signed them

            try {
                TrustedCert trustedCert = getTrustedCertForRequestCert( requestCert );
                if ( trustedCert == null ) throw new BadCredentialsException("Signer is not trusted");
                if ( !trustedCert.isTrustedForSigningClientCerts() )
                    throw new BadCredentialsException("The trusted certificate with DN '" +
                                                      trustedCert.getSubjectDn() +
                                                      "' is not trusted for signing client certificates");
                try {
                    CertificateExpiry exp = CertUtils.checkValidity(trustedCert.getCertificate());
                    if (exp.getDays() <= CertificateExpiry.FINE_DAYS)
                        trustedCertManager.logWillExpire(trustedCert, exp);
                } catch ( CertificateException e ) {
                    trustedCertManager.logInvalidCert(trustedCert, e);
                    throw new AuthenticationException(e.getMessage(), e);
                }

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
        if (u == null) {
            if (certOidSet.isEmpty()) {
                throw new BadCredentialsException("No Federated User with DN = '" + subjectDn +
                                                  "' could be found, and virtual groups" +
                                                  " are not permitted without trusted certs");
            }
        } else {
            X509Certificate importedCert = (X509Certificate)clientCertManager.getUserCert(u);
            if ( importedCert == null ) {
                // This is OK as long as it was signed by a trusted cert
                if (certOidSet.isEmpty() ) {
                    // No trusted certs means the request cert must match a previously imported client cert
                    throw new BadCredentialsException("User " + u + " has no client certificate imported, " +
                                                      "and this Federated Identity Provider has no CA certs " +
                                                      "that are trusted");
                }
            } else if ( !importedCert.equals(requestCert) ) {
                    throw new BadCredentialsException("Request certificate for user " + u +
                                                      " does not match previously imported certificate");
            }
        }

        final Class csa = pc.getCredentialSourceAssertion();

        if (csa == null) {
            logger.info("credential source assertion is not set but required by the federated id provider.");
        }

        if ( ( x509Config.isWssBinarySecurityToken() && csa == RequestWssX509Cert.class ) ||
             ( x509Config.isSslClientCert() && csa == HttpClientCert.class )) {
            if ( u == null ) {
                // Make a fake user so that a VirtualGroup can still resolve it
                u = new FederatedUser();
                u.setSubjectDn(subjectDn);
            }
            return u;
        }

        throw new BadCredentialsException("Federated IDP " + providerConfig.getName() + "(" + providerConfig.getOid() +
                                          ") is not configured to trust certificates found with " +
                                          csa.getName() );
    }

    private TrustedCert getTrustedCertForRequestCert( X509Certificate requestCert )
            throws FindException, IOException, CertificateException
    {
        TrustedCert cert = trustedCertManager.getCachedCertBySubjectDn( requestCert.getIssuerDN().getName(), MAX_CACHE_AGE );
        if (cert == null) certOidSet.remove(new Long(cert.getOid()));
        return cert;
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

    public void preSaveClientCert( User user, X509Certificate[] clientCertChain ) throws ClientCertManager.VetoSave {
        FederatedUser u = (FederatedUser)userManager.cast(user);
        final String userDn = u.getSubjectDn();
        final String clientCertDn = clientCertChain[0].getSubjectDN().getName();
        if (userDn != clientCertDn)
            throw new ClientCertManager.VetoSave("User's X.509 Subject DN '" + userDn +
                                                 "'doesn't match cert's Subject DN '" + clientCertDn + "'");
        try {
            if (certOidSet.isEmpty()) {
                X509Certificate caCert = KeystoreUtils.getInstance().getRootCert();
                if (clientCertChain.length > 1) {
                    if (caCert.equals(clientCertChain[1]) || caCert.getSubjectDN().equals(clientCertChain[1].getIssuerDN())) {
                        throw new ClientCertManager.VetoSave("User's cert was issued by the internal certificate authority");
                    }
                }
            } else {
                String caDn = clientCertChain[0].getIssuerDN().getName();
                TrustedCert caTrustedCert = trustedCertManager.getCachedCertBySubjectDn(caDn, MAX_CACHE_AGE);
                if (caTrustedCert == null) throw new ClientCertManager.VetoSave("User's cert was not signed by any of this identity provider's trusted certs");
                X509Certificate trustedCaCert = caTrustedCert.getCertificate();

            }
        } catch ( Exception e ) {
            final String msg = "Couldn't deserialize trusted cert";
            logger.log(Level.SEVERE, msg, e);
            throw new ClientCertManager.VetoSave(msg);
        }
    }

    private final FederatedIdentityProviderConfig providerConfig;
    private final FederatedUserManager userManager;
    private final FederatedGroupManager groupManager;
    private final TrustedCertManager trustedCertManager;
    private final ClientCertManager clientCertManager;

    private final Set certOidSet = new HashSet();

    private static final Logger logger = Logger.getLogger(FederatedIdentityProvider.class.getName());
    private static final int MAX_CACHE_AGE = 5000;
}
