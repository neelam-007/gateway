/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.prov;

import com.l7tech.message.Message;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.keys.AesKey;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
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
    private static final String DFLT_DIR = "/ssg/etc/keys/";
    private static final String DFLT_PROVIDER = "bc";
    public static final String KS = "ca.ks";
    private static String DFLT_STORE_TYPE = "JKS";

    public static final String USAGE = "Usage: JceProviderTest (phaos|bc|rsa|ncipher|entrust|ibm) scale dir keypass storepass [storetype] [concurrency]";
    private static final boolean LOAD_CSR_FROM_DISK = false;

    @Test
    public void testJceProviderMappings() {
        String bcmapped = JceProvider.mapEngine( "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine" );
        String sjmapped = JceProvider.mapEngine( "com.l7tech.common.security.prov.sun.SunJceProviderEngine" );
        String pjmapped = JceProvider.mapEngine( "com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine" );

        Assert.assertEquals("BC Provider mapped", JceProvider.BC_ENGINE, bcmapped);
        Assert.assertEquals("SUN Provider mapped", JceProvider.SUN_ENGINE, sjmapped);
        Assert.assertEquals("PKCS11 Provider mapped", JceProvider.PKCS11_ENGINE, pjmapped);
    }

    public static interface Testable {
        void run() throws Throwable;
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 2 || args[0].length() < 1 || args[1].length() < 1)
            throw new IllegalArgumentException(USAGE);
        String prov = null;
        String sscale = null;
        String dir = null;
        String pkpass = null;
        String kspass = null;
	    String kstype = null;
        String sconcur = null;

        if ( args.length >= 1 ) prov = args[0].trim();
        if ( args.length >= 2 ) sscale = args[1].trim();
        if ( args.length >= 3 ) dir = args[2].trim();
        if ( args.length >= 4 ) pkpass = args[3].trim();
        if ( args.length >= 5 ) kspass = args[4].trim();
	    if ( args.length >= 6 ) kstype = args[5].trim();
        if ( args.length >= 7 ) sconcur = args[6].trim();

        if ( prov == null || prov.length() == 0 ) prov = DFLT_PROVIDER;
        if ( sscale == null || sscale.length() == 0 ) sscale = "1";
        if ( dir == null || dir.length() == 0 ) dir = DFLT_DIR;
//        if ( pkpass == null || pkpass.length() == 0 ) pkpass = DFLT_PK_PASS;
//        if ( kspass == null || kspass.length() == 0 ) kspass = DFLT_STORE_PASS;
        if ( kstype == null || kstype.length() == 0 ) kstype = DFLT_STORE_TYPE;
        if ( sconcur == null || sconcur.length() == 0 ) sconcur = DFLT_CONCUR;

        int scale = Integer.parseInt(sscale);
        int concur = Integer.parseInt(sconcur);
        String ps = System.getProperty("file.separator");
        if (!dir.endsWith(ps)) dir += ps;

        String driver;
        if ("phaos".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.phaos.PhaosJceProviderEngine";
        } else if ("rsa".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.rsa.RsaJceProviderEngine";
        } else if ("bc".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
        } else if ("ncipher".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.ncipher.NcipherJceProviderEngine";
        } else if ("entrust".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
        }else
            throw new IllegalArgumentException("Unknown provider " + USAGE);
        System.setProperty(JceProvider.ENGINE_PROPERTY, driver);

        final byte[] keyBytes = new byte[]{
            (byte)0x9f, (byte)0x04, (byte)0xe4, (byte)0xcf, (byte)0x95, (byte)0x9e, (byte)0xd6, (byte)0x16,
            (byte)0xa1, (byte)0x58, (byte)0xf9, (byte)0x8a, (byte)0xc6, (byte)0x2c, (byte)0xf7, (byte)0x33,
            (byte)0xc8, (byte)0x1b, (byte)0xe2, (byte)0x67, (byte)0xd1, (byte)0x52, (byte)0xa0, (byte)0xb1,
            (byte)0x75, (byte)0xcf, (byte)0x38, (byte)0x0c, (byte)0xc1, (byte)0xf8, (byte)0x61, (byte)0xcd,
        };

        final Key key = new AesKey(keyBytes, 128); // test key for testing symmetric bulk operations
        CertificateRequest csr;
        KeyPair kp; // client cert private (and public) key
        X509Certificate signedClientCert; // signedClientCert client cert

        {
            JceProvider.init();
            String asymProvName = JceProvider.getAsymmetricJceProvider().getName();
            log.info("Using asymmetric cryptography provider: " + JceProvider.getAsymmetricJceProvider().getName());
            log.info("Using symmetric cryptography provider: " + JceProvider.getSymmetricJceProvider().getName());

            FileInputStream sslks = null;
            KeyStore ks = null;
            try {
                if ( prov.equals("ncipher") ) {
                    ks = KeyStore.getInstance(kstype,asymProvName);
                } else {
                    ks = KeyStore.getInstance(kstype);
                }
                sslks = new FileInputStream(dir + "ssl.ks");
                ks.load(sslks,kspass.toCharArray());
            } catch ( Exception e ) {
                log.log(Level.WARNING,e.getMessage(),e);
            } finally {
                if ( sslks != null ) sslks.close();
            }

            log.info("pretest: loading key pair...");
            PrivateKey priv = (PrivateKey)ks.getKey("tomcat",pkpass.toCharArray());
            X509Certificate cert = (X509Certificate)ks.getCertificate("tomcat");

            kp = new KeyPair(cert.getPublicKey(),priv);

            log.info("Public key: " + publicKeyInfo(kp.getPublic()));
            log.info("Private key: " + privateKeyInfo(kp.getPrivate()));

            if (LOAD_CSR_FROM_DISK) {
                // Load CSR from disk
                log.info("pretest: loading a certificate signing request...");
                byte[] bytes = IOUtils.slurpStream( new FileInputStream("/" + dir + "ssl.cer"));
                csr = new BouncyCastleCertificateRequest(new PKCS10CertificationRequest(bytes), asymProvName);
            } else {
                // Make our own CSR
                log.info("pretest: generating certificate signing requset...");
                KeyPair csrKeypair = JceProvider.generateRsaKeyPair();
                csr = JceProvider.makeCsr("mike", csrKeypair);
            }
            log.info("CSR username = " + csr.getSubjectAsString());

            signedClientCert = cert;
            log.info("Signed: " + CertUtils.toString(signedClientCert));

            log.info("pretest: signing XML message");
//            final WssDecoratorTest wssDecoratorTest = new WssDecoratorTest("WssDecoratorTest");
//            WssDecoratorTest.TestDocument td = wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument();
//            new WssDecoratorImpl().decorateMessage(new Message(td.c.message), wssDecoratorTest.makeDecorationRequirements(td));

            log.info("pretest: checking XML message signature");
            ProcessorResult processorResult = new WssProcessorImpl().undecorateMessage(
                    new Message(null/*td.c.message*/),
                    null,
                    null,
                    null//new WrapSSTR(TestDocuments.getDotNetServerCertificate(),
            //                     TestDocuments.getDotNetServerPrivateKey())
            );
            log.info("signature verified on " + processorResult.getElementsThatWereSigned().length + " elements");
        }

        reportTime("Empty loop (baseline)", 10000 * scale, concur, new Testable() {
            public void run() throws Throwable {
            }
        });

//        final WssDecoratorTest wssDecoratorTest = new WssDecoratorTest("WssDecoratorTest");
//        reportTime("Prepare test document (baseline)", 10000 * scale, concur, new Testable() {
//            public void run() throws Throwable {
//                WssDecoratorTest.TestDocument td = wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument();
//                wssDecoratorTest.makeDecorationRequirements(td);
//            }
//        });

        reportTime("Generate key pair", 4 * scale, concur, new Testable() {
            public void run() {
                JceProvider.generateRsaKeyPair();
            }
        });

        // TODO restore when not testing nCipher (which can't create CSRs)
//        final byte[] csrEnc = csr.getEncoded();
//        reportTime("Sign CSR", 50 * scale, concur, new Testable() {
//            public void run() throws Exception {
//                signer.createCertificate(csrEnc);
//            }
//        });

//        final Document testDoc = wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument().c.message;
//        final String testXml = XmlIOUtils.nodeToString(testDoc);

//        reportTime("Parse document (baseline)", 1000 * scale, concur, new Testable() {
//            public void run() throws Throwable {
//                XmlIOUtils.stringToDocument(testXml);
//            }
//        });
//
//        log.info("Before encryption: " + testXml);
        final String encryptedXml;
        {
//            WssDecoratorTest.TestDocument td = wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument();
//            DecorationRequirements decorationRequirements = wssDecoratorTest.makeDecorationRequirements(td);
//            new WssDecoratorImpl().decorateMessage(new Message(td.c.message), decorationRequirements);
//            encryptedXml = XmlIOUtils.nodeToString(td.c.message);
        }
//        log.info("Encrypted XML message: " + encryptedXml);
//
//        reportTime("Encrypt and sign document", 1000 * scale, concur, new Testable() {
//            public void run() throws Throwable {
//                WssDecoratorTest.TestDocument td = wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument();
//                DecorationRequirements decorationRequirements = wssDecoratorTest.makeDecorationRequirements(td);
//                new WssDecoratorImpl().decorateMessage(new Message(td.c.message), decorationRequirements);
//            }
//        });

        reportTime("Decrypt document and check signature", 200 * scale, concur, new Testable() {
            public void run() throws Throwable {
//                Document blah = XmlIOUtils.stringToDocument(encryptedXml);
//                new WssProcessorImpl().undecorateMessage(
//                        new Message(blah),
//                        null,
//                        null,
//                         null//new WrapSSTR(TestDocuments.getDotNetServerCertificate(),
//                 //                     TestDocuments.getDotNetServerPrivateKey())
//                );
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
            try {
                String modulus = rsaKey.getModulus().toString(16);
                String strength = (modulus.length() * 4) + " bits";
                String exponent = rsaKey.getPrivateExponent().toString(16);
                return "RSA key; " + strength + "; modulus=" + trunc10(modulus) + "; privateExponent=" + trunc10(exponent);
            } catch ( RuntimeException rte ) {
                return "RSA key; assuming hardware: " + rte.getMessage();
            }
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

    private static class Job extends Thread {
        Job( String name, Testable t ) {
            super(name);
            testable = t;
        }

        public void run() {
            try {
                long before = System.currentTimeMillis();
                testable.run();
                long after = System.currentTimeMillis();
                time = after - before;
            } catch ( Throwable throwable ) {
                throw new RuntimeException(throwable);
            }
        }

        Testable testable;
        long time;
    }

    private static void reportTime(String name, final int count, final int concur, final Testable r) throws Throwable {
        Job[] jobs = new Job[concur];
        // Create jobs
        for ( int i = 0; i < concur; i++ ) {
            jobs[i] = new Job(name + "#" + i, new Testable() {
                public void run() throws Throwable {
                    for (int i = 0; i < count; ++i)
                        r.run();
                }
            });
        }

        // Start jobs
        long before = System.currentTimeMillis();
        for ( int i = 0; i < jobs.length; i++ ) {
            jobs[i].start();
        }

        // Get results
        for ( int i = 0; i < jobs.length; i++ ) {
            Job job = jobs[i];
            job.join();
            long time = job.time;
            log.info("Thread #" + i + ": Timed " + count + " executions of " + name + ": "
                     + time + " ms; " + (((double)time) / count) + " ms per execution; "
                     + (((double)count) / (time + 1)) * 1000 + " per second");
        }

        long after = System.currentTimeMillis();

        long num = count * concur;
        long total = after - before;
        
        log.info("All Threads: Timed " + num + " executions of " + name
                 + ": " + total + " ms; " + (((double)total) / num) + " ms per execution; "
                 + (((double)num) / (total + 1)) * 1000 + " per second\n\n");
    }

    private static final String DFLT_CONCUR = "1";
}
