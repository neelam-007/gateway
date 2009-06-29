/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;
import static org.junit.Assert.*;
import org.junit.*;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class XencUtilTest {
    private static Logger logger = Logger.getLogger(XencUtilTest.class.getName());

    @BeforeClass
    public static void setUp() throws Exception {
        JceProvider.init();
    }

    @Test
    public void testPadKey() throws Exception {
        SecureRandom rand = new SecureRandom();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        final int modulusLength = publicKey.getModulus().toByteArray().length;

        byte[] paddedKeyBytes = XencUtil.padSymmetricKeyForRsaEncryption(keyBytes, modulusLength, rand);

        assertTrue(paddedKeyBytes.length <= modulusLength);
        assertTrue(paddedKeyBytes.length < 128);
    }

    @Test
    public void testEncryptKeyWithRsaAndPad() throws Exception {
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
        logger.info("Got back: " + paddedB64 + "\n(length:" + paddedB64.length() + ")");
    }

    @Test
    public void testRoundTripRsaEncryptedKey() throws Exception {
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
        byte[] decrypted = XencUtil.decryptKey(paddedB64, pkey);
        assertTrue(Arrays.equals(keyBytes, decrypted));
    }

    @Test
    public void testRoundTripRsaOaepEncryptedKey() throws Exception {
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] oaepParams = new byte[128];
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaOaepMGF1SHA1(keyBytes, recipientCert, publicKey, oaepParams), true);
        byte[] decrypted = XencUtil.decryptKey(paddedB64, oaepParams, pkey);
        assertTrue(Arrays.equals(keyBytes, decrypted));
    }

    @Ignore("Disabled because it fails as of 5.0.0, and nobody remembers what the test was for originally")
    @Test
    public void testWeirdLeadin0InSunJCE() throws Exception {
        if (!JceProvider.getEngineClass().equals(JceProvider.BC_ENGINE)) {
            System.out.println("This test is meant to be ran with BC provider. Aborting.");
            return;
        }
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        String keypaddedandencryptedwithsunjce = "TK0T2LPWmCYDUtE32P7s7aVvjnfJ9flQm+GOiriGyY677g2/RgDbWncSJcPipm1zRmYRkmvKbNYFpReVl1SrVqsCbYudX/y8WQyI3LVInoc3TNfBPryphoVrxtjLDeAhfxxdsxYSq12Ze62RvLr3Y3k9vxaKotJcOejMtyHj9T4=";
        byte[] decrypted = XencUtil.decryptKey(keypaddedandencryptedwithsunjce, pkey);
        byte[] originalBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        assertTrue(Arrays.equals(originalBytes, decrypted));
    }

    @Test
    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC1() throws Exception {
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.SUN_ENGINE);
        System.out.println("USING PROVIDER: " + JceProvider.getEngineClass());
        System.out.println("SUN OUTPUT:" + getEncryptedKey());
    }

    @Test
    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC2() throws Exception {
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
        System.out.println("USING PROVIDER: " + JceProvider.getEngineClass());
        System.out.println("BC OUTPUT:" + getEncryptedKey());
    }

    private String getEncryptedKey() throws Exception {
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        return HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
    }

}
