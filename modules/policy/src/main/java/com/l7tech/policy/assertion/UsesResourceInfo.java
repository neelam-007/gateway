/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionResourceInfo;

/**
 * Indicates that an assertion relies on at least one resource document (e.g. a stylesheet, an XML schema)
 * to do its work.  The implementation and properties of the {@link AssertionResourceInfo} returned from
 * {@link #getResourceInfo} determine how such resources are located.
 */
public interface UsesResourceInfo {
    /**
     * Gets the AssertionResourceInfo to use for this assertion.
     * @return the AssertionResourceInfo to use for this assertion. Never null.
     */
    AssertionResourceInfo getResourceInfo();

    /**
     * Sets the AssertionResourceInfo to use for this assertion. Must not be null.
     * @param resourceInfo the AssertionResourceInfo to use for this assertion
     * @throws IllegalArgumentException if the specified AssertionResourceInfo is of an unsupported type, or is misconfigured.
     */
    void setResourceInfo(AssertionResourceInfo resourceInfo) throws IllegalArgumentException;
}
