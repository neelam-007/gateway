/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Calendar;
import java.security.cert.X509Certificate;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.License;
import com.l7tech.common.LicenseException;
import org.w3c.dom.Document;

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

        spec.setDescription("Simple signed demo license");
        spec.setStartDate(cal.getTime());
        cal.set(2100, 12, 1);
        spec.setExpiryDate(cal.getTime());
        spec.setLicenseeName("Demo User");
        spec.setLicenseId(1001);
        spec.setLicenseeContactEmail("nomailbox@NOWHERE");
        spec.setHostname("blah.blee.bloo");
        spec.setIp("1.2.3.4");
        spec.setProduct("SecureSpan BaloneyMaker");
        spec.setVersionMajor("3");
        spec.setVersionMinor("*");
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
        assertEquals(license.getHostname(), spec.getHostname());
        assertEquals(license.getIp(), spec.getIp());
        assertEquals(license.getProduct(), spec.getProduct());
        assertEquals(license.getLicenseeName(), spec.getLicenseeName());
        assertEquals(license.getVersionMajor(), spec.getVersionMajor());
        assertEquals(license.getVersionMinor(), spec.getVersionMinor());
        license.checkValidity();
    }

    public void testExpiredLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis() - 50000);
        spec.setExpiryDate(cal.getTime());
        Document lic = LicenseGenerator.generateSignedLicense(spec);
        License license = new License(XmlUtil.nodeToString(lic), new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Expired license considered valid");
        } catch (LicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testUnsignedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Document lic = LicenseGenerator.generateUnsignedLicense(spec);
        License license = new License(XmlUtil.nodeToString(lic), new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Unsigned license considered valid");
        } catch (LicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testPostDatedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        LicenseSpec spec = makeSpec(signingCert);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis() + 50000);
        spec.setStartDate(cal.getTime());
        Document lic = LicenseGenerator.generateSignedLicense(spec);
        License license = new License(XmlUtil.nodeToString(lic), new X509Certificate[] {signingCert});
        try {
            license.checkValidity();
            fail("Not-yet-valid license considered valid");
        } catch (LicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }
}
