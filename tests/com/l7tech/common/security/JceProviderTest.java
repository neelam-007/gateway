/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import com.l7tech.common.security.xml.SoapMsgSigner;
import com.l7tech.common.security.xml.XmlMangler;
import com.l7tech.common.security.xml.XmlManglerTest;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

/**
 * Test the performance of a JceProviderEngine.
 *
 * @author mike
 * @version 1.0
 */
public class JceProviderTest {
    private static final Logger log = Logger.getLogger(JceProviderTest.class.getName());
    //$ ls c:/cygwin/kstores
    //ca.cer  ca.ks  ssl.csr  ssl.ks  ssl_self.cer
    private static final String DIR = "/opt/java/tomcat/kstores/";
    private static String KEY_STORE = DIR + "ca.ks";
    private static String PK_PASS = "tralala";
    private static String ALIAS = "ssgroot";
    private static String STORE_PASS = "tralala";
    public static final String USAGE = "Usage: JceProviderTest [phaos|bc|rsa|ncipher] scale";

    public static interface Testable {
        void run() throws Throwable;
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 2 || args[0].length() < 1 || args[1].length() < 1)
            throw new IllegalArgumentException(USAGE);
        String prov = args[0].trim();
        int scale = Integer.parseInt(args[1]);
        String driver;
        if ("phaos".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.phaos.PhaosJceProviderEngine";
        } else if ("rsa".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.rsa.RsaJceProviderEngine";
        } else if ("bc".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
        } else if ("ncipher".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.ncipher.NcipherJceProviderEngine";
        } else
            throw new IllegalArgumentException(USAGE);
        System.setProperty(JceProvider.ENGINE_PROPERTY, driver);

        final byte[] keyBytes = new byte[]{
            (byte)0x9f, (byte)0x04, (byte)0xe4, (byte)0xcf, (byte)0x95, (byte)0x9e, (byte)0xd6, (byte)0x16,
            (byte)0xa1, (byte)0x58, (byte)0xf9, (byte)0x8a, (byte)0xc6, (byte)0x2c, (byte)0xf7, (byte)0x33,
            (byte)0xc8, (byte)0x1b, (byte)0xe2, (byte)0x67, (byte)0xd1, (byte)0x52, (byte)0xa0, (byte)0xb1,
            (byte)0x75, (byte)0xcf, (byte)0x38, (byte)0x0c, (byte)0xc1, (byte)0xf8, (byte)0x61, (byte)0xcd,
        };

        final Key key = new AesKey(keyBytes, 128);
        final RsaSignerEngine signer = JceProvider.createRsaSignerEngine(KEY_STORE, STORE_PASS, ALIAS, PK_PASS);
        CertificateRequest csr;
        KeyPair kp; // client cert private (and public) key
        X509Certificate signedClientCert; // signedClientCert client cert
        Document testDoc = XmlManglerTest.makeTestMessage();
        final String testXml = XmlUtil.documentToString(testDoc);
        Document signedDocument;
        String signedXml;

        {
            System.setProperty("com.l7tech.common.security.jceProviderEngine", driver);
            JceProvider.init();
            log.info("Using crypto provider: " + JceProvider.getProvider().getName());

            log.info("pretest: generating key pair...");
            kp = JceProvider.generateRsaKeyPair();
            log.info("Public key: " + publicKeyInfo(kp.getPublic()));
            log.info("Private key: " + privateKeyInfo(kp.getPrivate()));

            log.info("pretest: generating a certificate signing request...");
            csr = JceProvider.makeCsr("mike", kp);
            log.info("CSR username = " + csr.getSubjectAsString());
            log.info("CSR public key: " + publicKeyInfo(csr.getPublicKey()));

            byte[] csrEnc = csr.getEncoded();
            signedClientCert = (X509Certificate)signer.createCertificate(csrEnc);
            log.info("Signed: " + CertUtils.toString(signedClientCert));

            log.info("pretest: signing XML message");
            signedDocument = XmlUtil.stringToDocument(testXml);
            new SoapMsgSigner().signEnvelope(signedDocument, kp.getPrivate(), signedClientCert);
            signedXml = XmlUtil.documentToString(signedDocument);
            log.info("Signed XML message: " + signedXml);

            log.info("pretest: checking XML message signature");
            new SoapMsgSigner().validateSignature(XmlUtil.stringToDocument(signedXml));
        }

        reportTime("Empty loop (baseline)", 10000 * scale, new Testable() {
            public void run() {
            }
        });

        reportTime("Generate key pair", 4 * scale, new Testable() {
            public void run() {
                JceProvider.generateRsaKeyPair();
            }
        });

        final byte[] csrEnc = csr.getEncoded();
        reportTime("Sign CSR", 50 * scale, new Testable() {
            public void run() throws Exception {
                signer.createCertificate(csrEnc);
            }
        });

        reportTime("Parse document (baseline)", 1000 * scale, new Testable() {
            public void run() throws Throwable {
                XmlUtil.stringToDocument(testXml);
            }
        });

        final Document encryptedDoc = XmlUtil.stringToDocument(testXml);
        XmlMangler.encryptXml(encryptedDoc, keyBytes, "MyKeyName");
        final String encryptedXml = XmlUtil.documentToString(encryptedDoc);

        reportTime("Encrypt document", 1000 * scale, new Testable() {
            public void run() throws Throwable {
                Document blah = XmlUtil.stringToDocument(testXml);
                XmlMangler.encryptXml(blah, keyBytes, "MyKeyName");
            }
        });

        reportTime("Decrypt document", 200 * scale, new Testable() {
            public void run() throws Throwable {
                Document blah = XmlUtil.stringToDocument(encryptedXml);
                XmlMangler.decryptXml(blah, key);
            }
        });

        final String fsigned = signedXml;
        final X509Certificate fsignedClientCert = signedClientCert;
        final PrivateKey privateKey = kp.getPrivate();
        reportTime("Sign XML message", 20 * scale, new Testable() {
            public void run() throws Throwable {
                new SoapMsgSigner().signEnvelope(XmlUtil.stringToDocument(fsigned), privateKey, fsignedClientCert);
            }
        });

        reportTime("Validate signedClientCert XML message", 80 * scale, new Testable() {
            public void run() throws Throwable {
                new SoapMsgSigner().validateSignature(XmlUtil.stringToDocument(fsigned));
            }
        });
    }

    private static String trunc10(String orig) {
        if (orig.length() <= 10) {
            return orig;
        } else {
            return orig.substring(0, 10) + "...";
        }
    }

    private static String privateKeyInfo(PrivateKey pk) {
        if (pk instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey = (RSAPrivateKey)pk;
            String modulus = rsaKey.getModulus().toString(16);
            String strength = (modulus.length() * 4) + " bits";
            String exponent = rsaKey.getPrivateExponent().toString(16);
            return "RSA key; " + strength + "; modulus=" + trunc10(modulus) + "; privateExponent=" + trunc10(exponent);
        } else
            return "Unknown private key";
    }

    private static String publicKeyInfo(PublicKey pk) {
        if (pk instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey)pk;
            String modulus = rsaKey.getModulus().toString(16);
            String strength = (modulus.length() * 4) + " bits";
            String exponent = rsaKey.getPublicExponent().toString(16);
            return "RSA key; " + strength + "; modulus=" + trunc10(modulus) + "; publicExponent=" + trunc10(exponent);
        } else
            return "Unknown public key";
    }

    /**
     * Time how long the specified operation takes to complete.
     */
    private static long time(Testable r) throws Throwable {
        long before = System.currentTimeMillis();
        r.run();
        long after = System.currentTimeMillis();
        return after - before;
    }

    private static void reportTime(String name, final int count, final Testable r) throws Throwable {
        long t = time(new Testable() {
            public void run() throws Throwable {
                for (int i = 0; i < count; ++i)
                    r.run();
            }
        });
        log.info("Timed " + count + " executions of " + name + ": " + t + " ms; " + (((double)t) / count) + " ms per execution; " + (((double)count) / (t + 1)) * 1000 + " per second");
    }
}
