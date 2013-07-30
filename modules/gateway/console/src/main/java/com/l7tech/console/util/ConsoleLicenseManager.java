/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.licensing.CompositeLicense;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component that caches the license installed on the currently-connected Gateway cluster.
 */
public class ConsoleLicenseManager implements AssertionLicense, LicenseManager {
    // Primary Licenses are defined/recognized by including this 'core' feature
    private static final String CORE_FEATURE_SET = "service:MessageProcessor";

    private static final ConsoleLicenseManager INSTANCE = new ConsoleLicenseManager();
    private static final SpecificUser SPECIFICUSER_PROTOTYPE = new SpecificUser();
    private static final MemberOfGroup MEMBEROFGROUP_PROTOTYPE = new MemberOfGroup();

    private CompositeLicense license = null;
    private Set<String> compat = new HashSet<>(); // Features to enable in GUI for license backward compat (even though they don't appear explicitly in the license as downloaded from an older Gateway)
    private Map<LicenseListener, Object> licenseListeners = new WeakHashMap<>();
    private Map<String, Assertion> prototypeCache = new ConcurrentHashMap<>();

    protected ConsoleLicenseManager() {
    }

    public static ConsoleLicenseManager getInstance() {
        return INSTANCE;
    }

    /** @return the cached license for the currently-connected Gateway.  Might be null. */
    public CompositeLicense getLicense() {
        return license;
    }

    /**
     * Check if a Primary License has been installed.
     * @return true iff the a Primary Feature License has been installed on the Gateway and is current and valid
     */
    public boolean isPrimaryLicenseInstalled() {
        return isFeatureEnabled(CORE_FEATURE_SET);
    }

    /**
     * Set the cached license for the currently-connected Gateway.
     * This is called by the licenseCheckingLogonListener in the MainWindow, and by the
     * license manager dialog when a new license is installed.
     * @param license the license to cache, or null to clear any cached license.
     */
    public void setLicense(CompositeLicense license) {
        if (this.license != license) {
            if (this.license != null && this.license.equals(license)) return;
            this.license = license;
            compat.clear();
            if (license != null) addCompatFeatures();
            fireLicenseEvent();
        }
    }

    /**
     * Add any compat feature sets, required by connecting to older versions of the software.
     */
    private void addCompatFeatures() {
        String v = LogonDialog.getLastRemoteSoftwareVersion();
        if (license == null || v == null) return;

        if (("HEAD".equals(v) || "3.6".equals(v)) &&
                license.isFeatureEnabled(Assertion.getFeatureSetName(SpecificUser.class)))
        {
            // 3.6 Gateway does not require the "service:TrustStore" feature set to be present
            // before allowing access to Trusted Cert management, so allow it as long as SpecificUser is available
            compat.add(SecureAction.TRUSTSTORE_FEATURESET_NAME);
        }

        if (("3.6".equals(v) || "3.6.5".equals(v) || "3.5".equals(v))) {
            // Pre-3.7 Gateway does not require any ui:Whatever features to be present before enabling UI-only features
            compat.add(SecureAction.UI_PUBLISH_SERVICE_WIZARD);
            compat.add(SecureAction.UI_PUBLISH_XML_WIZARD);
            compat.add(SecureAction.UI_WSDL_CREATE_WIZARD);
            compat.add(SecureAction.UI_AUDIT_WINDOW);
            compat.add(SecureAction.UI_RBAC_ROLE_EDITOR);
        }
    }

    /**
     * Add a license listener.
     * <p/>
     * License change events are always delivered on the Swing thread, but delivery is always synchronous
     * with the license change.
     * <p/>
     * Caller must ensure that they keep a hard reference to the listener, or it will immediately be GC'ed
     * and never receive any events.
     *
     * @param licenseListener the runnable to be invoked when the license is changed.  Must not be null.
     */
    public void addLicenseListener(LicenseListener licenseListener) {
        if (licenseListener == null) throw new NullPointerException();
        licenseListeners.put(licenseListener, null);
    }

    public boolean removeLicenseListener(LicenseListener licenseListener) {
        return licenseListeners.remove(licenseListener) != null;
    }

    protected void fireLicenseEvent() {
        Utilities.invokeOnSwingThreadAndWait(new Runnable() {
            public void run() {
                List<LicenseListener> listeners = new ArrayList<>(licenseListeners.keySet());
                for (LicenseListener listener : listeners) {
                    if (listener != null) listener.licenseChanged(ConsoleLicenseManager.this);
                }
            }
        });
    }

    public boolean isFeatureEnabled(String featureName) {
        return license != null && (compat.contains(featureName) || license.isFeatureEnabled(featureName));
    }

    public boolean isAuthenticationEnabled() {
        return isAssertionEnabled(SPECIFICUSER_PROTOTYPE) || isAssertionEnabled(MEMBEROFGROUP_PROTOTYPE);
    }

    public boolean isAtLeastOneAssertionEnabled(List<Class<? extends Assertion>> assertionClasses) {
        for (Class<? extends Assertion> aClass : assertionClasses) {
            if (isAssertionEnabled(aClass)) return true;
        }
        return false;
    }

    public boolean isAssertionEnabled(Class<? extends Assertion> assertionClass) {
        Assertion prototype = getPrototype(assertionClass);
        // Assertions with no public nullary constructor LOSE (as well they should IMO)
        return prototype != null && isAssertionEnabled(prototype);

    }

    private Assertion getPrototype(Class<? extends Assertion> assertionClass) {
        Assertion prototype = prototypeCache.get(assertionClass.getName());
        if (prototype != null)
            return prototype;
        try {
            prototype = assertionClass.newInstance();
            prototypeCache.put(assertionClass.getName(), prototype);
            return prototype;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public boolean isAssertionEnabled(Assertion prototype) {
        return isFeatureEnabled(prototype.getFeatureSetName());
    }

    public void requireFeature(String featureName) throws LicenseException {
        if (!isFeatureEnabled(featureName)) throw new LicenseException("Feature " + featureName + " is not available on this Gateway cluster.");
    }
}
