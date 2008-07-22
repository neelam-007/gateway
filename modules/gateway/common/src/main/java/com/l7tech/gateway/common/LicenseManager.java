/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common;

/**
 * Interface for LicenseManager.
 */
public interface LicenseManager {
    /**
     * Check if the specified feature is enabled with the current license.  This check is very quick if the license
     * does not need to be reloaded, which is most of the time.
     *
     * @param featureName the feature to check.  Must not be null.  See GatewayFeatureSets for some constants.
     *                    See Assertion.getFeatureSetName() for assertion classnames.
     * @return true iff. this feature is enabled with the currently installed license
     */
    boolean isFeatureEnabled(String featureName);

    /**
     * Require the specified feature to continue.  If this method returns, a valid license is installed which
     * enables access to the specified feature.
     *
     * @param featureName  the feature to require.  Must not be nul.    See GatewayFeatureSets for some constants.
     *                    See Assertion.getFeatureSetName() for assertion classnames.
     * @throws LicenseException if no valid license is currently installed which enables this feature
     */
    void requireFeature(String featureName) throws LicenseException;
}
