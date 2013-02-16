package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Pair;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility superclass for upgrade tasks that need to add new permissions to existing dynamically created roles.
 * <p/>
 * An example would be adding permission to READ a new entity type to every existing Manage Service X and Manage Policy X
 * dynamically-created role.
 * <p/>
 * Subclasses must implement {@link #permissionsToAdd()} to return a List of all-entity operations which shall be
 * added to existing roles -- for example (READ,ENCAPSULATED_ASSERTION).
 */
public abstract class AbstractDynamicRolePermissionsUpgradeTask implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade531To54UpdateRoles.class.getName());
    private ApplicationContext applicationContext;

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final PolicyManager policyManager = getBean("policyManager", PolicyManager.class);
        final ServiceManager serviceManager = getBean("serviceManager", ServiceManager.class);
        final RoleManager roleManager = getBean("roleManager", RoleManager.class);

        try {
            updateRolesForPolicies(policyManager, roleManager);
            updateRolesForServices(serviceManager, roleManager);
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
        List<Pair<OperationType, EntityType>> perms = permissionsToAdd();

        final Collection<PolicyHeader> policies = policyManager.findAllHeaders();

        for ( final PolicyHeader policyHeader : policies ) {
            final long policyOid = policyHeader.getOid();

            PolicyType policyType = policyHeader.getPolicyType();
            if ( shouldIgnorePolicyType( policyType ) )
                continue;

            boolean createRole = false;
            for ( Pair<OperationType, EntityType> perm : perms ) {
                createRole = addPermissionToRole(
                    roleManager,
                    EntityType.POLICY,
                    policyOid,
                    perm.left,
                    perm.right);
                if ( createRole )
                    break;
            }

            if ( createRole && shouldCreateMissingRoles() ) {
                final Policy policy = policyManager.findByPrimaryKey(policyOid);
                if ( policy != null ) {
                    logger.warning( "Missing role for policy '" + policyOid + "', creating new role." );
                    policyManager.createRoles( policy );
                } else {
                    logger.warning( "Missing role for policy '" + policyOid + "', not creating new role (unable to access policy)." );
                }
            }
        }
    }

    /**
     * Update manage service roles.
     */
    private void updateRolesForServices( final ServiceManager serviceManager,
                                         final RoleManager roleManager ) throws ObjectModelException {
        List<Pair<OperationType, EntityType>> perms = permissionsToAdd();

        final Collection<ServiceHeader> services = serviceManager.findAllHeaders(false);

        for ( final ServiceHeader serviceHeader : services ) {
            final long serviceOid = serviceHeader.getOid();

            boolean createRole = false;
            for ( Pair<OperationType, EntityType> perm : perms ) {
                createRole = addPermissionToRole(
                    roleManager,
                    EntityType.SERVICE,
                    serviceOid,
                    perm.left,
                    perm.right);
                if ( createRole )
                    break;
            }

            if ( createRole && shouldCreateMissingRoles() ) {
                final PublishedService service = serviceManager.findByPrimaryKey(serviceOid);
                if ( service != null ) {
                    logger.warning( "Missing role for service '" + serviceOid + "', creating new role." );
                    serviceManager.createRoles( service );
                } else {
                    logger.warning( "Missing role for service '" + serviceOid + "', not creating new role (unable to access service)." );
                }
            }
        }
    }

    protected abstract List<Pair<OperationType, EntityType>> permissionsToAdd();

    protected abstract boolean shouldCreateMissingRoles();

    protected abstract boolean shouldIgnorePolicyType( PolicyType policyType );

    private boolean addPermissionToRole( final RoleManager roleManager,
                                         final EntityType entityType,
                                         final long entityOid,
                                         final OperationType permissionOp,
                                         final EntityType permissionEntity ) throws ObjectModelException {
        boolean createRole = false;

        final Collection<Role> roles = roleManager.findEntitySpecificRoles(entityType, entityOid);

        if ( roles.isEmpty() ) {
            createRole = true;
        } else if ( roles.size() == 1 ) {
            final Role role = roles.iterator().next();
            addEntityPermission( role, permissionOp, permissionEntity );

            roleManager.update( role );
        } else {
            logger.warning( "Not upgrading roles for "+entityType.getName()+" '" + entityOid + "', expected one role but found " +roles.size()+ "." );
        }

        return createRole;
    }

    /**
     * Add entity permission if not present
     */
    private void addEntityPermission( final Role role,
                                      final OperationType operationType,
                                      final EntityType entityType ) {
        boolean hasPermission = false;

        for ( final Permission permission : role.getPermissions() ) {
            if ( permission.getEntityType() == entityType &&
                 permission.getOperation() == operationType &&
                 (permission.getScope() == null || permission.getScope().isEmpty()) ) {
                hasPermission = true;
                break;
            }
        }

        if ( !hasPermission ) {
            role.addEntityPermission( operationType, entityType, null );
        }
    }
}
