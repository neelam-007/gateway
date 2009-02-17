package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.Pair;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import static org.junit.Assert.*;
import org.junit.*;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Test for TestCertificateGenerator, that also shows how to use it for creating test certificates for QA purposes.
 */
public class TestCertificateGeneratorTest {
    @Test
    public void testTestCertificateGenerator() throws Exception {
        // Makes a new self-signed cert with the subject "cn=test", with 512-bit RSA key, configured the
        // way you'd get if you just used the "New Private Key" dialog in the SSM with default values.
        // Using it like this throws away the private key.
        X509Certificate cert1 = new TestCertificateGenerator().generate();
        assertNotNull(cert1);

        // As above, but saves the cert and the private key.
        TestCertificateGenerator certgen2 = new TestCertificateGenerator();
        X509Certificate cert2 = certgen2.generate();
        PrivateKey key2 = certgen2.getPrivateKey();
        assertNotNull(cert2);
        assertNotNull(key2);

        // As above, but returns the cert and key all at once in a tuple.
        Pair<X509Certificate,PrivateKey> certAndKey = new TestCertificateGenerator().generateWithKey();

        // Extract the cert and private key from the previous tuple
        X509Certificate cert3 = certAndKey.left;
        PrivateKey key3 = certAndKey.right;

        // Save a cert chain to disk along with its private key in a PKCS#12 file
        // Here we are saving a cert chain that just contains a single self-signed cert
        TestCertificateGenerator.saveAsPkcs12(new X509Certificate[] { cert3 }, key3, "/tmp/Cert3AndKey3.p12", "7layer");

        // Load back the file we just saved
        Pair<X509Certificate[], PrivateKey> loaded = TestCertificateGenerator.loadFromPkcs12("/tmp/Cert3AndKey3.p12", "7layer");
        assertTrue(CertUtils.certsAreEqual(loaded.left[0], cert3)); // Loaded cert matches the one we saved
        assertTrue(Arrays.equals(loaded.right.getEncoded(), key3.getEncoded())); // Loaded private key matches too

        // Generate a cert without any extensions at all
        X509Certificate cert4 = new TestCertificateGenerator().noExtensions().generate();
        assertNotNull(cert4);

        // Customize the subject, basic constraints, key usage, extended key usage, and country of residence
        X509Certificate cert5 = new TestCertificateGenerator().noExtensions()
                .subject("cn=Joe Blow,o=Layer 7 Tech")
                .basicConstraintsNoCa()
                .keyUsage(true, X509KeyUsage.digitalSignature & X509KeyUsage.keyEncipherment)
                .extKeyUsage(true, KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_emailProtection)
                .countriesOfCitizenship(true, "CA", "US", "UK")
                .generate();
        assertNotNull(cert5);

        //
        // Create a chain of certs 3 certs long, with a CA cert, an intermediate CA cert, and a user cert, each
        // signed by the previous cert in the chain (except the CA cert of course, which is self-signed).
        // Then, save the entire chain to disk as a PKCS#12 file.
        //
        TestCertificateGenerator generator = new TestCertificateGenerator();

        // Create CA cert and private key
        Pair<X509Certificate, PrivateKey> ca = generator.noExtensions()
                .subject("cn=ACME CA,o=ACME Inc.")
                .basicConstraintsCa(10)
                .keyUsage(true, X509KeyUsage.digitalSignature | X509KeyUsage.keyCertSign)
                .noExtKeyUsage()
                .generateWithKey();

        // Create Sales Department CA cert and private key
        Pair<X509Certificate, PrivateKey> dept = generator.reset().noExtensions()
                .subject("cn=ACME Sales Department,ou=Sales,o=ACME Inc.")
                .basicConstraintsCa(10)
                .keyUsage(true, X509KeyUsage.digitalSignature | X509KeyUsage.keyCertSign)
                .noExtKeyUsage()
                .issuer(ca.left, ca.right)
                .generateWithKey();

        // Create salesman Joe User's client cert and private key
        Pair<X509Certificate, PrivateKey> joeUser = generator.reset()
                .subject("cn=Joe User,ou=Sales,o=ACME Inc.")
                .issuer(dept.left, dept.right)
                .generateWithKey();

        // Create a certificate chain holding all three certs (but not the keys) ordered from subject to ca
        X509Certificate[] joeUserCertChain = new X509Certificate[] { joeUser.left, dept.left, ca.left };

        //
        // Save Joe User's cert chain and private key to /tmp/joe.p12 with the password "7layer"
        //
        TestCertificateGenerator.saveAsPkcs12(joeUserCertChain, joeUser.right, "7layer", "/tmp/joe.p12");

        System.out.println("Generated cert: " + joeUserCertChain[0]);
    }
}
