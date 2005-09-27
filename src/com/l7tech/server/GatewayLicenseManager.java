/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.event.admin.ClusterPropertyEvent;
import com.l7tech.server.event.system.LicenseEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps track of license permissions.
 */
public class GatewayLicenseManager extends ApplicationObjectSupport implements InitializingBean, ApplicationListener, LicenseManager {
    private static final Logger logger = Logger.getLogger(GatewayLicenseManager.class.getName());
    private static final long CHECK_INTERVAL = (5L * 60L * 1000L) + (new SecureRandom().nextInt(60000)); // recheck every 5 min + random desync interval
    private static final int CHECK_THRESHOLD = 30; // dont recheck more often than once per this many license hook calls
    private static final long DB_FAILURE_GRACE_PERIOD = 72L * 60L * 60L * 1000L; // ignore a DB failure for up to 72 hours before canceling any current license
    private static final String LICENSE_PROPERTY_NAME = "license";
    private static final Object LICENSE_PROPERTY_VAL = getVal();
    private static final int CHECKCOUNT_CHECK_NOW = 10000; // high number meaning check right now
    private static final long TIME_CHECK_NOW = -20000L;

    // This creates our trusted issuer cert.  It's supposed to be hardcoded here in encrypted form.
    private static Object getVal() {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            // I'll just let the obfuscator take care of encrypting this
            byte[] buf = ("-----BEGIN CERTIFICATE-----\n" +
                    "MIIC6DCCAdACBEM41hwwDQYJKoZIhvcNAQEEBQAwOTEdMBsGA1UEChMUTGF5ZXIg\n" +
                    "NyBUZWNobm9sb2dpZXMxGDAWBgNVBAMTD0xpY2Vuc2UgT2ZmaWNlcjAeFw0wNTA5\n" +
                    "MjcwNTE4MjBaFw0yNTA5MjYwNTE4MjBaMDkxHTAbBgNVBAoTFExheWVyIDcgVGVj\n" +
                    "aG5vbG9naWVzMRgwFgYDVQQDEw9MaWNlbnNlIE9mZmljZXIwggEhMA0GCSqGSIb3\n" +
                    "DQEBAQUAA4IBDgAwggEJAoIBAH6Zu7CyJnp/UqHlZ3WNEy4OKXzms7movyd4Bpqb\n" +
                    "6DRRzOq/qZfMMnoKCZ5tEpAODw9DPPJHoE3bXV67dDWTnDNwCU67r1fHBFqTqJaB\n" +
                    "WgU1Gzgy+Ve7N6BaoeAXVJgEXR5b9MVFabfG1FYsqEbvKwUvOVqow1XGLoPWqAKP\n" +
                    "3fdBDUPOJgGUnrzY1pBvBSLlQoKzGR+fHVrMn1zQRS9MFalwzIgrgvEUxeTA72DF\n" +
                    "G3ZJJ47ek+OmYP7q5Nzz1rCSBilv7CTW8TCZMKLJSBHfB0pPDaIMLdPdZqOes3ng\n" +
                    "9jXuWpVCHI/lljxjBBWNTne/fUmN8gayTKTztA4UbO/heJECAwEAATANBgkqhkiG\n" +
                    "9w0BAQQFAAOCAQEAEUDRup8nlBrK6z2114ReO2gt+k+ZwtqbSIGBMM6kCKvnUV7f\n" +
                    "Bmi9XnvglM/ekmKBNIqMXuCjbOcRqgU5eiuKpvctHRzUKTHT9CKUQfR7ow2+Kkq8\n" +
                    "0vD7JCcsbIqDyWD7tsf/RGNLNZIcOGuBFDrJx1+lNo8R/FlXnestXGVIRCLyH+Y2\n" +
                    "w8GvvmUdKMymq0Adpr14v4B6/+xikxWJoUVTwnBLCNWoAqizCjla9lm4wOtKqsS1\n" +
                    "8TyDvB+rL9Gz+K5SRUxpWt0ADRWUJRdmF29H8GcDUcaAK7Ka6BjyrOhE9t6emB7e\n" +
                    "cX/Yl+RgwYa4F314O0xBGP6baqtVy/5BObtucA==\n" +
                    "-----END CERTIFICATE-----").getBytes();
            InputStream is = new ByteArrayInputStream(buf);
            return certFactory.generateCertificate(is);
        } catch (CertificateException e) {
            // Very bad news
            logger.log(Level.SEVERE, "Unable to configure trusted issuer cert", e);
            return null;
        }
    }

    // Filled in by Spring
    private final ClusterPropertyManager clusterPropertyManager;

    // Brake to prevent calls to System.currentTimeMillis every time a license check is made.
    // This is unsynchronized because we don't care if some writes to it are lost, or if some reads are out-of-date.
    private volatile int checkCount = CHECKCOUNT_CHECK_NOW;
    private long lastCheck = TIME_CHECK_NOW;

    private boolean licenseSet = false;
    private License license = null;
    private InvalidLicenseException licenseLastError = null;
    private long licenseLoaded = TIME_CHECK_NOW;

    public GatewayLicenseManager(ClusterPropertyManager clusterPropertyManager)
    {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void afterPropertiesSet() throws Exception {
    }

    public boolean isFeatureEnabled(Feature feature) {
        check();
        // Currently, all features are enabled as long as any license exists.
        return license != null && feature != null && license.isFeatureEnabled(feature.getName());
    }

    public void requireFeature(Feature feature) throws LicenseException {
        if (!isFeatureEnabled(feature)) {
            checkCount = CHECKCOUNT_CHECK_NOW;
            throw new LicenseException("feature " + feature + " is not enabled");
        }
    }

    /** Update the license if we haven't done so in a while.  Returns quickly if no update is indicated. */
    private void check() {
        if (checkCount++ < CHECK_THRESHOLD) // Don't care much about out-of-date reads or lost writes here
            return;
        synchronized (this) {
            checkCount = 0;
            long now = System.currentTimeMillis();
            if ((now - lastCheck) > CHECK_INTERVAL) {
                reloadLicenseFromDatabase();
                lastCheck = System.currentTimeMillis();
            }
        }
    }

    /** (Re)read the license from the cluster property table. */
    private synchronized void reloadLicenseFromDatabase() {
        if (clusterPropertyManager == null) throw new IllegalStateException("ClusterPropertyManager has not been set");

        // Get current license XML from database.
        final String licenseXml;
        try {
            licenseXml = clusterPropertyManager.getProperty(LICENSE_PROPERTY_NAME);
        } catch (FindException e) {
            // We'll let this slide and keep the current license, if any, unless it was loaded more than 1 day ago
            if (license != null && (System.currentTimeMillis() - licenseLoaded < DB_FAILURE_GRACE_PERIOD)) {
                fireEvent(SystemMessages.LICENSE_DB_ERROR_RETRY, null);
                return;
            } else {
                fireEvent(SystemMessages.LICENSE_DB_ERROR_GAVEUP, null);
                setLicense(null);
                this.licenseLastError = new InvalidLicenseException("Unable to read license information from database: " + ExceptionUtils.getMessage(e), e);
                // Leave licenseLoaded the same so this message repeats.
                return;
            }
        }

        if (licenseXml == null || licenseXml.length() < 1) {
            fireEvent(SystemMessages.LICENSE_NO_LICENSE, null);
            setLicense(null);
            this.licenseLoaded = System.currentTimeMillis();
            return;
        }

        try {
            validateAndInstallLicense(licenseXml);
            this.licenseLastError = null;
        } catch (InvalidLicenseException e) {
            this.licenseLastError = e;
            fireEvent(SystemMessages.LICENSE_INVALID, e.getMessage());
            setLicense(null);
            this.licenseLoaded = System.currentTimeMillis();
            return;
        }
    }

    /**
     * Validate the specified license and, if it is valid for this issuer/product/version/date/etc, install
     * it as the current license in this LicenseManager.
     * <p>
     * This does not access the database in any way.
     *
     * @param licenseXml the license to validate and install
     */
    private void validateAndInstallLicense(String licenseXml) throws InvalidLicenseException {

        final License license;
        try {
            license = new License(licenseXml, getTrustedIssuers());
            license.checkValidity();
            checkProductVersion(license);
        } catch (InvalidLicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidLicenseException("License XML invalid: " + ExceptionUtils.getMessage(e), e);
        }

        setLicense(license);
        this.licenseLoaded = System.currentTimeMillis();
    }

    /**
     * Ensure that the specified license grants access to the current product and version
     * as gathered from BuildInfo.  If this method returns, the specified license allows access
     * to the current product version.
     * <p>
     * Note: this method does not check license signature or expiry or anything else except whether
     * it allows the current product name and major/minor version.
     *
     * @param license   the license to check
     * @throws InvalidLicenseException if this license does not grant access to this version of this product
     */
    private void checkProductVersion(License license) throws InvalidLicenseException {
        final String product = BuildInfo.getProductName();
        final String major = BuildInfo.getProductVersionMajor();
        final String minor = BuildInfo.getProductVersionMinor();
        if (!license.isProductEnabled(product, major, minor))
            throw new InvalidLicenseException("License " + license + " does not grant access to this version of this product (" +
                    product + " " + major + "." + minor);
    }

    private void fireEvent(AuditDetailMessage message, String suffix) {
        suffix = suffix == null ? "" : ": " + suffix;
        getApplicationContext().publishEvent(new LicenseEvent(this, message.getLevel(), message.getMessage() + suffix));
    }

    private X509Certificate[] getTrustedIssuers() {
        if (LICENSE_PROPERTY_VAL instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate)LICENSE_PROPERTY_VAL;
            return new X509Certificate[] { x509Certificate };
        }
        return null;
    }

    /** Set the current license.  Should only be called by updateLicense(), and only with a valid license. */
    private synchronized void setLicense(License license) {
        License old = this.license;
        this.license = license; // always replace the object with the new one...
        // ...but suppress the event/audit message if we are just reinstalling an identical license
        if (licenseSet && (old == license || (old != null && old.equals(this.license))))
            return;
        this.licenseSet = true;
        fireEvent(SystemMessages.LICENSE_UPDATED, license == null ? "<none>" : license.toString());
    }

    /** Listen for the license property being changed, and update the license immediately. */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ClusterPropertyEvent) {
            ClusterPropertyEvent evt = (ClusterPropertyEvent)applicationEvent;
            ClusterProperty clusterProperty = evt.getClusterProperty();
            if (LICENSE_PROPERTY_NAME.equals(clusterProperty.getKey())) {
                // Schedule an immediate update, next time anyone does a license check
                requestReload();
            }
        }
    }

    /**
     * Ensure that the next time a license check is performed, the license will be reloaded from the database.
     */
    private void requestReload() {
        synchronized (this) {
            checkCount = CHECKCOUNT_CHECK_NOW;
            lastCheck = TIME_CHECK_NOW;
        }
    }

    /**
     * Get the currently-installed valid license, or null if no valid license is installed or present in the database.
     * If a license is present in the database last time we checked but was not installed because it was invalid,
     * this method throws a LicenseException explaining the problem.
     *
     * @return the currently installed valid license, or null if no license is installed or present in the database.
     * @throws InvalidLicenseException if a license is present in the database but was not installed because it was invalid.
     */
    public synchronized License getCurrentLicense() throws InvalidLicenseException {
        reloadLicenseFromDatabase();
        if (license != null)
            return license;
        if (licenseLastError != null)
            throw new InvalidLicenseException(licenseLastError.getMessage(), licenseLastError);
        return null;
    }

    /**
     * Validate and install a new license, both in memory and to the database.  If the new license appears to be
     * valid it will be sent to the database and also immediately made live in this license manager.
     * <p>
     * If a new license is successfully installed, it replaces any previous license.  The previous license XML is
     * not saved anywhere and will be lost unless the administrator saved it up somewhere.
     * <p>
     * If the new license is not valid, any current license is left in place.
     *
     * @param newLicenseXml   the new license to install.  Must be a valid license file signed by a trusted issuer.
     * @throws InvalidLicenseException  if the license was not valid.
     * @throws UpdateException   if the database change could not be recorded (old license restored)
     */
    public synchronized void installNewLicense(String newLicenseXml) throws InvalidLicenseException, UpdateException {
        long oldLoadTime = this.licenseLoaded;
        License oldLicense = this.license;
        validateAndInstallLicense(newLicenseXml);

        // It was valid.  Save to db so the other cluster nodes can bask in its glory
        Exception oops;
        try {
            clusterPropertyManager.setProperty(LICENSE_PROPERTY_NAME, newLicenseXml);
            return;
        } catch (UpdateException e) {
            // Fallthrough and roll back to old license
            oops = e;
        } catch (SaveException e) {
            // Fallthrough and roll back to old license
            oops = e;
        } catch (DeleteException e) {
            // Fallthrough and roll back to old license
            oops = e;
        }

        // Roll back to the old license license
        setLicense(oldLicense);
        this.licenseLoaded = oldLoadTime;
        throw new UpdateException("New license was valid, but could not be installed: " + ExceptionUtils.getMessage(oops), oops);
    }
}
