/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * A {@link PolicyManager} that stores policies in memory.  This implementation is synchronized.
 */
public class LocalPolicyManager implements PolicyManager, Serializable {
    private static final Logger logger = Logger.getLogger(LocalPolicyManager.class.getName());
    private HashMap policyMap = new HashMap(); /* Policy cache */

    /**
     * Create a LocalPolicyManager with no delegate.
     */
    public LocalPolicyManager() {
    }

    /** Policy map accessor, for xml bean serializer.  Do not call this method. */
    protected synchronized HashMap getPolicyMap() {
        return policyMap;
    }

    /** Policy map mutator, for xml bean deserializer.  Do not call this method. */
    protected synchronized void setPolicyMap(HashMap policyMap) {
        this.policyMap = policyMap;
    }

    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalPolicyManager)) return false;

        final LocalPolicyManager that = (LocalPolicyManager)o;

        if (policyMap != null ? !policyMap.equals(that.policyMap) : that.policyMap != null) return false;

        return true;
    }

    public synchronized int hashCode() {
        return (policyMap != null ? policyMap.hashCode() : 0);
    }

    public synchronized void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        policyMap.remove(policyAttachmentKey);
    }

    public synchronized Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return (Policy)policyMap.get(policyAttachmentKey);
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy) {
        if (key == null) throw new NullPointerException();
        if (policy == null) throw new NullPointerException();
        policyMap.put(key, policy);
    }

    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = new TreeSet(policyMap.keySet());
        return setCopy;
    }

    public synchronized void clearPolicies() {
        policyMap.clear();
    }
}
