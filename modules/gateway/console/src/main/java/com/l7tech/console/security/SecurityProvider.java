package com.l7tech.console.security;

import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.security.rbac.EntityProtectionInfo;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final Set<Permission> subjectPermissions = new HashSet<>();
    private final AtomicReference<Map<String, EntityProtectionInfo>> protectedEntityMap = new AtomicReference<>();
    private static final Logger logger = Logger.getLogger( SecurityProvider.class.getName() );
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
            final Map<String, EntityProtectionInfo> protectionInfoMap = protectedEntityMap.getAndSet(null);
            if (protectionInfoMap != null) {
                protectionInfoMap.clear();
            }
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
            // return a copy as read-only, as HashSet Iterator can still throw ConcurrentModificationException if the set is modified afterwards
            // todo: rollback if returning a copy has significant performance issue (with user having many permissions)
            result = Collections.unmodifiableSet(new HashSet<>(subjectPermissions));
        }
        if (!result.isEmpty()) return result;
        refreshPermissionCache();
        synchronized (this) {
            // return a copy as read-only, as HashSet Iterator can still throw ConcurrentModificationException if the set is modified afterwards
            // todo: rollback if returning a copy has significant performance issue (with user having many permissions)
            result = Collections.unmodifiableSet(new HashSet<>(subjectPermissions));
        }
        return result;
    }

    @Override
    public Map<String, EntityProtectionInfo> getProtectedEntities() throws RuntimeException {
        final Map<String, EntityProtectionInfo> result = protectedEntityMap.get();
        if (result == null) {
            refreshProtectedEntitiesCache();
        }
        return Collections.unmodifiableMap(protectedEntityMap.get());
    }

    /**
     * Refreshes only protected entities cache.
     */
    public void refreshProtectedEntitiesCache() {
        final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        if (rbacAdmin == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }
        try {
            final Map<String, EntityProtectionInfo> entitiesProtectionInfo = rbacAdmin.findProtectedEntities();
            final Map<String, EntityProtectionInfo> protectionInfoMap = protectedEntityMap.getAndSet(new ConcurrentHashMap<>(entitiesProtectionInfo));
            if (protectionInfoMap != null) {
                protectionInfoMap.clear();
            }
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Refreshes user permissions and protected entities cache.
     */
    public void refreshPermissionCache() {
        final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        if (rbacAdmin == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }

        try {
            final Pair<Collection<Permission>, Map<String, EntityProtectionInfo>> permissionsAndEntitiesProtectionInfo = rbacAdmin.findCurrentUserPermissionsAndProtectedEntities();
            final Collection<Permission> perms = permissionsAndEntitiesProtectionInfo.left;
            assert perms != null;
            final Map<String, EntityProtectionInfo> entitiesProtectionInfo = permissionsAndEntitiesProtectionInfo.right;
            assert entitiesProtectionInfo != null;
            synchronized (this) {
                logger.log( Level.INFO, "Loaded {0} permissions.", perms.size() );
                logger.log( Level.INFO, "Loaded {0} EntitiesProtectionInfo.", entitiesProtectionInfo.size() );
                this.subjectPermissions.clear();
                this.subjectPermissions.addAll(perms);
                final Map<String, EntityProtectionInfo> protectionInfoMap = protectedEntityMap.getAndSet(new ConcurrentHashMap<>(entitiesProtectionInfo));
                if (protectionInfoMap != null) {
                    protectionInfoMap.clear();
                }
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
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}
