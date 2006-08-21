/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.GatewayFeatureSets;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class RbacAdminImpl implements RbacAdmin {
    private static final Logger logger = Logger.getLogger(RbacAdminImpl.class.getName());

    private final RoleManager roleManager;
    private final LicenseManager licenseManager;
    private final EntityFinder entityFinder;

    public RbacAdminImpl(RoleManager roleManager, LicenseManager licenseManager, EntityFinder entityFinder) {
        this.roleManager = roleManager;
        this.licenseManager = licenseManager;
        this.entityFinder = entityFinder;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    public Collection<Role> findAllRoles() throws FindException, RemoteException {
        checkLicense();
        final Collection<Role> roles = roleManager.findAll();
        for (Role role : roles) {
            attachHeaders(role);
        }
        return roles;
    }

    public Role findRoleByPrimaryKey(long oid) throws FindException, RemoteException {
        checkLicense();
        return attachHeaders(roleManager.findByPrimaryKey(oid));
    }


    public Collection<Permission> findCurrentUserPermissions() throws FindException, RemoteException {
        User u = JaasUtils.getCurrentUser();
        if (u == null) throw new FindException("Couldn't get current user");
        Set<Permission> perms = new HashSet<Permission>();
        // No license check--needed for SSM login
        final Collection<Role> assignedRoles = roleManager.getAssignedRoles(u);
        for (Role role : assignedRoles) {
            for (final Permission perm : role.getPermissions()) {
                Permission perm2 = perm.getAnonymousClone();
                perms.add(perm2);
            }
        }
        return perms;
    }

    private Role attachHeaders(Role theRole) {
        for (Permission permission : theRole.getPermissions()) {
            for (ScopePredicate scopePredicate : permission.getScope()) {
                if (scopePredicate instanceof ObjectIdentityPredicate) {
                    ObjectIdentityPredicate oip = (ObjectIdentityPredicate) scopePredicate;
                    final String id = oip.getTargetEntityId();
                    try {
                        oip.setHeader(entityFinder.findHeader(permission.getEntityType(), id));
                    } catch (FindException e) {
                        logger.severe("Couldn't look up EntityHeader for " +
                                e.getClass().getSimpleName() +
                                " #" + id);
                    }
                }
            }
        }
        return theRole;
    }

    public long saveRole(Role role) throws SaveException, RemoteException {
        checkLicense();
        if (role.getOid() == Role.DEFAULT_OID) {
            return roleManager.save(role);
        } else {
            long oid = role.getOid();
            try {
                roleManager.update(role);
            } catch (UpdateException e) {
                throw new SaveException(e);
            }
            return oid;
        }
    }

    public void deleteRole(Role role) throws DeleteException, RemoteException {
        checkLicense();
        roleManager.delete(role);
    }

    public void addUserToRole(User user, Role role) throws UpdateException, RemoteException {
        checkLicense();
        roleManager.assignUser(role, user);
    }

    public void removeUserFromRole(User user, Role role) throws UpdateException, RemoteException {
        checkLicense();
        roleManager.deleteAssignment(user, role);
    }

    public EntityHeader[] findEntities(Class<? extends Entity> entityClass) throws FindException, RemoteException {
        checkLicense();
        return entityFinder.findAll(entityClass);
    }
}
