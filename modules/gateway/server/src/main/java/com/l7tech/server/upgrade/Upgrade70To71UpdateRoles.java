package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Pair;

import java.util.Arrays;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.objectmodel.EntityType.ENCAPSULATED_ASSERTION;

/**
 * Update dynamic role permissions for 7.1.
 *
 * - Add READ permission for Encapsulated Assertion Configurations to Manage X Policy roles
 * - Add READ permission for Encapsulated Assertion Configurations to Manage X Service roles
 *
 */
public class Upgrade70To71UpdateRoles extends AbstractDynamicRolePermissionsUpgradeTask {

    //- PROTECTED

    @Override
    protected boolean shouldCreateMissingRoles() {
        return false;
    }

    @Override
    protected List<Pair<OperationType, EntityType>> permissionsToAdd() {
        return Arrays.asList(new Pair<OperationType, EntityType>(READ, ENCAPSULATED_ASSERTION));
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return !PolicyType.INCLUDE_FRAGMENT.equals(policyType);
    }
}
