package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;
import org.junit.*;
import sun.misc.BASE64Decoder;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 *
 */
public class RsaJceProviderEngineTest {

    @BeforeClass
    public static void installRsaProvider() {
        final String rsaEngineClass = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
        System.setProperty("com.l7tech.common.security.jceProviderEngine", rsaEngineClass);
        JceProvider.init();
        assertEquals(rsaEngineClass, JceProvider.getEngineClass());
    }

    @Ignore("Currently failing")
    @Test
    @BugNumber(7623)
    public void testSignatureInputPadding() throws Exception {
        final byte[] privateKeyBytes = new BASE64Decoder().decodeBuffer(
                "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDDk9GwJp2V9Mocze7kdaR+6cC0tpjJcFEurLd5IS/bbH738LuD/qA9rz/XClNmzpRw=");
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        byte[] shaCloneDigest = new byte[] { 29,-32,-40,21,-108,66,-111,87,90,6,-126,-127,113,-3,43,-25,-103,-96,15,45 };

        Signature sig = Signature.getInstance("NONEwithECDSA");
        sig.initSign(privateKey, new SecureRandom());
        sig.update(shaCloneDigest);
        sig.sign();  // java.security.SignatureException: The input requires padding, but NoPad was instantiated
    }

}
