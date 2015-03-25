package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

/**
 *
 */
public class X509CertificateAttributesExtractorTest {
    /** Base OID of Layer 7 Technologies (IANA-registered Private Enterprise) */
    private static final String OID_LAYER_7_BASE = "1.3.6.1.4.1.17304";

    /** Layer 7 test OIDs */
    private static final String OID_LAYER_7_TEST = OID_LAYER_7_BASE + ".99";

    @Test
    public void testEmailFromIssuerDN() throws Exception {
        X509Certificate certificate = CertUtils.decodeCert(HexUtils.decodeBase64(THAWTE_CERT_PEM));

        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(certificate);
        for ( String name : cae.getSuppotedAttributeNames() ) {
            System.out.println( name + " = " + CertificateAttribute.attributeValueToString(cae.getAttributeValue(name) ));
        }

        assertEquals( "Serial Number", "10", cae.getAttributeValue("serial") );
        assertEquals( "Not Before Date", "2003-08-06T00:00:00.000Z", cae.getAttributeValue("notBefore") );
        assertEquals( "Not After Date", "2013-08-05T23:59:59.000Z", cae.getAttributeValue("notAfter") );
        assertEquals( "Signature Algorithm Name", "SHA1withRSA", cae.getAttributeValue("signatureAlgorithmName") );
        assertEquals( "Signature Algorithm OID", "1.2.840.113549.1.1.5", cae.getAttributeValue("signatureAlgorithmOID") );
        assertEquals( "Issuer DN", "EMAILADDRESS=premium-server@thawte.com, CN=Thawte Premium Server CA, OU=Certification Services Division, O=Thawte Consulting cc, L=Cape Town, ST=Western Cape, C=ZA", cae.getAttributeValue("issuer") );
//        assertEquals( "Email from Issuer DN", "premium-server@thawte.com", cae.getAttributeValue("issuerEmail") );
        assertEquals( "Subject DN", "CN=Thawte Code Signing CA, O=Thawte Consulting (Pty) Ltd., C=ZA", cae.getAttributeValue("subject") );
        assertEquals( "Subject Public Key Algorithm", "RSA", cae.getAttributeValue("subjectPublicKeyAlgorithm") );

        for ( String name : cae.getSuppotedAttributeNames() ) {
            CertificateAttribute attribute = CertificateAttribute.fromString(name);
            assertNotNull( "Unknown attribute for name: " + name, attribute );
        }
    }

    @Test
    public void testEmailsFromAltNames() throws Exception {

        String altNameTest =
                "MIIDpjCCAo6gAwIBAgIBAjANBgkqhkiG9w0BAQUFADA/MRMwEQYKCZImiZPyLGQB\n" +
                "GRYDb3JnMRUwEwYKCZImiZPyLGQBGRYFdWdyaWQxETAPBgNVBAMTCFVHUklEIENB\n" +
                "MB4XDTA4MDIxNTE2NTczNVoXDTA5MDIxNDE2NTczNVoweDETMBEGCgmSJomT8ixk\n" +
                "ARkWA29yZzEVMBMGCgmSJomT8ixkARkWBXVncmlkMQ8wDQYDVQQKEwZwZW9wbGUx\n" +
                "DjAMBgNVBAoTBVVHUklEMQswCQYDVQQLEwJDQTEcMBoGA1UEAxMTU2VyZ2l5IFZl\n" +
                "bGljaGtldnljaDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAvY3377HOdna1\n" +
                "3qStiDRJLxviFR13de3Dv/JnWX0jnryoTJlbXQHYQ/G07Y9ViKNz6UboBEGr3Cgh\n" +
                "Hyakqu+Xtzw/nlY42k+ivOTbVx3zbAE2SqVaylJ1dm7MVLhvI6yCIyyWxR1/Fvuc\n" +
                "5lU+F5TWro2Nynzh9czOFhUJ4MLbI7cCAwEAAaOB9zCB9DAMBgNVHRMBAf8EAjAA\n" +
                "MA4GA1UdDwEB/wQEAwIE8DATBgNVHSUEDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQU\n" +
                "2yfrus8cbtAJVqeBfYLpTkBkDNEwHwYDVR0jBBgwFoAUzcDX4bV9n6mUSE7oFFZV\n" +
                "lO3/vKAwFwYDVR0SBBAwDoEMY2FAdWdyaWQub3JnMBkGA1UdEQQSMBCBDnNlcmdA\n" +
                "dWdyaWQub3JnMC4GA1UdHwQnMCUwI6AhoB+GHWh0dHA6Ly9jYS51Z3JpZC5vcmcv\n" +
                "Y2FjcmwuZGVyMBsGA1UdIAQUMBIwEAYOKoZIhvdMBQQCBgEBAQQwDQYJKoZIhvcN\n" +
                "AQEFBQADggEBABLTjNrC5pAmIm1WpPNFHUbdvv0nZbRiKgVlPD9vrTvgVIvi2Mn8\n" +
                "nlL74Jv/bXGndn5UPVwB0pqgnbLtOtd5IZlPCYz8midGlwi9Eb8mI6zSKzp70P9x\n" +
                "isxZez71AObSPo4HywhJ1xBJufDSSNgEmeo/rKBiviWaUAc/JdCHm8jUD0DCp2EG\n" +
                "7ruXlFy/9lZmmCfWXqmmdEJ7t0VBJQtp+HAGpHB8rPsLYktD4vVFAywP1ObN6evh\n" +
                "nAFMphW/iey8bws26egN0pYOap+T6e3PR86uCq/cY0YYyJN/nHvOXvnJ/elPwF/P\n" +
                "9jNX9q/OiImcwP3k/It/b1e/tICT/trAA34=";

        X509Certificate certificate = CertUtils.decodeCert(HexUtils.decodeBase64(altNameTest));

        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(certificate);
        for ( String name : cae.getSuppotedAttributeNames() ) {
            System.out.println( name + " = " + CertificateAttribute.attributeValueToString(cae.getAttributeValue(name)) );
        }

        assertEquals( "Serial Number", "2", cae.getAttributeValue("serial") );
        assertEquals( "Not Before Date", "2008-02-15T16:57:35.000Z", cae.getAttributeValue("notBefore") );
        assertEquals( "Not After Date", "2009-02-14T16:57:35.000Z", cae.getAttributeValue("notAfter") );
        assertEquals( "Signature Algorithm Name", "SHA1withRSA", cae.getAttributeValue("signatureAlgorithmName") );
        assertEquals( "Signature Algorithm OID", "1.2.840.113549.1.1.5", cae.getAttributeValue("signatureAlgorithmOID") );
        assertEquals( "Issuer DN", "CN=UGRID CA, DC=ugrid, DC=org", cae.getAttributeValue("issuer") );
        assertEquals( "Issuer Alternative Name Email", "ca@ugrid.org", cae.getAttributeValue("issuerAltNameEmail") );
        assertEquals( "Subject DN", "CN=Sergiy Velichkevych, OU=CA, O=UGRID, O=people, DC=ugrid, DC=org", cae.getAttributeValue("subject") );
        assertEquals( "Subject Alternative Name Email", "serg@ugrid.org", cae.getAttributeValue("subjectAltNameEmail") );
        assertEquals( "Subject Public Key Algorithm", "RSA", cae.getAttributeValue("subjectPublicKeyAlgorithm") );
        assertEquals( "Subject Public Key", "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC9jffvsc52drXepK2INEkvG+IVHXd17cO/8mdZfSOevKhMmVtdAdhD8bTtj1WIo3PpRugEQavcKCEfJqSq75e3PD+eVjjaT6K85NtXHfNsATZKpVrKUnV2bsxUuG8jrIIjLJbFHX8W+5zmVT4XlNaujY3KfOH1zM4WFQngwtsjtwIDAQAB",
                cae.getAttributeValue( "subjectPublicKey" ) );

        for ( String name : cae.getSuppotedAttributeNames() ) {
            CertificateAttribute attribute = CertificateAttribute.fromString(name);
            assertNotNull( "Unknown attribute for name: " + name, attribute );
        }
    }

    @Test
    public void testSimpleDnParsing() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();
        X509Certificate cert = gen.subject("cn=blah, ou=foo, ou=bar, ou=baz, c=ca").generate();

        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Simple CN", "blah", ((Object[])cae.getAttributeValue("subject.dn.cn"))[0]);

        final Object subjectOu = cae.getAttributeValue("subject.dn.ou");
        assertTrue("Multiple OU RDNs result in a multivalued subject.ou attribute", subjectOu instanceof Object[]);
        Object[] ous = (Object[]) subjectOu;
        assertEquals("OU sizes", 3, ous.length);
        assertEquals("OU value 0", "foo", ous[0]);
        assertEquals("OU value 1", "bar", ous[1]);
        assertEquals("OU value 2", "baz", ous[2]);

        assertEquals("DN by index retrieval", "C=ca", ((Object[])cae.getAttributeValue("subject.dn.1"))[0]);
        assertEquals("DN by index retrieval", "OU=baz", ((Object[])cae.getAttributeValue("subject.dn.2"))[0]);
        assertEquals("DN by index retrieval", "OU=bar", ((Object[])cae.getAttributeValue("subject.dn.3"))[0]);
        assertEquals("DN by index retrieval", "OU=foo", ((Object[])cae.getAttributeValue("subject.dn.4"))[0]);
        assertEquals("DN by index retrieval", "CN=blah", ((Object[])cae.getAttributeValue("subject.dn.5"))[0]);
    }

    @Test
    public void testDefaultDn() throws Exception {
        X509Certificate cert = CertUtils.decodeCert(HexUtils.decodeBase64(THAWTE_CERT_PEM));
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Default form issuer DN",
                "EMAILADDRESS=premium-server@thawte.com, CN=Thawte Premium Server CA, OU=Certification Services Division, O=Thawte Consulting cc, L=Cape Town, ST=Western Cape, C=ZA",
                cae.getAttributeValue("issuer"));
    }

    @Test
    public void testCanonicalFormDn() throws Exception {
        X509Certificate cert = CertUtils.decodeCert(HexUtils.decodeBase64(THAWTE_CERT_PEM));
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Canonical form issuer DN",
                "1.2.840.113549.1.9.1=#16197072656d69756d2d736572766572407468617774652e636f6d,cn=thawte premium server ca,ou=certification services division,o=thawte consulting cc,l=cape town,st=western cape,c=za",
                cae.getAttributeValue("issuer.canonical"));
    }

    @Test
    public void testRfc2253FormDn() throws Exception {
        X509Certificate cert = CertUtils.decodeCert(HexUtils.decodeBase64(THAWTE_CERT_PEM));
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("RFC 2253 form issuer DN",
                "1.2.840.113549.1.9.1=#16197072656d69756d2d736572766572407468617774652e636f6d,CN=Thawte Premium Server CA,OU=Certification Services Division,O=Thawte Consulting cc,L=Cape Town,ST=Western Cape,C=ZA",
                cae.getAttributeValue("issuer.rfc2253"));
    }

    @Test
    public void testDnAttributeUnrecognizedDnAttributeOid() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();
        String thawteEmailBer = "#16197072656d69756d2d736572766572407468617774652e636f6d"; // (Thawte's email address)
        String testOid = OID_LAYER_7_TEST + ".1.1";
        X509Certificate cert = gen.subject("cn=blah," + testOid + "=" + thawteEmailBer).generate();

        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Layer 7-specific DN attribute OID", thawteEmailBer, ((Object[])cae.getAttributeValue("subject.dn." + testOid))[0]);
    }

    @Test
    public void testCountriesOfCitizenship() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();
        X509Certificate cert = gen.countriesOfCitizenship(false, "CA", "US", "FR").generate();

        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);

        final Object countriesOfCitizenship = cae.getAttributeValue("countryOfCitizenship");
        assertTrue("Countries of citizenship returns an array", countriesOfCitizenship instanceof Object[]);
        Object[] citizenships = (Object[]) countriesOfCitizenship;
        assertEquals("citizenships", 3, citizenships.length);
        assertEquals("CA", citizenships[0]);
        assertEquals("US", citizenships[1]);
        assertEquals("FR", citizenships[2]);
    }

    @Test
    @BugNumber(10211)
    public void testDodCertCountryOfCitizenShipAndSubjectAltName() throws Exception {
        X509Certificate cert = CertUtils.decodeFromPEM(ALT_NAME_CERT_B64, false);
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);

        assertNull("otherName not exposed as subjectAltNameDNS", cae.getAttributeValue(CertificateAttribute.SUBJECT_ALT_DNS.toString()));
        assertNull("otherName not exposed as subjectAltNameEmail", cae.getAttributeValue(CertificateAttribute.SUBJECT_ALT_EMAIL.toString()));
        assertNull("otherName not exposed as subjectAltNameURI", cae.getAttributeValue(CertificateAttribute.SUBJECT_ALT_URI.toString()));
        assertEquals("otherName exposed as subjectAltNameOther, Base-64 encoded", "MCcGCisGAQQBgjcUAgOgGaAXDBUxNzAwMDAwMDAwLlZAc21pbC5taWw=", cae.getAttributeValue(CertificateAttribute.SUBJECT_ALT_OTHER.toString()));

        // Recognized as to "1.3.6.1.5.5.7.9.4" (id-pda-countryOfCitizenship), or as "2.5.4.6" (X.509 Name "C" country code).  Update test if this is later modified.
        final Object countriesOfCitizenship = cae.getAttributeValue("countryOfCitizenship");
        assertTrue("Countries of citizenship returns an array", countriesOfCitizenship instanceof Object[]);
        Object[] citizenships = (Object[]) countriesOfCitizenship;
        assertEquals("citizenships should have 1 entry", 1, citizenships.length);
        assertEquals("country of citizenship should be US", "US", citizenships[0]);
    }

    @Test
    public void testKeyUsage() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();

        X509Certificate cert = gen.reset().noExtensions().keyUsage(false, 22).generate();
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Key usage non-critical", "noncrit", cae.getAttributeValue("keyUsageCriticality"));
        assertFalse("Bit 1 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.digitalSignature"));
        assertFalse("Bit 2 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.nonRepudiation"));
        assertFalse("Bit 3 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyEncipherment"));
        assertTrue("Bit 4 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.dataEncipherment"));
        assertFalse("Bit 5 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyAgreement"));
        assertTrue("Bit 6 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyCertSign"));
        assertTrue("Bit 7 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.crlSign"));
        assertFalse("Bit 8 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.encipherOnly"));
        assertFalse("Bit 9 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.decipherOnly"));

        cert = gen.reset().noExtensions().generate();
        cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Key usage not present", "none", cae.getAttributeValue("keyUsageCriticality"));
        assertFalse("Bit 1 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.digitalSignature"));
        assertFalse("Bit 2 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.nonRepudiation"));
        assertFalse("Bit 3 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.keyEncipherment"));
        assertFalse("Bit 4 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.dataEncipherment"));
        assertFalse("Bit 5 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.keyAgreement"));
        assertFalse("Bit 6 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.keyCertSign"));
        assertFalse("Bit 7 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.crlSign"));
        assertFalse("Bit 8 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.encipherOnly"));
        assertFalse("Bit 9 of the key usage is false if ext not present", (Boolean)cae.getAttributeValue("keyUsage.decipherOnly"));

        cert = gen.reset().noExtensions().keyUsage(true, 32791).generate();
        cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Key usage critical", "critical", cae.getAttributeValue("keyUsageCriticality"));
        assertFalse("Bit 1 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.digitalSignature"));
        assertFalse("Bit 2 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.nonRepudiation"));
        assertFalse("Bit 3 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyEncipherment"));
        assertTrue("Bit 4 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.dataEncipherment"));
        assertFalse("Bit 5 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyAgreement"));
        assertTrue("Bit 6 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.keyCertSign"));
        assertTrue("Bit 7 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.crlSign"));
        assertTrue("Bit 8 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.encipherOnly"));
        assertTrue("Bit 9 of the key usage", (Boolean)cae.getAttributeValue("keyUsage.decipherOnly"));

    }

    @Test
    public void testExtKeyUsage() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();

        X509Certificate cert = gen.reset().noExtensions().extKeyUsage(false, "1.3.6.1.5.5.7.3.13", "1.3.6.1.5.5.7.3.14").generate();
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Ext Key usage non-critical", "noncrit", cae.getAttributeValue("extendedKeyUsageCriticality"));
        Object oidsObject = cae.getAttributeValue("extendedKeyUsageValues");
        assertTrue("Extended key usage returns an array", oidsObject instanceof Object[]);
        Object[] oids = (Object[])oidsObject;
        assertEquals("OIDs", 2, oids.length);
        assertEquals("1.3.6.1.5.5.7.3.13", oids[0]);
        assertEquals("1.3.6.1.5.5.7.3.14", oids[1]);

        cert = gen.reset().noExtensions().extKeyUsage(true, "1.3.6.1.5.5.7.3.13", "1.3.6.1.5.5.7.3.14").generate();
        cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Ext Key usage critical", "critical", cae.getAttributeValue("extendedKeyUsageCriticality"));
        oidsObject = cae.getAttributeValue("extendedKeyUsageValues");
        assertTrue("Extended key usage returns an array", oidsObject instanceof Object[]);
        oids = (Object[])oidsObject;
        assertEquals("OIDs", 2, oids.length);
        assertEquals("1.3.6.1.5.5.7.3.13", oids[0]);
        assertEquals("1.3.6.1.5.5.7.3.14", oids[1]);

        cert = gen.reset().noExtensions().generate();
        cae = new X509CertificateAttributesExtractor(cert);
        assertEquals("Ext Key usage not present", "none", cae.getAttributeValue("extendedKeyUsageCriticality"));
        oidsObject = cae.getAttributeValue("extendedKeyUsageValues");
        assertTrue("Extended key usage returns an array", oidsObject instanceof Object[]);
        oids = (Object[])oidsObject;
        assertEquals("eku not prsent; OIDs empty", 0, oids.length);
    }

    @Test
    @BugNumber(10644)
    public void testDNEmail() throws Exception {
        X509Certificate cert = CertUtils.decodeFromPEM(EMAIL_IN_DN_CERT_B64, false);
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        assertArrayEquals( "issuerEmail", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "issuerEmail" ) );
        assertArrayEquals( "issuer.dn.emailaddress", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "issuer.dn.emailaddress" ) );
        assertArrayEquals( "issuer.dn.7.emailaddress", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "issuer.dn.7.emailaddress" ) );
        assertArrayEquals( "subjectEmail", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "subjectEmail" ) );
        assertArrayEquals( "subject.dn.emailaddress", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "subject.dn.emailaddress" ) );
        assertArrayEquals( "subject.dn.7.emailaddress", new Object[]{"jdy@layer7tech.com"}, (Object[])cae.getAttributeValue( "subject.dn.7.emailaddress" ) );
    }

    @Test
    public void testLegacyNames() throws Exception {
        testLegacyNamesForCert( CertUtils.decodeCert(HexUtils.decodeBase64(THAWTE_CERT_PEM)) );
        testLegacyNamesForCert( CertUtils.decodeCert(HexUtils.decodeBase64(ALT_NAME_CERT_B64)) );
        testLegacyNamesForCert( CertUtils.decodeCert(HexUtils.decodeBase64(EMAIL_IN_DN_CERT_B64)) );
    }

    private void testLegacyNamesForCert( X509Certificate cert ) throws Exception {
        X509CertificateAttributesExtractor cae = new X509CertificateAttributesExtractor(cert);
        for(String legacyName : OLD_NAMES) {
            CertificateAttribute attr = CertificateAttribute.fromString(legacyName);
            assertNotNull("No certificate attribute found for previously supported name: " + legacyName, attr);

            Object legacyValues = cae.getAttributeValue(legacyName);
            Object newValues = cae.getAttributeValue(attr.getNewName(legacyName));

            if (attr.isMultiValued()) {
                assertEquals("Different values extracted for legacy/new attribute name: "+ legacyName, legacyValues == null, newValues == null);
                assertTrue("Expected Object[] value for: " + legacyName, legacyValues == null || legacyValues instanceof Object[]);
                assertTrue("Expected Object[] value for: " + attr.toString(), newValues == null || newValues instanceof Object[]);
                if (legacyValues != null && newValues != null)
                    assertArrayEquals("Different values extracted for legacy/new attribute name: " + legacyName, (Object[])legacyValues, (Object[])newValues);
            } else {
                assertEquals("Different values extracted for legacy/new attribute name: " + legacyName, newValues, legacyValues);
            }
        }
    }

    private static final String THAWTE_CERT_PEM =
            "MIIDTjCCAregAwIBAgIBCjANBgkqhkiG9w0BAQUFADCBzjELMAkGA1UEBhMCWkEx\n" +
            "FTATBgNVBAgTDFdlc3Rlcm4gQ2FwZTESMBAGA1UEBxMJQ2FwZSBUb3duMR0wGwYD\n" +
            "VQQKExRUaGF3dGUgQ29uc3VsdGluZyBjYzEoMCYGA1UECxMfQ2VydGlmaWNhdGlv\n" +
            "biBTZXJ2aWNlcyBEaXZpc2lvbjEhMB8GA1UEAxMYVGhhd3RlIFByZW1pdW0gU2Vy\n" +
            "dmVyIENBMSgwJgYJKoZIhvcNAQkBFhlwcmVtaXVtLXNlcnZlckB0aGF3dGUuY29t\n" +
            "MB4XDTAzMDgwNjAwMDAwMFoXDTEzMDgwNTIzNTk1OVowVTELMAkGA1UEBhMCWkEx\n" +
            "JTAjBgNVBAoTHFRoYXd0ZSBDb25zdWx0aW5nIChQdHkpIEx0ZC4xHzAdBgNVBAMT\n" +
            "FlRoYXd0ZSBDb2RlIFNpZ25pbmcgQ0EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJ\n" +
            "AoGBAMa4uSdgrwvjkWll236N7ZHmqvG+1e3+bdQsf9Fwd/smmVe03T8wuNwh6miN\n" +
            "gZL8LkuRNYQg8tpKurT85tqI8iDFIZIJR5WgCRymeb6xTB388YpuVNJpofFMkzpB\n" +
            "/n3UZHtjRfdgYB0xHaTp0w+L+24mJLOo/+XlkNS0wtxQYK5ZAgMBAAGjgbMwgbAw\n" +
            "EgYDVR0TAQH/BAgwBgEB/wIBADBABgNVHR8EOTA3MDWgM6Axhi9odHRwOi8vY3Js\n" +
            "LnRoYXd0ZS5jb20vVGhhd3RlUHJlbWl1bVNlcnZlckNBLmNybDAdBgNVHSUEFjAU\n" +
            "BggrBgEFBQcDAgYIKwYBBQUHAwMwDgYDVR0PAQH/BAQDAgEGMCkGA1UdEQQiMCCk\n" +
            "HjAcMRowGAYDVQQDExFQcml2YXRlTGFiZWwyLTE0NDANBgkqhkiG9w0BAQUFAAOB\n" +
            "gQB2spzuE58b9i00kpRFczTcjmsuXPxMfYnrw2jx15kPLh0XyLUWi77NigUG8hlJ\n" +
            "OgNbBckgjm1S4XaBoMNliiJn5BxTUzdGv7zXL+t7ntAURWxAIQjiXXV2ZjAe9N+C\n" +
            "ii+986IMvx3bnxSimnI3TbB3SOhKPwnOVRks7+YHJOGv7A==";

    private static final String ALT_NAME_CERT_B64 =
            "MIIFETCCA/mgAwIBAgIDFo8TMA0GCSqGSIb3DQEBBQUAMIGQMQswCQYDVQQGEwJV\n" +
            "UzEYMBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNOU1MxDDAKBgNV\n" +
            "BAsTA0RvRDEiMCAGA1UECxMZQ2VydGlmaWNhdGlvbiBBdXRob3JpdGllczEnMCUG\n" +
            "A1UEAxMeVE1TLUNBLTEgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTExMDMwOTE0\n" +
            "MTQyN1oXDTE0MDMwOTE0MTQyN1oweDELMAkGA1UEBhMCVVMxGDAWBgNVBAoTD1Uu\n" +
            "Uy4gR292ZXJubWVudDEMMAoGA1UECxMDTlNTMQwwCgYDVQQLEwNEb0QxDDAKBgNV\n" +
            "BAsTA1VTQTElMCMGA1UEAxMcVEVTVFVTLkpPSE4uSEFSUlkuMTcwMDAwMDAwMDCC\n" +
            "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIv0rry390Ob1Wn85E5dT2o2\n" +
            "GTvh8HYMwtGzn8hyz7ZDc2crbDq8GlwkZZWmy9Ok8T+NsQp+uweKnGpC86UXbYDA\n" +
            "/nSnrXUxUD83IRJ6KF8RmyznkEjmeZy27U3qPueR9Ef0p4QDobA3EiHQT78MDNB0\n" +
            "aN0R/k+cvD+ughe+LedJmpka1IfFzqAaSwnT9GFDmD8mRgWZE375EsK9Pye8qkzW\n" +
            "/1VYu12IrcE5py0+bb1UwuqipKPhV3Bokfzl2uhmItya8fSXmY/I4qzbfh6mKzXc\n" +
            "kSQIuXycM+WP0yJ1jdlxBQgfDs+aqUCsTSTf6jBnn59/ZYAvzpAi/asWnO0ZhPkC\n" +
            "AwEAAaOCAYkwggGFMB8GA1UdIwQYMBaAFB7SsZ2vfcozUGxW2zwTpKaBrkAwME0G\n" +
            "A1UdHwRGMEQwQqBAoD6GPGh0dHA6Ly9jcmwuZ2RzLm5pdC5kaXNhLm1pbC9jcmwv\n" +
            "TlNTSklUQ0RPRFNVQk9SRElOQVRFQ0ExLmNybDAOBgNVHQ8BAf8EBAMCB4AwFwYD\n" +
            "VR0gBBAwDjAMBgpghkgBZQMCARUCMB0GA1UdDgQWBBSzU9dWEVcG+IA36kxiMYGd\n" +
            "v0lKIzBgBggrBgEFBQcBAQRUMFIwUAYIKwYBBQUHMAKGRGh0dHA6Ly9jcmwuZ2Rz\n" +
            "Lm5pdC5kaXNhLm1pbC9pc3N1ZWR0by9OU1NKSVRDRE9EU1VCT1JESU5BVEVDQTFf\n" +
            "SVQucDdjMBYGA1UdCQQPMA0wCwYDVQQGMQQTAlVTMDAGA1UdEQQpMCegJQYKKwYB\n" +
            "BAGCNxQCA6AXDBUxNzAwMDAwMDAwLlZAc21pbC5taWwwHwYDVR0lBBgwFgYKKwYB\n" +
            "BAGCNxQCAgYIKwYBBQUHAwIwDQYJKoZIhvcNAQEFBQADggEBAGkOHcAv25n8rkoo\n" +
            "v2lAcENZ/AUICTqsnQ5C14ji+ijAxu0+wSvCWP5AOUcdrdk9zEXeJatZh8HVpdkk\n" +
            "xzSfkpUurJusbXnLQBEC8fL/QouYnDqyn1i1c3VRVXpXUEa1WpIA7sDhAi2tvIDU\n" +
            "gRdFDERqH2HGWUfIxUSTNhCpZ+rn+5CpQ529VLyKhdmx7VPjr2hKF2ImK9phjbbH\n" +
            "XR9iyR6uUd47hl5gXAXVbDqWcFpzEZgkUme6CAxkF4feFyLejz24r8xUXzDFXnoR\n" +
            "02X6Cs2Gws7gYPRuDsh6/+gwZsMcO1dHaAL1azeGHtT3JSPAG9wS5XpDtglPseNE\n" +
            "loz7OH4=";

    private static final String EMAIL_IN_DN_CERT_B64 =
            "MIIEEzCCAvugAwIBAgIJAKrJb6VY9CsQMA0GCSqGSIb3DQEBBQUAMIGZMQswCQYD\n" +
            "VQQGEwJDQTEZMBcGA1UECBMQQnJpdGlzaCBDb2x1bWJpYTESMBAGA1UEBxMJVmFu\n" +
            "Y291dmVyMR0wGwYDVQQKExRMYXllciA3IFRlY2hub2xvZ2llczELMAkGA1UECxMC\n" +
            "UUExDDAKBgNVBAMTA2pkeTEhMB8GCSqGSIb3DQEJARYSamR5QGxheWVyN3RlY2gu\n" +
            "Y29tMB4XDTExMDYxNTE3MTkwNloXDTIxMDYxMjE3MTkwNlowgZkxCzAJBgNVBAYT\n" +
            "AkNBMRkwFwYDVQQIExBCcml0aXNoIENvbHVtYmlhMRIwEAYDVQQHEwlWYW5jb3V2\n" +
            "ZXIxHTAbBgNVBAoTFExheWVyIDcgVGVjaG5vbG9naWVzMQswCQYDVQQLEwJRQTEM\n" +
            "MAoGA1UEAxMDamR5MSEwHwYJKoZIhvcNAQkBFhJqZHlAbGF5ZXI3dGVjaC5jb20w\n" +
            "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCkl+99liFjd602Hou2bq0+\n" +
            "6Zo0TGW82q0JZ44FJ2kTSNUwUz3Lwi4Sk2TqZDGEP9J04AcDjdtTUJLRGR/lnxlq\n" +
            "fm9h8CYZaHePCOqh1uQn/iGFoSganP0Pz/8K8MmpKjz7W/y4GleduwQtpZ5e9JWO\n" +
            "YrmWK0jE3w+5cjlw+VclXaDoi+RtcEcjAVsk9JDCUVFbUOSXhpW3cCXEiwBGvYZV\n" +
            "h614nxzZvth85DvB6mt5Q3cT5ACSBnaTkIBy1o7KvyhHILcmUojIalYSjMiRMkLN\n" +
            "84Xn3mQ/r6zAB5Dj9KMtFH+PD92wW5Psl48pbwaDbuj1KCeqr0db/5ZKlr3OUK/7\n" +
            "AgMBAAGjXDBaMAkGA1UdEwQCMAAwCwYDVR0PBAQDAgXgMEAGA1UdEQQ5MDeCEWhh\n" +
            "cnVoaS5sN3RlY2guY29tghFtaWt1cnUubDd0ZWNoLmNvbYIPeXVraS5sN3RlY2gu\n" +
            "Y29tMA0GCSqGSIb3DQEBBQUAA4IBAQBlqM1+EnGbuDQDzzjnBjHc2z1G6/S71Ebd\n" +
            "xi4Or/dZem2eDeFsEWg4TDWM/upJkb4q0qvvSMCaIb6mGLy5JNTVvRnPvw/cRrhE\n" +
            "soLm+E4SINP95XFxEOybhLFTU9wkh0C/fajmcTFKLJGCbu7TqFQy4WHPK9caFCZp\n" +
            "qYQnfLuYZY5a0yq5qNqOH7Ul3UA/mUA5OeHhG5+54zp/nZZ2YxXa8IxPXQ6gWm44\n" +
            "LR8FPSgtIUQHWh+v7Y45ytVDtknN+ZbYGC1Ww9FviQPY+hMo1vexoACAlFWi2W4j\n" +
            "e5zngcaJASgin9LD7o1+A+BK5MkKQNe/S5pDoHgeX+cTYDXfU+ug";

    /**
     * Certificate attribute names exposed as context variables before 5.1 / multiple signature support.
     */
    private static final String[] OLD_NAMES = new String[]{
        "certificatePolicies",
        "countryOfCitizenship",
        "extendedKeyUsage",
        "extendedKeyUsage.criticality",
        "issuer",
        "issuerAltNameDNS",
        "issuerAltNameEmail",
        "issuerAltNameURI",
        "issuer.dn",
        "issuer.dn.canonical",
        "issuer.dn.rfc2253",
        "issuerEmail",
        "keyUsage.criticality",
        "keyUsage.cRLSign",
        "keyUsage.dataEncipherment",
        "keyUsage.decipherOnly",
        "keyUsage.digitalSignature",
        "keyUsage.keyAgreement",
        "keyUsage.keyCertSign",
        "keyUsage.keyEncipherment",
        "keyUsage.nonRepudiation",
        "notAfter",
        "notBefore",
        "serial",
        "signatureAlgorithmName",
        "signatureAlgorithmOID",
        "subject",
        "subjectAltNameDNS",
        "subjectAltNameEmail",
        "subjectAltNameURI",
        "subject.dn",
        "subject.dn.canonical",
        "subject.dn.rfc2253",
        "subjectEmail",
        "subjectPublicKeyAlgorithm",
    };
}
