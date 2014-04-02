package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

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
    protected List<Triple<OperationType, String, EntityType>> permissionsToAdd(EntityType entityType) {
        return Arrays.asList(
            new Triple<OperationType, String, EntityType>(READ, null, ASSERTION_ACCESS),
            new Triple<OperationType, String, EntityType>(CREATE, null, ASSERTION_ACCESS));
    }

    @Override
    protected boolean shouldSetPermissionScope() {
        return false;
    }

    @Override
    protected boolean shouldIgnorePolicyType(PolicyType policyType) {
        return !PolicyType.INCLUDE_FRAGMENT.equals(policyType);
    }

    @NotNull
    @Override
    protected Collection<EntityType> getEntityTypesToUpgrade() {
        return Arrays.asList(EntityType.SERVICE, EntityType.POLICY, EntityType.FOLDER);
    }
}
