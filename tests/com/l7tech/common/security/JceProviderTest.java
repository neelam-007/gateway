/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import org.apache.log4j.Category;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.l7tech.common.util.CertUtils;

/**
 * Test the performance of a JceProviderEngine.
 * @author mike
 * @version 1.0
 */
public class JceProviderTest {
    private static final Category log = Category.getInstance(JceProviderTest.class);
    //$ ls c:/cygwin/kstores
    //ca.cer  ca.ks  ssl.csr  ssl.ks  ssl_self.cer
    private static final String DIR = "c:/cygwin/kstores/";
    private static String KEY_STORE = DIR + "ca.ks";
    private static String PK_PASS = "secret";
    private static String ALIAS = "ssgroot";
    private static String STORE_PASS = "secret";

    public static interface Testable {
        void run() throws Throwable;
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1 || args[0] == null || args[0].length() < 1)
            throw new IllegalArgumentException("Usage: JceProviderTest [phaos|bc|rsa]");
        String prov = args[0].trim();

        String driver;
        if ("phaos".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.phaos.PhaosJceProviderEngine";
        } else if ("rsa".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.rsa.RsaJceProviderEngine";
        } else if ("bc".equalsIgnoreCase(prov)) {
            driver = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
        } else
            throw new IllegalArgumentException("Usage: JceProviderTest [phaos|bc|rsa]");

        System.setProperty("com.l7tech.common.security.jceProviderEngine", driver);
        JceProvider.init();
        log.info("Using crypto provider: " + JceProvider.getProvider().getName());

        log.info("pretest: generating key pair...");
        KeyPair kp = JceProvider.generateRsaKeyPair();
        log.info("Public key: " + publicKeyInfo(kp.getPublic()));
        log.info("Private key: " + privateKeyInfo(kp.getPrivate()));

        log.info("pretest: generating a certificate signing request...");
        CertificateRequest csr = JceProvider.makeCsr("mike", kp);
        log.info("CSR username = " + csr.getSubjectAsString());
        log.info("CSR public key: " + publicKeyInfo(csr.getPublicKey()));

        final byte[] csrEnc = csr.getEncoded();

        final RsaSignerEngine signer = JceProvider.createRsaSignerEngine(KEY_STORE, STORE_PASS, ALIAS, PK_PASS);
        Certificate signed = signer.createCertificate(csrEnc);
        log.info("Signed: " + CertUtils.toString((X509Certificate) signed));

        reportTime("Empty loop", 10000, new Testable() {
            public void run() {
            }
        });

        reportTime("Generate key pair", 50, new Testable() {
            public void run() {
                JceProvider.generateRsaKeyPair();
            }
        });

        reportTime("Sign CSR", 50, new Testable() {
            public void run() throws Exception {
                signer.createCertificate(csrEnc);
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
            RSAPrivateKey rsaKey = (RSAPrivateKey) pk;
            String modulus = rsaKey.getModulus().toString(16);
            String strength = (modulus.length() * 4) + " bits";
            String exponent = rsaKey.getPrivateExponent().toString(16);
            return "RSA key; " + strength + "; modulus=" + trunc10(modulus) + "; privateExponent=" + trunc10(exponent);
        } else
            return "Unknown private key";
    }

    private static String publicKeyInfo(PublicKey pk) {
        if (pk instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) pk;
            String modulus = rsaKey.getModulus().toString(16);
            String strength = (modulus.length() * 4) + " bits";
            String exponent = rsaKey.getPublicExponent().toString(16);
            return "RSA key; " + strength + "; modulus=" + trunc10(modulus) + "; publicExponent=" + trunc10(exponent);
        } else
            return "Unknown public key";
    }

    /** Time how long the specified operation takes to complete. */
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
        log.info("Timed " + count + " executions of " + name + ": " + t + " ms");
    }
}
