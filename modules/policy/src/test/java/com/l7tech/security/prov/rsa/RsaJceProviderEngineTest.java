package com.l7tech.security.prov.rsa;

import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.test.FixedSecureRandom;
import static org.junit.Assert.*;
import org.junit.*;
import sun.misc.BASE64Decoder;

import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

/**
 *
 */
public class RsaJceProviderEngineTest {
    private static final Logger logger = Logger.getLogger(RsaJceProviderEngineTest.class.getName());

    // Unpadded 20 byte SHA-1 hash output to be signed
    private static final byte[] UNPADDED_20_BYTES = new byte[] { 29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45 };

    // Same hash output as above, but padded by hand to 48 bytes (384 bits) to match size of P-384 curve used by private key
    private static final byte[] PADDED_48_BYTES = new byte[] { 
            29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private static byte[] testPublicKeyBytes;
    private static byte[] testPrivateKeyBytes;

    @BeforeClass
    public static void installRsaProvider() {
        final String rsaEngineClass = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
        System.setProperty("com.l7tech.common.security.jceProviderEngine", rsaEngineClass);
        JceProvider.init();
        assertEquals(rsaEngineClass, JceProvider.getEngineClass());
    }

    @BeforeClass
    public static void createTestKey() throws GeneralSecurityException {
        Pair<X509Certificate,PrivateKey> gen = new TestCertificateGenerator().curveName("secp384r1").generateWithKey();
        X509Certificate testCert = gen.left;
        PrivateKey testPrivateKey = gen.right;
        assertEquals("PKCS#8", testPrivateKey.getFormat());
        testPrivateKeyBytes = testPrivateKey.getEncoded();
        assertEquals("X509", testCert.getPublicKey().getFormat());
        testPublicKeyBytes = testCert.getPublicKey().getEncoded();
    }

    // Playback of Signature usage by ClientHandshaker during one of the failing handshakes
    @Ignore("Sig fail: 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testSignatureInputPaddingSunjssePlayback() throws Exception {
        final byte[] privateKeyBytes = new BASE64Decoder().decodeBuffer(
                "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDDk9GwJp2V9Mocze7kdaR+6cC0tpjJcFEurLd5IS/bbH738LuD/qA9rz/XClNmzpRw=");
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        byte[] shaCloneDigest = UNPADDED_20_BYTES;

        Signature sig = Signature.getInstance("NONEwithECDSA");
        sig.initSign(privateKey, secureRandom());
        sig.update(shaCloneDigest);
        byte[] result = sig.sign();  // java.security.SignatureException: The input requires padding, but NoPad was instantiated
        System.out.println("Sign result (original): " + result.length + " bytes: " + HexUtils.hexDump(result));
    }

    @Ignore("Fail 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testNoPadWithRsaOrigKey() throws Exception {
        sign(null, false, false);
    }

    @Ignore("Fail 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithRsa_VrfyNoPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, false);
        verifyWithTestPublicKey(sigValue, false, false);
    }

    @Ignore("Fail 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithRsa_VrfyNoPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, false);
        verifyWithTestPublicKey(sigValue, false, true);
    }

    @Ignore("Fail 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithRsa_VrfyPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, false);
        verifyWithTestPublicKey(sigValue, true, false, true); // expected to fail: sign with no pad won't verify with pad
    }

    @Ignore("Fail 'input requires padding' due to bug 7623")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithRsa_VrfyPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, false);
        verifyWithTestPublicKey(sigValue, true, true, true); // expected to fail: sign with no pad won't verify with pad
    }

    @Ignore("Verify fail: verify returns false.  Likely due to bug 7623 (if RSA won't sign unpadded input, makes sense that it won't verify it either)")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithBc_VrfyNoPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, true);
        verifyWithTestPublicKey(sigValue, false, false);
    }

    @Test
    @BugNumber(7623)
    public void testSigNoPadWithBc_VrfyNoPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, true);
        verifyWithTestPublicKey(sigValue, false, true);
    }

    @Ignore("Verify fail: verify returns false.  Padded verify with RSA expects different sig value than produced by unpadded sign with BC")
    @Test
    @BugNumber(7623)
    public void testSigNoPadWithBc_VrfyPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, true);
        verifyWithTestPublicKey(sigValue, true, false, true); // expected to fail: sign with no pad won't verify with pad
    }

    @Test
    @BugNumber(7623)
    public void testSigNoPadWithBc_VrfyPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, false, true);
        verifyWithTestPublicKey(sigValue, true, true, true); // expected to fail: sign with no pad won't verify with pad
    }

    @Test
    @BugNumber(7623)
    public void testSigPadWithRsa_VrfyNoPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, false);
        verifyWithTestPublicKey(sigValue, false, false, true); // expected to fail: sign with pad won't verify with no pad
    }

    @Ignore("Verify fail: 'error decoding signature bytes'")
    @Test
    @BugNumber(7623)
    public void testSigPadWithRsa_VrfyNoPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, false);
        verifyWithTestPublicKey(sigValue, false, true, true); // expected to fail: sign with pad won't verify with no pad
    }

    @Test
    @BugNumber(7623)
    public void testSigPadWithRsa_VrfyPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, false);
        verifyWithTestPublicKey(sigValue, true, false);
    }

    @Ignore("Verify fail: 'error decoding signature bytes'")
    @Test
    @BugNumber(7623)
    public void testSigPadWithRsa_VrfyPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, false);
        verifyWithTestPublicKey(sigValue, true, true);
    }

    @Test
    @BugNumber(7623)
    public void testSigPadWithBc_VrfyNoPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, true);
        verifyWithTestPublicKey(sigValue, false, false, true); // expected to fail: signing with padding won't verify without padding
    }

    @Test
    @BugNumber(7623)
    public void testSigPadWithBc_VrfyNoPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, true);
        verifyWithTestPublicKey(sigValue, false, true, true); // expected to fail: sign with pad won't verify without pad
    }

    @Ignore("Verify fail: verify returns false.  Padded verify with RSA expects different sig value than produced by padded sign with BC")
    @Test
    @BugNumber(7623)
    public void testSigPadWithBc_VrfyPadWithRsa() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, true);
        verifyWithTestPublicKey(sigValue, true, false);
    }

    @Test
    @BugNumber(7623)
    public void testSigPadWithBc_VrfyPadWithBc() throws Exception {
        byte[] sigValue = sign(testPrivateKeyBytes, true, true);
        verifyWithTestPublicKey(sigValue, true, true);
    }

    // Test ClientHandshaker playback, with and without using Bouncy Castle instead of RSA jSafe, and with and without manually padding the output
    private byte[] sign(byte[] privateKeyBytes, boolean manualPadding, boolean useBc) throws Exception {
        if (privateKeyBytes == null)
            privateKeyBytes = new BASE64Decoder().decodeBuffer(
                    "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDDk9GwJp2V9Mocze7kdaR+6cC0tpjJcFEurLd5IS/bbH738LuD/qA9rz/XClNmzpRw=");
        final KeyFactory keyFactory = JceProvider.getKeyFactory("EC", useBc ? new BouncyCastleProvider() : null);
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        byte[] shaCloneDigest = manualPadding ? PADDED_48_BYTES : UNPADDED_20_BYTES;

        Signature sig = JceProvider.getSignature("NONEwithECDSA", useBc ? new BouncyCastleProvider() : null);
        sig.initSign(privateKey, secureRandom());
        sig.update(shaCloneDigest);
        byte[] result = sig.sign(); // fail here if RSA and no manualPadding: java.security.SignatureException: The input requires padding, but NoPad was instantiated
        logger.info("Sign result: "  + (manualPadding ? "padded " : "notPadded ") +
                    (useBc ? "BC " : "RSA ") + result.length + " bytes: " + HexUtils.hexDump(result));
        return result;
    }

    private void verifyWithTestPublicKey(byte[] sigValue, boolean manualPadding, boolean useBc) throws Exception {
        verifyWithTestPublicKey(sigValue, manualPadding, useBc, false);
    }

    private void verifyWithTestPublicKey(byte[] sigValue, boolean manualPadding, boolean useBc, boolean expectFail) throws Exception {
        final KeyFactory keyFactory = JceProvider.getKeyFactory("EC", useBc ? new BouncyCastleProvider() : null);
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(testPublicKeyBytes));
        byte[] shaCloneDigest = manualPadding ? PADDED_48_BYTES : UNPADDED_20_BYTES;

        Signature verifier = JceProvider.getSignature("NONEwithECDSA", useBc ? new BouncyCastleProvider() : null);
        verifier.initVerify(publicKey);
        verifier.update(shaCloneDigest);
        boolean result = verifier.verify(sigValue);
        logger.info("Verify result: "  + (manualPadding ? "padded " : "notPadded ") +
                    (useBc ? "BC " : "RSA ") + (result ? "VERIFIED" : "**MISMATCH**"));
        if (expectFail)
            assertFalse("Signature verify expected to fail", result);
        else
            assertTrue("Signature verify expected to succeed", result);
    }

    private SecureRandom secureRandom() {
        // Create a fake SecureRandom that will produce the same random bytes each time
        int b = 34, c = 1, d = 17;
        byte[] randomBytes = new byte[8192];
        for (int i = 0; i < randomBytes.length; ++i) {
            randomBytes[i] = (byte)(b ^ c ^ d);
            c = (c + 1) * 1487;
            d = (d + 3) * 2767;
            b = (b + 7) * 23;
        }
        return new FixedSecureRandom(randomBytes);
    }
}
