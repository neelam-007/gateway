package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Pair;

import java.util.Arrays;
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
    protected List<Pair<OperationType, EntityType>> permissionsToAdd() {
        return Arrays.asList(new Pair<OperationType, EntityType>(READ, HTTP_CONFIGURATION));
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return false;
    }
}
