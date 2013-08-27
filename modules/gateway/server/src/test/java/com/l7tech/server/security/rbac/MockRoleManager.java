package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class MockRoleManager extends EntityManagerStub<Role,EntityHeader> implements RoleManager, RbacServices {
    private GroupManager groupManager;

    public MockRoleManager(EntityFinder entityFinder) {
        rbacServices = new RbacServicesImpl(this, entityFinder);
    }

    private final RbacServices rbacServices;

    @Override
    public Collection<Role> getAssignedRoles(User user, boolean ignore1, boolean ignore2) throws FindException {
        return getAssignedRoles(user);
    }

    @Override
    public Collection<Pair<Goid, String>> getExplicitRoleAssignments() throws FindException {
        return null;
    }

    @Override
    public Collection<Role> getAssignedRoles(User thisGuy) throws FindException {
        final Set<Role> assignedRoles = new HashSet<Role>();
        Map<Goid, IdentityHeader> thisGuysGroupMap = null;

        for (Role role : entities.values()) {
            for (RoleAssignment assignment : role.getRoleAssignments()) {
                final EntityType type = EntityType.valueOf(assignment.getEntityType().toUpperCase());
                switch(type) {
                    case USER:
                        if (assignment.getProviderId().equals(thisGuy.getProviderId()) &&
                            assignment.getIdentityId().equals(thisGuy.getId()))
                        {
                            assignedRoles.add(role);
                        }
                        break;
                    case GROUP:
                        if (groupManager == null) break;
                        if (thisGuysGroupMap == null) thisGuysGroupMap = getThisGuysGroups(thisGuy);
                        IdentityHeader groupHeader = thisGuysGroupMap.get(Goid.parseGoid(assignment.getIdentityId()));
                        if (groupHeader != null &&
                            groupHeader.getProviderGoid().equals(thisGuy.getProviderId())) {
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

    @Override
    public Collection<Role> getAssignedRoles(@NotNull Group group) throws FindException {
        return null;
    }
    
    private Map<Goid, IdentityHeader> getThisGuysGroups(User thisGuy) throws FindException {
        if (groupManager == null) return Collections.emptyMap();
        final Map<Goid, IdentityHeader> thisGuysGroupMap = new HashMap<Goid, IdentityHeader>();
        final Set<IdentityHeader> groups = groupManager.getGroupHeaders(thisGuy);
        for (IdentityHeader header : groups)
            thisGuysGroupMap.put(header.getGoid(), header);
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
    public Collection<Role> findEntitySpecificRoles(EntityType etype, Goid entityGoid) throws FindException {
        for (Role role : entities.values()) {
            if (role.getEntityType() == etype && role.getEntityGoid().equals(entityGoid)) return Collections.singletonList(role);
        }
        return null;
    }

    @Override
    public void deleteEntitySpecificRoles(EntityType etype, Goid entityGoid) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEntitySpecificPermissions(@NotNull EntityType etype, @NotNull String entityId) throws DeleteException {
    }

    @Override
    public void renameEntitySpecificRoles(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
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
                        ass.getProviderId().equals(user.getProviderId()) &&
                        ass.getIdentityId().equals(user.getId()))
                    it.remove();
            }
        }
    }

    @Override
    public void deleteRoleAssignmentsForGroup(Group group) throws DeleteException {
        for (Role role : entities.values()) {
            for (Iterator<RoleAssignment> it = role.getRoleAssignments().iterator(); it.hasNext();) {
                final RoleAssignment ass = it.next();
                if (EntityType.GROUP.name().equals(ass.getEntityType()) &&
                        ass.getProviderId().equals(group.getProviderId()) &&
                        ass.getIdentityId().equals(group.getId()))
                    it.remove();
            }
        }
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public Class<? extends PersistentEntityImp> getImpClass() {
        return Role.class;
    }

    // DELEGATED!

    @Override
    public boolean isPermittedForEntitiesOfTypes(User authenticatedUser, OperationType requiredOperation, Set<EntityType> requiredTypes)
        throws FindException {
        return rbacServices.isPermittedForEntitiesOfTypes(authenticatedUser, requiredOperation, requiredTypes);
    }

    @Override
    public boolean isPermittedForAnyEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType)
        throws FindException {
        return rbacServices.isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, requiredType);
    }

    @Override
    public boolean isPermittedForSomeEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType)
        throws FindException {
        return rbacServices.isPermittedForSomeEntityOfType(authenticatedUser, requiredOperation, requiredType);
    }

    @Override
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName)
        throws FindException {
        return rbacServices.isPermittedForEntity(user, entity, operation, otherOperationName);
    }

    @Override
    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser, OperationType requiredOperation, Iterable<T> headers, EntityFinder entityFinder)
        throws FindException {
        return rbacServices.filterPermittedHeaders(authenticatedUser, requiredOperation, headers, entityFinder);
    }

    @Override
    public boolean isAdministrativeUser(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return rbacServices.isAdministrativeUser(providerAndUserId, user);
    }

    @Override
    public Collection<Role> getAssignedRoles(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return getAssignedRoles(user, false, true);
    }
}
