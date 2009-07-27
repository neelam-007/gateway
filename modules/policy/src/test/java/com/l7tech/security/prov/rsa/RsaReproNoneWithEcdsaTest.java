package com.l7tech.security.prov.rsa;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.prov.JceProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import static org.junit.Assert.*;
import org.junit.*;
import sun.misc.BASE64Decoder;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 */
public class RsaReproNoneWithEcdsaTest {
    // Unpadded 20 byte SHA-1 hash output to be signed
    private static final byte[] UNPADDED_20_BYTES = new byte[] { 29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45 };

    // Same hash output as above, but padded by hand to 32 (256 bits) to match size of P-256 curve
    private static final byte[] PADDED_32_BYTES = new byte[] {
            29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private static final String P256_CERT =
            "MIIBfDCCASGgAwIBAgIJAI4eZo646tuYMAoGCCqGSM49BAMDMA8xDTALBgNVBAMTBHRlc3QwHhcN\n" +
            "MDkwNzI3MDA1ODA0WhcNMjkwNzIyMDA1ODA0WjAPMQ0wCwYDVQQDEwR0ZXN0MFkwEwYHKoZIzj0C\n" +
            "AQYIKoZIzj0DAQcDQgAET/rMl0ywQ/Vb336yLhZF7F37WhsP4bJbFJOzJQGWHhekn+5g4uD3Tk/O\n" +
            "+xoDUCqhRRlVuBy/vPB/RnX18Q22LKNmMGQwDgYDVR0PAQH/BAQDAgXgMBIGA1UdJQEB/wQIMAYG\n" +
            "BFUdJQAwHQYDVR0OBBYEFNGUPQS6v5tLb3xYdy9Er7P2NvqZMB8GA1UdIwQYMBaAFNGUPQS6v5tL\n" +
            "b3xYdy9Er7P2NvqZMAoGCCqGSM49BAMDA0kAMEQCIHiKkuf7A62tJf5EKIWlDrGqsXYIYgAgC/4T\n" +
            "BnXUCatsAiAOk/nSlC5tpShTrmrGumjsWlthzWvhe8a0lGEwgQm38QAA";
    private static final String P256_PRIVATE_KEY =
            "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCBSlamiMaWM8Z2QbLFq3sNVImZdlLqr\n" +
            "J7SkOFowlSN2LQ==";

    private static final String SECP160R1_CERT =
            "MIIBSDCCAQWgAwIBAgIIaVi9XB1uoDIwCgYIKoZIzj0EAwMwDzENMAsGA1UEAxMEdGVzdDAeFw0w\n" +
            "OTA3MjcwMTE2NTNaFw0yOTA3MjIwMTE2NTNaMA8xDTALBgNVBAMTBHRlc3QwPjAQBgcqhkjOPQIB\n" +
            "BgUrgQQACAMqAAS1J9BiJ/nxD12gRjl9zygzlzCjD8yNlTYW5M6TcwVNn0uIapPvTpH/o2YwZDAO\n" +
            "BgNVHQ8BAf8EBAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQUCcRyMtI3nFNO1X5I\n" +
            "TYGmRmc7UI8wHwYDVR0jBBgwFoAUCcRyMtI3nFNO1X5ITYGmRmc7UI8wCgYIKoZIzj0EAwMDMQAw\n" +
            "LgIVAN54EBXEUw6ohPD+Cpaso128XJVmAhUA1IcgBsgzNujK1LzmX6ugBMpVgpo=";
    private static final String SECP160R1_KEY =
            "MDICAQAwEAYHKoZIzj0CAQYFK4EEAAgEGzAZAgEBBBT0Z0Mn8li6x2LuDZziui2pWhplvQ==";

    @BeforeClass
    public static void installRsaProvider() {
        final String rsaEngineClass = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
        System.setProperty("com.l7tech.common.security.jceProviderEngine", rsaEngineClass);
        JceProvider.init();
        assertEquals(rsaEngineClass, JceProvider.getEngineClass());
    }

    @Test
    public void testP256SigRsaVrfyRsa() throws Exception {
        Signature sig = Signature.getInstance("NONEwithECDSA");
        sig.initSign(privateKey(P256_PRIVATE_KEY));
        sig.update(PADDED_32_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA");
        vrfy.initVerify(cert(P256_CERT).getPublicKey());
        vrfy.update(PADDED_32_BYTES);
        assertTrue(vrfy.verify(sigValue));
    }

    @Ignore("Fails verify with 'error decoding signature bytes'")
    @Test
    public void testP256SigRsaVrfyBc() throws Exception {
        Signature sig = Signature.getInstance("NONEwithECDSA");
        sig.initSign(privateKey(P256_PRIVATE_KEY));
        sig.update(PADDED_32_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA", new BouncyCastleProvider());
        vrfy.initVerify(toBc(cert(P256_CERT).getPublicKey()));
        vrfy.update(PADDED_32_BYTES);
        assertTrue(vrfy.verify(sigValue));
    }

    @Test
    public void testP256SigBcVrfyBc() throws Exception {
        Signature sig = Signature.getInstance("NONEwithECDSA", new BouncyCastleProvider());
        sig.initSign(toBc(privateKey(P256_PRIVATE_KEY)));
        sig.update(PADDED_32_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA", new BouncyCastleProvider());
        vrfy.initVerify(toBc(cert(P256_CERT).getPublicKey()));
        vrfy.update(PADDED_32_BYTES);
        assertTrue(vrfy.verify(sigValue));
    }

    @Ignore("Fails verify: verify returns false")
    @Test
    public void testP256SigBcVrfyRsa() throws Exception {
        Signature sig = Signature.getInstance("NONEwithECDSA", new BouncyCastleProvider());
        sig.initSign(toBc(privateKey(P256_PRIVATE_KEY)));
        sig.update(PADDED_32_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA");
        vrfy.initVerify(cert(P256_CERT).getPublicKey());
        vrfy.update(PADDED_32_BYTES);
        assertTrue(vrfy.verify(sigValue));
    }

    @Ignore("Fails with InvalidKeySpecException while trying to parse cert with RSA; RSA doesn't support the secp160r1 curve")
    @Test
    public void testP160SigBcVrfyRsa() throws Exception {
        Signature sig = Signature.getInstance("NONEwithECDSA", new BouncyCastleProvider());
        sig.initSign(toBc(privateKey(SECP160R1_KEY)));
        sig.update(UNPADDED_20_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA");
        vrfy.initVerify(cert(SECP160R1_CERT).getPublicKey());
        vrfy.update(UNPADDED_20_BYTES);
        assertTrue(vrfy.verify(sigValue));
    }

    private static PrivateKey toBc(PrivateKey in) throws Exception {
        assertEquals("PKCS#8", in.getFormat());
        return KeyFactory.getInstance(in.getAlgorithm(), new BouncyCastleProvider()).generatePrivate(new PKCS8EncodedKeySpec(in.getEncoded()));
    }

    private static PublicKey toBc(PublicKey in) throws Exception {
        assertEquals("X509", in.getFormat());
        return KeyFactory.getInstance(in.getAlgorithm(), new BouncyCastleProvider()).generatePublic(new X509EncodedKeySpec(in.getEncoded()));
    }

    private static X509Certificate cert(String in) throws Exception {
        return CertUtils.decodeFromPEM(in, false);
    }

    private static PrivateKey privateKey(String in) throws Exception {
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(new BASE64Decoder().decodeBuffer(in)));
    }

}
