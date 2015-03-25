/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.licensing.UpdatableCompositeLicenseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A permissive license manager, for testing, that enables all features by default.
 */
public class TestLicenseManager implements AssertionLicense, UpdatableCompositeLicenseManager {
    private final Set<String> disabledFeatures = new HashSet<>();

    public TestLicenseManager() {
    }

    public TestLicenseManager(final Set<String> disabledFeatures) {
        for (String feature : disabledFeatures) {
            disableFeature(feature);
        }
    }

    public boolean isAssertionEnabled(Assertion assertion) {
        return true;
    }

    public boolean isFeatureEnabled(String feature) {
        return !disabledFeatures.contains(feature);
    }

    public void requireFeature(String feature) throws LicenseException {
        if (!isFeatureEnabled(feature))
            throw new LicenseException("feature " + feature + " is not enabled");
    }

    public void disableFeature(String feature) {
        disabledFeatures.add(feature);
    }

    @Override
    public CompositeLicense getCurrentCompositeLicense() {
        return new CompositeLicense(new HashMap<Long, FeatureLicense>(),
                new HashMap<Long, FeatureLicense>(), new HashMap<Long, FeatureLicense>(),
                new ArrayList<LicenseDocument>(), GatewayFeatureSets.getFeatureSetExpander());
    }

    @Override
    public FeatureLicense createFeatureLicense(LicenseDocument licenseDocument) throws InvalidLicenseException {
        return null;
    }

    @Override
    public void validateLicense(FeatureLicense license) throws InvalidLicenseException {}

    @Override
    public void installLicense(FeatureLicense license) throws LicenseInstallationException {}

    @Override
    public void uninstallLicense(LicenseDocument licenseDocument) throws LicenseRemovalException {}
}
