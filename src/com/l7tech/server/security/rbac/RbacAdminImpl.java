/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author alex
 */
public class RbacAdminImpl implements RbacAdmin {
    private final RoleManager roleManager;
    private final LicenseManager licenseManager;

    public RbacAdminImpl(RoleManager roleManager, LicenseManager licenseManager) {
        this.roleManager = roleManager;
        this.licenseManager = licenseManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public Collection<Role> findAllRoles() throws FindException, RemoteException {
        checkLicense();
        return roleManager.findAll();
    }

    public Role findRoleByPrimaryKey(long oid) throws FindException, RemoteException {
        checkLicense();
        return roleManager.findByPrimaryKey(oid);
    }

    public Collection<Role> findRolesForUser(User user, boolean includeGroups) throws FindException, RemoteException {
        checkLicense();
        return roleManager.getAssignedRoles(user);
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

    public Collection<User> findAssignedUsers(Role role) throws FindException, RemoteException {
        checkLicense();
        return roleManager.getAssignedUsers(role);
    }
}
