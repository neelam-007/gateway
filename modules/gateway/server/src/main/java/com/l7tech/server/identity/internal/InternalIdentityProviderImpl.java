/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.internal;

import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.identity.Authenticated;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.PersistentIdentityProviderImpl;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.util.HexUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The internal identity provider.  Currently there is always exactly one of these in an SSG, but there's no practical
 * reason why there couldn't be more.
 *
 * TODO consider splitting up SSM authentication and message processing authentication into separate providers and/or separate mixed-in provider aspects
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class InternalIdentityProviderImpl
        extends PersistentIdentityProviderImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements ApplicationContextAware, InternalIdentityProvider, ConfigurableIdentityProvider
{
    private static final Logger logger = Logger.getLogger(InternalIdentityProviderImpl.class.getName());

    private IdentityProviderConfig config;
    private InternalUserManager userManager;
    private InternalGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;
    private ApplicationContext springContext;
    private Auditor auditor;

    public InternalIdentityProviderImpl() {
    }

    @Override
    public InternalUserManager getUserManager() {
        return userManager;
    }

    @Override
    public InternalGroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, noRollbackFor=AuthenticationException.class)
    public AuthenticationResult authenticate( LoginCredentials pc )
            throws AuthenticationException
    {
        AuthenticationResult ar = null;
        try {
            String login = pc.getLogin();

            InternalUser dbUser;
            try {
                dbUser = userManager.findByLogin(login);
            } catch (FindException e) {
                throw new AuthenticationException("Couldn't authenticate credentials", e);
            }
            
            if (dbUser == null) {
                logger.info("Couldn't find user with login " + login);
                return null;
            }

            if ( isExpired(dbUser) ) {
                String err = "Credentials' login matches an internal user " + login + " but that " +
                        "account is now expired.";
                logger.info(err);
                auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, login);
                throw new AuthenticationException(err);
            }

            CredentialFormat format = pc.getFormat();
            if (format.isClientCert() || format == CredentialFormat.SAML) {
                ar = certificateAuthenticator.authenticateX509Credentials(pc, dbUser, config.getCertificateValidationType(), auditor);
            } else {
                ar = autenticatePasswordCredentials(pc, dbUser);
            }

            return ar;
        } finally {
            if (ar != null) {
                springContext.publishEvent(new Authenticated(ar));
            }
        }
    }

    @Override
    public InternalUser findUserByCredential(final LoginCredentials lc) throws FindException {
        String login = lc.getLogin();
        return userManager.findByLogin(login);
    }

    private AuthenticationResult autenticatePasswordCredentials(LoginCredentials pc, InternalUser dbUser)
            throws MissingCredentialsException, BadCredentialsException
    {
        CredentialFormat format = pc.getFormat();
        String login = dbUser.getLogin();
        char[] credentials = pc.getCredentials();
        String dbPassHash = dbUser.getHashedPassword();
        String authPassHash;

        if (format == CredentialFormat.CLEARTEXT) {
            authPassHash = HexUtils.encodePasswd(login, new String(credentials), HttpDigest.REALM);
            if (dbPassHash.equals(authPassHash))
                return new AuthenticationResult(dbUser);
            logger.info("Incorrect password for login " + login);
            throw new BadCredentialsException();
        } else if (format == CredentialFormat.DIGEST) {
            return DigestAuthenticator.authenticateDigestCredentials(pc, dbUser);
        } else {
            throwUnsupportedCredentialFormat(format);
            /* NEVER REACHED */
            return null;
        }
    }

    /** Always throws. */
    private void throwUnsupportedCredentialFormat(CredentialFormat format) {
        IllegalArgumentException iae = new IllegalArgumentException( "Unsupported credential format: " + format.toString() );
        logger.log( Level.WARNING, iae.toString(), iae );
        throw iae;
    }

    @Override
    public IdentityProviderConfig getConfig() {
        return config;
    }

    // TODO: Make this customizable
    @Override
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    @Override
    public void test(boolean quick) {
    }

    @Override
    public void preSaveClientCert(InternalUser user, X509Certificate[] certChain ) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    @Override
    public void setUserManager(InternalUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(InternalGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    @Override
    public void setIdentityProviderConfig(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException {
        this.config = configuration;

        if ( userManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }

        userManager.configure( this );
        if (groupManager != null) groupManager.configure( this );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /*
    * ValidationException exceptions do not state that the user belongs to an ldap or in which
    * ldap the user was not found
    * */
    @Override
    public void validate(InternalUser u) throws ValidationException {
        InternalUser validatedUser;
        try{
            validatedUser = userManager.findByPrimaryKey(u.getId());
        }
        catch (FindException e){
            throw new ValidationException("User '"+u.getLogin()+"' did not validate.", e);
        }

        if(validatedUser == null){
            throw new ValidationException("IdentityProvider User " + u.getLogin()+" not found.");
        } else if ( isExpired( validatedUser ) ) {
            throw new ValidationException("User '"+u.getLogin()+"' did not validate (expired account)");
        }
    }

    /**
     * Check if the given user account is expired.
     *
     * @param dbUser The user to check
     * @return True if expired.
     */
    private boolean isExpired( final InternalUser dbUser ) {
        boolean expired = false;

        if ( dbUser.getExpiration() > -1 && dbUser.getExpiration() < System.currentTimeMillis() ) {
            expired = true;
        }

        return expired;
    }

    @Override
    public boolean hasClientCert(LoginCredentials lc) throws AuthenticationException {
        String login = lc.getLogin();
        InternalUser dbUser;
        try {
            dbUser = userManager.findByLogin(login);
            if (dbUser == null) return false;

            //check if this user has already been assigned with a certificate, if the user has been issued with a
            //certificate, it must use the certificate for the login
            X509Certificate temp = (X509Certificate) clientCertManager.getUserCert(dbUser);
            if ( temp != null ) return true;
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't authenticate credentials", e);
        }
        return false;
    }
}
