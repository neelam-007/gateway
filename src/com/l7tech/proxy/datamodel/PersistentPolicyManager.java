/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

import java.util.HashMap;
import java.util.Map;


/**
 * A {@link PolicyManager} that stores policies that are persisted to disk, and possibly originated locally.
 * Usually delegated from a {@link TransientPolicyManager}.
 */
public class PersistentPolicyManager extends LocalPolicyManager {

    PersistentPolicyManager() {
    }

    public void setPolicy(PolicyAttachmentKey key, Policy policy ) throws PolicyLockedException {
        key.setPersistent(true);
        policy.setVersion(null);
        policy.setValid(true); // statically-configured policies must never be ignored
        super.setPolicy(key, policy);
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        Policy policy = super.getPolicy(policyAttachmentKey);

        // Double-check flags just in case ssgs.xml was hand-edited to insert a policy
        if (policy != null) {
            policyAttachmentKey.setPersistent(true);
            policy.setVersion(null);
            policy.setValid(true); // statically-configured policies must never be ignored
        }

        return policy;
    }

    public Policy findMatchingPolicy(PolicyAttachmentKey pak) {
        final Policy policy = super.findMatchingPolicy(pak);

        // Double-check flags just in case ssgs.xml was hand-edited to insert a policy
        if (policy != null) {
            pak.setPersistent(true);
            policy.setVersion(null);
            policy.setValid(true); // statically-configured policies must never be ignored
        }

        return policy;
    }

    /** Policy map accessor, for xml bean serializer.  Do not call this method. */
    public HashMap getPolicyMap() {
        return super.getPolicyMap();
    }

    /** Policy map mutator, for xml bean deserializer.  Do not call this method. */
    public void setPolicyMap(HashMap policyMap) {
        super.setPolicyMap(policyMap);
    }

    /**
     * Wildcard map accessor, for xml bean serializer.  Do not call this method.
     */
    public Map getWildcardMatches() {
        return super.getWildcardMatches();
    }

    /**
     * Wildcard map mutator, for xml bean deserializer.  Do not call this method.
     */
    public void setWildcardMatches(Map wildcardMatches) {
        super.setWildcardMatches(wildcardMatches);
    }
}
