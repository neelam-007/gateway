/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy;

/**
 * Interface implemented by components that can check to see if an assertion is enabled.
 */
public interface AssertionLicense {
    /** @return true only if the specified assertion class name is enabled by this assertion license. */
    public boolean isAssertionEnabled(String assertionClassname);
}
