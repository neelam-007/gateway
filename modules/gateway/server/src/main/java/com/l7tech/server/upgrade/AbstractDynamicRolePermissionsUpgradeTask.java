package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility superclass for upgrade tasks that need to add new permissions to existing dynamically created roles.
 * <p/>
 * An example would be adding permission to READ a new entity type to every existing Manage Service X and Manage Policy X
 * dynamically-created role.
 * <p/>
 * Subclasses must implement {@link #permissionsToAdd()} to return a List of all-entity operations which shall be
 * added to existing roles for the given entity -- for example (READ,ENCAPSULATED_ASSERTION).
 */
public abstract class AbstractDynamicRolePermissionsUpgradeTask implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(AbstractDynamicRolePermissionsUpgradeTask.class.getName());
    private ApplicationContext applicationContext;

    protected ServiceManager serviceManager;

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final PolicyManager policyManager = getBean("policyManager", PolicyManager.class);
        this.serviceManager = getBean("serviceManager", ServiceManager.class);
        final RoleManager roleManager = getBean("roleManager", RoleManager.class);
        final FolderManager folderManager = getBean("folderManager", FolderManager.class);

        try {
            if (getEntityTypesToUpgrade().contains(EntityType.POLICY)) {
                updateRolesForPolicies(policyManager, roleManager);
            }
            if (getEntityTypesToUpgrade().contains(EntityType.SERVICE)) {
                updateRolesForServices(serviceManager, roleManager);
            }
            if (getEntityTypesToUpgrade().contains(EntityType.FOLDER)) {
                updateRolesForFolders(folderManager, roleManager);
            }
        } catch ( ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({ "unchecked" })
    private <T> T getBean( final String name,
                           final Class<T> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, beanClass);
        } catch ( BeansException be ) {
            throw new FatalUpgradeException("Error accessing  bean '"+name+"' from ApplicationContext.");
        }
    }

    /**
     * Update manage policy roles.
     */
    private void updateRolesForPolicies( final PolicyManager policyManager,
                                         final RoleManager roleManager ) throws ObjectModelException {
        List<Triple<OperationType, String, EntityType>> perms = permissionsToAdd();

        final Collection<PolicyHeader> policies = policyManager.findAllHeaders();

        for ( final PolicyHeader policyHeader : policies ) {
            final Goid policyGoid = policyHeader.getGoid();

            PolicyType policyType = policyHeader.getPolicyType();
            if ( shouldIgnorePolicyType( policyType ) )
                continue;

            boolean createRole = false;
            for ( Triple<OperationType, String, EntityType> perm : perms ) {
                createRole = addPermissionToRole(
                    roleManager,
                    EntityType.POLICY,
                    policyGoid,
                    perm.left,
                    perm.middle,
                    perm.right);
                if ( createRole )
                    break;
            }

            if ( createRole && shouldCreateMissingRoles() ) {
                final Policy policy = policyManager.findByPrimaryKey(policyGoid);
                if ( policy != null ) {
                    logger.warning( "Missing role for policy '" + policyGoid + "', creating new role." );
                    policyManager.createRoles( policy );
                } else {
                    logger.warning( "Missing role for policy '" + policyGoid + "', not creating new role (unable to access policy)." );
                }
            }
        }
    }

    /**
     * Update manage service roles.
     */
    private void updateRolesForServices( final ServiceManager serviceManager,
                                         final RoleManager roleManager ) throws ObjectModelException {
        List<Triple<OperationType, String, EntityType>> perms = permissionsToAdd();

        final Collection<ServiceHeader> services = serviceManager.findAllHeaders(false);

        for ( final ServiceHeader serviceHeader : services ) {
            final Goid serviceGoid = serviceHeader.getGoid();

            boolean createRole = false;
            for ( Triple<OperationType, String, EntityType> perm : perms ) {
                createRole = addPermissionToRole(
                    roleManager,
                    EntityType.SERVICE,
                    serviceGoid,
                    perm.left,
                    perm.middle,
                    perm.right);
                if ( createRole )
                    break;
            }

            if ( createRole && shouldCreateMissingRoles() ) {
                final PublishedService service = serviceManager.findByPrimaryKey(serviceGoid);
                if ( service != null ) {
                    logger.warning( "Missing role for service '" + serviceGoid + "', creating new role." );
                    serviceManager.createRoles( service );
                } else {
                    logger.warning( "Missing role for service '" + serviceGoid + "', not creating new role (unable to access service)." );
                }
            }
        }
    }

    private void updateRolesForFolders( final FolderManager folderManager,
                                         final RoleManager roleManager ) throws ObjectModelException {
        List<Triple<OperationType, String, EntityType>> perms = permissionsToAdd();
        final Collection<FolderHeader> folders = folderManager.findAllHeaders();

        for ( final FolderHeader folder : folders ) {
            final Goid folderGoid = folder.getGoid();
            boolean createRole = false;
            for ( Triple<OperationType, String, EntityType> perm : perms ) {
                // expect both Manage X Folder and View X Folder roles
                createRole = addPermissionToRoles(
                        roleManager,
                        EntityType.FOLDER,
                        folderGoid,
                        perm.left,
                        perm.middle,
                        perm.right,
                        2);
                if ( createRole )
                    break;
            }
            if ( createRole && shouldCreateMissingRoles() ) {
                final Folder f = folderManager.findByPrimaryKey(folderGoid);
                if ( f != null ) {
                    logger.warning( "Missing role for folder '" + folderGoid + "', creating new role." );
                    folderManager.createRoles( f );
                } else {
                    logger.warning( "Missing role for folder '" + folderGoid + "', not creating new role (unable to access folder)." );
                }
            }
        }
    }

    /**
     * Returns a list of permissions to add.
     * The list contains {@link Triple} which consists of,
     *  - the operation type.
     *  - the OTHER operation name. Set if operation type is OTHER, otherwise set to null.
     *  - the entity type.
     *
     * @return a list of permissions to add
     */
    protected abstract List<Triple<OperationType, String, EntityType>> permissionsToAdd();

    protected abstract boolean shouldCreateMissingRoles();

    protected abstract boolean shouldIgnorePolicyType( PolicyType policyType );

    /**
     * @return a collection of EntityTypes for which roles will should be upgraded.
     */
    @NotNull
    protected abstract Collection<EntityType> getEntityTypesToUpgrade();

    private boolean addPermissionToRole( final RoleManager roleManager,
                                         final EntityType entityType,
                                         final Goid entityGoid,
                                         final OperationType permissionOp,
                                         final String otherOperationName,
                                         final EntityType permissionEntity ) throws ObjectModelException {
        return addPermissionToRoles(roleManager, entityType, entityGoid, permissionOp, otherOperationName, permissionEntity, 1);
    }

    /**
     * Returns the GOID of the entity the permission is scoped to. Returns null if no scope.
     *
     * @param entityType if this Role is scoped to a particular entity, the type of that entity. Otherwise null.
     * @param entityGoid if this Role is scoped to a particular entity, the GOID of that entity. Otherwise null.*
     * @return the GOID of the entity the permission should be scoped to. Null if no scope.
     * @throws ObjectModelException ObjectModelException
     */
    protected String getScopeId (EntityType entityType, Goid entityGoid) throws ObjectModelException {
        return null;
    }

    private boolean addPermissionToRoles(final RoleManager roleManager,
                                         final EntityType entityType,
                                         final Goid entityGoid,
                                         final OperationType permissionOp,
                                         final String otherOperationName,
                                         final EntityType permissionEntity,
                                         final int numExpectedRoles) throws ObjectModelException {
    boolean createRole = false;

    final Collection<Role> roles = roleManager.findEntitySpecificRoles(entityType, entityGoid);

    if ( roles.isEmpty() ) {
        createRole = true;
    } else if ( roles.size() == numExpectedRoles ) {
        for (final Role role : roles) {
            if (addEntityPermission( role, permissionOp, otherOperationName, permissionEntity )) {
                roleManager.update( role );
            }
        }
    } else {
        logger.warning( "Not upgrading roles for "+entityType.getName()+" '" + entityGoid + "', expected " + numExpectedRoles + " role(s) but found " +roles.size()+ "." );
    }

    return createRole;
}

    /**
     * Add entity permission if not present
     */
    private boolean addEntityPermission( final Role role,
                                         final OperationType operationType,
                                         final String otherOperationName,
                                         final EntityType entityType) throws ObjectModelException {
        boolean hasPermission = false;

        String id = getScopeId(role.getEntityType(), role.getEntityGoid());

        for ( final Permission permission : role.getPermissions() ) {
            if ( permission.getEntityType() == entityType &&
                 permission.getOperation() == operationType &&
                 isScopeEqual(permission.getScope(), id) &&
                 isOtherOperationNameEqual(permission.getOtherOperationName(), otherOperationName) ) {
                hasPermission = true;
                break;
            }
        }

        if ( !hasPermission ) {
            if (operationType.equals(OperationType.OTHER)) {
                role.addEntityOtherPermission(entityType, id, otherOperationName);
            } else {
                role.addEntityPermission(operationType, entityType, id);
            }
        }

        return !hasPermission;
    }

    private boolean isScopeEqual(Set<ScopePredicate> scope, String id) {
        // Return true is the scope Set has exactly 1 scope set,
        // which is an ObjectIdentityPredicate for the provided id.
        //
        boolean result;

        if (id == null || id.isEmpty()) {
            result = scope == null || scope.isEmpty();
        } else {
            if (scope.size() != 1) {
                result = false;
            } else {
                ScopePredicate theScope = scope.iterator().next();
                if (theScope instanceof ObjectIdentityPredicate) {
                    result = ((ObjectIdentityPredicate) theScope).getTargetEntityId().equals(id);
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    private boolean isOtherOperationNameEqual(String s1, String s2) {
        // Compare other operation name. Treat empty string and null as equal.
        //
        boolean result;

        if (s1 != null) {
            if (!s1.isEmpty()) {
                result = s1.equals(s2);
            } else {
                result = (s2 == null || s2.isEmpty());
            }
        } else {
            result = (s2 == null || s2.isEmpty());
        }

        return result;
    }
}
