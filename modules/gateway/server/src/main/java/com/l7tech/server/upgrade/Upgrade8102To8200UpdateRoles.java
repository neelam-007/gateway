package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Update dynamic role permissions for 8.2.00
 *
 * - Add OTHER "debugger" permission for Policy to Manage X Policy roles (Global Policy Fragments Only).
 * - Add OTHER "debugger" permission for Service to Manage X Service roles
 */
public class Upgrade8102To8200UpdateRoles extends AbstractDynamicRolePermissionsUpgradeTask {

    @Override
    protected List<Triple<OperationType, String, EntityType>> permissionsToAdd(EntityType entityType) {
        if ( !EntityType.SERVICE.equals(entityType) && !EntityType.POLICY.equals(entityType) ) {
            // Only Service and Policy expected to be upgraded.
            //
            throw new IllegalArgumentException("Unexpected entity type.");
        }

        return Arrays.asList(
            new Triple<>(OperationType.OTHER, OtherOperationName.DEBUGGER.getOperationName(), entityType));
    }

    @Override
    protected boolean shouldSetPermissionScope() {
        return true;
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