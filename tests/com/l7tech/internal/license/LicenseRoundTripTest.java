/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Tries to generate some licenses and then parse them.
 */
public class LicenseRoundTripTest extends TestCase {
    private static Logger log = Logger.getLogger(LicenseRoundTripTest.class.getName());

    public LicenseRoundTripTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(LicenseRoundTripTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private LicenseSpec makeSpec(X509Certificate signingCert) throws Exception {
        LicenseSpec spec = new LicenseSpec(signingCert,
                                           TestDocuments.getDotNetServerPrivateKey());

        Calendar cal = Calendar.getInstance();

        spec.setDescription("Layer 7 Internal Developer License");
        spec.setStartDate(cal.getTime());
        cal.set(2100, 12, 1);
        spec.setExpiryDate(cal.getTime());
        spec.setLicenseeName("Layer 7 Developer");
        spec.setLicenseId(1001);
        spec.setLicenseeContactEmail("developers@layer7tech.com");
        spec.setHostname("*");
        spec.setIp("*");
        spec.setProduct(BuildInfo.getProductName());
        spec.setVersionMajor("3");
        spec.setVersionMinor("4");
        return spec;
    }

    public void testSignedRoundTrip() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);

        Document lic = LicenseGenerator.generateSignedLicense(spec);

        log.info("Generated signed license (raw): \n" + XmlUtil.nodeToString(lic));

        String prettyString = XmlUtil.nodeToFormattedString(lic);

        log.info("Generated signed license (pretty-printed): \n" + prettyString);

        License license = new License(prettyString, new X509Certificate[] { signingCert });
        assertTrue(license.isValidSignature());
        assertTrue(CertUtils.certsAreEqual(license.getTrustedIssuer(), signingCert)); // must match
        assertTrue(license.getTrustedIssuer() == signingCert); // in fact, must be the exact same object we passed in
                                                               // (rather than a copy, which may have come from the signature)

        assertEquals(license.getExpiryDate(), spec.getExpiryDate());
        assertEquals(license.getStartDate(), spec.getStartDate());
        assertEquals(license.getDescription(), spec.getDescription());
        assertEquals(license.getLicenseeName(), spec.getLicenseeName());

        assertTrue(license.isProductEnabled(spec.getProduct(), spec.getVersionMajor(), spec.getVersionMinor()));
        assertTrue(license.isHostnameEnabled(spec.getHostname()));
        assertTrue(license.isIpEnabled(spec.getIp()));

        license.checkValidity();
    }

    public void testExpiredLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis() - 50000);
        spec.setExpiryDate(cal.getTime());
        Document lic = LicenseGenerator.generateSignedLicense(spec);
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Generated signed license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Expired license considered valid");
        } catch (InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testUnsignedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Document lic = LicenseGenerator.generateUnsignedLicense(spec);
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Generated unsigned license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Unsigned license considered valid");
        } catch (InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testPostDatedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis() + 500000000000L);
        spec.setStartDate(cal.getTime());
        Document lic = LicenseGenerator.generateSignedLicense(spec);
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Generated signed license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Not-yet-valid license considered valid");
        } catch (InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }
}
