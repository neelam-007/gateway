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
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.sql.SQLException;
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

    public AdminContext login(String username, String password)
      throws RemoteException, AccessControlException, LoginException {
        if (username == null || password == null) {
            throw new AccessControlException("Illegal username or password");
        }
        try {
            LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
            User user = identityProviderConfigManager.getInternalIdentityProvider().authenticate(creds);
            if (user == null) {
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            }
            String[] roles = getUserRoles(user);
            if (!hasPermission(user, roles)) {
                throw new AccessControlException(user.getName() + " does not have privilege to access administrative services");
            }
            logger.info(""+getHighestRole(roles)+" user.getLogin() "+"logged in from IP " + UnicastRemoteObject.getClientHost());
            AdminContext adminContext = (AdminContext)getApplicationContext().getBean("adminContextRemote");
            return adminContext;
        } catch (AuthenticationException e) {
            logger.trace("Authentication failed", e);
            AccessControlException ae = new AccessControlException("Authentication failed");
            ae.initCause(e);
            throw ae;
        } catch (FindException e) {
            logger.warn("Authentication provider error", e);
            AccessControlException ae = new AccessControlException("Authentication failed");
            ae.initCause(e);
            throw ae;
        } catch (IOException e) {
            logger.warn("Authentication provider error", e);
            AccessControlException ae = new AccessControlException("Authentication failed");
            ae.initCause(e);
            throw ae;
        } catch (ServerNotActiveException e) {
            throw new RemoteException("Illegal state/server not exported", e);
        } finally {
            closeContext();
        }
    }


    public void setIdentityProviderConfigManager(IdentityProviderConfigManager cm) {
        this.identityProviderConfigManager = cm;
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

    private boolean hasPermission(User user, String[] groups) {
        IdentityProvider provider = identityProviderConfigManager.getInternalIdentityProvider();
        GroupManager gman = provider.getGroupManager();
        for (int i = 0; i < groups.length; i++) {
            String group = groups[i];
            if (adminGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    private String[] getUserRoles(User user) throws FindException {
        IdentityProvider provider = identityProviderConfigManager.getInternalIdentityProvider();
        GroupManager gman = provider.getGroupManager();
        List roles = new ArrayList();
        for (Iterator i = gman.getGroupHeaders(user).iterator(); i.hasNext();) {
            EntityHeader grp = (EntityHeader)i.next();
            roles.add(grp.getName());
        }
        return (String[])roles.toArray(new String[]{});
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.warn("error closing context", e);
        }
    }

}