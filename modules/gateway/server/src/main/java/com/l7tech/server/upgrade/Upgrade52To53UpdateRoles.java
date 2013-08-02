package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.JDBC_CONNECTION;

/**
 * Upgrade task to update roles for 5.3.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class Upgrade52To53UpdateRoles implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade51To52UpdateRoles.class.getName());
    private ApplicationContext applicationContext;

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final ServiceManager serviceManager = getBean("serviceManager", ServiceManager.class);
        final PolicyManager policyManager = getBean("policyManager", PolicyManager.class);
        final RoleManager roleManager = getBean("roleManager", RoleManager.class);

        try {
            updateRolesForServices(serviceManager, roleManager);
            updateRolesForPolicies(policyManager, roleManager);
        } catch (ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }


    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    private <T> T getBean( final String name,
                           final Class<T> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            //noinspection unchecked
            return applicationContext.getBean(name, beanClass);
        } catch ( BeansException be ) {
            throw new FatalUpgradeException("Error accessing  bean '"+name+"' from ApplicationContext.");
        }
    }

    /**
     * Update manage service roles to include READ permission on JDBC_CONNECTION.
     */
    private void updateRolesForServices(final ServiceManager serviceManager,
                                        final RoleManager roleManager) throws FindException, SaveException, UpdateException {
        final Collection<ServiceHeader> services = serviceManager.findAllHeaders(false);

        for (ServiceHeader serviceHeader : services) {
            final Goid serviceGoid = serviceHeader.getGoid();
            final Collection<Role> roles = roleManager.findEntitySpecificRoles( EntityType.SERVICE, serviceGoid );

            if (roles.isEmpty()) {
                PublishedService service = serviceManager.findByPrimaryKey(serviceGoid);
                if (service != null) {
                    logger.warning("Missing role for service '" + serviceGoid + "', creating new role.");
                    serviceManager.createRoles(service);
                } else {
                    logger.warning("Missing role for service '" + serviceGoid + "', not creating new role (error finding service).");
                }
            } else if (roles.size() == 1) {
                final Role role = roles.iterator().next();

                addEntityPermission(role, READ, JDBC_CONNECTION);
                roleManager.update(role);
            } else {
                logger.warning("Not upgrading roles for service '" + serviceGoid + "', expected one role but found " +roles.size()+ ".");
            }
        }
    }

    /**
     * Update manage policy roles to include READ permission on JDBC_CONNECTION.
     */
    private void updateRolesForPolicies(final PolicyManager policyManager,
                                        final RoleManager roleManager) throws FindException, SaveException, UpdateException {
        final Collection<PolicyHeader> policies = policyManager.findHeadersByType(PolicyType.INCLUDE_FRAGMENT);

        for (PolicyHeader policyHeader : policies) {
            final Goid policyGoid = policyHeader.getGoid();
            final Collection<Role> roles = roleManager.findEntitySpecificRoles(EntityType.POLICY, policyGoid);

            if (roles.isEmpty()) {
                Policy policy = policyManager.findByPrimaryKey(policyGoid);
                if (policy != null) {
                    logger.warning("Missing role for policy '" + policyGoid + "', creating new role.");
                    policyManager.createRoles(policy);
                } else {
                    logger.warning("Missing role for policy '" + policyGoid + "', not creating new role (error finding policy).");
                }
            } else if (roles.size() == 1) {
                final Role role = roles.iterator().next();

                addEntityPermission(role, READ, JDBC_CONNECTION);
                roleManager.update(role);
            } else {
                logger.warning("Not upgrading roles for service '" + policyGoid + "', expected one role but found " +roles.size()+ ".");
            }
        }
    }

    /**
     * Add entity permission if not present
     */
    private void addEntityPermission( final Role role,
                                      final OperationType operationType,
                                      final EntityType entityType ) {
        boolean hasPermission = false;

        for ( Permission permission : role.getPermissions() ) {
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
