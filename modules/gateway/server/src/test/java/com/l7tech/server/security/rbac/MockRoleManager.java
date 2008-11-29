package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityManagerStub;

import java.util.*;
import java.util.regex.Pattern;

public class MockRoleManager extends EntityManagerStub<Role,EntityHeader> implements RoleManager, RbacServices {
    private GroupManager groupManager;

    private final RbacServices rbacServices = new RbacServicesImpl(this);

    @Override
    public Collection<Role> getAssignedRoles(User thisGuy) throws FindException {
        final Set<Role> assignedRoles = new HashSet<Role>();
        Map<Long, IdentityHeader> thisGuysGroupMap = null;

        for (Role role : entities.values()) {
            for (RoleAssignment assignment : role.getRoleAssignments()) {
                final EntityType type = EntityType.valueOf(assignment.getEntityType().toUpperCase());
                switch(type) {
                    case USER:
                        if (assignment.getProviderId() == thisGuy.getProviderId() &&
                            assignment.getIdentityId().equals(thisGuy.getId()))
                        {
                            assignedRoles.add(role);
                        }
                        break;
                    case GROUP:
                        if (groupManager == null) break;
                        if (thisGuysGroupMap == null) thisGuysGroupMap = getThisGuysGroups(thisGuy);
                        IdentityHeader groupHeader = thisGuysGroupMap.get(Long.valueOf(assignment.getIdentityId()));
                        if (groupHeader != null &&
                            groupHeader.getProviderOid() == thisGuy.getProviderId()) {
                            assignedRoles.add(role);
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
        return assignedRoles;
    }

    private Map<Long, IdentityHeader> getThisGuysGroups(User thisGuy) throws FindException {
        if (groupManager == null) return Collections.emptyMap();
        final Map<Long, IdentityHeader> thisGuysGroupMap = new HashMap<Long, IdentityHeader>();
        final Set<IdentityHeader> groups = groupManager.getGroupHeaders(thisGuy);
        for (IdentityHeader header : groups)
            thisGuysGroupMap.put(header.getOid(), header);
        return thisGuysGroupMap;
    }

    @Override
    public Role findByTag(Role.Tag tag) throws FindException {
        for (Role role : entities.values()) {
            if (role.getTag() == tag) return role;
        }
        return null;
    }

    @Override
    public Role findEntitySpecificRole(EntityType etype, long entityOid) throws FindException {
        for (Role role : entities.values()) {
            if (role.getEntityType() == etype && role.getEntityOid() == entityOid) return role;
        }
        return null;
    }

    @Override
    public void deleteEntitySpecificRole(EntityType etype, long entityOid) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameEntitySpecificRole(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateRoleAssignments() throws UpdateException {
    }

    @Override
    public void deleteRoleAssignmentsForUser(User user) throws DeleteException {
        for (Role role : entities.values()) {
            for (Iterator<RoleAssignment> it = role.getRoleAssignments().iterator(); it.hasNext();) {
                final RoleAssignment ass = it.next();
                if (EntityType.USER.name().equals(ass.getEntityType()) &&
                        ass.getProviderId() == user.getProviderId() &&
                        ass.getIdentityId().equals(user.getId()))
                    it.remove();
            }
        }
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return Role.class;
    }

    // DELEGATED!

    public boolean isPermittedForEntitiesOfTypes(User authenticatedUser, OperationType requiredOperation, Set<EntityType> requiredTypes)
        throws FindException {
        return rbacServices.isPermittedForEntitiesOfTypes(authenticatedUser, requiredOperation, requiredTypes);
    }

    public boolean isPermittedForAnyEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType)
        throws FindException {
        return rbacServices.isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, requiredType);
    }

    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName)
        throws FindException {
        return rbacServices.isPermittedForEntity(user, entity, operation, otherOperationName);
    }

    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser, OperationType requiredOperation, Iterable<T> headers, EntityFinder entityFinder)
        throws FindException {
        return rbacServices.filterPermittedHeaders(authenticatedUser, requiredOperation, headers, entityFinder);
    }
}
