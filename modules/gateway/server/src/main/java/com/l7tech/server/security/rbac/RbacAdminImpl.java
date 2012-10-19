/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * @author alex
 */
public class RbacAdminImpl implements RbacAdmin {
    private static final Logger logger = Logger.getLogger(RbacAdminImpl.class.getName());

    private final RoleManager roleManager;
    private final EntityFinder entityFinder;

    public RbacAdminImpl(RoleManager roleManager, EntityFinder entityFinder) {
        this.roleManager = roleManager;
        this.entityFinder = entityFinder;
    }

    public Collection<Role> findAllRoles() throws FindException {
        final Collection<Role> roles = roleManager.findAll();
        for (Role role : roles) {
            attachEntities(role);
        }
        return roles;
    }

    public Role findRoleByPrimaryKey(long oid) throws FindException {
        return attachEntities(roleManager.findByPrimaryKey(oid));
    }


    public Collection<Permission> findCurrentUserPermissions() throws FindException {
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

    /**
     * Note: this method <em>intentionally</em> avoids validating user accounts (e.g. whether they're expired or disabled). 
     * Don't ever use it for authorization!
     */
    public Collection<Role> findRolesForUser(User user) throws FindException {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        return roleManager.getAssignedRoles(user, true, false);
    }

    private Role attachEntities(Role theRole) {
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

        Long entityOid = theRole.getEntityOid();
        EntityType entityType = theRole.getEntityType();
        if (entityOid != null && entityType != null) {
            try {
                theRole.setCachedSpecificEntity(entityFinder.find(entityType.getEntityClass(), entityOid));
            } catch (FindException e) {
                logger.log( Level.WARNING, MessageFormat.format( "Couldn''t find {0} (# {1}) to attach to ''{2}'' Role", entityType.name(), entityOid, theRole.getName() ), ExceptionUtils.getDebugException( e ) );
            }
        }
        return theRole;
    }

    public long saveRole(Role role) throws SaveException {
        if (role.getOid() == Role.DEFAULT_OID) {
            return roleManager.save(role);
        } else {
            long oid = role.getOid();
            try {
                roleManager.update(role);
            } catch (UpdateException e) {
                throw new SaveException(e.getMessage(), e);
            }
            return oid;
        }
    }

    public void deleteRole(Role role) throws DeleteException {
        roleManager.delete(role);
    }

    public EntityHeaderSet<EntityHeader> findEntities(EntityType entityType) throws FindException {
        return entityFinder.findAll(entityType.getEntityClass());
    }
}
