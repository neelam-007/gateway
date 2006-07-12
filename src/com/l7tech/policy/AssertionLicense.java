/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy;

import com.l7tech.common.LicenseManager;

/**
 * Interface implemented by components that can check to see if an assertion is enabled.
 */
public interface AssertionLicense extends LicenseManager {
    /** @return true only if the specified assertion class name is enabled by this assertion license. */
    public boolean isAssertionEnabled(String assertionClassname);
}
