/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

import java.util.Set;

/**
 * A {@link PolicyManager} that manages the transient policies in the SSB.  Delegates to another PolicyManager,
 * typically the {@link PersistentPolicyManager}.
 * <p>
 * Incoming policy keys marked as "persistent" will be routed to the delegate.  Incoming policy keys not marked as
 * persistent will not be permitted to override policies already existing in the delegate.
 *
 */
public class TransientPolicyManager extends LocalPolicyManager {
    private final PolicyManager delegate;

    public TransientPolicyManager() {
        this.delegate = new PersistentPolicyManager(); // ensure delegate is never null
    }

    /**
     * Create a TransientPolicyManager that will get policies from the specified delegate if there is a local cache miss.
     * As a side-effect, incoming Policies with the persistent bit set will be passed through to the delegate
     * instead of being stored locally.
     *
     * @param delegate PolicyManger to use if the current PolicyManager does not find a policy.
     */
    public TransientPolicyManager(PolicyManager delegate) {
        this.delegate = delegate;
    }

    /** Delegate accessor, for xml bean serializer.  Do not call this method. */
    public synchronized PolicyManager getDelegate() {
        return delegate;
    }

    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = super.getPolicyAttachmentKeys();
        // mix in delegate's immediately available policies as immediately-available from us
        setCopy.addAll(delegate.getPolicyAttachmentKeys());
        return setCopy;
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy ) throws PolicyLockedException {
        if (key == null) throw new NullPointerException();
        if (policy == null) throw new NullPointerException();

        if (key.isPersistent()) {
            delegate.setPolicy(key, policy);
            super.flushPolicy(key);
        } else {
            if (delegate.getPolicy(key) != null) {
                // Attempt to save non-persistent policy over top of persistent policy.
                // TODO add hook here to alert an observer and allow them to choose a course of action.
                // For now, we will always respond as follows: reject the new policy.
                throw new PolicyLockedException("Unable to store new policy: a locked local policy already exists with this policy attachment key.");
            }
            super.setPolicy(key, policy);
        }
    }

    public synchronized Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        Policy policy = super.getPolicy(policyAttachmentKey);
        if (policy == null)
            policy = delegate.getPolicy(policyAttachmentKey);
        return policy;
    }

    public synchronized Policy findMatchingPolicy(PolicyAttachmentKey pak) {
        Policy policy = super.findMatchingPolicy(pak);
        if (policy == null)
            policy = delegate.findMatchingPolicy(pak);
        return policy;
    }

    public synchronized void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        super.flushPolicy(policyAttachmentKey);
        delegate.flushPolicy(policyAttachmentKey);
    }

    /** Clear transient policies only.  Does not affect persistent policies. */
    public void clearPolicies() {
        super.clearPolicies();
    }
}
