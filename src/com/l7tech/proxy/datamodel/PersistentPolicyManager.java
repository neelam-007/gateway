/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.util.HashMap;


/**
 * A {@link PolicyManager} that stores policies that are persisted to disk, and possibly originated locally.
 * Usually delegated from a {@link TransientPolicyManager}.
 */
public class PersistentPolicyManager extends LocalPolicyManager {

    PersistentPolicyManager() {
    }

    public void setPolicy(PolicyAttachmentKey key, Policy policy ) {
        policy.setPersistent(true);
        policy.setVersion(null);
        policy.setValid(true); // statically-configured policies must never be ignored
        super.setPolicy(key, policy);
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        Policy policy = super.getPolicy(policyAttachmentKey);

        // Double-check flags just in case ssgs.xml was hand-edited to insert a policy
        if (policy != null) {
            policy.setPersistent(true);
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
}
