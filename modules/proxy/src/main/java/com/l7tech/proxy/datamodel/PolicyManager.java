/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

import java.io.Serializable;
import java.util.Set;

/**
 * Manages policies for SSGs.
 */
public interface PolicyManager extends Serializable {
    /**
     * Notify the PolicyManager that a policy may be out-of-date and should be flushed from the cache.
     * The PolicyManager will not attempt to obtain a replacement one at this time.
     *
     * @param policyAttachmentKey the {@link PolicyAttachmentKey} of the {@link Policy} to flush.  Must not be null.
     */
    void flushPolicy(PolicyAttachmentKey policyAttachmentKey);

    /**
     * Look up a policy in this PolicyFinder.  Depending on the implementation, this may be a quick cache lookup
     * or may involve disk or network activity.
     * <p>
     * Even if this method fails to locate a policy, it may be possible to "try harder" and locate a policy
     * in an underlying policy source by calling {@link #findMatchingPolicy} instead.
     *
     * @param policyAttachmentKey the {@link PolicyAttachmentKey} describing the policy to locate.
     * @return the requested {@link Policy}, or null if it was not found.
     */
    Policy getPolicy(PolicyAttachmentKey policyAttachmentKey);

    /**
     * Search for a policy that matches this policyAttachmentKey.  Unlike getPolicy() which only finds exact matches,
     * this might check for pattern matches as well.
     *
     * @param policyAttachmentKey  the {@link PolicyAttachmentKey} describing the policy to locate.
     * @return the requested {@link Policy}, or null if it was not found.
     */
    Policy findMatchingPolicy(PolicyAttachmentKey policyAttachmentKey);

    /**
     * Set a policy, if this PolicyManager allows this.  When calling this method, be very careful that you
     * are not about to reuse a PolicyAttachmentKey instance that is already being used to index another policy
     * elsewhere in the system.  If there is any doubt, make a copy of it before calling setPolicy().
     *
     * @param key    the {@link PolicyAttachmentKey} under which to file this {@link Policy}.  May not be null.
     * @param policy the Policy to file.  May not be null.
     * @throws PolicyLockedException if this PolicyManager is read-only or an existing policy cannot be replaced.
     */
    void setPolicy(PolicyAttachmentKey key, Policy policy) throws PolicyLockedException;

    /**
     * Get the set of PolicyAttachmentKey that we currently know about.  These are the ones that
     * are immediateley available from {@link #getPolicy}.
     *
     * @return a defensively-copied Set of PolicyAttachmentKey objects.  Might be read-only.  Never null.
     */
    Set getPolicyAttachmentKeys();

    /**
     * Clear all policies in this PolicyManager.  This is a shallow deletion -- it does not affect any
     * child PolicyManagers that might exist.
     */
    void clearPolicies();
}
