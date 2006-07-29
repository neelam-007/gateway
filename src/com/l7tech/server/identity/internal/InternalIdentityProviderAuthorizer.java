/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.internal;

import com.l7tech.common.Authorizer;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.beans.factory.InitializingBean;

import javax.security.auth.Subject;
import java.util.*;
import java.util.logging.Logger;

/**
 * The <code>AdminAuthorizer</code> that authorizes the internal users against their
 * groups.
 *
 * @author emil
 * @version Sep 2, 2004
 */
public class InternalIdentityProviderAuthorizer extends Authorizer implements InitializingBean {
    private static final Logger logger = Logger.getLogger(InternalIdentityProviderAuthorizer.class.getName());

    IdentityProviderConfigManager identityProviderConfigManager;

    private IdentityProvider identityProvider;
    private UserManager userManager;
    private RoleManager roleManager;

    /**
     * Determine the roles (groups) for hte given subject
     *
     * @param subject the subject
     * @return the set of user roles for the given subject
     * @throws RuntimeException if
     */
    public Collection<Role> getUserRoles(Subject subject) throws RuntimeException {
        Set<User> principals = subject.getPrincipals(User.class);
        if (principals.isEmpty()) {
            logger.fine("No principal set, returning empty set");
            return Collections.emptySet();
        }
        try {
            for (User user : principals) {
                User dbUser = userManager.findByLogin(user.getLogin());
                if (dbUser != null) {
                    return roleManager.getAssignedRoles(dbUser);
                }
            }
            return Collections.emptySet();
        } catch (FindException e) {
            throw new RuntimeException("Error accessing user roles", e);
        }
    }

    public void setIdentityProviderConfigManager(IdentityProviderConfigManager identityProviderConfigManager) {
        this.identityProviderConfigManager = identityProviderConfigManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void afterPropertiesSet() throws Exception {
        if (identityProviderConfigManager == null) {
            throw new IllegalArgumentException("Identity Provider required");
        }
        Iterator it = identityProviderConfigManager.findAllIdentityProviders().iterator();
        while (it.hasNext()) {
            IdentityProvider prov = (IdentityProvider)it.next();
            if (prov.getConfig().type() == IdentityProviderType.INTERNAL) {
                identityProvider = prov;
                break;
            }
        }
        if (identityProvider == null) {
            throw new IllegalStateException("Could not find the internal identity provider");
        }
        userManager = identityProvider.getUserManager();
    }
}