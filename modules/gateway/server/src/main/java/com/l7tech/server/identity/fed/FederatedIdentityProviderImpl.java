/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.fed;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SessionSecurityToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.PersistentIdentityProviderImpl;
import com.l7tech.server.identity.SessionAuthenticator;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Federated Identity Provider allows authorization of {@link User}s and {@link Group}s
 * whose identities are managed by trusted third-party authorities.  It supports federated credentials
 * that have been provided using both X509 and SAML.
 *
 * @see FederatedIdentityProviderConfig
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class FederatedIdentityProviderImpl
        extends PersistentIdentityProviderImpl<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager>
        implements FederatedIdentityProvider, ConfigurableIdentityProvider
{

    public FederatedIdentityProviderImpl() {
    }

    @Override
    public IdentityProviderConfig getConfig() {
        return providerConfig;
    }

    @Override
    public FederatedUserManager getUserManager() {
        return userManager;
    }

    @Override
    public FederatedGroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public Set<Goid> getValidTrustedCertOids() {
        return Collections.unmodifiableSet(validTrustedCertOids);
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true, noRollbackFor=AuthenticationException.class)
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT ) {
            User user;
            try {
                user = x509Handler.authorize(pc);
            } catch (AuthenticationException e) {
                throw new AuthenticationException("Couldn't authorize X.509 credentials: " + ExceptionUtils.getMessage( e ), e);
            } catch (Exception e) {
                throw new AuthenticationException("Error authorizing X.509 credentials: " + ExceptionUtils.getMessage( e ), e);
            }
            return user == null ? null : new AuthenticationResult(user, pc.getSecurityTokens(), pc.getClientCert(), false);
        } else if ( pc.getFormat() == CredentialFormat.SAML ) {
            User user = samlHandler.authorize(pc);
            return user == null ? null : new AuthenticationResult(user, pc.getSecurityTokens(), pc.getClientCert(), false);
        } else if ( pc.getFormat() == CredentialFormat.SESSIONTOKEN ) {
            final FederatedUser user = getUserForCredential( pc );
            return user == null ? null : sessionAuthenticator.authenticateSessionCredentials( pc, user );
        } else {
            throw new BadCredentialsException("Can't authenticate without SAML or X.509 certificate credentials");
        }
    }

    /**
     * This provider does not support lookup of users by credential. 
     */
    @Override
    public FederatedUser findUserByCredential( LoginCredentials pc ) {
        return null;
    }

    /**
     * Meaningless - no passwords in FIP anyway
     */
    @Override
    public String getAuthRealm() {
        return HexUtils.REALM;
    }

    @Override
    public void test(boolean quick, String testUser, char[] testPassword) {
        // TODO
    }

    @Override
    public void preSaveClientCert(FederatedUser user, X509Certificate[] clientCertChain ) throws ClientCertManager.VetoSave {
        FederatedUser u = userManager.cast(user);
        final String userDn = u.getSubjectDn();
        final String clientCertDn = clientCertChain[0].getSubjectDN().getName();
        if (!CertUtils.isEqualDNCanonical(userDn, clientCertDn))
            throw new ClientCertManager.VetoSave("User's X.509 Subject DN '" + userDn +
                    "'doesn't match cert's Subject DN '" + clientCertDn + "'");
        try {
            if (!validTrustedCertOids.isEmpty()) {
                checkSignedByRecognizedTrustedCert(clientCertChain);
            }
        } catch (FindException e) {
            throw new ClientCertManager.VetoSave("Unable to look up trusted certificates: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void checkSignedByRecognizedTrustedCert(X509Certificate[] clientCertChain) throws FindException, ClientCertManager.VetoSave {
        final X509Certificate clientCert = clientCertChain[0];
        String issuerDn = CertUtils.getIssuerDN( clientCert );
        Collection<TrustedCert> trustedCerts = trustedCertServices.getCertsBySubjectDnFiltered(issuerDn, true, EnumSet.of(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS), null);
        for (TrustedCert trustedCert : trustedCerts) {
            try {
                CertVerifier.cachedVerify(clientCert, trustedCert.getCertificate());
                // Success.
                return;
            } catch (Exception e) {
                String subjectDn = clientCert.getSubjectDN().toString();
                logger.log(Level.WARNING, "Unable to verify cert with DN '" + subjectDn +
                        "' against trusted issuer cert with DN '" + issuerDn +
                        "': " + ExceptionUtils.getMessage(e), e);
                // FALLTHROUGH and check next matching trusted cert
            }
        }

        throw new ClientCertManager.VetoSave("User's cert was not signed by an issuer cert trusted by this identity provider to sign client certs");
    }

    public void setTrustedCertManager(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    @Override
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
            throw new InvalidIdProviderCfgException("GroupManager is not set");
        }

        userManager.configure( this );
        groupManager.configure( this );

        Goid[] certOids = providerConfig.getTrustedCertGoids();
        for (Goid certOid : certOids) {
            String msg = "Federated Identity Provider '" + providerConfig.getName() + "' refers to Trusted Cert #" + certOid;
            try {
                TrustedCert trust = trustedCertManager.getCachedEntity(certOid, MAX_CACHE_AGE);
                if (trust == null) {
                    logger.log(Level.WARNING, msg + ", which no longer exists");
                    continue;
                }
                validTrustedCertOids.add(certOid);
            } catch (FindException e) {
                logger.log(Level.SEVERE, msg + ", which could not be found", e);
            }
        }

        Auditor auditor = new Auditor(this, applicationContext, logger);
        this.x509Handler = new X509AuthorizationHandler(this, trustedCertServices, clientCertManager, certValidationProcessor, auditor, validTrustedCertOids);
        this.samlHandler = new SamlAuthorizationHandler(this, trustedCertServices, clientCertManager, certValidationProcessor, auditor, validTrustedCertOids);
        this.sessionAuthenticator = new SessionAuthenticator( providerConfig.getGoid() );
    }

    @Override
    public void setUserManager(FederatedUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(FederatedGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public boolean hasClientCert(String login) throws AuthenticationException {
        return false;  
    }

    public void setCertValidationProcessor(CertValidationProcessor certValidationProcessor) {
        this.certValidationProcessor = certValidationProcessor;
    }

    public void setTrustedCertServices(TrustedCertServices trustedCertServices) {
        this.trustedCertServices = trustedCertServices;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("The Trusted Certificate Manager is required");
        }
        if (trustedCertServices == null) {
            throw new IllegalArgumentException("The Trusted Certificate Services are required");
        }
        if (certValidationProcessor == null) {
            throw new IllegalArgumentException("The Certificate Validation Processor is required");
        }
    }

    /**
     * Get the FederatedUser that matches the given session credential.
     */
    private FederatedUser getUserForCredential( final LoginCredentials pc ) throws AuthenticationException {
        FederatedUser user;

        // extract token
        final SecurityToken securityToken = pc.getSecurityToken();
        if ( !(securityToken instanceof SessionSecurityToken) ) {
            throw new BadCredentialsException("Unexpected token");
        }
        final SessionSecurityToken sessionSecurityToken = (SessionSecurityToken) securityToken;

        // verify provider
        if ( !sessionSecurityToken.getProviderId().equals(getConfig().getGoid()) ) {
            return null;
        }

        // verify user
        final String id = sessionSecurityToken.getUserId();
        if ( id != null ) {
            try {
                user = getUserManager().findByPrimaryKey( id );
            } catch ( FindException e ) {
                throw new AuthenticationException("Error authorizing session credentials: " + ExceptionUtils.getMessage( e ), e);
            }
            if ( user == null ) {
                throw new AuthenticationException("Couldn't authorize session credentials: user not found");
            }
        } else {
            // re-create virtual user
            user = new FederatedUser(getConfig().getGoid(), null);
        }

        return user;
    }

    private X509AuthorizationHandler x509Handler;
    private SamlAuthorizationHandler samlHandler;
    private SessionAuthenticator sessionAuthenticator;

    private FederatedIdentityProviderConfig providerConfig;
    private FederatedUserManager userManager;
    private FederatedGroupManager groupManager;
    private TrustedCertManager trustedCertManager;
    private TrustedCertServices trustedCertServices;
    private CertValidationProcessor certValidationProcessor;

    private final Set<Goid> validTrustedCertOids = new HashSet<>();

    private static final Logger logger = Logger.getLogger(FederatedIdentityProviderImpl.class.getName());
    private static final int MAX_CACHE_AGE = 5000;
}
