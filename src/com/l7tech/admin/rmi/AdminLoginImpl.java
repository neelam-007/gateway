/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin.rmi;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminLogin;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author emil
 * @version Dec 2, 2004
 */
public class AdminLoginImpl extends ApplicationObjectSupport
  implements AdminLogin, InitializingBean {
    //todo: consider moving to Spring bean configuration
    private final List adminGroups = Arrays.asList(new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME});
    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private X509Certificate serverCertificate;

    public AdminContext login(String username, String password)
      throws RemoteException, AccessControlException, LoginException {
        if (username == null || password == null) {
            throw new AccessControlException("Illegal username or password");
        }
        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            User user = getInternalIdentityProvider().authenticate(creds);
            if (user == null) {
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            }
            String[] roles = getUserRoles(user);
            if (!containsAdminAccessRole(roles)) {
                throw new AccessControlException(user.getName() + " does not have privilege to access administrative services");
            }
            logger.info("" + getHighestRole(roles) + " user.getLogin() " + "logged in from IP " + UnicastRemoteObject.getClientHost());
            AdminContext adminContext = (AdminContext)getApplicationContext().getBean("adminContextRemote");
            return adminContext;
        } catch (AuthenticationException e) {
            logger.trace("Authentication failed", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (FindException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (IOException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (ServerNotActiveException e) {
            throw new RemoteException("Illegal state/server not exported", e);
        } catch (InvalidIdProviderCfgException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
        }
    }

    /**
     * Method that returns the SHA-1 hash over admin certificate and the admin
     * username.
     * This provides a way for the admin to validate the server certificate.
     *
     * @param username The name of the user.
     * @return The Server certificate.
     * @throws java.security.AccessControlException
     *                                  on access denied for the given credentials
     * @throws java.rmi.RemoteException on remote communicatiOn error
     */
    public byte[] getServerCertificate(String username)
      throws RemoteException, AccessControlException {
        if (username == null) {
            throw new AccessControlException("Illegal username or password");
        }
        try {
            User user = getInternalIdentityProvider().getUserManager().findByLogin(username);
            return getDigest(user.getPassword(), serverCertificate);
        } catch (FindException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication failed").initCause(e);
        } catch (InvalidIdProviderCfgException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Server error", e);
            throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        } catch (CertificateEncodingException e) {
            logger.warn("Server error", e);
               throw (AccessControlException)new AccessControlException("Server Error").initCause(e);
        }
    }

    private IdentityProvider getInternalIdentityProvider() throws FindException, InvalidIdProviderCfgException {
        IdentityProviderConfig cfg = identityProviderConfigManager.findByPrimaryKey(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        if (cfg == null) {
            throw new IllegalStateException("Could not find the internal identoty manager!");
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

    private String getHighestRole(String[] roles) {
        List aRoles = Arrays.asList(roles);
        if (aRoles.contains(Group.ADMIN_GROUP_NAME)) {
            return "Gateway Administrator";
        }
        if (aRoles.contains(Group.OPERATOR_GROUP_NAME)) {
            return "Gateway Operator";
        }

        return "Unknown administrative role";
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
        d.update(password.getBytes());
        d.update(serverCertificate.getEncoded());
        return d.digest();
    }
}
