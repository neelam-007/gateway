package com.l7tech.console.security;

import com.l7tech.common.Authorizer;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * For SSM-side admin session management.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 5, 2003
 */
public abstract class SecurityProvider extends Authorizer implements AuthenticationProvider {
    protected User user = null;
    private final Set<Permission> subjectPermissions = new HashSet<Permission>();
    private boolean hasNoRoles = false;  // Keep track if a NoRoleException has been thrown or not.

    public synchronized User getUser() {
        return user;
    }

    /**
     * Return the authentication provider associated with this security provider
     *
     * @return the <code>AuthenticationProvider</code>
     */
    public AuthenticationProvider getAuthenticationProvider() {
        return this;
    }

    /**
     * Subclasses reset the credentials using this method.
     */
    protected void resetCredentials() {
        synchronized (SecurityProvider.class) {
            user = null;
            subjectPermissions.clear();
        }
    }

    /**
     * Determine the permissions for the current user
     *
     * @return the set of permission for the current subject
     * @throws RuntimeException on error retrieving user permissions
     */
    public Collection<Permission> getUserPermissions() throws RuntimeException {
        Set<Permission> result;
        synchronized (this) {
            result = Collections.unmodifiableSet(subjectPermissions);
        }
        if (!result.isEmpty()) return result;
        return refreshPermissionCache();
    }

    public Collection<Permission> refreshPermissionCache() {
        RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        if (rbacAdmin == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }

        try {
            final Collection<Permission> perms = rbacAdmin.findCurrentUserPermissions();
            synchronized (this) {
                subjectPermissions.clear();
                subjectPermissions.addAll(perms);
            }
            // The below condition checking prevents SSM from hanging after services/policy fragments deleted.
            if (! perms.isEmpty()) {
                hasNoRoles = false;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        TopComponents.getInstance().firePermissionRefresh();
                    }
                });
            } else if (!hasNoRoles) {
                hasNoRoles = true;  // Set it true to avoid many NoRoleExceptions thrown.
                throw new NoRoleException();
            }
            return perms;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}
