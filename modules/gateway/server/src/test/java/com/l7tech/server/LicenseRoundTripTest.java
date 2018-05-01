/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.util.BuildInfo;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.InvalidLicenseException;
import org.junit.Test;
import static org.junit.Assert.*;

import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Tries to generate some licenses and then parse them.
 */
public class LicenseRoundTripTest {
    private static Logger log = Logger.getLogger(LicenseRoundTripTest.class.getName());

    @Test
    public void testSignedRoundTrip() throws Exception {
        doTestSignedRoundTrip(TestDocuments.LICENSE_PLAIN, false, 45, 359 );
    }

    @Test
    public void testSignedRoundTripWithFeatureSets() throws Exception {
        doTestSignedRoundTrip(TestDocuments.LICENSE_FEATS, true, 46, 117 );
    }

    public void doTestSignedRoundTrip( final String licenseXmlPath,
                                       final boolean includeFeatureSets,
                                       final int licenseSecs,
                                       final int licenseMillis ) throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();

        Document lic = TestDocuments.getTestDocument(licenseXmlPath);

        log.info("Generated signed license (raw): \n" + XmlUtil.nodeToString(lic));

        String prettyString = XmlUtil.nodeToFormattedString(lic);

        log.info("Generated signed license (pretty-printed): \n" + prettyString);

        License license = new License(prettyString, new X509Certificate[] { signingCert }, GatewayFeatureSets.getFeatureSetExpander());
        assertTrue(license.isValidSignature());
        assertTrue(CertUtils.certsAreEqual(license.getTrustedIssuer(), signingCert)); // must match
        assertTrue(license.getTrustedIssuer() == signingCert); // in fact, must be the exact same object we passed in
                                                               // (rather than a copy, which may have come from the signature)

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ));
        calendar.set( Calendar.MILLISECOND, licenseMillis );
        calendar.set( 2008, 4, 7, 1, 48, licenseSecs );
        Date startDate = calendar.getTime();
        calendar.set( 2101, 0, 2, 2, 48, licenseSecs );
        Date expiryDate = calendar.getTime();

        assertEquals(startDate, license.getStartDate());
        assertEquals(expiryDate, license.getExpiryDate());
        assertEquals("Layer 7 Internal Developer License", license.getDescription());
        assertEquals("Layer 7 Developer", license.getLicenseeName());
        assertEquals("developers@layer7tech.com", license.getLicenseeContactEmail());
        assertEquals("phuckoff", license.getEulaText());

        assertTrue(license.isProductEnabled(BuildInfo.getProductName(), "3", "4")  || license.isProductEnabled(BuildInfo.getLegacyProductName(), "3", "4"));
        assertTrue(license.isHostnameEnabled("*"));
        assertTrue(license.isIpEnabled("*"));

        if ( includeFeatureSets ) {
            // Must enable the ones we gave it
            assertTrue(license.isFeatureEnabled("set:Profile:IPS"));
            assertTrue(license.isFeatureEnabled("service:SnmpQuery"));
            assertTrue(license.isFeatureEnabled("assertion:JmsRouting"));

            // Must enable the leaf features implied by the ones we gave it
            assertTrue(license.isFeatureEnabled("assertion:composite.All"));
            assertTrue(license.isFeatureEnabled("assertion:OversizedText"));

            // Must not enable any other features
            assertFalse(license.isFeatureEnabled("service:Bridge"));
            assertFalse(license.isFeatureEnabled("blarf:" + new Random().nextInt()));
        }

        license.checkValidity();
    }

    @Test
    public void testExpiredLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        Document lic = TestDocuments.getTestDocument(TestDocuments.LICENSE_EXPIRED);
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Testing expired signed license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert}, GatewayFeatureSets.getFeatureSetExpander());
        try {
            license.checkValidity();
            fail("Expired license considered valid");
        } catch ( InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    @Test
    public void testUnsignedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        Document lic = XmlUtil.stringAsDocument( LICENSE_UNSIGNED );
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Testing unsigned license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert}, GatewayFeatureSets.getFeatureSetExpander());
        try {
            license.checkValidity();
            fail("Unsigned license considered valid");
        } catch (InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    @Test
    public void testPostDatedLicense() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        Document lic = TestDocuments.getTestDocument(TestDocuments.LICENSE_POSTDATED);
        final String licenseXml = XmlUtil.nodeToFormattedString(lic);
        log.info("Testing postdated signed license: " + licenseXml);
        License license = new License(licenseXml, new X509Certificate[] {signingCert}, GatewayFeatureSets.getFeatureSetExpander());
        try {
            license.checkValidity();
            fail("Not-yet-valid license considered valid");
        } catch (InvalidLicenseException e) {
            // Ok
            log.info("The expected exception was thrown: " + e.getMessage());
        }
    }

    private static final String LICENSE_UNSIGNED = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2008-05-07T02:07:34.016Z</valid><expires>2101-01-02T03:07:34.016Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version><featureset name=\"set:Profile:IPS\"></featureset><featureset name=\"service:SnmpQuery\"></featureset><featureset name=\"assertion:JmsRouting\"></featureset></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext></license>";
}
