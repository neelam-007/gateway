package com.l7tech.internal.certgen;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.Pair;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GenerateCertificate} command line utility.
 */
public class GenerateCertificateTest {

    @Test
    public void testGenDefault() throws Exception {
        assertNotNull(CertUtils.decodeFromPEM(generate()));
    }

    @Test
    public void testHelp() throws Exception {
        String got = generate("-help");
        System.out.println(got);
        assertTrue(got.contains("cRLSign"));
    }

    @Test
    public void testGenCustom() throws Exception {
        int wantKeySize = 1024 + 256;
        String got = generate("-keySize", Integer.toString(wantKeySize), "-subject", "cn=2hasdfiuh2,dc=blah,dc=asdf", "-noExtKeyUsagE", "-daysUntilExpiry", "17");
        final X509Certificate cert = CertUtils.decodeFromPEM(got);
        assertNotNull(cert);
        assertEquals("cn=2hasdfiuh2,dc=blah,dc=asdf", cert.getSubjectX500Principal().getName().toLowerCase());
        int keybits = CertUtils.getRsaKeyBits((RSAPublicKey) cert.getPublicKey());
        assertTrue("Should get at least the number of key bits we asked for", keybits >= wantKeySize);
        assertTrue("Should not get too many key bits", keybits <= wantKeySize);
        assertTrue("Should have default key usage", cert.getKeyUsage()[2]);
        assertFalse("Should have default key usage", cert.getKeyUsage()[6]);
        assertNull("Should not have ext key usage", cert.getExtendedKeyUsage());
        assertTrue("Should have non-CA basic constraints by default", cert.getBasicConstraints() == -1);

        long millis = cert.getNotAfter().getTime() - new Date().getTime();
        assertTrue("Expiry days should be in the right range", millis > (86400 * 16 * 1000L) && millis < (86400 * 18 * 1000L));
    }

    @Test
    public void testGenText() throws Exception {
        String got = generate("-noBase64", "-text");
        assertFalse(got.contains("--BEGIN"));
        assertTrue(got.contains("Signature Algorithm:"));
    }

    @Test
    public void testGenNotYetValid() throws Exception {
        String got = generate("-notBefore", "Tue Apr 15 17:01:08 GMT 2036");
        final X509Certificate cert = CertUtils.decodeFromPEM(got);
        assertNotNull(cert);
        assertTrue( cert.getNotBefore().toString().contains( "2036" ) );
    }

    @Test
    public void testGenKeystore() throws Exception {
        final String file = "CertificateGeneratorTest.p12";
        try {
            String got = generate("-noBase64", "-outfile", file, "7layer");
            assertTrue(got.startsWith("Cert chain with private key saved to " + file));
        } finally {
            new File(file).delete();
        }
    }

    @Test
    public void testMissingParameter() throws Exception {
        try {
            generate("-subject");
            fail("Expected exception was not thrown");
        } catch (NoSuchElementException e) {
            // Ok
        } catch (IllegalArgumentException e) {
            // Also ok
        }
    }

    @Test
    public void testMalformedSubject() throws Exception {
        try {
            generate("-subject", "823723");
            fail("Expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            // Ok
        }
    }

    @Test
    public void testBadArgumentName() throws Exception {
        try {
            generate("-blarflegortz");
            fail("Expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            // Ok
        }
    }

    @Test
    public void testBadArgumentSyntax() throws Exception {
        try {
            generate("subject", "cn=blah");
            fail("Expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            // Ok
        }
    }

    @Test
    public void testKeyUsage() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-keyusage", "encipherOnly", "-keyusage", "cRLSign"));
        assertFalse(got.getKeyUsage()[0]); // digitalSignature
        assertFalse(got.getKeyUsage()[8]); // decipherOnly
        assertTrue(got.getKeyUsage()[7]); // encipherOnly
        assertTrue(got.getKeyUsage()[6]); // cRLSign
    }

    @Test
    public void testExtKeyUsage() throws Exception {
        String ipsecOid = GenerateCertificate.KEY_PURPOSE_IDS_BY_NAME.get("id-kp-ipsecTunnel");
        String smartcardOid = GenerateCertificate.KEY_PURPOSE_IDS_BY_NAME.get("id-kp-smartcardlogon");

        X509Certificate got = CertUtils.decodeFromPEM(generate("-extkeyusage", "id-kp-ipsecTunnel", "-extkeyusage", smartcardOid));

        assertTrue(got.getExtendedKeyUsage().contains(ipsecOid));
        assertTrue(got.getExtendedKeyUsage().contains(smartcardOid));
        assertTrue(got.getExtendedKeyUsage().size() == 2);
    }

    @Test
    public void testCountriesOfCitizenship() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-countriesOfCitizenship", "ca", "-countriesOfCitizenship", "jp"));
        assertNotNull(got.getExtensionValue(X509Extensions.SubjectDirectoryAttributes.getId()));
    }

    @Test
    public void testBasicConstraintsCa() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-basicConstraintsCa", "4", "-keyUsage", "keyCertSign"));
        assertNotNull(got.getExtensionValue(X509Extensions.BasicConstraints.getId()));
        assertEquals(got.getBasicConstraints(), 4);
    }

    @Test
    public void testBasicConstraintsNoCa() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-basicConstraintsNoCa"));
        assertNotNull(got.getExtensionValue(X509Extensions.BasicConstraints.getId()));
        assertEquals(got.getBasicConstraints(), -1);
    }

    @Test
    public void testCertPolicies() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-certificatePolicies", "1.2.3.4", "-certificatePolicies", "1.2.3.5.6.7"));
        assertNotNull(got.getExtensionValue(X509Extensions.CertificatePolicies.getId()));
        assertTrue(got.toString().contains("CertificatePolicies ["));
        assertTrue(got.toString().contains("  [CertificatePolicyId: [1.2.3.4]"));
        assertTrue(got.toString().contains("  [CertificatePolicyId: [1.2.3.5.6.7]"));
    }

    @Test
    public void testOcspUrls() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-ocspUrl", "http://ocsp1.blah/asdf", "-ocspUrl", "http://ocsp2.blah/qwer"));
        assertNotNull("AIA ext shall be present", got.getExtensionValue(X509Extensions.AuthorityInfoAccess.getId()));
        assertFalse("AIA shall not be critical by default", got.getCriticalExtensionOIDs().contains(X509Extensions.AuthorityInfoAccess.getId()));
        String[] urls = CertUtils.getAuthorityInformationAccessUris(got, "1.3.6.1.5.5.7.48.1"); // OID_AIA_OCSP
        assertEquals(2, urls.length);
        assertEquals("http://ocsp1.blah/asdf", urls[0]);
        assertEquals("http://ocsp2.blah/qwer", urls[1]);
    }

    @Test
    public void testOcspUrlsCritical() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-ocspUrl", "http://ocsp1.blah/asdf", "-ocspUrlCritical", "true"));
        assertNotNull("AIA ext shall be present", got.getExtensionValue(X509Extensions.AuthorityInfoAccess.getId()));
        assertTrue("AIA shall be critical when requested", got.getCriticalExtensionOIDs().contains(X509Extensions.AuthorityInfoAccess.getId()));
        String[] urls = CertUtils.getAuthorityInformationAccessUris(got, "1.3.6.1.5.5.7.48.1"); // OID_AIA_OCSP
        assertEquals(1, urls.length);
        assertEquals("http://ocsp1.blah/asdf", urls[0]);
    }

    @Test
    public void testSubjectAlternativeName() throws Exception {
        X509Certificate got = CertUtils.decodeFromPEM(generate("-subjectAltName", "1.2.3.4", "-subjectAltName", "*.foo.bar.com"));
        final Collection<List<?>> altEntries = got.getSubjectAlternativeNames();
        assertNotNull(altEntries);
        assertEquals(2, altEntries.size());
        List<X509GeneralName> gens = X509GeneralName.fromList(altEntries);
        Set<String> stringVals = new HashSet<String>();
        for (X509GeneralName gen : gens) {
            X509GeneralName.Type type = gen.getType();
            if (X509GeneralName.Type.iPAddress.equals(type)) {
                assertEquals("1.2.3.4", gen.getStringVal());
            } else if (X509GeneralName.Type.dNSName.equals(type)) {
                assertEquals("*.foo.bar.com", gen.getStringVal());
            }
            stringVals.add(gen.getStringVal());
        }
        assertTrue(stringVals.contains("1.2.3.4"));
        assertTrue(stringVals.contains("*.foo.bar.com"));
    }

    @Test
    public void testMakeCaAndSslCerts() throws Exception {
        final String cafile = "CertificateGeneratorTest-ca.p12";
        final String sslfile = "CertificateGeneratorTest-ssl.p12";
        final String pass = "7layerzz";
        try {
            String got = generate("-noBase64", "-subject", "cn=theca,ou=cathing,ou=moreca,o=blah", "-outfile", cafile, pass);
            assertTrue(got.startsWith("Cert chain with private key saved to " + cafile));

            String sslGot = generate("-noBase64", "-subject", "cn=thessl,ou=sslthing,o=blah", "-outfile", sslfile, pass, "-issuer", cafile, pass);
            assertTrue(sslGot.startsWith("Cert chain with private key saved to " + sslfile));

            // Make sure chain makes sense
            Pair<X509Certificate[],PrivateKey> cachain = TestCertificateGenerator.loadFromPkcs12(cafile, pass);
            assertTrue(cachain.left.length == 1);

            Pair<X509Certificate[], PrivateKey> sslchain = TestCertificateGenerator.loadFromPkcs12(sslfile, pass);
            assertTrue(sslchain.left.length == 2);
            assertTrue(CertUtils.certsAreEqual(sslchain.left[1], cachain.left[0]));

            // SSL cert should verify using CA cert's public key
            sslchain.left[0].verify(cachain.left[0].getPublicKey());

        } finally {
            new File(cafile).delete();
            new File(sslfile).delete();
        }
    }

    private String generate(String... args) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        new GenerateCertificate(ps, Arrays.asList(args)).generate();
        return baos.toString();
    }

    // Test this last since the main method calls system.exit(1) on failure
    @Test
    public void testMainMethod() throws Exception {
        GenerateCertificate.main(new String[0]);
        // We'll assume it did the same thing as testGenDefault, but would have printed the base64 to System.out
    }
}