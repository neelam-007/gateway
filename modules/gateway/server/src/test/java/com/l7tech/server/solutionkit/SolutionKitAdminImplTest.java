package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.*;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Solution Kit Admin implementation test
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitAdminImplTest {

    private static SignatureVerifier SIGNATURE_VERIFIER;
    private static final String[] SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com",
            "cn=signer.team3.apim.ca.com",
            "cn=signer.team4.apim.ca.com"
    };

    @Mock
    private LicenseManager licenseManager;
    @Mock
    private SolutionKitManager solutionKitManager;

    private SolutionKitAdmin solutionKitAdmin;

    @BeforeClass
    public static void beforeClass() throws Exception {
        SignatureTestUtils.beforeClass();

        SIGNATURE_VERIFIER = SignatureTestUtils.createSignatureVerifier(SIGNER_CERT_DNS);
        Assert.assertNotNull("signature verifier is created", SIGNATURE_VERIFIER);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SignatureTestUtils.afterClass();
    }

    @Before
    public void before() throws Exception {
        solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager, SIGNATURE_VERIFIER);
        Assert.assertNotNull("SolutionKitAdmin is created", solutionKitAdmin);
    }


    @Test
    public void toDo() throws Exception {
        // TODO
    }

    @Test
    public void testVerifySkarSignature() throws Exception {
        final byte[] sampleBytes = "test data".getBytes(Charsets.UTF8);
        final byte[] sampleBytesDigest = calcSha256Digest(sampleBytes);

        // test using a trusted signer with all signing cert DNs
        for (final String signerDN : SIGNER_CERT_DNS) {
            final byte[] signedBytes = SignatureTestUtils.sign(SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleBytes), signerDN);
            solutionKitAdmin.verifySkarSignature(sampleBytesDigest, SignatureTestUtils.getSignatureString(signedBytes));
        }

        // create untrusted signer with same DNs plus a new one
        final String[] untrustedDNs = ArrayUtils.concat(
                SIGNER_CERT_DNS,
                new String[] {
                        "cn=signer.untrusted.apim.ca.com"
                }
        );
        final SignatureVerifier untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

        // test using untrusted signer with all signing cert DNs
        for (final String signerDN : untrustedDNs) {
            final byte[] signedBytes = SignatureTestUtils.sign(untrustedSigner, new ByteArrayInputStream(sampleBytes), signerDN);
            try {
                solutionKitAdmin.verifySkarSignature(sampleBytesDigest, SignatureTestUtils.getSignatureString(signedBytes));
                Assert.fail("verifySkarSignature should have failed with SignatureException.");
            } catch (SignatureException e) {
                Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to verify signer certificate"));
            }
        }

        // tamper with bytes after signing
        byte[] tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleBytes),
                SIGNATURE_VERIFIER,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);
                        return Pair.pair(SignatureTestUtils.flipRandomByte(dataBytes), sigProps);
                    }
                }
        );
        try {
            // make sure tamperedSignedBytes digest is different then the sampleBytesDigest
            final byte[] calculatedDigest = SignatureTestUtils.calcSignedDataDigest(tamperedSignedBytes);
            Assert.assertFalse(Arrays.equals(calculatedDigest, sampleBytesDigest));
            // verify signature with tampered data
            solutionKitAdmin.verifySkarSignature(calculatedDigest, SignatureTestUtils.getSignatureString(tamperedSignedBytes));
            Assert.fail("verifySkarSignature should have failed with SignatureException.");
        } catch (SignatureException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Signature not verified"));
        }

        // tamper with signature after signing (flipping random byte)
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleBytes),
                SIGNATURE_VERIFIER,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature property
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        // flip random byte
                        final byte[] modSignBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty("signature", HexUtils.encodeBase64(modSignBytes));
                        Assert.assertThat(signatureB64, Matchers.not(Matchers.equalTo((String) sigProps.get("signature"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        try {
            // make sure tamperedSignedBytes digest is the same as sampleBytesDigest, as the data was not changed during tampering
            final byte[] calculatedDigest = SignatureTestUtils.calcSignedDataDigest(tamperedSignedBytes);
            Assert.assertTrue(Arrays.equals(calculatedDigest, sampleBytesDigest));
            // verify signature with tampered signature props
            solutionKitAdmin.verifySkarSignature(calculatedDigest, SignatureTestUtils.getSignatureString(tamperedSignedBytes));
            Assert.fail("verifySkarSignature should have failed with SignatureException.");
        } catch (SignatureException e) {
            Assert.assertThat(e.getMessage(), Matchers.either(Matchers.containsString("Signature not verified")).or(Matchers.containsString("Could not verify signature")));
        }

        // tamper with signer cert after signing (flipping random byte)
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleBytes),
                SIGNATURE_VERIFIER,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signer cert property
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        // flip random byte
                        final byte[] modSignerCertBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty("cert", HexUtils.encodeBase64(modSignerCertBytes));
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        try {
            // make sure tamperedSignedBytes digest is the same as sampleBytesDigest, as the data was not changed during tampering
            final byte[] calculatedDigest = SignatureTestUtils.calcSignedDataDigest(tamperedSignedBytes);
            Assert.assertTrue(Arrays.equals(calculatedDigest, sampleBytesDigest));
            // verify signature with tampered signature props
            solutionKitAdmin.verifySkarSignature(calculatedDigest, SignatureTestUtils.getSignatureString(tamperedSignedBytes));
            Assert.fail("verifySkarSignature should have failed with SignatureException.");
        } catch (SignatureException e) {
            Assert.assertThat(
                    e.getMessage(),
                    Matchers.anyOf(
                            Matchers.containsString("Failed to verify signer certificate"),
                            Matchers.containsString("Signature not verified"),
                            Matchers.containsString("Failed to verify and extract signer certificate")
                    )
            );
        }

        // sign sample with a trusted and sample1 with untrusted signer and swap the untrusted signature props with the trusted ones
        // create and sign, using the trusted signer, a new sample
        final byte[] anotherSampleBytes = "test another data".getBytes(Charsets.UTF8);
        final byte[] anotherSampleBytesDigest = calcSha256Digest(anotherSampleBytes);
        // sign with trusted signer
        final byte[] signedTrustedAnotherSampleBytes = SignatureTestUtils.sign(SIGNATURE_VERIFIER, new ByteArrayInputStream(anotherSampleBytes), SIGNER_CERT_DNS[0]);
        // verify digest
        Assert.assertTrue(Arrays.equals(anotherSampleBytesDigest, SignatureTestUtils.calcSignedDataDigest(signedTrustedAnotherSampleBytes)));
        // make sure this is trusted
        solutionKitAdmin.verifySkarSignature(anotherSampleBytesDigest, SignatureTestUtils.getSignatureString(signedTrustedAnotherSampleBytes));
        // sign our first sample using untrusted signer and swap the signature from signedTrustedAnotherSampleBytes
        Assert.assertThat(untrustedDNs[0], Matchers.equalTo(SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get("signature");
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get("cert");
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, trustedSigProps);
                    }
                }
        );
        try {
            // verify the tamperedSignedBytes have the same signature properties raw-bytes as signedTrustedAnotherSampleBytes
            Assert.assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedBytes), Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleBytes)));
            // make sure tamperedSignedBytes digest is the same as sampleBytesDigest, as the data was not changed during tampering
            final byte[] calculatedDigest = SignatureTestUtils.calcSignedDataDigest(tamperedSignedBytes);
            Assert.assertTrue(Arrays.equals(calculatedDigest, sampleBytesDigest));
            // verify signature with tampered signature props
            solutionKitAdmin.verifySkarSignature(calculatedDigest, SignatureTestUtils.getSignatureString(tamperedSignedBytes));
            Assert.fail("verifySkarSignature should have failed with SignatureException.");
        } catch (SignatureException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Signature not verified"));
        }

        // same as above but instead of swapping the entire signature properties bytes, swap only the signer cert and leave signature unchanged
        // sign our first sample using untrusted signer and swap the signing cert from signedTrustedAnotherSampleBytes
        Assert.assertThat(untrustedDNs[0], Matchers.equalTo(SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get("signature");
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get("cert");
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // swap signing cert property
                        sigProps.setProperty("cert", HexUtils.encodeBase64(trustedSignerCertBytes));
                        // make sure after the swap the signer cert is different
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));
                        // make sure after the swap the signature is unchanged
                        Assert.assertThat(signatureB64, Matchers.equalTo((String) sigProps.get("signature")));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        try {
            // verify that the tamperedSignedBytes signature properties raw-bytes differ than signedTrustedAnotherSampleBytes
            Assert.assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedBytes), Matchers.not(Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleBytes))));
            // make sure tamperedSignedBytes digest is the same as sampleBytesDigest, as the data was not changed during tampering
            final byte[] calculatedDigest = SignatureTestUtils.calcSignedDataDigest(tamperedSignedBytes);
            Assert.assertTrue(Arrays.equals(calculatedDigest, sampleBytesDigest));
            // verify signature with tampered signature props
            solutionKitAdmin.verifySkarSignature(calculatedDigest, SignatureTestUtils.getSignatureString(tamperedSignedBytes));
            Assert.fail("verifySkarSignature should have failed with SignatureException.");
        } catch (SignatureException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Signature not verified"));
        }
    }

    private static byte[] calcSha256Digest(final byte[] bytes) throws Exception {
        return calcSha256Digest(new ByteArrayInputStream(bytes));
    }

    private static byte[] calcSha256Digest(final InputStream stream) throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final DigestInputStream dis = new DigestInputStream(stream, messageDigest);
        IOUtils.copyStream(dis, new com.l7tech.common.io.NullOutputStream());
        return dis.getMessageDigest().digest();
    }
}
