/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.common.policy.Policy;

/**
 * A reference to a {@link com.l7tech.common.policy.Policy} with {@link com.l7tech.common.policy.Policy#getType()} ==
 * {@link com.l7tech.common.policy.PolicyType#INCLUDE_FRAGMENT}. The fragment is considered to be part of the enclosing
 * policy for validation and runtime purposes.
 * 
 * @author alex
 */
public class Include extends Assertion implements UsesEntities {
    private Long policyOid;
    private String policyName;
    private transient Policy fragmentPolicy;

    public Include() {
    }

    public Include(Long policyOid, String policyName) {
        this.policyOid = policyOid;
        this.policyName = policyName;
    }

    /**
     * The OID of the attached {@link com.l7tech.common.policy.Policy} object.  Must not be null.
     */
    public Long getPolicyOid() {
        return policyOid;
    }

    public void setPolicyOid(Long policyOid) {
        this.policyOid = policyOid;
    }

    /**
     * The name of the attached {@link com.l7tech.common.policy.Policy} object, for display purposes only.  May be null.
     */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] {
            new EntityHeader(Long.toString(policyOid), EntityType.POLICY, policyName, null)
        };
    }

    public Policy retrieveFragmentPolicy() {
        return fragmentPolicy;
    }

    public void replaceFragmentPolicy(Policy policy) {
        fragmentPolicy = policy;
    }

    @Override
    public void updateTemporaryData(Assertion assertion) {
        if(!(assertion instanceof Include)) {
            return;
        }

        Include includeAssertion = (Include)assertion;
        fragmentPolicy = includeAssertion.retrieveFragmentPolicy();
    }
}
