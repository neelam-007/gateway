/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
<<<<<<< XencUtilTest.java
=======
 *
 * $Id$
>>>>>>> 1.8.18.1
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class XencUtilTest extends TestCase {
    private static Logger logger = Logger.getLogger(XencUtilTest.class.getName());

    public XencUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(XencUtilTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        // setup the provider you want to test here
        //System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.SUN_ENGINE);
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
    }

    public void testPadKey() throws Exception {
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        SecureRandom rand = new SecureRandom();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        final int modulusLength = publicKey.getModulus().toByteArray().length;

        byte[] paddedKeyBytes = XencUtil.padSymmetricKeyForRsaEncryption(keyBytes, modulusLength, rand);

        assertTrue(paddedKeyBytes.length <= modulusLength);
        assertTrue(paddedKeyBytes.length < 128);
    }
    
    public void testEncryptKeyWithRsaAndPad() throws Exception {
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        SecureRandom rand = new SecureRandom();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        String paddedB64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, publicKey, rand);
        logger.info("Got back: " + paddedB64 + "\n(length:" + paddedB64.length() + ")");
    }

    public void testRoundTripRsaEncryptedKey() throws Exception {
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        SecureRandom rand = new SecureRandom();
        String paddedB64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, publicKey, rand);
        byte[] decrypted = XencUtil.decryptKey(paddedB64, pkey).getDecryptedKeyBytes();
        assertTrue(Arrays.equals(keyBytes, decrypted));
    }

    public void testWeirdLeadin0InSunJCE() throws Exception {
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        if (!JceProvider.getAsymmetricJceProvider().getName().equals("BC")) {
            System.out.println("This test is meant to be ran with BC provider. Aborting.");
            return;
        }
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        String keypaddedandencryptedwithsunjce = "TK0T2LPWmCYDUtE32P7s7aVvjnfJ9flQm+GOiriGyY677g2/RgDbWncSJcPipm1zRmYRkmvKbNYFpReVl1SrVqsCbYudX/y8WQyI3LVInoc3TNfBPryphoVrxtjLDeAhfxxdsxYSq12Ze62RvLr3Y3k9vxaKotJcOejMtyHj9T4=";
        byte[] decrypted = XencUtil.decryptKey(keypaddedandencryptedwithsunjce, pkey).getDecryptedKeyBytes();
        byte[] originalBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        assertTrue(Arrays.equals(originalBytes, decrypted));
    }

    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC1() throws Exception {
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.SUN_ENGINE);
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        System.out.println("SUN OUTPUT:" + getEncryptedKey());
    }

    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC2() throws Exception {
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
        System.out.println("USING PROVIDER: " + JceProvider.getAsymmetricJceProvider().getName());
        System.out.println("BC OUTPUT:" + getEncryptedKey());
    }

    private String getEncryptedKey() throws Exception {
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        Random notrandomonpurpose = new Random(123);
        String paddedB64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, publicKey, notrandomonpurpose);
        return paddedB64;
    }

}
