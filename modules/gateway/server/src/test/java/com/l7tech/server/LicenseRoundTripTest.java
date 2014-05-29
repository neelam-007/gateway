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
        doTestSignedRoundTrip(LICENSE_PLAIN, false, 45, 359 );
    }

    @Test
    public void testSignedRoundTripWithFeatureSets() throws Exception {
        doTestSignedRoundTrip(LICENSE_FEATS, true, 46, 117 );
    }

    public void doTestSignedRoundTrip( final String licenseXmlStr,
                                       final boolean includeFeatureSets,
                                       final int licenseSecs,
                                       final int licenseMillis ) throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();

        Document lic = XmlUtil.stringAsDocument( licenseXmlStr );

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

        assertTrue(license.isProductEnabled(BuildInfo.getProductName(), "3", "4"));
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
        Document lic = XmlUtil.stringAsDocument( LICENSE_EXPIRED );
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
        Document lic = XmlUtil.stringAsDocument( LICENSE_POSTDATED );
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

    private static final String LICENSE_PLAIN = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2008-05-07T01:48:45.359Z</valid><expires>2101-01-02T02:48:45.359Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#1001\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>mLwOQKJLNKyz6byUOrAjErd22vo=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>VFt3rCcVAiJpa42aOfRCXGyekvok+syFL/eaA1cNfpkZRBtukwf/fdVgi6alluRh0UZNUJ9o9VnKLeg1nSyT2osL32l1BQ/sam5XTq32BB6LbaYcIZaDi/fsY2kzBXKq1i34l5RRkqM1oXrUMJgPBCsN9k+O8XXmTQpVWxVw0oE=</ds:SignatureValue><ds:KeyInfo><ds:KeyName>CN=Bob, OU=OASIS Interop Test Cert, O=OASIS</ds:KeyName></ds:KeyInfo></ds:Signature></license>";
    private static final String LICENSE_FEATS = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2008-05-07T01:48:46.117Z</valid><expires>2101-01-02T02:48:46.117Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version><featureset name=\"set:Profile:IPS\"></featureset><featureset name=\"service:SnmpQuery\"></featureset><featureset name=\"assertion:JmsRouting\"></featureset></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#1001\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>18apgVE2e/gQAvnmsacDwh/pDIo=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>Nptok2FGvWeXzHL/AZzQ/oVa/dSaq3VMpxRHdY7sil6jmeQMy609AHVgaR73fPhrEfbi5HkS8yOMKgmp2CGvmYXDLvO9zmxHlevJYNCHXQWn5pgVSdX+t9yQYmEPd3PNYSUKiVgWMpHOMC8BsdMJbLEYiaS16LbLpHbefR6LDVs=</ds:SignatureValue><ds:KeyInfo><ds:KeyName>CN=Bob, OU=OASIS Interop Test Cert, O=OASIS</ds:KeyName></ds:KeyInfo></ds:Signature></license>";
    private static final String LICENSE_EXPIRED = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2008-05-07T02:07:33.980Z</valid><expires>2008-05-07T02:06:43.980Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version><featureset name=\"set:Profile:IPS\"></featureset><featureset name=\"service:SnmpQuery\"></featureset><featureset name=\"assertion:JmsRouting\"></featureset></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#1001\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>HR0zlPWhOF26iTo3pi2ku0x7Zu8=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>obO3tpeJ+rA7FaYjg0eH6JgJkic7tcg4CyqfGtAGTfXG/mdg5kXW3boqHHDCvdplTpCKU47zCsGdsN9FqXRp9c7KoyG9gGBkQdzUfQX4Ok5U7mve0o5inACsh8BeDkgHsp3uCs0OCOALQaOxlvTjNtj9C0YsUiNbMcrC1sTWavc=</ds:SignatureValue><ds:KeyInfo><ds:KeyName>CN=Bob, OU=OASIS Interop Test Cert, O=OASIS</ds:KeyName></ds:KeyInfo></ds:Signature></license>";
    private static final String LICENSE_UNSIGNED = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2008-05-07T02:07:34.016Z</valid><expires>2101-01-02T03:07:34.016Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version><featureset name=\"set:Profile:IPS\"></featureset><featureset name=\"service:SnmpQuery\"></featureset><featureset name=\"assertion:JmsRouting\"></featureset></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext></license>";
    private static final String LICENSE_POSTDATED = "<license xmlns=\"http://l7tech.com/license\" Id=\"1001\"><description>Layer 7 Internal Developer License</description><licenseAttributes></licenseAttributes><valid>2024-03-11T03:00:54.025Z</valid><expires>2101-01-02T03:07:34.025Z</expires><host name=\"*\"></host><ip address=\"*\"></ip><product name=\"Layer 7 SecureSpan Suite\"><version major=\"3\" minor=\"4\"></version><featureset name=\"set:Profile:IPS\"></featureset><featureset name=\"service:SnmpQuery\"></featureset><featureset name=\"assertion:JmsRouting\"></featureset></product><licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"></licensee><eulatext>H4sIAAAAAAAAACvIKE3Ozk9LAwDHKYzMCAAAAA==</eulatext><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#1001\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>A3d+w/89n4c7qV9Pr9cSUhQnUiQ=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>eRO2hraQ/3DYEGvjpFEbIHVQEhm1uqJ2lv9a7FG+KgT1rYSmb9GL93MC7Vm1LeweR+zX7gopY/6hqb3/Y1BF0Yo/hp1hbbTgGriqT6f2615bfGDyddYRbxmz+qL5TXnhSGeKwTH7au1/o6Cqr9pkH0PGPB7WQWiM4gXEsULW0eQ=</ds:SignatureValue><ds:KeyInfo><ds:KeyName>CN=Bob, OU=OASIS Interop Test Cert, O=OASIS</ds:KeyName></ds:KeyInfo></ds:Signature></license>";
}
