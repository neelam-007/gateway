/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.Policy;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class RbacAdminImpl implements RbacAdmin {
    private static final Logger logger = Logger.getLogger(RbacAdminImpl.class.getName());

    private final RoleManager roleManager;
    @Inject
    private SecurityZoneManager securityZoneManager;

    @Inject
    private AssertionAccessManager assertionAccessManager;

    @Inject
    private EntityCrud entityCrud;

    public RbacAdminImpl(RoleManager roleManager) {
        this.roleManager = roleManager;
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
        final Collection<Role> assignedRoles = roleManager.getAssignedRoles(user, true, false);
        for (final Role assignedRole : assignedRoles) {
            attachEntities(assignedRole);
        }
        return assignedRoles;
    }

    private Role attachEntities(Role theRole) {
        for (Permission permission : theRole.getPermissions()) {
            for (ScopePredicate scopePredicate : permission.getScope()) {
                if (scopePredicate instanceof ObjectIdentityPredicate) {
                    ObjectIdentityPredicate oip = (ObjectIdentityPredicate) scopePredicate;
                    final String id = oip.getTargetEntityId();
                    try {
                        oip.setHeader(entityCrud.findHeader(permission.getEntityType(), id));
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Couldn't look up EntityHeader for " + permission.getEntityType().getName() + " id=" + id + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
                else if(scopePredicate instanceof EntityFolderAncestryPredicate){
                    EntityFolderAncestryPredicate predicate = (EntityFolderAncestryPredicate)scopePredicate;
                    try {
                        EntityHeader header = new EntityHeader(predicate.getEntityId(), predicate.getEntityType(), null, null);
                        //custom role we can just look for the folder name
                        if(theRole.isUserCreated()){
                            header.setType(EntityType.FOLDER);
                        }
                        Entity entity = entityCrud.find(header);
                        if(entity != null){
                            Folder folder = null;
                            if(entity instanceof Folder){
                                folder = (Folder)entity;
                            } else if(entity instanceof Policy){
                                folder = ((Policy) entity).getFolder();
                            } else if(entity instanceof PublishedService){
                                folder = ((PublishedService) entity).getFolder();
                            }
                            if(folder != null) predicate.setName(folder.getName());
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Couldn't look up EntityHeader for " + predicate.getEntityType().getName() + " id=" + predicate.getEntityId() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            }
        }

        Long entityOid = theRole.getEntityOid();
        EntityType entityType = theRole.getEntityType();
        if (entityOid != null && entityType != null) {
            try {
                theRole.setCachedSpecificEntity(entityCrud.find(entityType.getEntityClass(), entityOid));
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
        return entityCrud.findAll(entityType.getEntityClass());
    }

    @Override
    public Collection<SecurityZone> findAllSecurityZones() throws FindException {
        return securityZoneManager.findAll();
    }

    @Override
    public SecurityZone findSecurityZoneByPrimaryKey(long oid) throws FindException {
        return securityZoneManager.findByPrimaryKey(oid);
    }

    @Override
    public long saveSecurityZone(SecurityZone securityZone) throws SaveException {
        if (SecurityZone.DEFAULT_OID == securityZone.getOid()) {
            final long oid = securityZoneManager.save(securityZone);
            securityZoneManager.createRoles(securityZone);
            return oid;
        } else {
            long oid = securityZone.getOid();
            try {
                securityZoneManager.update(securityZone);
                securityZoneManager.updateRoles(securityZone);
            } catch (UpdateException e) {
                throw new SaveException(ExceptionUtils.getMessage(e), e);
            }
            return oid;
        }
    }

    @Override
    public void deleteSecurityZone(SecurityZone securityZone) throws DeleteException {
        securityZoneManager.deleteRoles(securityZone.getOid());
        securityZoneManager.delete(securityZone);
    }

    @Override
    public Collection<EntityHeader> findEntitiesByTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException {
        return entityCrud.findByEntityTypeAndSecurityZoneOid(type, securityZoneOid);
    }

    @Override
    public void setSecurityZoneForEntities(final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneOid, entityType, entityIds);
    }

    @Override
    public void setSecurityZoneForEntities(final Long securityZoneOid, @NotNull final Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneOid, entityIds);
    }

    @Override
    public Collection<AssertionAccess> findAccessibleAssertions() throws FindException {
        // Return them all, and allow the RBAC interceptor to filter out any the current admin can't see
        return assertionAccessManager.findAllRegistered();
    }

    @Override
    public long saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException {
        String assname = assertionAccess.getName();
        if (assname == null)
            throw new IllegalArgumentException("AssertionAccess must have an assertion class name");

        try {
            long oid = assertionAccess.getOid();
            if (AssertionAccess.DEFAULT_OID == oid) {
                oid = assertionAccessManager.save(assertionAccess);
                assertionAccess.setOid(oid);
                return oid;
            } else {
                AssertionAccess existing = assertionAccessManager.findByPrimaryKey(oid);
                if (existing != null && !assname.equals(existing.getName()))
                    throw new UpdateException("Unable to change the assertion class name of an existing AssertionAccess");

                assertionAccessManager.update(assertionAccess);
                return oid;
            }
        } catch (FindException | SaveException e) {
            throw new UpdateException("Unable to update assertion access: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
