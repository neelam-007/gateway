package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.objectmodel.EntityType.HTTP_CONFIGURATION;

/**
 * Update dynamic role permissions for 5.4.
 *
 * - Add READ permission for HTTP Configurations to Manage X Policy roles
 * - Add READ permission for HTTP Configurations to Manage X Service roles
 *
 */
public class Upgrade531To54UpdateRoles extends AbstractDynamicRolePermissionsUpgradeTask {

    //- PROTECTED

    @Override
    protected boolean shouldCreateMissingRoles() {
        return true;
    }

    @Override
    protected List<Triple<OperationType, String, EntityType>> permissionsToAdd(EntityType entityType) {
        return Arrays.asList(new Triple<OperationType, String, EntityType>(READ, null, HTTP_CONFIGURATION));
    }

    @Override
    protected boolean shouldSetPermissionScope() {
        return false;
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return false;
    }

    @NotNull
    @Override
    protected Collection<EntityType> getEntityTypesToUpgrade() {
        return Arrays.asList(EntityType.SERVICE, EntityType.POLICY);
    }
}
