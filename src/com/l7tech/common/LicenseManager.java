/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

import com.l7tech.common.Feature;
import com.l7tech.common.LicenseException;

/**
 * Interface for LicenseManager.
 */
public interface LicenseManager {
    /**
     * Check if the specified feature is enabled with the current license.  This check is very quick if the license
     * does not need to be reloaded, which is most of the time.
     *
     * @param feature the feature to check.  Must not be null.
     * @return true iff. this feature is enabled with the currently installed license
     */
    boolean isFeatureEnabled(Feature feature);

    /**
     * Require the specified feature to continue.  If this method returns, a valid license is installed which
     * enables access to the specified feature.
     *
     * @param feature  the feature to require
     * @throws com.l7tech.common.LicenseException if no valid license is currently installed which enables this feature
     */
    void requireFeature(Feature feature) throws LicenseException;
}
