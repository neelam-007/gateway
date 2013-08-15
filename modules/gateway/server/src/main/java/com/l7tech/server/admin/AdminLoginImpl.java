/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.admin;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.GatewayConfiguration;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.FailedAdminLoginEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminLoginImpl
        extends ApplicationObjectSupport
        implements AdminLogin, InitializingBean {
    @SuppressWarnings({"FieldNameHidesFieldInSuperclass"})
    private static final Logger logger = Logger.getLogger(AdminLoginImpl.class.getName());

    private AdminSessionManager sessionManager;

    private final SecureRandom secureRandom;
    private final DefaultKey defaultKey;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private PasswordHasher passwordHasher;
    private Config config;

    public AdminLoginImpl(DefaultKey defaultKey, SsgKeyStoreManager ssgKeyStoreManager, PasswordHasher passwordHasher, SecureRandom secureRandom) {
        this.defaultKey = defaultKey;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.passwordHasher = passwordHasher;
        this.secureRandom = secureRandom;
    }

    @Override
    public AdminLoginResult login(final String username, final String password)
            throws AccessControlException, LoginException {
        final X509Certificate cert = RemoteUtils.getClientCertificate();
        if (username == null || password == null || (cert == null && password.isEmpty())) { // Don't trim password here, i.e., NOT use password.trim().isEmpty(), because white space password is probably valid and allowed.
            throw new AccessControlException(ERR_MSG_USERNAME_PSWD_BOTH_REQUIRED);
        }
        String login = username;

        try {
            LoginCredentials creds;
            if (!username.equalsIgnoreCase("")) {
                creds = LoginCredentials.makeLoginCredentials(
                        new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, username, password.toCharArray()), null);
            } else {
                if (cert == null) {
                    throw new AccessControlException("Username and password or certificate is required.");
                }

                creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(cert), null);
                login = creds.getLogin();
            }

            boolean remoteLogin = true;
            String remoteIp = null;
            try {
                remoteIp = RemoteUtils.getClientHost();
            } catch (ServerNotActiveException snae) {
                remoteLogin = false;
            }

            User user;
            try {
                final AuthenticationResult authResult = sessionManager.authenticate(creds);
                user = (authResult != null)? authResult.getUser(): null;
            } catch (LoginRequireClientCertificateException e) {
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + login + "'"));
                throw e;
            } catch (FailAttemptsExceededException faee) {
                //reached to the max number of failed attempts, we'll need to lock the account.
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + login + "'"));
                throw new AccountLockedException("'" + creds.getLogin() + "'" + " exceeded max. failed logon attempts.");
            } catch (FailInactivityPeriodExceededException faee) {
                //reached to the max inactivity period, we'll need to lock the account.
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + login + "'"));
                throw new AccountLockedException("'" + creds.getLogin() + "'" + " exceeded inactivity period.");
            } catch (UserDisabledException e1) {
                //user is disabled, treat it as if user could not be authenticated
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + login + "'"));
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " is disabled.");
            }

            if (user == null) {
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + login + "'"));
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            }

            if (remoteLogin) {
                logger.info("User '" + user.getLogin() + "' logged in from IP '" + remoteIp + "'.");
            } else {
                logger.finer("User '" + user.getLogin() + "' logged in locally.");
            }

            String cookie = "-";
            if (remoteLogin) {
                // If local, caller is responsible for generating event/session if required
                getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGON));
                cookie = sessionManager.createSession(user, null);
            }

            return new AdminLoginResult(user, cookie, SecureSpanConstants.ADMIN_PROTOCOL_VERSION, BuildInfo.getProductVersion(), getLogonWarningBanner());
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw buildAccessControlException("Authentication failed", e);
        }
    }

    @Override
    public AdminLoginResult loginWithPasswordUpdate(final String username, final String oldPassword, final String newPassword)
            throws AccessControlException, LoginException, InvalidPasswordException {

        //attempt to change the password
        try {
            if (!sessionManager.changePassword(username, oldPassword, newPassword)) {
                throw new FailedLoginException("'" + username + "'" + " could not be authenticated");
            }
        } catch (InvalidPasswordException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw buildAccessControlException("Authentication failed", e);
        }

        //password change was successful, proceed to login
        return login(username, newPassword);
    }


    @Override
    public void changePassword(final String currentPassword, final String newPassword) throws LoginException, InvalidPasswordException {
        if (currentPassword == null || newPassword == null) {
            throw new AccessControlException("currentPassword and newPassword are both required");
        }

        try {
            User remoteUser = JaasUtils.getCurrentUser();
            if (remoteUser == null)
                throw new AccessControlException("Authentication error, no user.");

            if (!sessionManager.changePassword(remoteUser, currentPassword, newPassword)) {
                throw new FailedLoginException("'" + remoteUser.getLogin() + "'" + " could not be authenticated");
            }
        } catch (InvalidPasswordException e) {
            throw e;
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw buildAccessControlException("Authentication failed", e);
        }
    }

    @Override
    public AdminLoginResult resume(final String sessionId) throws AuthenticationException {
        User user = null;
        try {
            user = sessionManager.resumeSession(sessionId);
        } catch (ObjectModelException fe) {
            logger.log(Level.WARNING, "Error resuming session.", fe);
        }

        if (user == null) {
            throw new AuthenticationException("Authentication failed");
        }

        return new AdminLoginResult(user, sessionId, SecureSpanConstants.ADMIN_PROTOCOL_VERSION, BuildInfo.getProductVersion(), getLogonWarningBanner());
    }

    @Override
    public void logout() {
        User user = JaasUtils.getCurrentUser();
        getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGOFF));
        sessionManager.destroySession(AdminLoginHelper.getSessionId());
    }

    @Override
    public void ping() {
    }

    public void setAdminSessionManager(AdminSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public ServerCertificateVerificationInfo getServerCertificateVerificationInfo(final String username, byte[] clientNonce) throws AccessControlException {
        try {
            byte[] serverNonce = new byte[20];
            secureRandom.nextBytes(serverNonce);
            byte[] passwordSalt = null;
            byte[] verifierSharedSecret = null;

            if (username != null && config.getBooleanProperty("admin.certificateDiscoveryEnabled", true)) {
                try {
                    InternalUser user = getInternalIdentityProvider().getUserManager().findByLogin(username);
                    if (user != null) {
                        String hashedPassword = user.getHashedPassword();
                        if (passwordHasher != null && passwordHasher.isVerifierRecognized(hashedPassword)) {
                            passwordSalt = passwordHasher.extractSaltFromVerifier(hashedPassword);
                            verifierSharedSecret = hashedPassword.getBytes(Charsets.UTF8);
                        }
                    }
                } catch (ObjectModelException e) {
                    // catch here so there is no difference to the client for one username vs another.
                    logger.log(Level.WARNING, "Authentication provider error", e);
                }
            }

            // If we don't known the password use a value that will fail but will
            // always give the same value for the name. This may help prevent discovery of
            // admin account usernames
            if (verifierSharedSecret == null) {
                verifierSharedSecret = (username + AdminLogin.class.hashCode()).getBytes(Charsets.UTF8);
                passwordSalt = passwordHasher.extractSaltFromVerifier(passwordHasher.hashPassword(verifierSharedSecret));
            }

            if (clientNonce == null) {
                // Invalid call, but we'll generate a bogus client salt to prevent an NPE and return a bogus verifier hash instead
                clientNonce = new byte[]{(byte) 66, (byte) 33, (byte) 11, (byte) 44};
            }

            X509Certificate certificate = getCurrentConnectorCertificate();
            final byte[] checkHash = CertUtils.getVerifierBytes(verifierSharedSecret, clientNonce, serverNonce, certificate.getEncoded());
            return new ServerCertificateVerificationInfo("L7VF-6.0", serverNonce, passwordSalt, checkHash);

        } catch (InvalidIdProviderCfgException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw buildAccessControlException("Authentication provider error", e);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        } catch (ObjectNotFoundException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw buildAccessControlException("Server Error", e);
        }
    }

    @Override
    public byte[] getServerCertificate( final String username ) {
        return new byte[0];
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private AccessControlException buildAccessControlException(final String message, final Throwable cause) {
        return (AccessControlException) new AccessControlException(message).initCause(cause);
    }

    private X509Certificate getDefaultSslCertificate() throws IOException {
        return defaultKey.getSslInfo().getCertificate();
    }

    private X509Certificate getCurrentConnectorCertificate() throws IOException, ObjectModelException, KeyStoreException {
        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null)
            throw new AccessControlException("Admin request disallowed: No request context available");

        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null)
            throw new AccessControlException("Admin request disallowed: Unable to determine which connector this request came in on");

        Long keystoreId = connector.getKeystoreOid();
        String keyAlias = connector.getKeyAlias();
        if (keystoreId == null || keyAlias == null)
            return getDefaultSslCertificate();

        SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, keystoreId);
        return entry == null ? getDefaultSslCertificate() : entry.getCertificate();
    }

    public Config getServerConfig() {
        return config;
    }

    public void setServerConfig(Config config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        checkidentityProviderConfigManager();
    }

    private InternalIdentityProvider getInternalIdentityProvider() throws ObjectModelException, InvalidIdProviderCfgException {
        IdentityProvider provider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        if (!(provider instanceof InternalIdentityProvider)) {
            throw new IllegalStateException("Could not find the internal identity provider!");
        }
        return (InternalIdentityProvider) provider;
    }


    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setIdentityProviderConfigManager(IdentityProviderConfigManager cm) {
        this.identityProviderConfigManager = cm;
    }

    private void checkidentityProviderConfigManager() {
        if (identityProviderConfigManager == null || identityProviderFactory == null) {
            throw new IllegalArgumentException("IPCM and IPF are required");
        }
    }

    private String getLogonWarningBanner() {
        String prop = config.getProperty( ServerConfigParams.PARAM_LOGON_WARNING_BANNER );

        // If the banner prop value just contains whitespace, then set the prop as null.
        if (prop != null && prop.trim().isEmpty()) prop = null;

        return prop;
    }
}
