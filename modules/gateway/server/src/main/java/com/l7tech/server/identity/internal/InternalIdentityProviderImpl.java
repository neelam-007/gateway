package com.l7tech.server.identity.internal;

import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SshSecurityToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.identity.Authenticated;
import com.l7tech.server.identity.*;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
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
    private final static boolean allowDigestFallback = ConfigFactory.getBooleanProperty( "com.l7tech.server.identity.internal.allowDigestFallback", true );
    
    private IdentityProviderConfig config;
    private InternalUserManager userManager;
    private InternalGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;
    private SessionAuthenticator sessionAuthenticator;
    private ApplicationContext springContext;
    private Auditor auditor;
    private PasswordHasher passwordHasher;


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
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        return authenticate(pc, false);
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, noRollbackFor=AuthenticationException.class)
    public AuthenticationResult authenticate(LoginCredentials pc, boolean allowUserUpgrade) throws AuthenticationException {
        final String login = pc.getLogin();
        final InternalUser dbUser = getUser(login);

        if (dbUser == null) {
            logger.info("Couldn't find user with login " + login);
            return null;
        }

        validateUserAccount(login, dbUser);

        CredentialFormat format = pc.getFormat();
        final AuthenticationResult ar;
        if ( format.isClientCert() || (format == CredentialFormat.SAML)) {
            ar = certificateAuthenticator.authenticateX509Credentials(pc, dbUser, config.getCertificateValidationType(), auditor);
        } else if ( format == CredentialFormat.SESSIONTOKEN ) {
            ar = sessionAuthenticator.authenticateSessionCredentials( pc, dbUser );
        } else if (pc.getSecurityToken() != null && pc.getSecurityToken() instanceof SshSecurityToken) {
            ar = authenticateSshCredentials(pc, dbUser);
        } else {
            ar = authenticatePasswordCredentials(pc, dbUser, allowUserUpgrade);
        }

        if (ar == null) {
            // Probably a bug, turn into auth exception to prevent subsequent NPE
            throw new AuthenticationException("Authentication produced no result for credential format " + format);
        }

        springContext.publishEvent(new Authenticated(ar));
        return ar;
    }

    private InternalUser getUser(String login) throws AuthenticationException {
        InternalUser dbUser;
        try {
            dbUser = userManager.findByLogin(login);
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't authenticate credentials", e);
        }
        return dbUser;
    }

    private void validateUserAccount(String login, InternalUser dbUser) throws AuthenticationException {
        if ( !dbUser.isEnabled() ) {
            String err = "Credentials' login matches an internal user " + login + " but that " +
                    "account is disabled.";
            logger.info(err);
            auditor.logAndAudit(SystemMessages.AUTH_USER_DISABLED, login);
            throw new AuthenticationException(err);
        }

        if ( isExpired(dbUser) ) {
            String err = "Credentials' login matches an internal user " + login + " but that " +
                    "account is now expired.";
            logger.info(err);
            auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, login);
            throw new AuthenticationException(err);
        }
    }

    @Override
    public InternalUser findUserByCredential(final LoginCredentials lc) throws FindException {
        InternalUser user = null;
        if ( lc.getFormat() == CredentialFormat.SESSIONTOKEN ) {
            final String id = sessionAuthenticator.getUserId( lc );
            if ( id != null ) {
                user = userManager.findByPrimaryKey( id );
            }
        } else {
            final String login = lc.getLogin();
            if ( login != null ) {
                user = userManager.findByLogin(login);
            }
        }
        return user;
    }

    private AuthenticationResult authenticateSshCredentials(LoginCredentials pc, InternalUser dbUser)
            throws MissingCredentialsException, BadCredentialsException
    {
        // make sure we have a public key
        SshSecurityToken sshSecurityToken = (SshSecurityToken) pc.getSecurityToken();
        String publicKey = sshSecurityToken.getPublicKey();
        if (StringUtils.isEmpty(publicKey)) {
            String msg = "No SSH public key provided for login " + sshSecurityToken.getUsername();
            if(logger.isLoggable(Level.INFO)){
                logger.info(msg);
            }
            throw new MissingCredentialsException(msg);
        }

        // make sure we have a public key from the database
        String dbPublicKey = dbUser.getProperty(InternalUser.PROPERTIES_KEY_SSH_USER_PUBLIC_KEY);
        if (StringUtils.isEmpty(dbPublicKey)) {
            String msg = "No SSH public key found in database record for login " + sshSecurityToken.getUsername();
            if(logger.isLoggable(Level.INFO)){
                logger.info(msg);
            }
            throw new BadCredentialsException(msg);
        }

        // strip newline characters, including Unix newline for Windows dev environments
        dbPublicKey = dbPublicKey.replace(SyspropUtil.getProperty("line.separator"), "");
        dbPublicKey = dbPublicKey.replace("\n", "");

        // authenticate public key
        if (!dbPublicKey.equals(publicKey)) {
            String msg = "Incorrect public key for login " + sshSecurityToken.getUsername();
            if(logger.isLoggable(Level.INFO)){
                logger.info(msg);
            }
            throw new BadCredentialsException(msg);
        }

        return new AuthenticationResult(dbUser, pc.getSecurityTokens());
    }

    private AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc,
                                                                 InternalUser dbUser,
                                                                 boolean allowUserUpgrade)
            throws MissingCredentialsException, BadCredentialsException
    {
        CredentialFormat format = pc.getFormat();
        String login = dbUser.getLogin();
        char[] credentials = pc.getCredentials();

        final BadCredentialsException credsException = new BadCredentialsException("Invalid password");

        if (format == CredentialFormat.CLEARTEXT) {
            final String userHashedPassword = dbUser.getHashedPassword();
            final boolean userHasHashedPassword = userHashedPassword != null && !userHashedPassword.trim().isEmpty();

            boolean userAuthenticated = false;
            if(userHasHashedPassword){
                try {
                    if(passwordHasher.isVerifierRecognized(userHashedPassword)){
                        passwordHasher.verifyPassword(new String(credentials).getBytes(Charsets.UTF8), userHashedPassword);
                        userAuthenticated = true;
                    }
                } catch (IncorrectPasswordException e) {
                    //fall through ok - expected when password is incorrect
                }
                
                if(!userAuthenticated){
                    if(logger.isLoggable(Level.INFO)){
                        logger.info("Incorrect password for login " + login);
                    }
                    throw credsException;
                }
            }

            //check if we need to fall back on old digest
            final String userDigest = dbUser.getHttpDigest();
            final boolean userHasDigest = userDigest != null && !userDigest.trim().isEmpty();
            final String calculatedDigest = HexUtils.encodePasswd(login, new String(credentials), HexUtils.REALM);

            if(!userAuthenticated && allowDigestFallback){
                if(userHasDigest){
                    if(calculatedDigest.equals(userDigest)){
                        //message traffic or admin - we just authenticated the user based on the users old digest
                        if(logger.isLoggable(Level.FINE)){
                            logger.log(Level.FINE, "Authenticated user '" + dbUser.getLogin()+"' using http digest compatible password hash.");
                        }
                        userAuthenticated = true;
                    }
                }
            }

            if (userAuthenticated && allowUserUpgrade) {
                //Warning - we may do a db update here...We ARE NOT on a message processing thread!! If this is changed for
                //message traffic users then the db update will need to be handed off for background processing.
                final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
                final boolean userNeedsUpdate = passwordManager.configureUserPasswordHashes(dbUser, new String(credentials));
                if(userNeedsUpdate){
                    try {
                        //user manager is responsible for logic including appropriate logging.
                        final InternalUser disconnectedUser = new InternalUser();
                        disconnectedUser.copyFrom(dbUser);
                        disconnectedUser.setVersion(dbUser.getVersion());
                        userManager.update(disconnectedUser);
                        logger.log(Level.FINE, "Upgraded password storage for user with login '" + login + "'.");
                    } catch (Exception e) {
                        final String msg = "Could not upgrade password storage for login '" + dbUser.getLogin() + "'. " + ExceptionUtils.getMessage(e);
                        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
                        //fall through, cannot authenticate, cannot update. Should not happen.
                    }
                }
            }

            if(userAuthenticated){
                return new AuthenticationResult(dbUser, pc.getSecurityTokens());
            }

            if(logger.isLoggable(Level.INFO)){
                logger.info("Incorrect password for login " + login);
            }
            throw credsException;
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
        return HexUtils.REALM;
    }

    @Override
    public void test(boolean quick, String testUser, char[] testPassword) {
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

        this.sessionAuthenticator = new SessionAuthenticator( config.getGoid() );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    public void setPasswordHasher(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
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
        } else if ( !validatedUser.isEnabled()) {
            throw new ValidationException("User '"+u.getLogin()+"' did not validate (disabled account)");
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
    public boolean hasClientCert(String login) throws AuthenticationException {
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
