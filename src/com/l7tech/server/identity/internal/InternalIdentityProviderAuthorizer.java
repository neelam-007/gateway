/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.internal;

import com.l7tech.common.Authorizer;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The <code>AdminAuthorizer</code> that authorizes the internal users against their
 * groups.
 *
 * @author emil
 * @version Sep 2, 2004
 */
public class InternalIdentityProviderAuthorizer extends Authorizer
    implements ApplicationContextAware, InitializingBean {
    private static final Logger logger = Logger.getLogger(InternalIdentityProviderAuthorizer.class.getName());

    private ApplicationContext applicationContext;
    IdentityProviderConfigManager identityProviderConfigManager;

    private IdentityProvider identityProvider;
    private UserManager userManager;
    private GroupManager groupManager;

    /**
     * Determine the roles (groups) for hte given subject
     *
     * @param subject the subject
     * @return the set of user roles for the given subject
     * @throws RuntimeException if
     */
    public Set getUserRoles(Subject subject) throws RuntimeException {
        Set principals = subject.getPrincipals(User.class);
        if (principals.isEmpty()) {
            logger.fine("No principal set, returning empty set");
            return Collections.EMPTY_SET;
        }
        try {
            Set principalGroups = new HashSet();
            for (Iterator iterator = principals.iterator(); iterator.hasNext();) {
                User user = (User)iterator.next();
                User dbUser = userManager.findByLogin(user.getLogin());
                if (dbUser != null) {
                    Set groups = groupManager.getGroupHeaders(dbUser);
                    for (Iterator iterator1 = groups.iterator(); iterator1.hasNext();) {
                        EntityHeader group = (EntityHeader)iterator1.next();
                        principalGroups.add(group.getName());
                    }
                }
            }
            return principalGroups;
        } catch (FindException e) {
            throw new RuntimeException("Error accessing user roles", e);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setIdentityProviderConfigManager(IdentityProviderConfigManager identityProviderConfigManager) {
        this.identityProviderConfigManager = identityProviderConfigManager;
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
        groupManager = identityProvider.getGroupManager();
    }
}