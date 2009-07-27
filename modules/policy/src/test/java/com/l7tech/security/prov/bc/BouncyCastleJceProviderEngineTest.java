package com.l7tech.security.prov.bc;

import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.Pair;
import static org.junit.Assert.*;
import org.junit.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

/**
 *
 */
public class BouncyCastleJceProviderEngineTest {
    // Unpadded 20 byte SHA-1 hash output to be signed
    private static final byte[] UNPADDED_20_BYTES = new byte[] { 29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45 };

    @BeforeClass
    public static void installBcProvider() {
        final String bcEngineClass = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
        System.setProperty("com.l7tech.common.security.jceProviderEngine", bcEngineClass);
        JceProvider.init();
        assertEquals(bcEngineClass, JceProvider.getEngineClass());
    }

    @Test
    public void test160BitCurve() throws Exception {
        Pair<X509Certificate,PrivateKey> got = new TestCertificateGenerator().curveName("secp160r1").generateWithKey();
        PrivateKey privateKey = got.right;
        PublicKey publicKey = got.left.getPublicKey();

        Signature sig = Signature.getInstance("NONEwithECDSA");
        sig.initSign(privateKey);
        sig.update(UNPADDED_20_BYTES);
        byte[] sigValue = sig.sign();

        Signature vrfy = Signature.getInstance("NONEwithECDSA");
        vrfy.initVerify(publicKey);
        vrfy.update(UNPADDED_20_BYTES);
        assertTrue(vrfy.verify(sigValue));

    }
}
