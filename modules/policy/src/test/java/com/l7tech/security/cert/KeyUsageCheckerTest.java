package com.l7tech.security.cert;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.security.keys.AesKey;
import com.l7tech.util.FileUtils;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import static org.junit.Assert.*;
import org.junit.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

/**
 *
 */
public class KeyUsageCheckerTest {

    private static X509Certificate CERT_WITH_NO_EXTENSIONS;
    private static X509Certificate CERT_WITH_NEITHER;
    private static X509Certificate CERT_WITH_EKU_ANY;
    private static X509Certificate CERT_WITH_KU_DIGSIG_KEYENC_EKU_ANY;
    private static X509Certificate CERT_WITH_KU_DIGSIG_KEYENC;
    private static X509Certificate CERT_WITH_EKU_SERVAUTH;
    private static X509Certificate CERT_WITH_UNCRIT_KU_CRIT_EKU_ANY;
    private static PrivateKey CERT_WITH_EKU_SERVAUTH_PRIVATE_KEY;

    @BeforeClass
    public static void generateTestCerts() throws Exception {
        final TestCertificateGenerator certgen = new TestCertificateGenerator();

        CERT_WITH_NO_EXTENSIONS =
                certgen.reset().subject("cn=No Extensions").noExtensions().generate();

        CERT_WITH_NEITHER =
                certgen.reset().subject("cn=No-KU No-EKU").noKeyUsage().noExtKeyUsage().generate();

        CERT_WITH_EKU_ANY =
                certgen.reset().subject("cn=No-KU EKU-Any").noKeyUsage().generate();

        CERT_WITH_KU_DIGSIG_KEYENC_EKU_ANY =
                certgen.reset().subject("cn=KU-SigEnc EKU-Any").generate();

        CERT_WITH_KU_DIGSIG_KEYENC =
                certgen.reset().subject("cn=KU-SigEnc No-EKU").noExtKeyUsage().generate();

        CERT_WITH_EKU_SERVAUTH =
                certgen.reset().subject("cn=No-KU EKU-Servauth").noKeyUsage().extKeyUsage(true, KeyPurposeId.id_kp_serverAuth).generate();
        CERT_WITH_EKU_SERVAUTH_PRIVATE_KEY = certgen.getPrivateKey();

        CERT_WITH_UNCRIT_KU_CRIT_EKU_ANY =
                certgen.reset().subject("cn=Uncrit-KU EKU-Any").keyUsage(false, X509KeyUsage.decipherOnly).extKeyUsage(true, KeyPurposeId.anyExtendedKeyUsage).generate();
    }

    @Before
    public void beforeTest() {
        System.clearProperty(KeyUsageChecker.PROPERTY_POLICY_FILE);
        System.clearProperty(KeyUsageChecker.PROPERTY_POLICY_XML);
        System.clearProperty(KeyUsageChecker.PROPERTY_ENFORCEMENT_MODE);
    }

    @Test
    public void testFailCritKuWithNoPolicy() throws Exception {
        try {
            KeyUsageChecker.setDefault(new KeyUsageChecker(null, null));
            KeyUsageChecker.requireActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_KU_DIGSIG_KEYENC);
            fail("checker should fail cert with critical keyusage if no policy installed");
        } catch (KeyUsageException e) {
            // Ok
        }
    }

    @Test
    public void testFailCritEkuWithNoPolicy() throws Exception {
        try {
            KeyUsageChecker.setDefault(new KeyUsageChecker(null, null));
            KeyUsageChecker.requireActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_EKU_SERVAUTH);
            fail("checker should fail cert with critical extended key usage if no policy installed");
        } catch (KeyUsageException e) {
            // Ok
        }
    }

    @Test
    public void testSucceedNoCritsWithNoPolicy() throws Exception {
        try {
            KeyUsageChecker.setDefault(new KeyUsageChecker(null, null));
            KeyUsageChecker.requireActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_NEITHER);
        } catch (KeyUsageException e) {
            fail("checker should succeed if no policy installed, if the cert has no critical extensions");
        }
    }

    @Test
    public void defaultMayNotBeNull() {
        try {
            KeyUsageChecker.setDefault(null);
            fail("Must not allow null default");
        } catch (NullPointerException e) {
            // Ok
        }
    }

    @Test
    public void testDefaultInvalidXml() {
        try {
            System.setProperty(KeyUsageChecker.PROPERTY_POLICY_XML, "<bad-xml <very-bad/>");
            KeyUsageChecker kuc = KeyUsageChecker.makeDefaultKeyUsageChecker();


            fail("Must not succeed with malformed keyusage policy xml");
        } catch (RuntimeException e) {
            // Ok
        }
    }

    @Test
    public void testSyspropEnforcementMode() throws CertificateParsingException {
        System.setProperty(KeyUsageChecker.PROPERTY_ENFORCEMENT_MODE, "BLAHBLAH");
        testEnforcement(KeyUsageChecker.makeDefaultKeyUsageChecker());

        System.setProperty(KeyUsageChecker.PROPERTY_ENFORCEMENT_MODE, "ENFORCE");
        testEnforcement(KeyUsageChecker.makeDefaultKeyUsageChecker());

        System.setProperty(KeyUsageChecker.PROPERTY_ENFORCEMENT_MODE, "IGNORE");
        KeyUsageChecker kuc = KeyUsageChecker.makeDefaultKeyUsageChecker();
        assertTrue("Enforcement disabled permits anything", kuc.permitsActivity(KeyUsageActivity.verifyCrl, CERT_WITH_KU_DIGSIG_KEYENC));
        assertTrue("Enforcement disabled permits anything", kuc.permitsActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_EKU_SERVAUTH));
        assertTrue("Enforcement disabled permits anything, even with null cert", kuc.permitsActivity(KeyUsageActivity.verifyCrl, null));
    }

    @Test
    public void testKeyUsageEnforcementWithDefaultPolicy() throws Exception {
        testEnforcement(makeChecker(false));
    }

    @Test
    public void testKeyUsageEnforcementWithTestPolicy() throws Exception {
        testEnforcement(makeChecker(true));
    }

    private void testEnforcement(KeyUsageChecker kuc) throws CertificateParsingException {
        assertTrue(kuc.permitsActivity(KeyUsageActivity.sslServerRemote, CERT_WITH_EKU_SERVAUTH));
        assertTrue(kuc.permitsActivity(KeyUsageActivity.verifyXml, CERT_WITH_KU_DIGSIG_KEYENC_EKU_ANY));
        assertTrue(kuc.permitsActivity(KeyUsageActivity.verifyXml, CERT_WITH_EKU_ANY));
        assertTrue(kuc.permitsActivity(KeyUsageActivity.verifyXml, CERT_WITH_UNCRIT_KU_CRIT_EKU_ANY));
        assertFalse(kuc.permitsActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_EKU_SERVAUTH));
        assertTrue(kuc.permitsActivity(KeyUsageActivity.decryptXml, CERT_WITH_NO_EXTENSIONS));
        assertTrue(kuc.permitsActivity(KeyUsageActivity.decryptXml, CERT_WITH_NEITHER));
    }

    @Test
    public void readPolicyFileFromDisk() throws Exception {
        assertNull(KeyUsageChecker.open("./nonexistent-file-98237423529385"));
        final String dummyPath = "./KeyUsageCheckerTest-dummyFile-298723923";
        final File testFile = new File(dummyPath);
        try {
            FileUtils.save(new EmptyInputStream(), testFile);
            assertNotNull(KeyUsageChecker.open(dummyPath));
        } finally {
            testFile.delete();
        }
    }

    @Test
    public void testEnforcementDisabled() throws Exception {
        assertTrue("Default enforcement mode is ENFORCE", policyEnforcedWhenEnforcementModeIs(null));
        assertTrue("Default enforcement mode is ENFORCE", policyEnforcedWhenEnforcementModeIs("WHARRGARBL"));
        assertTrue("ENFORCE enforcement mode does enforcement", policyEnforcedWhenEnforcementModeIs("ENFORCE"));
        assertFalse("IGNORE enforcement mode does not do enforcement", policyEnforcedWhenEnforcementModeIs("IGNORE"));
        assertFalse("Case and surrounding whitespace ignored for IGNORE mode", policyEnforcedWhenEnforcementModeIs(" iGnoRE  \t"));
    }

    private boolean policyEnforcedWhenEnforcementModeIs(String enforcementMode) throws CertificateParsingException, SAXException {
        return !makeChecker(true, enforcementMode).permitsActivity(KeyUsageActivity.sslClientRemote, CERT_WITH_EKU_SERVAUTH);
    }

    private static KeyUsageChecker makeChecker(boolean useTestPolicy) throws SAXException {
        return makeChecker(useTestPolicy, null);
    }

    private static KeyUsageChecker makeChecker(boolean useTestPolicy, String enforcementMode) throws SAXException {
        if (useTestPolicy) {
            return new KeyUsageChecker(KeyUsagePolicy.fromXml(KeyUsagePolicyTest.makeTestPolicyXml()), enforcementMode);
        }
        else
            return new KeyUsageChecker(KeyUsageChecker.makeDefaultPolicy(), enforcementMode);
    }

    @Test
    public void testRequireActivityForKey() throws Exception {
        KeyUsageChecker kuc = makeChecker(true);
        KeyUsageChecker.setDefault(kuc);

        // Test success with RSA private key and cert
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, CERT_WITH_EKU_SERVAUTH, CERT_WITH_EKU_SERVAUTH_PRIVATE_KEY);

        // Test success with no cert, but private key
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, null, CERT_WITH_EKU_SERVAUTH_PRIVATE_KEY);

        // Test success with RSA public key and cert
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, CERT_WITH_EKU_SERVAUTH, CERT_WITH_EKU_SERVAUTH.getPublicKey());

        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, null, CERT_WITH_EKU_SERVAUTH.getPublicKey());
            fail("Should have rejected null cert when given RSA PublicKey, when there's an active key usage policy");
        } catch (CertificateException e) {
            // Ok
        }

        // But same thing should succeed if enforcement is turned off
        KeyUsageChecker.setDefault(new KeyUsageChecker(null, "IGNORE"));
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, null, CERT_WITH_EKU_SERVAUTH.getPublicKey());
        KeyUsageChecker.setDefault(kuc);

        // Test success with SecretKey and no cert
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, null, new AesKey(new byte[64], 128));

        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, null, new Key() {
                public String getAlgorithm() {
                    return "BOGUS";
                }

                public String getFormat() {
                    return null;
                }

                public byte[] getEncoded() {
                    return new byte[0];
                }
            });
            fail("Should have rejected unrecognized key type");
        } catch (CertificateException e) {
            // Ok
        }


    }
}
