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
            logger.info(""+getHighestRole(roles)+" user.getLogin() "+"logged in from IP " + UnicastRemoteObject.getClientHost());
            AdminContext adminContext = (AdminContext)getApplicationContext().getBean("adminContextRemote");
            return adminContext;
        } catch (AuthenticationException e) {
            logger.trace("Authentication failed", e);
            throw (AccessControlException) new AccessControlException("Authentication failed").initCause(e);
        } catch (FindException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException) new AccessControlException("Authentication failed").initCause(e);
        } catch (IOException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException) new AccessControlException("Authentication failed").initCause(e);
        } catch (ServerNotActiveException e) {
            throw new RemoteException("Illegal state/server not exported", e);
        } catch (InvalidIdProviderCfgException e) {
            logger.warn("Authentication provider error", e);
            throw (AccessControlException)new AccessControlException("Authentication provider error").initCause(e);
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
}