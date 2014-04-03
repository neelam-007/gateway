package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Update dynamic role permissions for 8.2.00
 *
 * - Add OTHER "debugger" permission for Policy to Manage X Policy roles (Global Policy Fragments Only).
 * - Add OTHER "debugger" permission for Policy to Manage X Service roles
 */
public class Upgrade8102To8200UpdateRoles extends AbstractDynamicRolePermissionsUpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade8102To8200UpdateRoles.class.getName());

    @Override
    protected List<Triple<OperationType, String, EntityType>> permissionsToAdd() {
        return Arrays.asList(
            new Triple<>(OperationType.OTHER, OtherOperationName.DEBUGGER.getOperationName(), EntityType.POLICY));
    }

    @Override
    protected String getScopeId (EntityType entityType, Goid entityGoid) throws ObjectModelException {
        String id = null;

        if (EntityType.POLICY.equals(entityType)) {
            id = entityGoid.toString();
        } else if (EntityType.SERVICE.equals(entityType)) {
            if (entityGoid != null) {
                PublishedService service = serviceManager.findByPrimaryKey(entityGoid);
                if (service != null) {
                    id = service.getPolicy().getId();
                } else {
                    logger.warning("Unable to access service '" + entityGoid + "', not adding debugger permission.");
                }
            }
        }

        return id;
    }

    @Override
    protected boolean shouldCreateMissingRoles() {
        return false;
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return !PolicyType.GLOBAL_FRAGMENT.equals(policyType);
    }

    @NotNull
    @Override
    protected Collection<EntityType> getEntityTypesToUpgrade() {
        return Arrays.asList(EntityType.SERVICE, EntityType.POLICY);
    }
}