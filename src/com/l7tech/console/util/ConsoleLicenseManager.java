/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import com.l7tech.common.License;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.util.*;

/**
 * Component that caches the license installed on the currently-connected Gateway cluster.
 */
public class ConsoleLicenseManager implements AssertionLicense, LicenseManager {
    private static final ConsoleLicenseManager INSTANCE = new ConsoleLicenseManager();
    private License license = null;
    private Map listeners = new WeakHashMap();

    protected ConsoleLicenseManager() {
    }

    public static ConsoleLicenseManager getInstance() {
        return INSTANCE;
    }

    /** @return the cached license for the currently-connected Gateway.  Might be null. */
    public License getLicense() {
        return license;
    }

    /**
     * Set the cached license for the currently-connected Gateway.
     * This is called by the licenseCheckingLogonListener in the MainWindow, and by the
     * license manager dialog when a new license is installed.
     * @param license the license to cache, or null to clear any cached license.
     */
    public void setLicense(License license) {
        this.license = license;
        fireLicenseEvent();
    }

    public void addLicenseListener(Runnable licenseListener) {
        listeners.put(licenseListener, null);
    }

    public boolean removeLicenseListener(Runnable licenseListener) {
        return listeners.remove(licenseListener) != null;
    }

    protected void fireLicenseEvent() {
        for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
            Runnable runnable = (Runnable)i.next();
            if (runnable != null) runnable.run();
        }
    }

    public boolean isFeatureEnabled(String featureName) {
        if (license == null) return false;
        return license.isFeatureEnabled(featureName);
    }

    public boolean isAuthenticationEnabled() {
        return isAssertionEnabled(SpecificUser.class.getName()) || isAssertionEnabled(MemberOfGroup.class.getName());
    }

    public boolean isAtLeastOneAssertionEnabled(List<Class<? extends Assertion>> assertionClasses) {
        for (Class<? extends Assertion> aClass : assertionClasses) {
            if (isAssertionEnabled(aClass.getName())) return true;
        }
        return false;
    }

    public boolean isAssertionEnabled(Assertion prototype) {
        return isAssertionEnabled(prototype.getClass().getName());
    }

    public boolean isAssertionEnabled(String assertionClassname) {
        return isFeatureEnabled(Assertion.getFeatureSetName(assertionClassname));
    }

    public void requireFeature(String featureName) throws LicenseException {
        if (!isFeatureEnabled(featureName)) throw new LicenseException("Feature " + featureName + " is not available on this Gateway cluster.");
    }
}
