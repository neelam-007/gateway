/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.admin.rmi;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminContextBean;
import com.l7tech.admin.AdminLogin;
import com.l7tech.admin.AdminLoginResult;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.FailedAdminLoginEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.spring.remoting.RemoteUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminLoginImpl
        extends ApplicationObjectSupport
        implements AdminLogin, InitializingBean, ApplicationListener
{
    private static final Logger logger = Logger.getLogger(AdminLoginImpl.class.getName());

    private AdminSessionManager sessionManager;

    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private final Object providerSync = new Object();
    private Set<IdentityProvider> adminProviders;
    private RoleManager roleManager;
    private X509Certificate serverCertificate;

    public AdminLoginResult login(String username, String password)
            throws AccessControlException, LoginException
    {
        if (username == null || password == null) {
            throw new AccessControlException("Username and password are both required");
        }

        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            // Try internal first (internal accounts with the same credentials should hide externals)
            List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
            Set<IdentityProvider> tempProviders;
            synchronized(providerSync) {
                tempProviders = adminProviders;
            }
            providers.addAll(tempProviders);
            User user = null;

            for (IdentityProvider provider : providers) {
                try {
                    AuthenticationResult authResult = provider.authenticate(creds);
                    User authdUser = authResult == null ? null : authResult.getUser();
                    if (authdUser != null) {
                        checkPerms(authdUser);
                        logger.info("Authenticated on " + provider.getConfig().getName());
                        user = authdUser;
                        break;
                    }
                } catch (AuthenticationException e) {
                    logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                }
            }

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

            AdminContext adminContext = makeAdminContext();
            String cookie = "-";
            if (remoteLogin) {
                // If local, caller is responsible for generating event/session if required
                getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGON));
                cookie = sessionManager.createSession(user, null);
            }

            return new AdminLoginResult(user, adminContext, cookie);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        }
    }

    private void checkPerms(User user) throws AuthenticationException, FindException {
        boolean ok = false;
        // TODO is holding any CRUD permission sufficient?
        Collection<Role> roles = roleManager.getAssignedRoles(user);
        roles: for (Role role : roles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getEntityType() != null && perm.getOperation() != OperationType.NONE) {
                    ok = true;
                    break roles;
                }
            }
        }

        if (!ok) throw new AuthenticationException(user.getName() +
                " does not have privilege to access administrative services");
    }

    private AdminContext makeAdminContext() {
        return new AdminContextBean(
                    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
                    SecureSpanConstants.ADMIN_PROTOCOL_VERSION,
                    BuildInfo.getProductVersion());
    }

    public void changePassword(final String currentPassword, final String newPassword) throws LoginException {
        if (currentPassword == null || newPassword == null) {
            throw new AccessControlException("currentPassword and newPassword are both required");
        }

        try {
            User remoteUser = JaasUtils.getCurrentUser();
            if (remoteUser == null)
                throw new AccessControlException("Authentication error, no user.");

            LoginCredentials creds = new LoginCredentials(remoteUser.getLogin(), currentPassword.toCharArray(), null);

            // Try internal first (internal accounts with the same credentials should hide externals)
            List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
            Set<IdentityProvider> tempProviders;
            synchronized(providerSync) {
                tempProviders = adminProviders;
            }
            providers.addAll(tempProviders);
            User user = null;

            for (IdentityProvider provider : providers) {
                try {
                    AuthenticationResult authResult = provider.authenticate(creds);
                    User authenticatedUser = authResult == null ? null : authResult.getUser();
                    if (authenticatedUser != null) {
                        checkPerms(authenticatedUser);
                        user = authenticatedUser;
                        logger.info("Authenticated on " + provider.getConfig().getName() + " (changing password)");

                        if (authenticatedUser instanceof InternalUser) {
                            ((InternalUser)user).setCleartextPassword(newPassword);
                            provider.getUserManager().update(user);
                            break;
                        } else {
                            throw new IllegalStateException("Cannot change password for user.");
                        }
                    }
                } catch (AuthenticationException e) {
                    logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                }
            }

            if (user == null)
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");

        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException) new AccessControlException("Authentication failed").initCause(e);
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException) new AccessControlException("Update failed").initCause(e);
        } catch (InvalidPasswordException ipe) {
            throw new IllegalArgumentException(ipe.getMessage());    
        }
    }

    public AdminLoginResult resume(String sessionId) throws AuthenticationException {
        Principal userObj = sessionManager.resumeSession(sessionId);
        if (!(userObj instanceof User)) {
            logger.log(Level.WARNING,  "Authentication failed: attempt to resume unrecognized session");
            throw new AuthenticationException("Authentication failed");
        }

        try {
            User user = (User) userObj;
            checkPerms(user);
            AdminContext adminContext = makeAdminContext();
            return new AdminLoginResult(user, adminContext, sessionId);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw new AuthenticationException("Authentication failed", e);
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "User no longer has any admin permissions", e);
            throw new AuthenticationException("User no longer has any admin permissions", e);
        }
    }

    public void logout() {
        User user = JaasUtils.getCurrentUser();
        sessionManager.destroySession(user);
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
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
                } catch (FindException e) {
                    // catch here so there is no difference to the client for one username vs another.
                    logger.log(Level.WARNING, "Authentication provider error", e);
                }
            }

            if (digestWith == null) {
                digestWith = Integer.toString(AdminLoginImpl.class.hashCode() * 17) + username;
            }

            return getDigest(digestWith, serverCertificate);
        } catch (InvalidIdProviderCfgException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        }
    }

    private InternalIdentityProvider getInternalIdentityProvider() throws FindException, InvalidIdProviderCfgException {
        IdentityProviderConfig cfg = identityProviderConfigManager.findByPrimaryKey(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        if (cfg == null) {
            throw new IllegalStateException("Could not find the internal identity manager!");
        }

        return (InternalIdentityProvider) identityProviderFactory.makeProvider(cfg);
    }


    public void setIdentityProviderConfigManager(IdentityProviderConfigManager cm) {
        this.identityProviderConfigManager = cm;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setServerCertificate(X509Certificate serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    public void afterPropertiesSet() throws Exception {
        checkidentityProviderConfigManager();
        setupAdminProviders();
    }

    private void setupAdminProviders() throws FindException {
        // Find any non-internal providers that support admin
        SortedSet<IdentityProvider> adminProviders = new TreeSet<IdentityProvider>(INTERNAL_FIRST_COMPARATOR);

        for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders()) {
            IdentityProviderConfig config = provider.getConfig();
            if (config.isAdminEnabled()) {
                logger.info("Enabling " + config.getName() + " for admin logins");
                adminProviders.add(provider);
            }
        }
        
        synchronized(providerSync) {
            this.adminProviders = Collections.unmodifiableSet(adminProviders);
        }
    }

    private void checkidentityProviderConfigManager() {
        if (identityProviderConfigManager == null || identityProviderFactory == null) {
            throw new IllegalArgumentException("IPCM and IPF are required");
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

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (eie.getEntityClass() == IdentityProviderConfig.class) {
                try {
                    setupAdminProviders();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Couldn't setup admin providers", e);
                }
            }
        }
    }

    /**
     * Within internal providers, lower-numbered providers sort first, and all internal providers sort before all
     * non-internal providers.
     */
    private static class InternalFirstComparator implements Comparator<IdentityProvider> {
        public int compare(IdentityProvider o1, IdentityProvider o2) {
            IdentityProviderConfig config1 = o1.getConfig();
            IdentityProviderConfig config2 = o2.getConfig();
            if (config1.type() == IdentityProviderType.INTERNAL && config2.type() == IdentityProviderType.INTERNAL) {
                // Both internal, lower-numbered (i.e. -2 for "the" internal provider) comes first
                return config1.getOid() < config2.getOid() ? -1 : 1;
            } else {
                // Internal comes first
                return config1.type() == IdentityProviderType.INTERNAL ? -1 : 1;
            }
        }
    }

    private static final InternalFirstComparator INTERNAL_FIRST_COMPARATOR = new InternalFirstComparator();
}
