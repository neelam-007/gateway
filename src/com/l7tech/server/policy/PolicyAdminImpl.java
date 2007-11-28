/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.security.rbac.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin {
    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final RoleManager roleManager;

    public PolicyAdminImpl(PolicyManager policyManager, PolicyCache policyCache, RoleManager roleManager) {
        this.policyManager = policyManager;
        this.policyCache = policyCache;
        this.roleManager = roleManager;
    }

    public Policy findPolicyByPrimaryKey(long oid) throws FindException {
        return policyManager.findByPrimaryKey(oid);
    }

    public Collection<EntityHeader> findPolicyHeadersByType(PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    public void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        policyManager.delete(oid);
        roleManager.deleteEntitySpecificRole(com.l7tech.common.security.rbac.EntityType.POLICY, oid);
    }

    public long savePolicy(Policy policy) throws SaveException {
        if (policy.getOid() == Policy.DEFAULT_OID) {
            final long oid = policyManager.save(policy);
            policyManager.addManagePolicyRole(policy);
            return oid;
        } else {
            try {
                policyManager.update(policy);
                return policy.getOid();
            } catch (UpdateException e) {
                throw new SaveException("Couldn't update policy", e.getCause());
            }
        }
    }

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    public Set<Policy> findUsages(long oid) throws FindException {
        return policyCache.findUsages(oid);
    }
}
