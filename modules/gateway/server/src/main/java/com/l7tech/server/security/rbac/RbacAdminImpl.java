/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.identity.HasDefaultRole;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;
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

    @Inject
    private IdentityProviderFactory identityProviderFactory;

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

    public Role findRoleByPrimaryKey(Goid goid) throws FindException {
        return attachEntities(roleManager.findByPrimaryKey(goid));
    }

    @Override
    public Collection<EntityHeader> findAllRoleHeaders() throws FindException {
        final Collection<EntityHeader> allHeaders = roleManager.findAllHeaders();
        return allHeaders;
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

        // TODO move this hack into the roleManager
        if (perms.isEmpty()) {
            // Check for an IDP that declares a default role
            Role role = findDefaultRole(u.getProviderId());
            for (Permission perm : role.getPermissions()) {
                Permission perm2 = perm.getAnonymousClone();
                perms.add(perm2);
            }
        }

        return perms;
    }

    private Role findDefaultRole(Goid providerId) throws FindException {
        // TODO move this hack into the roleManager
        IdentityProvider prov = identityProviderFactory.getProvider(providerId);
        if (prov instanceof HasDefaultRole) {
            HasDefaultRole hasDefaultRole = (HasDefaultRole) prov;
            Goid roleId = hasDefaultRole.getDefaultRoleId();
            if (roleId != null)
                return roleManager.findByPrimaryKey(roleId);
        }
        return null;
    }

    /**
     * Note: this method <em>intentionally</em> avoids validating user accounts (e.g. whether they're expired or disabled). 
     * Don't ever use it for authorization!
     */
    public Collection<Role> findRolesForUser(User user) throws FindException {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        List<Role> assignedRoles = new ArrayList<>();
        assignedRoles.addAll(roleManager.getAssignedRoles(user, true, false));

        for (final Role assignedRole : assignedRoles) {
            attachEntities(assignedRole);
        }
        return assignedRoles;
    }

    @Override
    public Role findDefaultRoleForIdentityProvider(Goid identityProviderId) throws FindException {
        return findDefaultRole(identityProviderId);
    }

    /**
     * Does not validate the group - do not use for authorization.
     */
    @Override
    public Collection<Role> findRolesForGroup(@NotNull final Group group) throws FindException {
        final Collection<Role> assignedRoles = roleManager.getAssignedRoles(group);

        // No support for "default role" hack for groups currently

        for (final Role assignedRole : assignedRoles) {
            attachEntities(assignedRole);
        }

        return assignedRoles;
    }

    private Role attachEntities(Role theRole) {
        Goid entityGoid = theRole.getEntityGoid();
        EntityType entityType = theRole.getEntityType();
        if (entityType != null && PersistentEntity.class.isAssignableFrom(entityType.getEntityClass())) {
            if(entityGoid != null) {
                try {
                    theRole.setCachedSpecificEntity(entityCrud.find(entityType.getEntityClass(), entityGoid));
                } catch (FindException e) {
                    logger.log(Level.WARNING, MessageFormat.format("Couldn't find {0} (# {1}) to attach to ''{2}'' Role", entityType.name(), entityGoid, theRole.getName()), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return theRole;
    }

    public Goid saveRole(Role role) throws SaveException {
        if (role.isUnsaved()) {
            return roleManager.save(role);
        } else {
            Goid goid = role.getGoid();
            try {
                roleManager.update(role);
            } catch (UpdateException e) {
                throw new SaveException(e.getMessage(), e);
            }
            return goid;
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
    public SecurityZone findSecurityZoneByPrimaryKey(Goid goid) throws FindException {
        return securityZoneManager.findByPrimaryKey(goid);
    }

    @Override
    public Goid saveSecurityZone(SecurityZone securityZone) throws SaveException {
        if (SecurityZone.DEFAULT_GOID.equals(securityZone.getGoid())) {
            final Goid goid = securityZoneManager.save(securityZone);
            securityZoneManager.createRoles(securityZone);
            return goid;
        } else {
            Goid goid = securityZone.getGoid();
            try {
                securityZoneManager.update(securityZone);
                securityZoneManager.updateRoles(securityZone);
            } catch (UpdateException e) {
                throw new SaveException(ExceptionUtils.getMessage(e), e);
            }
            return goid;
        }
    }

    @Override
    public void deleteSecurityZone(SecurityZone securityZone) throws DeleteException {
        securityZoneManager.delete(securityZone);
    }

    @Override
    public Collection<EntityHeader> findEntitiesByTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException {
        return entityCrud.findByEntityTypeAndSecurityZoneGoid(type, securityZoneGoid);
    }

    @Override
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneGoid, entityType, entityIds);
    }

    @Override
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull final Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneGoid, entityIds);
    }

    @Override
    public Collection<AssertionAccess> findAccessibleAssertions() throws FindException {
        // Return them all, and allow the RBAC interceptor to filter out any the current admin can't see
        return assertionAccessManager.findAllRegistered();
    }

    @Override
    public Goid saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException {
        String assname = assertionAccess.getName();
        if (assname == null)
            throw new IllegalArgumentException("AssertionAccess must have an assertion class name");

        try {
            Goid oid = assertionAccess.getGoid();
            if (assertionAccess.isUnsaved()) {
                oid = assertionAccessManager.save(assertionAccess);
                assertionAccess.setGoid(oid);
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

    @Override
    public EntityHeader findHeader(EntityType entityType, Serializable pk) throws FindException {
        return entityCrud.findHeader(entityType, pk);
    }

    @Override
    public Entity find(@NotNull EntityHeader header) throws FindException {
        return entityCrud.find(header);
    }
}
