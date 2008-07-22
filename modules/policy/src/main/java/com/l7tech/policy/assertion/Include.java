/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;

import java.util.UUID;

/**
 * A reference to a {@link Policy} with {@link Policy#getType()} ==
 * {@link com.l7tech.policy.PolicyType#INCLUDE_FRAGMENT}. The fragment is considered to be part of the enclosing
 * policy for validation and runtime purposes.
 * 
 * @author alex
 */
public class Include extends Assertion implements UsesEntities, PolicyReference {
    private Long policyOid;
    private String policyGuid;
    private String policyName;
    private transient Policy fragmentPolicy;

    public Include() {
    }

    public Include(String policyGuid) {
        this.policyGuid = policyGuid;
    }

    public Include(String policyGuid, String policyName) {
        this.policyGuid = policyGuid;
        this.policyName = policyName;
    }

    /**
     * The name of the attached {@link com.l7tech.policy.Policy} object, for display purposes only.  May be null.
     */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /**
     * The GUID of the attached {@link Policy} object.  Must not be null.
     */
    public String getPolicyGuid() {
        if(policyGuid == null && policyOid != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(policyOid.longValue());
            sb.append('#');
            sb.append(policyName);

            String uuidString = sb.toString();

            UUID uuid = UUID.nameUUIDFromBytes(uuidString.getBytes());
            return uuid.toString();
        }
        return policyGuid;
    }

    public String retrievePolicyGuid() {
        return getPolicyGuid();
    }

    public void setPolicyGuid(String policyGuid) {
        this.policyGuid = policyGuid;
    }

    public Long getPolicyOid() {
        return policyOid;
    }

    public void setPolicyOid(Long policyOid) {
        this.policyOid = policyOid;
    }

    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] {
            new EntityHeader(policyGuid, EntityType.POLICY, policyName, null)
        };
    }

    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.POLICY) && oldEntityHeader.getStrId().equals(policyGuid) &&
                newEntityHeader.getType().equals(EntityType.POLICY))
        {
            policyGuid = newEntityHeader.getStrId();
        }
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
