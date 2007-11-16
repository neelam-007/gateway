/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy;

import com.l7tech.common.LicenseManager;
import com.l7tech.policy.assertion.Assertion;

/**
 * Interface implemented by components that can check to see if an assertion is enabled.
 */
public interface AssertionLicense extends LicenseManager {
    /**
     * Check if the specified assertion is enabled by this AssertionLicense.
     *
     * <p>The configuration for the assertion may be taken into account when
     * determining if enabled. Be aware that different instances of the same
     * assertion class may return different values.</p>
     *
     * @param assertion the assertion to check.  Must not be null.
     * @return true only if the specified assertion is enabled by this assertion license.
     */
    public boolean isAssertionEnabled(Assertion assertion);
}
