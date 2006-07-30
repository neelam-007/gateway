/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
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
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.spring.remoting.RemoteUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version Dec 2, 2004
 */
public class AdminLoginImpl extends ApplicationObjectSupport implements AdminLogin, InitializingBean {
    private static final Logger logger = Logger.getLogger(AdminLoginImpl.class.getName());

    private AdminSessionManager sessionManager;

    //todo: consider moving to Spring bean configuration
    private final List adminGroups = Arrays.asList(new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME});
    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private RoleManager roleManager;
    private X509Certificate serverCertificate;

    public AdminLoginResult login(String username, String password)
            throws RemoteException, AccessControlException, LoginException
    {
        if (username == null || password == null) {
            throw new AccessControlException("Illegal username or password");
        }

        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            AuthenticationResult authResult = getInternalIdentityProvider().authenticate(creds);
            if (authResult == null) {
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            }
            User user = authResult.getUser();

            boolean ok = false;
            // TODO is holding any CRUD permission sufficient?
            Collection<Role> roles = roleManager.getAssignedRoles(user);
            Set<Permission> perms = new HashSet<Permission>();
            for (Role role : roles) {
                perms.addAll(role.getPermissions());
                for (Permission perm : role.getPermissions()) {
                    if (perm.getEntityType() != null && perm.getOperation() != OperationType.OTHER) {
                        ok = true;
                    }
                }
            }

            if (!ok) throw new AccessControlException(user.getName() +
                    " does not have privilege to access administrative services");

            logger.info("User '" + user.getLogin() + "' logged in from IP '" +
                    RemoteUtils.getClientHost() + "'.");

            AdminContext adminContext = new AdminContextBean(
                        null,null,null,null,null,null,null,null,null,null,
                        SecureSpanConstants.ADMIN_PROTOCOL_VERSION,
                        BuildInfo.getProductVersion());


            getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGON, perms));

            String cookie = sessionManager.createSession(user);

            return new AdminLoginResult(user, perms, adminContext, cookie);
        } catch (ServerNotActiveException snae) {
            logger.log(Level.FINE, "Authentication failed", snae);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(snae);
        } catch (AuthenticationException e) {
            logger.log(Level.FINE, "Authentication failed", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (InvalidIdProviderCfgException e) {
            logger.log(Level.WARNING, "Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
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
                    if (user != null) {
                        String[] roles = getUserRoles(user);
                        if (containsAdminAccessRole(roles)) {
                            digestWith = user.getPassword();
                        }
                    }
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
    }

    private void checkidentityProviderConfigManager() {
        if (identityProviderConfigManager == null) {
            throw new IllegalArgumentException("identity provider config is required");
        }
    }

    private boolean containsAdminAccessRole(String[] groups) {
        for (int i = 0; i < groups.length; i++) {
            String group = groups[i];
            if (adminGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    private String[] getUserRoles(User user) throws FindException, InvalidIdProviderCfgException {
        IdentityProvider provider = getInternalIdentityProvider();
        GroupManager gman = provider.getGroupManager();
        List roles = new ArrayList();
        for (Iterator i = gman.getGroupHeaders(user).iterator(); i.hasNext();) {
            EntityHeader grp = (EntityHeader)i.next();
            roles.add(grp.getName());
        }
        return (String[])roles.toArray(new String[]{});
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
}
