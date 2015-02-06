/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerStub;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

/**
 * @author alex
*/
public class PolicyManagerStub extends EntityManagerStub<Policy,PolicyHeader> implements PolicyManager {
    public PolicyManagerStub(Policy... entities) {
        super(entities);
    }

    public PolicyManagerStub() {
        super(new Policy[0]);
    }

    @Override
    public Collection<PolicyHeader> findHeadersByType(PolicyType type) throws FindException {
        return findHeadersWithTypes(EnumSet.of(type));
    }

    @Override
    public Policy findByGuid(String guid) throws FindException {
        if(guid == null) {
            return null;
        }

        for(Policy policy : entities.values()) {
            if(guid.equals(policy.getGuid())) {
                return policy;
            }
        }

        return null;
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types, boolean includeAliases) {
        return findHeadersWithTypes(types);        
    }

    @Override
    public void deleteWithoutValidation(Policy policy) throws DeleteException {
        delete(policy);
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types) {
        Set<PolicyHeader> hs = new HashSet<PolicyHeader>();
        for (Policy policy : entities.values()) {
            if (types.contains(policy.getType())) hs.add(new PolicyHeader(policy));
        }
        return hs;
    }

    @Override
    public void addManagePolicyRole(Policy policy) throws SaveException {
        // No-op for stub mode
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return Policy.class;
    }

    @Override
    protected PolicyHeader header(Policy entity) {
        return new PolicyHeader(entity);
    }
}
