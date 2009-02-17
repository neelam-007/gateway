package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.Pair;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import static org.junit.Assert.*;
import org.junit.*;

import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 *
 */
public class CertVerifierTest {
    @Test
    public void testGetCertProperteis() throws Exception {
        X509Certificate cert =
                new TestCertificateGenerator()
                        .subject("cn=blorg")
                        .keyUsage(true, X509KeyUsage.cRLSign | X509KeyUsage.keyEncipherment)
                        .basicConstraintsCa(4)
                        .extKeyUsage(true, KeyPurposeId.anyExtendedKeyUsage, KeyPurposeId.id_kp_codeSigning)
                        .generate();

        List<Pair<String,String>> props = CertUtils.getCertProperties(cert);
        assertTrue(props.contains(new Pair<String, String>(CertUtils.CERT_PROP_KEY_USAGE, "Key Encipherment, CRL Signing")));
        assertTrue(props.contains(new Pair<String, String>(CertUtils.CERT_PROP_EXT_KEY_USAGE, "any, id-kp-codeSigning")));
        assertTrue(props.contains(new Pair<String, String>(CertUtils.CERT_PROP_ISSUED_TO, "CN=blorg")));
    }

    @Test
    public void testCheckForMismatchingKey() throws Exception {
        TestCertificateGenerator cg = new TestCertificateGenerator();
        Pair<X509Certificate, PrivateKey> a = cg.reset().subject("cn=cert_a").generateWithKey();
        Pair<X509Certificate, PrivateKey> b = cg.reset().subject("cn=cert_b").generateWithKey();

        CertUtils.checkForMismatchingKey(a.left, a.right);

        try {
            CertUtils.checkForMismatchingKey(a.left, b.right);
            fail("Should have failed due to detectable mismatching key");
        } catch (CertificateException e) {
            // Ok
        }
    }

    @Test
    public void testVerifyCertificateChain() throws Exception {
        TestCertificateGenerator cg = new TestCertificateGenerator();

        // Should pass with simple self-signed cert that is trusted directly by fiat
        X509Certificate selfsigned = cg.reset().generate();
        CertVerifier.verifyCertificateChain(new X509Certificate[] { selfsigned }, selfsigned);

        // Should pass with simple self-signed X.509v1 cert (no extensions) that is trusted directly by fiat
        X509Certificate selfsignedv1 = cg.reset().noExtensions().generate();
        CertVerifier.verifyCertificateChain(new X509Certificate[] { selfsignedv1 }, selfsignedv1);

        // Chain of V1 certs should work...
        Pair<X509Certificate, PrivateKey> v1ca = cg.reset().noExtensions().generateWithKey();
        Pair<X509Certificate, PrivateKey> v1dept = cg.reset().noExtensions().issuer(v1ca.left, v1ca.right).generateWithKey();
        Pair<X509Certificate, PrivateKey> v1alice = cg.reset().noExtensions().issuer(v1dept.left, v1dept.right).generateWithKey();
        CertVerifier.verifyCertificateChain(new X509Certificate[] { v1alice.left, v1dept.left, v1ca.left }, v1dept.left);

        // ...as long as there are no intermediate CA certs
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[] { v1alice.left, v1dept.left, v1ca.left }, v1ca.left);
            fail("Should have rejected chain of V1 certs with more than zero intermediate CAs");
        } catch (CertificateException e) {
            assertTrue(e.getMessage().toLowerCase().matches(".*path\\s*length.*"));
            // Ok
        }

        Pair<X509Certificate, PrivateKey> ca = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.keyCertSign).basicConstraintsCa(1).generateWithKey();
        Pair<X509Certificate, PrivateKey> dept = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.keyCertSign).basicConstraintsCa(0).issuer(ca.left, ca.right).generateWithKey();
        Pair<X509Certificate, PrivateKey> alice = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.digitalSignature).basicConstraintsNoCa().issuer(dept.left, dept.right).generateWithKey();

        // Should pass with dept cert as trust anchor
        CertVerifier.verifyCertificateChain(new X509Certificate[] { alice.left, dept.left, ca.left }, dept.left);

        // Should pass with ca cert as trust anchor
        CertVerifier.verifyCertificateChain(new X509Certificate[] { alice.left, dept.left, ca.left }, ca.left);

        // Should pass with user cert as trust anchor
        CertVerifier.verifyCertificateChain(new X509Certificate[] { alice.left, dept.left, ca.left }, alice.left);

        // Should fail with reverse order
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[] { ca.left, dept.left, alice.left }, alice.left);
            fail("Should have failed with reversed cert order");
        } catch (CertificateException e) {
            // Ok
        }

        // Should fail with too may intermediate certs
        Pair<X509Certificate, PrivateKey> d1 = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.keyCertSign).basicConstraintsCa(0).issuer(ca.left, ca.right).generateWithKey();
        Pair<X509Certificate, PrivateKey> d2 = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.keyCertSign).basicConstraintsCa(0).issuer(d1.left, d1.right).generateWithKey();
        Pair<X509Certificate, PrivateKey> bob = cg.reset().noExtensions().keyUsage(true, X509KeyUsage.digitalSignature).basicConstraintsNoCa().issuer(d2.left, d2.right).generateWithKey();
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[] { bob.left, d2.left, d1.left, ca.left  }, ca.left);
            fail("Verification should have failed due to path length exceeded");
        } catch (CertificateException e) {
            assertTrue(e.getMessage().toLowerCase().matches(".*path\\s*length.*"));
            // Ok
        }

        // Should fail with rogue CA cert
        Pair<X509Certificate, PrivateKey> charlie = cg.reset().issuer(alice.left, alice.right).generateWithKey();
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[] { charlie.left, alice.left }, alice.left);
            fail("Verification should have failed with rogue user attempting to act as their own CA");
        } catch (CertificateException e) {
            assertTrue(e.getMessage().toLowerCase().matches(".*constraint.*"));
            // Ok
        }

        // Should fail with no trust anchor found
        X509Certificate someRandomCaCert = cg.reset().subject("cn=blahca").basicConstraintsCa(10).keyUsage(true, X509KeyUsage.keyCertSign).generate();
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[] { alice.left, dept.left, ca.left }, someRandomCaCert);
            fail("Verification should have failed with no recognized trust anchor in chain");
        } catch (CertUtils.CertificateUntrustedException e) {
            // Ok
        }

        // Should fail with null chain
        try {
            CertVerifier.verifyCertificateChain(null, someRandomCaCert);
            fail("Verification should have failed with null cert chain");
        } catch (CertificateException e) {
            // Ok
        } catch (NullPointerException e) {
            // Also Ok
        }

        // Should fail with empty chain
        try {
            CertVerifier.verifyCertificateChain(new X509Certificate[0], someRandomCaCert);
            fail("Verification should have failed with empty cert chain");
        } catch (CertificateException e) {
            // Ok
        }
    }
}
