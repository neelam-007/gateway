package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.ASSERTION_ACCESS;

/**
 * Update dynamic role permissions for 8.0.
 *
 * - Add READ and CREATE permission for Assertion Access to Manage X Policy roles
 * - Add READ and CREATE permission for Assertion Access to Manage X Service roles
 * - Add READ and CREATE permission for Assertion Access to Manage/View X Folder roles
 *
 */
public class Upgrade71To80UpdateRoles extends AbstractDynamicRolePermissionsUpgradeTask {

    //- PROTECTED

    @Override
    protected boolean shouldCreateMissingRoles() {
        return false;
    }

    @Override
    protected List<Pair<OperationType, EntityType>> permissionsToAdd() {
        return Arrays.asList(new Pair<OperationType, EntityType>(READ, ASSERTION_ACCESS), new Pair<OperationType, EntityType>(CREATE, ASSERTION_ACCESS));
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return !PolicyType.INCLUDE_FRAGMENT.equals(policyType);
    }

    @Override
    protected Collection<EntityType> getEntityTypesToUpgrade() {
        return Arrays.asList(EntityType.SERVICE, EntityType.POLICY, EntityType.FOLDER);
    }
}
