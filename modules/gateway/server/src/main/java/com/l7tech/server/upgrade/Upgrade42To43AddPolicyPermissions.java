/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.upgrade;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;
import org.springframework.context.ApplicationContext;

import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.Collection;

/**
 * Finds Manage Specific Service roles, and adds permissions to read and update the policies for those services.
 * Note that there <em>shouldn't</em> be any policy fragments in the database yet, so we don't need to create roles for
 * those policies.
 * 
 * @author alex
 */
public class Upgrade42To43AddPolicyPermissions implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade42To43AddPolicyPermissions.class.getName());

    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        PolicyManager policyManager = (PolicyManager) applicationContext.getBean("policyManager");
        RoleManager roleManager = (RoleManager) applicationContext.getBean("roleManager");
        ServiceManager serviceManager = (ServiceManager) applicationContext.getBean("serviceManager");

        if (policyManager == null || roleManager == null || serviceManager == null)
            throw new FatalUpgradeException("A required component is not available");

        try {
            for (PublishedService service : serviceManager.findAll()) {
                final String uri = service.getRoutingUri();
                final Policy policy = service.getPolicy();
                if (policy == null) {
                    logger.warning(MessageFormat.format("Policy for Service #{0} ({1}) [{2}] is missing, cannot update permissions", service.getGoid(), service.getName(), uri == null ? "" : uri));
                    continue;
                } else if (Goid.isDefault(policy.getGoid())) {
                    logger.warning(MessageFormat.format("Policy for Service #{0} ({1}) [{2}] is has not been saved, cannot update permissions", service.getGoid(), service.getName(), uri == null ? "" : uri));
                    continue;
                }

                Collection<Role> roles = roleManager.findEntitySpecificRoles(EntityType.SERVICE, service.getGoid());
                if (roles == null) {
                    logger.warning(MessageFormat.format("Role for Service #{0} ({1}) [{2}] is missing, cannot update permissions", service.getGoid(), service.getName(), uri == null ? "" : uri));
                    continue;
                }

                for ( Role role : roles ) {
                    role.addEntityPermission(OperationType.READ, EntityType.POLICY, policy.getId());
                    role.addEntityPermission(OperationType.UPDATE, EntityType.POLICY, policy.getId());
                    roleManager.update(role);
                }
            }
        } catch (ObjectModelException e) {
            throw new FatalUpgradeException("Unable to continue upgrade task", e);
        }
    }
}
