package com.l7tech.server.identity.internal;

import com.l7tech.server.audit.Auditor;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.event.identity.Authenticated;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.PersistentIdentityProviderImpl;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IdentityProvider implementation for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class InternalIdentityProviderImpl
        extends PersistentIdentityProviderImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements ApplicationContextAware, InternalIdentityProvider
{
    private static final Logger logger = Logger.getLogger(InternalIdentityProviderImpl.class.getName());
    public static final String ENCODING = "UTF-8";

    private IdentityProviderConfig config;
    private InternalUserManager userManager;
    private InternalGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;
    private ApplicationContext springContext;
    private Auditor auditor;

    public InternalIdentityProviderImpl(IdentityProviderConfig config) {
        this.config = config;
    }

    public InternalUserManager getUserManager() {
        return userManager;
    }

    public InternalGroupManager getGroupManager() {
        return groupManager;
    }

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

            if (dbUser.getExpiration() > -1 && dbUser.getExpiration() < System.currentTimeMillis()) {
                String err = "Credentials' login matches an internal user " + login + " but that " +
                        "account is now expired.";
                logger.info(err);
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

    public IdentityProviderConfig getConfig() {
        return config;
    }

    // TODO: Make this customizable
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public void test(boolean quick) {
    }

    public void preSaveClientCert(InternalUser user, X509Certificate[] certChain ) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    public void setUserManager(InternalUserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(InternalGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
        this.auditor = new Auditor(this, applicationContext, logger);
    }
}
