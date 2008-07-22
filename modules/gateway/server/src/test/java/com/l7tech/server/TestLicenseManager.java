/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;

import java.util.HashSet;
import java.util.Set;

/**
 * A permissive license manager, for testing, that enables all features by default.
 */
public class TestLicenseManager implements AssertionLicense, LicenseManager {
    Set disabledFeatures = new HashSet();

    public boolean isAssertionEnabled( Assertion assertion ) {
        return true;
    }

    public boolean isFeatureEnabled(String feature) {
        return !disabledFeatures.contains(feature);
    }

    public void requireFeature(String feature) throws LicenseException {
        if (!isFeatureEnabled(feature))
            throw new LicenseException("feature " + feature + " is not enabled");
    }

    public void enableFeature(String feature) {
        disabledFeatures.remove(feature);
    }

    public void disableFeature(String feature) {
        disabledFeatures.add(feature);
    }
}
