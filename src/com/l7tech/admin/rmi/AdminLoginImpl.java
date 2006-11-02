/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.admin.rmi;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminContextBean;
import com.l7tech.admin.AdminLogin;
import com.l7tech.admin.AdminLoginResult;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.spring.remoting.RemoteUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version Dec 2, 2004
 */
public class AdminLoginImpl
        extends ApplicationObjectSupport
        implements AdminLogin, InitializingBean, ApplicationListener
{
    private static final Logger logger = Logger.getLogger(AdminLoginImpl.class.getName());

    private AdminSessionManager sessionManager;

    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private Set<IdentityProvider> adminProviders;
    private RoleManager roleManager;
    private X509Certificate serverCertificate;

    public AdminLoginResult login(String username, String password)
            throws RemoteException, AccessControlException, LoginException
    {
        if (username == null || password == null) {
            throw new AccessControlException("Username and password are both required");
        }

        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            // Try internal first (internal accounts with the same credentials should hide externals)
            List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
            Set<IdentityProvider> tempProviders;
            synchronized(this) {
                tempProviders = adminProviders;
            }
            providers.addAll(tempProviders);
            Set<Permission> perms = null;
            User user = null;

            for (IdentityProvider provider : providers) {
                try {
                    AuthenticationResult authResult = provider.authenticate(creds);
                    user = authResult == null ? null : authResult.getUser();
                    if (user != null) {
                        perms = getPerms(user);
                        logger.info("Authenticated on " + provider.getConfig().getName());
                        break;
                    }
                } catch (AuthenticationException e) {
                    logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                }
            }

            if (perms == null)
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            
            logger.info("User '" + user.getLogin() + "' logged in from IP '" +
                    RemoteUtils.getClientHost() + "'.");

            AdminContext adminContext = makeAdminContext();

            getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGON, perms));

            String cookie = sessionManager.createSession(user);

            return new AdminLoginResult(user, perms, adminContext, cookie);
        } catch (ServerNotActiveException snae) {
            logger.log(Level.FINE, "Authentication failed", snae);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(snae);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        }
    }

    private Set<Permission> getPerms(User user) throws AuthenticationException, FindException {
        boolean ok = false;
        // TODO is holding any CRUD permission sufficient?
        Collection<Role> roles = roleManager.getAssignedRoles(user);
        Set<Permission> perms = new HashSet<Permission>();
        roles: for (Role role : roles) {
            perms.addAll(role.getPermissions());
            for (Permission perm : role.getPermissions()) {
                if (perm.getEntityType() != null && perm.getOperation() != OperationType.NONE) {
                    ok = true;
                    break roles;
                }
            }
        }

        if (!ok) throw new AuthenticationException(user.getName() +
                " does not have privilege to access administrative services");
        return perms;
    }

    private AdminContext makeAdminContext() {
        return new AdminContextBean(
                    null,null,null,null,null,null,null,null,null,null,
                    SecureSpanConstants.ADMIN_PROTOCOL_VERSION,
                    BuildInfo.getProductVersion());
    }

    public AdminLoginResult resume(String sessionId) throws RemoteException, AuthenticationException, LoginException {
        Principal userObj = sessionManager.resumeSession(sessionId);
        if (!(userObj instanceof User)) {
            logger.log(Level.WARNING,  "Authentication failed: attempt to resume unrecognized session");
            throw new AccessControlException("Authentication failed");
        }

        try {
            User user = (User) userObj;
            Set<Permission> perms = getPerms(user);
            AdminContext adminContext = makeAdminContext();
            return new AdminLoginResult(user, perms, adminContext, sessionId);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw new AuthenticationException("Authentication failed", e);
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "User no longer has any admin permissions", e);
            throw new AuthenticationException("User no longer has any admin permissions", e);
        }
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
     * @throws java.rmi.RemoteException on remote communicatiOn error
     */
    public byte[] getServerCertificate(String username)
      throws RemoteException, AccessControlException {
        try {
            String digestWith = null;

            if (username != null) {
                try {
                    User user = getInternalIdentityProvider().getUserManager().findByLogin(username);
                    if (user != null) digestWith = user.getPassword(); 
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

    private IdentityProvider getInternalIdentityProvider() throws FindException, InvalidIdProviderCfgException {
        IdentityProviderConfig cfg = identityProviderConfigManager.findByPrimaryKey(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        if (cfg == null) {
            throw new IllegalStateException("Could not find the internal identity manager!");
        }

        return identityProviderFactory.makeProvider(cfg);
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

        for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders(identityProviderConfigManager)) {
            IdentityProviderConfig config = provider.getConfig();
            if (config.isAdminEnabled()) {
                logger.info("Enabling " + config.getName() + " for admin logins");
                adminProviders.add(provider);
            }
        }
        
        synchronized(this) {
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
