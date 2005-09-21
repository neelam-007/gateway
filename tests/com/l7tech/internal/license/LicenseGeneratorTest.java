/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license;

import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.SimpleCertificateResolver;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Test the license generator to see if it can emit a document.  The emitted license isn't actually parsed --
 * for that, see LicenseRoundTripTest.  Instead, the output is just spot checked to make sure it looks sane.
 */
public class LicenseGeneratorTest extends TestCase {
    private static Logger log = Logger.getLogger(LicenseGeneratorTest.class.getName());

    public LicenseGeneratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(LicenseGeneratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testInvalid() throws Exception {
        LicenseSpec spec = new LicenseSpec();

        try {
            LicenseGenerator.generateUnsignedLicense(spec); // no id
            fail("Expected exception was not thrown");
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            // ok
        }

        spec.setLicenseId(1);

        try {
            LicenseGenerator.generateUnsignedLicense(spec); // no licensee
            fail("Expected exception was not thrown");
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            // ok
        }

        spec.setLicenseeName("Blah Blah");

        // Should work OK now
        LicenseGenerator.generateUnsignedLicense(spec);

        spec.setLicenseId(0);

        try {
            LicenseGenerator.generateUnsignedLicense(spec); // non-positive license id
            fail("Expected exception was not thrown");
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            // ok
        }

        spec.setLicenseId(1);
        spec.setLicenseeName("");

        try {
            LicenseGenerator.generateUnsignedLicense(spec); // empty licensee name
            fail("Expected exception was not thrown");
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            // ok
        }

        try {
            LicenseGenerator.generateSignedLicense(spec); // missing cert and key for signing
            fail("Expected exception was not thrown");
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            // ok
        }

    }

    public void testSimpleUnsigned() throws Exception {
        LicenseSpec spec = new LicenseSpec();

        Calendar cal = Calendar.getInstance();

        spec.setDescription("Simple unsigned demo license");
        spec.setStartDate(cal.getTime());
        cal.set(2100, 12, 1);
        spec.setExpiryDate(cal.getTime());
        spec.setLicenseeName("Demo User");
        spec.setLicenseId(1001);
        spec.setLicenseeContactEmail("nomailbox@NOWHERE");

        Document lic = LicenseGenerator.generateUnsignedLicense(spec);

        log.info("Generated unsigned license (pretty-printed): \n" + XmlUtil.nodeToFormattedString(lic));

    }

    public void testSimpleSigned() throws Exception {
        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
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

        Document lic = LicenseGenerator.generateSignedLicense(spec);

        log.info("Generated unsigned license (pretty-printed): \n" + XmlUtil.nodeToFormattedString(lic));

        // Check the signature
        Element sigElement = XmlUtil.findOnlyOneChildElementByName(lic.getDocumentElement(), SoapUtil.DIGSIG_URI, "Signature");
        assertNotNull(sigElement);
        DsigUtil.checkSimpleSignature(sigElement, new SimpleCertificateResolver(signingCert));
        log.info("Signature validated successfully.");

    }


}
