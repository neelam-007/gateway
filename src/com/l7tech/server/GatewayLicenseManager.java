/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.Feature;
import com.l7tech.common.License;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.objectmodel.FindException;
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
            // TODO replace this with encrypted version of whatever cert we're supposed to use for signing licenses
            byte[] buf = ("-----BEGIN CERTIFICATE-----\n" +
                "MIICBjCCAW+gAwIBAgIIKbOOqzbBdVAwDQYJKoZIhvcNAQEFBQAwFTETMBEGA1UE\n" +
                "AxMKcm9vdC5yaWtlcjAeFw0wNDAzMjQyMzAyMTFaFw0wNjAzMjQyMzEyMTFaMBAx\n" +
                "DjAMBgNVBAMTBXJpa2VyMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6OKq/\n" +
                "fMQAnsN5JQnZ9QqTBimVpnGQA5WAeuxqkTL3iLLvWMAhdyUJsCvAzc1S1zZ4yVPZ\n" +
                "kkE9emTLeIMXtt8ae9UejWAwPsVgRIR4zAiciYCkiSO8odchOYxgp2uRZbzk5OpE\n" +
                "egTO0Gbfdkitdk285I9yNarJLC9FywOH3Kq6fQIDAQABo2QwYjAPBgNVHRMBAf8E\n" +
                "BTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwHQYDVR0OBBYEFHVi/P0XVUNE93zBx0wD\n" +
                "/80kkT7wMB8GA1UdIwQYMBaAFB+jwNtU61J/yk++WodvTbQrBux2MA0GCSqGSIb3\n" +
                "DQEBBQUAA4GBAB5/WyqIb7QLot5Xbh+MIlrTwoXtjHwnqRPrb8YkxARdhh4hB+63\n" +
                "2yMLDv90ReZg41kXZ1Lxd/lh8PaNFlSFerCg/6stx3xIbU4eT3WW9ws8ohL7NaaH\n" +
                "gW3Yqo4e6aqxJ5T9NEfW6CQEdE32AZfDz6HLzWdcj0Gnqp3yxOSenf2T\n" +
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
                updateLicense();
                lastCheck = System.currentTimeMillis();
            }
        }
    }

    /** (Re)read the license from the cluster property table. */
    private synchronized void updateLicense() {
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

        final License license;
        try {
            license = new License(licenseXml, getTrustedIssuers());
            license.checkValidity();
        } catch (Exception e) {
            fireEvent(SystemMessages.LICENSE_INVALID, e.getMessage());
            setLicense(null);
            this.licenseLoaded = System.currentTimeMillis();
            return;
        }

        setLicense(license);
        this.licenseLoaded = System.currentTimeMillis();
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
        if (licenseSet && (this.license == license || (license != null && license.equals(this.license))))
            return;
        this.license = license;
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
                synchronized (this) {
                    checkCount = CHECKCOUNT_CHECK_NOW;
                    lastCheck = TIME_CHECK_NOW;
                }
            }
        }
    }
}
