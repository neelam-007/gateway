/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

/**
 * Interface implemented by components interested in receiving license change events.
 */
public interface LicenseListener {
    /** Notify a listener that a new license has been cached, and capabilities may have changed as a result. */
    void licenseChanged(ConsoleLicenseManager licenseManager);
}
