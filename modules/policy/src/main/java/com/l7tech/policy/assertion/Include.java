/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;

import java.util.UUID;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * A reference to a {@link Policy} with {@link Policy#getType()} ==
 * {@link com.l7tech.policy.PolicyType#INCLUDE_FRAGMENT}. The fragment is considered to be part of the enclosing
 * policy for validation and runtime purposes.
 * 
 * @author alex
 */
public class Include extends Assertion implements UsesEntities, PolicyReference {

    //- PUBLIC

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

    @Override
    public String retrievePolicyGuid() {
        return getPolicyGuid();
    }

    public void setPolicyGuid(String policyGuid) {
        this.policyGuid = policyGuid;
    }

    @Deprecated
    public Long getPolicyOid() {
        return policyOid;
    }

    @Deprecated
    public void setPolicyOid(Long policyOid) {
        this.policyOid = policyOid;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        GuidEntityHeader header = new GuidEntityHeader(policyGuid, EntityType.POLICY, policyName, null, fragmentPolicy == null ? null : fragmentPolicy.getVersion());
        header.setGuid( policyGuid );
        return new EntityHeader[] {
            header
        };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (!(newEntityHeader instanceof PolicyHeader)) throw new IllegalArgumentException("newEntityHeader is not a PolicyHeader");

        policyGuid = ((PolicyHeader)newEntityHeader).getGuid();
    }

    @Override
    public Policy retrieveFragmentPolicy() {
        return fragmentPolicy;
    }

    @Override
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

    /**
     * Get the meta data for this assertion.
     *
     * @return The metadata for this assertion
     */
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (!Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            populateMeta(meta);
            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

    //- PRIVATE

    // Metadata flag
    private static final String META_INITIALIZED = Include.class.getName() + ".metadataInitialized";

    private Long policyOid;
    private String policyGuid;
    private String policyName;
    private transient Policy fragmentPolicy;

    /**
     * Populate the given metadata.
     */
    private void populateMeta( final DefaultAssertionMetadata meta ) {
        meta.put(PALETTE_FOLDERS, new String[] { "policyLogic" });

        meta.put(SHORT_NAME, "Include Policy Fragment");
        meta.put(DESCRIPTION, "Include a Policy Fragment in the policy.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        
        meta.put(POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddIncludeAdvice");
        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.IncludeAssertionPolicyNode");

        meta.put(PROPERTIES_ACTION_NAME, "Select Policy Fragment to Include");
    }
}
