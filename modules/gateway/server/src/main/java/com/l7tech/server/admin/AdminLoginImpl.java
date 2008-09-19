/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.admin;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.system.FailedAdminLoginEvent;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BuildInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminLoginImpl
        extends ApplicationObjectSupport
        implements AdminLogin, InitializingBean
{
    private static final Logger logger = Logger.getLogger(AdminLoginImpl.class.getName());
    
    private AdminSessionManager sessionManager;

    private final DefaultKey defaultKey;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;

    public AdminLoginImpl(DefaultKey defaultKey, SsgKeyStoreManager ssgKeyStoreManager) {
        this.defaultKey = defaultKey;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    public AdminLoginResult login( final String username, final String password )
            throws AccessControlException, LoginException
    {
        if (username == null || password == null) {
            throw new AccessControlException("Username and password are both required");
        }

        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            User user = sessionManager.authenticate( creds );

            boolean remoteLogin = true;
            String remoteIp = null;
            try {
                remoteIp = RemoteUtils.getClientHost();
            } catch (ServerNotActiveException snae) {
                remoteLogin = false;
            }

            if (user == null) {
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + username + "'"));
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

            return new AdminLoginResult(user, cookie, SecureSpanConstants.ADMIN_PROTOCOL_VERSION, BuildInfo.getProductVersion());
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        }
    }

    public void changePassword(final String currentPassword, final String newPassword) throws LoginException {
        if (currentPassword == null || newPassword == null) {
            throw new AccessControlException("currentPassword and newPassword are both required");
        }

        try {
            User remoteUser = JaasUtils.getCurrentUser();
            if (remoteUser == null)
                throw new AccessControlException("Authentication error, no user.");

            if ( !sessionManager.changePassword( remoteUser, currentPassword, newPassword ) ) {
                throw new FailedLoginException("'" + remoteUser.getLogin() + "'" + " could not be authenticated");
            }

        } catch (InvalidPasswordException ipe) {
            throw new IllegalArgumentException(ipe.getMessage());    
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException) new AccessControlException("Authentication failed").initCause(e);
        } 
    }

    public AdminLoginResult resume( final String sessionId ) throws AuthenticationException {
        User user = null;
        try {
            user = sessionManager.resumeSession(sessionId);
        } catch ( ObjectModelException fe ) {
            logger.log(Level.WARNING,  "Error resuming session.", fe );
        }

        if ( user == null ) {
            throw new AuthenticationException("Authentication failed");
        }

        return new AdminLoginResult(user, sessionId, SecureSpanConstants.ADMIN_PROTOCOL_VERSION, BuildInfo.getProductVersion());
    }

    public void logout() {
        User user = JaasUtils.getCurrentUser();
        sessionManager.destroySession(user);
    }

    public void setAdminSessionManager(AdminSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Method that returns the SHA-1 hash over admin certificate and the admins
     * password.
     *
     * <p>This provides a way for the admin to validate the server certificate.</p>
     *
     * <p>Note that if you pass in an incorrect admin username you will get back
     * garbage.</p>
     *
     * @param username The name of the user.
     * @return The hash.
     * @throws java.security.AccessControlException
     *                                  on access denied for the given credentials
     */
    public byte[] getServerCertificate(String username)
      throws AccessControlException {
        try {
            String digestWith = null;

            if (username != null) {
                try {
                    InternalUser user = getInternalIdentityProvider().getUserManager().findByLogin(username);
                    if (user != null) digestWith = user.getHashedPassword(); 
                } catch (ObjectModelException e) {
                    // catch here so there is no difference to the client for one username vs another.
                    logger.log(Level.WARNING, "Authentication provider error", e);
                }
            }

            if (digestWith == null) {
                digestWith = Integer.toString(AdminLoginImpl.class.hashCode() * 17) + username;
            }

            X509Certificate certificate = getCurrentConnectorCertificate();
            return getDigest(digestWith, certificate);
        } catch (InvalidIdProviderCfgException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (ObjectNotFoundException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        }
    }




    private byte[] getDigest(String password, X509Certificate serverCertificate)
      throws NoSuchAlgorithmException, CertificateEncodingException {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
        byte[] bytes = password.getBytes();
        d.update(bytes);
        d.update(serverCertificate.getEncoded());
        d.update(bytes);
        return d.digest();
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

    public void afterPropertiesSet() throws Exception {
        checkidentityProviderConfigManager();
    }

    private InternalIdentityProvider getInternalIdentityProvider() throws ObjectModelException, InvalidIdProviderCfgException {
        IdentityProviderConfig cfg = identityProviderConfigManager.findByPrimaryKey(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        if (cfg == null) {
            throw new IllegalStateException("Could not find the internal identity provider!");
        }

        return (InternalIdentityProvider) identityProviderFactory.makeProvider(cfg);
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

}
