/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Random;
import java.security.cert.X509Certificate;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.HexUtils;

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
    
    public void testEncryptKeyWithRsaAndPad() throws Exception {
        SecureRandom rand = new SecureRandom();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        String paddedB64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, publicKey, rand);
        logger.info("Got back: " + paddedB64);
    }

}
