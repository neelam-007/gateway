/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
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

        this.x509Handler = new X509AuthorizationHandler(this, trustedCertManager, clientCertManager, certOidSet);
        this.samlHandler = new SamlAuthorizationHandler(this, trustedCertManager, clientCertManager, certOidSet);
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
                // TODO 
            }
        } catch ( Exception e ) {
            final String msg = "Couldn't deserialize trusted cert";
            logger.log(Level.SEVERE, msg, e);
            throw new ClientCertManager.VetoSave(msg);
        }
    }

    private final X509AuthorizationHandler x509Handler;
    private final SamlAuthorizationHandler samlHandler;

    private final FederatedIdentityProviderConfig providerConfig;
    private final FederatedUserManager userManager;
    private final FederatedGroupManager groupManager;
    private final TrustedCertManager trustedCertManager;
    private final ClientCertManager clientCertManager;

    private final Set certOidSet = new HashSet();

    private static final Logger logger = Logger.getLogger(FederatedIdentityProvider.class.getName());
    private static final int MAX_CACHE_AGE = 5000;
}
