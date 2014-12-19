package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The role dependency processor. This will find entities that are for any auto generated role. The replace
 * dependencies
 * method will properly map the entities referenced as well. It will also map the associated role assignments.
 *
 * Note, we are purposely do not return role assignments as dependencies here. At this time I don't think we should...
 * but I reserve the right to change my mind.
 *
 * @author Victor Kazakov
 */
public class RoleDependencyProcessor extends DefaultDependencyProcessor<Role> {

    @Inject
    private EntityCrud entityCrud;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final Role role, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final List<Dependency> dependencies = super.findDependencies(role, finder);

        //return the roles entity as a dependency of the role.
        if (role.getEntityGoid() != null && role.getEntityType() != null && com.l7tech.search.Dependency.DependencyType.hasDependencyType(role.getEntityType())) {
            DependencyFinder.FindResults roleEntity = DependencyFinder.FindResults.create(entityCrud.find(role.getEntityType().getEntityClass(), role.getEntityGoid()), new EntityHeader(role.getEntityGoid(), role.getEntityType(), null, null));
            dependencies.addAll(finder.getDependenciesFromObjects(role, finder, Arrays.asList(roleEntity)));
        }

        // Note, we are purposely do not return role assignments as dependencies here. At this time I don't think we should... but I reserve the right to change my mind.

        return dependencies;
    }


    @Override
    public void replaceDependencies(@NotNull final Role role, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        super.replaceDependencies(role, replacementMap, finder, replaceAssertionsDependencies);

        //replace the roles entity with one that is mapped
        if (role.getEntityGoid() != null && role.getEntityType() != null && com.l7tech.search.Dependency.DependencyType.hasDependencyType(role.getEntityType())) {
            final EntityHeader roleEntityHeader = new EntityHeader(role.getEntityGoid(), role.getEntityType(), null, null);
            final EntityHeader replacementRoleEntityHeader = replacementMap.get(roleEntityHeader);
            if (replacementRoleEntityHeader != null) {
                role.setEntityGoid(replacementRoleEntityHeader.getGoid());
                role.setEntityType(replacementRoleEntityHeader.getType());
            }
        }

        //map the role assignments.
        for (final RoleAssignment roleAssignment : role.getRoleAssignments()) {
            final IdentityHeader roleAssignmentHeader = new IdentityHeader(roleAssignment.getProviderId(), roleAssignment.getIdentityId(), getAssignmentEntityType(roleAssignment.getEntityType()), null, null, null, 0);
            final IdentityHeader replacementRoleIdentityHeader = (IdentityHeader) replacementMap.get(roleAssignmentHeader);
            if (replacementRoleIdentityHeader != null) {
                roleAssignment.setProviderId(replacementRoleIdentityHeader.getProviderGoid());
                roleAssignment.setIdentityId(replacementRoleIdentityHeader.getStrId());
            }
        }
    }

    /**
     * Returns the entity type from the role assignment entity type
     *
     * @param entityType The role assignment entity type. Either 'User' or 'Group'
     * @return The entity type associated with the given role assignment entity type
     * @throws CannotReplaceDependenciesException
     */
    private EntityType getAssignmentEntityType(String entityType) throws CannotReplaceDependenciesException {
        if (EntityType.USER.getName().equalsIgnoreCase(entityType)) {
            return EntityType.USER;
        } else if (EntityType.GROUP.getName().equalsIgnoreCase(entityType)) {
            return EntityType.GROUP;
        } else {
            throw new CannotReplaceDependenciesException(Role.class, "Unknown Role assignment type '" + entityType + "'. Expected either '" + EntityType.USER.name() + "' or '" + EntityType.GROUP.name() + "'");
        }
    }
}
