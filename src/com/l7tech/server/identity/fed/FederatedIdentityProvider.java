/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.identity.PersistentIdentityProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
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

    public FederatedIdentityProvider(IdentityProviderConfig config) {
        if (config instanceof FederatedIdentityProviderConfig) {
            this.providerConfig = (FederatedIdentityProviderConfig)config;
        } else {
            throw new IllegalArgumentException("Federated Provider Config required");
        }
    }

    protected FederatedIdentityProvider() {
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

    public Set getValidTrustedCertOids() {
        return Collections.unmodifiableSet(validTrustedCertOids);
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException, IOException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT )
            return x509Handler.authorize(pc);
        else if ( pc.getFormat() == CredentialFormat.SAML ) {
            return samlHandler.authorize(pc);
        } else {
            throw new BadCredentialsException("Can't authenticate without SAML or X.509 certificate credentials");
        }
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
        if (validTrustedCertOids.isEmpty()) {
            X509Certificate caCert = null;
            try {
                caCert = KeystoreUtils.getInstance().getRootCert();
                if (clientCertChain.length > 1) {
                    if (CertUtils.certsAreEqual(caCert, clientCertChain[1]) || caCert.getSubjectDN().equals(clientCertChain[1].getIssuerDN())) {
                        throw new ClientCertManager.VetoSave("User's cert was issued by the internal certificate authority");
                    }
                }
            } catch ( IOException e ) {
                throw new ClientCertManager.VetoSave("Couldn't parse CA cert");
            } catch ( CertificateException e ) {
                throw new ClientCertManager.VetoSave("CA cert is not valid");
            }
        } else {
            String caDn = clientCertChain[0].getIssuerDN().getName();
            TrustedCert caTrust = null;
            X509Certificate trustedCaCert = null;
            try {
                caTrust = trustedCertManager.getCachedCertBySubjectDn(caDn, MAX_CACHE_AGE);
                if (caTrust == null)
                    throw new ClientCertManager.VetoSave("User's cert was not signed by a recognized trusted cert");
                if (!validTrustedCertOids.contains(new Long(caTrust.getOid())))
                    throw new ClientCertManager.VetoSave("User's cert was not signed by any of this identity provider's trusted certs");
                if (!caTrust.isTrustedForSigningClientCerts())
                    throw new ClientCertManager.VetoSave("User's cert was signed by an authority that is not trusted for signing client certs");
                trustedCaCert = caTrust.getCertificate();
            } catch ( FindException e ) {
                final String msg = "Couldn't find issuer cert";
                logger.log(Level.SEVERE, msg, e);
                throw new ClientCertManager.VetoSave(msg);
            } catch ( IOException e ) {
                final String msg = "Couldn't parse CA cert";
                logger.log( Level.WARNING, msg, e );
                throw new ClientCertManager.VetoSave(msg);
            } catch ( CertificateException e ) {
                logger.log( Level.INFO, e.getMessage(), e );
            }

            try {
                clientCertChain[0].verify(trustedCaCert.getPublicKey());
            } catch (GeneralSecurityException e ) {
                final String msg = "Couldn't verify that client cert was signed by trusted CA";
                logger.log( Level.WARNING, msg, e );
                throw new ClientCertManager.VetoSave(msg);
            }
        }
    }

    public void setTrustedCertManager(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    public void setUserManager(FederatedUserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(FederatedGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    protected void initDao() throws Exception {
        super.initDao();
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("The Trusted Certificate Manager is required");
        }

        long[] certOids = providerConfig.getTrustedCertOids();
        for ( int i = 0; i < certOids.length; i++ ) {
            String msg = "Federated Identity Provider '" + providerConfig.getName() + "' refers to Trusted Cert #" + certOids[i];
            try {
                TrustedCert trust = trustedCertManager.getCachedCertByOid(certOids[i], MAX_CACHE_AGE);
                if (trust == null) {
                    logger.log(Level.WARNING, msg + ", which no longer exists");
                    continue;
                }
                Long oid = new Long(certOids[i]);
                validTrustedCertOids.add(oid);
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, msg + ", which could not be found", e );
            } catch ( IOException e ) {
                logger.log( Level.WARNING, msg + ", which could not be parsed", e );
            } catch ( CertificateException e ) {
                logger.log( Level.WARNING, msg + ", which is not valid", e );
            }
        }

        this.x509Handler = new X509AuthorizationHandler(this, trustedCertManager, clientCertManager, validTrustedCertOids);
        this.samlHandler = new SamlAuthorizationHandler(this, trustedCertManager, clientCertManager, validTrustedCertOids);
    }

    private X509AuthorizationHandler x509Handler;
    private SamlAuthorizationHandler samlHandler;

    private FederatedIdentityProviderConfig providerConfig;
    private FederatedUserManager userManager;
    private FederatedGroupManager groupManager;
    private TrustedCertManager trustedCertManager;

    private final Set validTrustedCertOids = new HashSet();

    private static final Logger logger = Logger.getLogger(FederatedIdentityProvider.class.getName());
    private static final int MAX_CACHE_AGE = 5000;
}
