/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link PolicyManager} that stores policies in memory.  This implementation is synchronized.
 */
public class LocalPolicyManager implements PolicyManager, Serializable {

    private PolicyManager delegate = null;
    private transient HashMap policyMap = new HashMap(); /* Policy cache */

    /**
     * Create a LocalPolicyManager with no delegate.
     */
    public LocalPolicyManager() {
    }

    /**
     * Create a LocalPolicyManager that will get policies from the specified delegate if there is a local cache miss.
     *
     * @param delegate PolicyManger to use if the current PolicyManager does not find a policy.
     */
    public LocalPolicyManager(PolicyManager delegate) {
        this.delegate = delegate;
    }

    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = new TreeSet(policyMap.keySet());
        if (delegate != null) // mix in delegate's immediately available policies as immediately-available from us
            setCopy.addAll(delegate.getPolicyAttachmentKeys());
        return setCopy;
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy ) {
        policyMap.put(key, policy);
    }

    public synchronized Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) throws IOException {
        Policy policy = (Policy) policyMap.get(policyAttachmentKey);
        if (policy == null && delegate != null)
            policy = delegate.getPolicy(policyAttachmentKey);
        return policy;
    }

    public synchronized void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        policyMap.remove(policyAttachmentKey);
        if (delegate != null)
            delegate.flushPolicy(policyAttachmentKey);
    }

    /**
     * Clear policies in this LocalPolicyManager.  Does not affect the delegate.
     */
    public synchronized void clearPolicies() {
        policyMap.clear();
    }
}
