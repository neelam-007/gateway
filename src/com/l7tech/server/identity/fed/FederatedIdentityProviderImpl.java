/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.server.audit.Auditor;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.PersistentIdentityProviderImpl;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.security.cert.CertValidationProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class FederatedIdentityProviderImpl
        extends PersistentIdentityProviderImpl<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager>
        implements FederatedIdentityProvider, ConfigurableIdentityProvider
{

    public FederatedIdentityProviderImpl() {
    }

    public IdentityProviderConfig getConfig() {
        return providerConfig;
    }

    public FederatedUserManager getUserManager() {
        return userManager;
    }

    public FederatedGroupManager getGroupManager() {
        return groupManager;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public Set<Long> getValidTrustedCertOids() {
        return Collections.unmodifiableSet(validTrustedCertOids);
    }

    @Transactional(propagation=Propagation.REQUIRED, readOnly=true, noRollbackFor=AuthenticationException.class)
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT ) {
            User user;
            try {
                user = x509Handler.authorize(pc);
            } catch (Exception e) {
                throw new AuthenticationException("Couldn't authorize X.509 credentials", e);
            }
            return user == null ? null : new AuthenticationResult(user);
        }
        else if ( pc.getFormat() == CredentialFormat.SAML ) {
            User user = samlHandler.authorize(pc);
            return user == null ? null : new AuthenticationResult(user);
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

    public void test(boolean quick) {
        // TODO
    }

    public void preSaveClientCert(FederatedUser user, X509Certificate[] clientCertChain ) throws ClientCertManager.VetoSave {
        FederatedUser u = userManager.cast(user);
        final String userDn = u.getSubjectDn();
        final String clientCertDn = clientCertChain[0].getSubjectDN().getName();
        if (!userDn.equals(clientCertDn))
            throw new ClientCertManager.VetoSave("User's X.509 Subject DN '" + userDn +
                                                 "'doesn't match cert's Subject DN '" + clientCertDn + "'");
        if (validTrustedCertOids.isEmpty()) {
            X509Certificate caCert;
            try {
                caCert = keystore.getRootCert();
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
            TrustedCert caTrust;
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
            } catch ( CertificateException e ) {
                logger.log( Level.INFO, e.getMessage(), e );
            }

            try {
                assert trustedCaCert != null;
                CertUtils.cachedVerify(clientCertChain[0], trustedCaCert.getPublicKey());
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

    public void setIdentityProviderConfig(IdentityProviderConfig config) throws InvalidIdProviderCfgException {
        if (config instanceof FederatedIdentityProviderConfig) {
            this.providerConfig = (FederatedIdentityProviderConfig)config;
        } else {
            throw new InvalidIdProviderCfgException("Federated Provider Config required");
        }

        if ( userManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }
        if ( groupManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }

        userManager.configure( this );
        groupManager.configure( this );

        long[] certOids = providerConfig.getTrustedCertOids();
        for (long certOid : certOids) {
            String msg = "Federated Identity Provider '" + providerConfig.getName() + "' refers to Trusted Cert #" + certOid;
            try {
                TrustedCert trust = trustedCertManager.getCachedCertByOid(certOid, MAX_CACHE_AGE);
                if (trust == null) {
                    logger.log(Level.WARNING, msg + ", which no longer exists");
                    continue;
                }
                validTrustedCertOids.add(certOid);
            } catch (FindException e) {
                logger.log(Level.SEVERE, msg + ", which could not be found", e);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, msg + ", which is not valid", e);
            }
        }

        Auditor auditor = new Auditor(this, applicationContext, logger);
        this.x509Handler = new X509AuthorizationHandler(this, trustedCertManager, clientCertManager, certValidationProcessor, auditor, validTrustedCertOids);
        this.samlHandler = new SamlAuthorizationHandler(this, trustedCertManager, clientCertManager, certValidationProcessor, auditor, validTrustedCertOids);        
    }

    public void setUserManager(FederatedUserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(FederatedGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setCertValidationProcessor(CertValidationProcessor certValidationProcessor) {
        this.certValidationProcessor = certValidationProcessor;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("The Trusted Certificate Manager is required");
        }
        if (certValidationProcessor == null) {
            throw new IllegalArgumentException("The Certificate Validation Processor is required");
        }
    }

    private X509AuthorizationHandler x509Handler;
    private SamlAuthorizationHandler samlHandler;

    private FederatedIdentityProviderConfig providerConfig;
    private FederatedUserManager userManager;
    private FederatedGroupManager groupManager;
    private TrustedCertManager trustedCertManager;
    private CertValidationProcessor certValidationProcessor;

    private final Set<Long> validTrustedCertOids = new HashSet<Long>();

    private static final Logger logger = Logger.getLogger(FederatedIdentityProviderImpl.class.getName());
    private static final int MAX_CACHE_AGE = 5000;
}
