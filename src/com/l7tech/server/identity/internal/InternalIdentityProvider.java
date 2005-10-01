package com.l7tech.server.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.PersistentIdentityProvider;
import com.l7tech.server.identity.cert.CertificateAuthenticator;

import java.io.IOException;
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
public class InternalIdentityProvider extends PersistentIdentityProvider {
    private static final Logger logger = Logger.getLogger(InternalIdentityProvider.class.getName());
    public static final String ENCODING = "UTF-8";

    private IdentityProviderConfig config;
    private UserManager userManager;
    private GroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;

    public InternalIdentityProvider(IdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * constructor for subclassing
     */
    protected InternalIdentityProvider() {
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public AuthenticationResult authenticate( LoginCredentials pc )
            throws AuthenticationException, FindException, IOException
    {
        String login = pc.getLogin();

        InternalUser dbUser;
        dbUser = (InternalUser)userManager.findByLogin(login);
        if (dbUser == null) {
            String err = "Couldn't find user with login " + login;
            logger.info(err);
            throw new AuthenticationException(err);
        }

        if (dbUser.getExpiration() > -1 && dbUser.getExpiration() < System.currentTimeMillis()) {
            String err = "Credentials' login matches an internal user " + login + " but that " +
                    "account is now expired.";
            logger.info(err);
            throw new AuthenticationException(err);
        }

        CredentialFormat format = pc.getFormat();
        if (format.isClientCert() || format == CredentialFormat.SAML) {
            return certificateAuthenticator.authenticateX509Credentials(pc, dbUser);
        } else {
            return autenticatePasswordCredentials(pc, dbUser);
        }
    }

    private AuthenticationResult autenticatePasswordCredentials(LoginCredentials pc, InternalUser dbUser)
            throws MissingCredentialsException, BadCredentialsException
    {
        CredentialFormat format = pc.getFormat();
        String login = dbUser.getLogin();
        char[] credentials = pc.getCredentials();
        String dbPassHash = dbUser.getPassword();
        String authPassHash = null;

        if (format == CredentialFormat.CLEARTEXT) {
            authPassHash = UserBean.encodePasswd(login, new String(credentials), HttpDigest.REALM);
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

    public void test() throws InvalidIdProviderCfgException {
        if (config.getOid() != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID) {
            logger.warning("Testing an internal id provider with no good oid. Throwing InvalidIdProviderCfgException");
            throw new InvalidIdProviderCfgException("This internal ID provider config is not valid.");
        }
    }

    public void preSaveClientCert( User user, X509Certificate[] certChain ) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    protected void initDao() throws Exception {
        super.initDao();
    }

}
