/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.io.IOException;
import java.util.Set;

/**
 * Manages policies for SSGs.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:19:51 AM
 */
public interface PolicyManager {

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
     * in an underlying policy source.  {@see #updatePolicy}
     *
     * @param policyAttachmentKey the {@link PolicyAttachmentKey} describing the policy to locate.
     * @return the requested {@link Policy}, or null if it was not found.
     * @throws java.io.IOException if a disk or network problem prevented the policy lookup.
     */
    Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) throws IOException;

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
