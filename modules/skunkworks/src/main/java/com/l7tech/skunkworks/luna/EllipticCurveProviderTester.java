package com.l7tech.skunkworks.luna;

import sun.misc.BASE64Decoder;

import javax.crypto.KeyAgreement;
import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone test of various functionality required to do ECC key generation, key import, message level
 * signing and verification, and SSL cipher suites.
 */
public class EllipticCurveProviderTester {
    private static final Logger logger = Logger.getLogger(EllipticCurveProviderTester.class.getName());

    //
    //
    // --- Test parameters ---
    //
    //

    final SecurityProvider securityProvider;

    public EllipticCurveProviderTester(SecurityProvider securityProvider) {
        this.securityProvider = securityProvider;
    }

    //
    //
    // --- Test runner ---
    //
    //

    public boolean checkAllRequirements() {
        return checkSpecificRequirements(findTopLevelRequirements());
    }

    public boolean checkSpecificRequirements(String[] requirements) {
        List<String> results = new ArrayList<String>();
        boolean sawFailure = false;
        for (String requirement : requirements) {
            boolean result = testRequirement(requirement, results);
            if (!result)
                sawFailure = true;
        }
        System.err.flush();
        System.out.println("\nResults for provider " + securityProvider.shortname + ":\n");
        for (String result : results)
            System.out.println(result);

        System.out.println(sawFailure ? "\nAt least one top-level requirement failed." : "\nAll top-level requirements passed or skipped.");

        return !sawFailure;
    }

    String[] findTopLevelRequirements() {
        List<String> ret = new ArrayList<String>();
        Method[] methods = getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("test") && method.getReturnType() == void.class)
                ret.add(method.getName());
        }
        return ret.toArray(new String[ret.size()]);
    }

    boolean testRequirement(String requirement, List<String> results) {
        List<String> subresults = new ArrayList<String>();
        String err = doTestRequirement(requirement, subresults);
        String mess, ts, sts;
        boolean failed = false;
        if (err == null) {
            mess = "";
            ts = "PASS";
            sts = "OK";
        } else if (err.startsWith("skiptest: ")) {
            mess = err;
            ts = "SKIP";
            sts = "NA";
        } else {
            mess = err;
            ts = "FAIL";
            sts = "NO";
            failed = true;
        }
        if (requirement.startsWith("subsub"))
            results.add(String.format("        %-50s %s             %s", requirement, sts, mess));
        else if (requirement.startsWith("sub"))
            results.add(String.format("    %-54s %s             %s", requirement, sts, mess));
        else
            results.add(String.format("%-65s %s    %s", requirement, ts, mess));
        results.addAll(subresults);
        System.err.flush();
        System.out.flush();
        return !failed;
    }

    String doTestRequirement(String requirement, List<String> results) {
        Method method;
        Object[] args = {};
        try {
            method = getClass().getMethod(requirement);
        } catch (NoSuchMethodException e) {
            if (results == null)
                throw new RuntimeException("Missing requirement method: " + requirement, e);
            try {
                method = getClass().getMethod(requirement, List.class);
                args = new Object[] { results };
            } catch (NoSuchMethodException e1) {
                throw new RuntimeException("Missing requirement method: " + requirement, e);
            }
        }

        try {
            method.invoke(this, args);
            return null;
        } catch (InvocationTargetException e) {
            logger.log(Level.INFO, "failed " + requirement, e);
            return getMessage(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMessage(Throwable t) {
        if (t == null)
            return "null";
        Throwable r = t;
        while (r.getCause() != null)
            r = r.getCause();
        String msg = r.getMessage();
        return msg != null ? msg : r.getClass().getName();
    }

    //
    //
    // --- Tests ---
    //
    //

    public void testAtLeastOneMethodToGenerateEcKeyIsAvailable(List<String> results) {
        boolean worksWithStandardWay = testRequirement("subtestGenerateEcKey_standardJava5", results);
        boolean worksWithOidWay = testRequirement("subtestGenerateEcKey_standardJava5_byOid", results);
        assertTrue(worksWithStandardWay || worksWithOidWay, "need at least one to succeed");
    }

    /**
     * Generate an EC keypair using a named curve, using the Java 5 standard names for EC and secp384r1.
     * <p/>
     * This is the preferred method for generating EC keypairs.
     */
    public KeyPair subtestGenerateEcKey_standardJava5() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair kp = kpg.generateKeyPair();

        checkEcPublicKey(kp.getPublic());
        checkEcPrivateKey(kp.getPrivate());
        return kp;
    }

    public KeyPair subtestGenerateEcKey_standardJava5_byOid() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("1.2.840.10045.3.1.7"));
        KeyPair kp = kpg.generateKeyPair();

        checkEcPublicKey(kp.getPublic());
        checkEcPrivateKey(kp.getPrivate());
        return kp;
    }

    /**
     * Generate an EC keypair using a named curve, using the Certicom-specific API (if available).
     */
    public KeyPair subtestGenerateEcKey_certicomSpecific() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        AlgorithmParameterSpec p384spec;
        try {
            p384spec = (AlgorithmParameterSpec)invokeStaticMethod("com.certicom.ecc.jcae.CurveList", "byName", new Class[] {String.class}, "secp384r1");
        } catch (ClassNotFoundException e) {
            throw new SkipTestException("Certicom SecurityBuilder Crypto-J not present in classpath", null);
        }
        kpg.initialize(p384spec);
        KeyPair kp = kpg.generateKeyPair();

        checkEcPublicKey(kp.getPublic());
        checkEcPrivateKey(kp.getPrivate());
        return kp;
    }

    public void testAtLeastOneMethodToImportRsaKeyIsAvailable(List<String> results) {
        boolean worksWithSunPkcs12Parser = testRequirement("subtestRsaSun", results);
        boolean worksWithBcPkcs12Parser = testRequirement("subtestRsaBc", results);
        assertTrue(worksWithBcPkcs12Parser || worksWithSunPkcs12Parser, "need at least one to succeed");
    }
    public void subtestRsaSun() throws Exception { storeKeyEntry(BC_SELFSIGNED_RSA.parse(false)); }
    public void subtestRsaBc()  throws Exception { storeKeyEntry(BC_SELFSIGNED_RSA.parse(true)); }

    public void testAtLeastOneMethodToImportEcKeyIsAvailable(List<String> results) {
        boolean worksWithSunPkcs12Parser = testRequirement("subtestParseAndImportEcKey_withSunPKCS12", results);
        boolean worksWithBcPkcs12Parser = testRequirement("subtestParseAndImportEcKey_withBouncyCastle", results);
        assertTrue(worksWithSunPkcs12Parser || worksWithBcPkcs12Parser, "need at least one to succeed");
    }

    /**
     * Import an ECC private key and self-signed certificate into the Luna keystore,
     * using Sun's PKCS12 implementation to parse the keystore.
     */
    public void subtestParseAndImportEcKey_withSunPKCS12(List<String> results) throws Exception {
        boolean eccSun = testRequirement("subsubtestEccSun", results);
        boolean certicomSun = testRequirement("subsubtestCerticomSun", results);
        boolean bcSun = testRequirement("subsubtestBcSun", results);
        boolean vinSun = testRequirement("subsubtestVinCertSun", results);
        boolean cdSun = testRequirement("subsubtestCerticomDemoSun", results);
        assertTrue(eccSun && certicomSun && bcSun && vinSun && cdSun, "need all to succeed");
    }
    public void subsubtestEccSun()          throws Exception { storeKeyEntry(TEST_CERT_ECC.parse(false)); }
    public void subsubtestCerticomSun()     throws Exception { storeKeyEntry(CERTICOM_SELFSIGNED_ECC.parse(false)); }
    public void subsubtestBcSun()           throws Exception { storeKeyEntry(BC_SELFSIGNED_ECC.parse(false)); }
    public void subsubtestVinCertSun()      throws Exception { storeKeyEntry(VIN_ECC_TEST_7.parse(false)); }
    public void subsubtestCerticomDemoSun() throws Exception { storeKeyEntry(CERTICOM_DEMO_CHAIN.parse(false)); }

    /**
     * Import an ECC private key and self-signed certificate into the Luna keystore,
     * using Bouncy Castle's PKCS12 implementation to parse the keystore.
     */
    public void subtestParseAndImportEcKey_withBouncyCastle(List<String> results) throws Exception {
        boolean eccBc = testRequirement("subsubtestEccBc", results);
        boolean certicomBc = testRequirement("subsubtestCerticomBc", results);
        boolean bcBc = testRequirement("subsubtestBcBc", results);
        boolean vinBc = testRequirement("subsubtestVinCertBc", results);
        boolean cdBc = testRequirement("subsubtestCerticomDemoBc", results);
        assertTrue(eccBc && certicomBc && bcBc && vinBc && cdBc, "need all to succeed");
    }
    public void subsubtestEccBc()          throws Exception { storeKeyEntry(TEST_CERT_ECC.parse(true)); }
    public void subsubtestCerticomBc()     throws Exception { storeKeyEntry(CERTICOM_SELFSIGNED_ECC.parse(true)); }
    public void subsubtestBcBc()           throws Exception { storeKeyEntry(BC_SELFSIGNED_ECC.parse(true)); }
    public void subsubtestVinCertBc()      throws Exception { storeKeyEntry(VIN_ECC_TEST_7.parse(true)); }
    public void subsubtestCerticomDemoBc() throws Exception { storeKeyEntry(CERTICOM_DEMO_CHAIN.parse(true)); }

    /**
     * Ensure that all five security services required for SunJSSE to be able to use
     * the elliptic curve cipher suites are available.
     */
    public void testServicesNeededForEccCipherSuitesInJsseAreAvailable(List<String> results) throws Exception {
        boolean sigNoneWithEcdsa = testRequirement("subtestSigNoneWithEcdsa", results);
        boolean sigSha1WithEcdsa = testRequirement("subtestSigSha1WithEcdsa", results);
        boolean kaEcdh = testRequirement("subtestKaEcdh", results);
        boolean kfEc = testRequirement("subtestKfEc", results);
        boolean kpgEc = testRequirement("subtestKpgEc", results);
        assertTrue(sigNoneWithEcdsa && sigSha1WithEcdsa && kaEcdh && kfEc && kpgEc, "need all to succeed");
    }
    public void subtestSigNoneWithEcdsa() throws Exception { Signature.getInstance("NONEwithECDSA"); }
    public void subtestSigSha1WithEcdsa() throws Exception { Signature.getInstance("SHA1withECDSA"); }
    public void subtestKaEcdh() throws Exception { KeyAgreement.getInstance("ECDH"); }
    public void subtestKfEc() throws Exception { KeyFactory.getInstance("EC"); }
    public void subtestKpgEc() throws Exception { KeyPairGenerator.getInstance("EC"); }

    public void testSha2SignatureAlgorithmsAreAvailable(List<String> results) throws Exception {
        boolean sha256 = testRequirement("subtestSha256", results);
        boolean sha384 = testRequirement("subtestSha384", results);
        testRequirement("subtestSha512_optional", results); // optional
        assertTrue(sha256 && sha384, "need at least SHA256 and SHA384");
    }
    public void subtestSha256() throws Exception { Signature.getInstance("SHA256withECDSA"); }
    public void subtestSha384() throws Exception { Signature.getInstance("SHA384withECDSA"); }
    public void subtestSha512_optional() throws Exception { Signature.getInstance("SHA512withECDSA"); }

    /**
     * Test that message level signatures can be produced using ECDSA.
     */
    public void testMessageLevelSignature_generatedKey() throws Exception {
        KeyPair kp = makeSigKeypair();
        doSignature(kp.getPrivate());
    }

    /**
     * Test that message level signatures can be verified using a
     * public key that is still in memory and available after doing the signature.
     */
    public void testMessageLevelVerification_generatedKey() throws Exception {
        KeyPair kp = makeSigKeypair();

        byte[] originalSignature;
        try {
            originalSignature = doSignature(kp.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException("Can't test this -- signature half failed");
        }

        Signature sig = Signature.getInstance("SHA1withECDSA");
        sig.initVerify(kp.getPublic());
        sig.update(PAYLOAD_DATA.getBytes());
        assertTrue(sig.verify(originalSignature), "Signature.verify() returned false");
    }

    /**
     * Test that a message level signature can be created using a private key
     * loaded from elsewhere.
     */
    public void testMessageLevelSignature_existingKey() throws Exception {
        KeyEntry entry = getTestEccKeyEntry();
        byte[] got = doSignature(entry.privateKey);
    }

    /**
     * Test that message level signatures can be verified using a public
     * key that is extracted from the message.
     */
    public void testMessageLevelVerification_existingKey() throws Exception {
        KeyEntry entry = getTestEccKeyEntry();
        byte[] originalSignature = doSignature(entry.privateKey);
        PublicKey publicKey = entry.chain[0].getPublicKey();

        Signature sig = Signature.getInstance("SHA1withECDSA");
        sig.initVerify(publicKey);
        sig.update(PAYLOAD_DATA.getBytes());
        assertTrue(sig.verify(originalSignature), "Signature.verify() returned false");
    }

    /**
     * Test the ability to parse a certificate and ECC private key from PEM files.
     */
    public void testParseEccKeyFromPem(List<String> results) throws Exception {
        boolean parseCert = testRequirement("subtestParseX509EccCertFromPem", results);
        boolean parseKey = testRequirement("subtestParsePkcs8EccKeyFromPem", results);
        assertTrue(parseCert && parseKey, "both subrequirements must succeed");
    }
    public void subtestParseX509EccCertFromPem() throws Exception { getTestEccCert(); }
    public void subtestParsePkcs8EccKeyFromPem() throws Exception { getTestEccPrivateKey(); }

    /**
     * Test the ability to open an SSL connection to ourself using various RSA cipher suites.
     */
    public void testRsa_TLS_1_0(List<String> results) throws Exception {
        boolean dj = testRequirement("subtestRsa_TLS_1_0_DefaultJSSE", results);
        boolean cj = testRequirement("subtestRsa_TLS_1_0_CerticomJSSE", results);
        boolean rj = testRequirement("subtestRsa_TLS_1_0_RsaJSSE", results);
        assertTrue(dj || cj || rj, "at least one subrequirements must succeed");
    }

    public void subtestRsa_TLS_1_0_DefaultJSSE(List<String> results) throws Exception {
        boolean djRsaAes128 = testRequirement("subsubtestDefJsse_RSA_AES128", results);
        boolean djRsaAes256 = testRequirement("subsubtestDefJsse_RSA_AES256", results);
        boolean djDheRsaAes128 = testRequirement("subsubtestDefJsse_DHE_RSA_AES128", results);
        boolean djDheRsa256 = testRequirement("subsubtestDefJsse_DHE_RSA_AES256", results);
        assertTrue(djRsaAes128 && djRsaAes256 && djDheRsaAes128 && djRsaAes128 && djDheRsa256, "all sub-subrequirements must succeed");
    }
    public void subsubtestDefJsse_RSA_AES128()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_128_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_RSA_AES256()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_256_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_DHE_RSA_AES128()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_DHE_RSA_AES256()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", JsseProvider.DEFAULT); }

    public void subtestRsa_TLS_1_0_CerticomJSSE(List<String> results) throws Exception {
        boolean cjRsaAes128 = testRequirement("subsubtestCrtJsse_RSA_AES128", results);
        boolean cjRsaAes256 = testRequirement("subsubtestCrtJsse_RSA_AES256", results);
        boolean cjDheRsaAes128 = testRequirement("subsubtestCrtJsse_DHE_RSA_AES128", results);
        boolean cjDheRsa256 = testRequirement("subsubtestCrtJsse_DHE_RSA_AES256", results);
        assertTrue(cjRsaAes128 && cjRsaAes256 && cjDheRsaAes128 && cjRsaAes128 && cjDheRsa256, "all sub-subrequirements must succeed");
    }
    public void subsubtestCrtJsse_RSA_AES128()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_128_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_RSA_AES256()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_256_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_DHE_RSA_AES128()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_DHE_RSA_AES256()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", JsseProvider.CERTICOM); }

    public void subtestRsa_TLS_1_0_RsaJSSE(List<String> results) throws Exception {
        boolean cjRsaAes128 = testRequirement("subsubtestRsaJsse_RSA_AES128", results);
        boolean cjRsaAes256 = testRequirement("subsubtestRsaJsse_RSA_AES256", results);
        boolean cjDheRsaAes128 = testRequirement("subsubtestRsaJsse_DHE_RSA_AES128", results);
        boolean cjDheRsa256 = testRequirement("subsubtestRsaJsse_DHE_RSA_AES256", results);
        assertTrue(cjRsaAes128 && cjRsaAes256 && cjDheRsaAes128 && cjRsaAes128 && cjDheRsa256, "all sub-subrequirements must succeed");
    }
    public void subsubtestRsaJsse_RSA_AES128()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_128_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_RSA_AES256()      throws Exception { sslTest(SslKey.RSA_L7, "TLS_RSA_WITH_AES_256_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_DHE_RSA_AES128()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_DHE_RSA_AES256()  throws Exception { sslTest(SslKey.RSA_L7, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", JsseProvider.RSA); }

    /**
     * Test the ability to open an SSL connection to ourself using various ECC cipher suites.
     */
    public void testEcc_TLS_1_0(List<String> results) throws Exception {
        boolean dj = testRequirement("subtestEcc_TLS_1_0_DefaultJSSE", results);
        boolean cj = testRequirement("subtestEcc_TLS_1_0_CerticomJSSE", results);
        boolean rj = testRequirement("subtestEcc_TLS_1_0_RSA_JSSE", results);
        assertTrue(dj || cj || rj, "at least one subrequirement must succeed");
    }

    public void subtestEcc_TLS_1_0_DefaultJSSE(List<String> results) throws Exception {
        boolean djEcdhAes128EcKey = testRequirement("subsubtestDefJsse_ECDH_ECDSA_AES128_tstKey", results);
        boolean djEcdheAes256EcKey = testRequirement("subsubtestDefJsse_ECDHE_ECDSA_AES256_tstKey", results);
        boolean djEcdheAes128EcKey = testRequirement("subsubtestDefJsse_ECDHE_ECDSA_AES128_tstKey", results);
        boolean djEcdhAes128BcKey = testRequirement("subsubtestDefJsse_ECDH_ECDSA_AES128_BcKey", results);
        boolean djEcdheAes256BcKey = testRequirement("subsubtestDefJsse_ECDHE_ECDSA_AES256_BcKey", results);
        assertTrue(djEcdhAes128EcKey && djEcdheAes256EcKey && djEcdheAes128EcKey && djEcdhAes128BcKey && djEcdheAes256BcKey,
                "all sub-subrequirements must succeed");
    }
    public void subsubtestDefJsse_ECDH_ECDSA_AES128_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_ECDHE_ECDSA_AES256_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_ECDHE_ECDSA_AES128_tstKey() throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_ECDH_ECDSA_AES128_BcKey()   throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.DEFAULT); }
    public void subsubtestDefJsse_ECDHE_ECDSA_AES256_BcKey()  throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.DEFAULT); }

    public void subtestEcc_TLS_1_0_CerticomJSSE(List<String> results) throws Exception {
        boolean cjEcdhAes128EcKey = testRequirement("subsubtestCrtJsse_ECDH_ECDSA_AES128_tstKey", results);
        boolean cjEcdheAes256EcKey = testRequirement("subsubtestCrtJsse_ECDHE_ECDSA_AES256_tstKey", results);
        boolean cjEcdheAes128EcKey = testRequirement("subsubtestCrtJsse_ECDHE_ECDSA_AES128_tstKey", results);
        boolean cjEcdhAes128BcKey = testRequirement("subsubtestCrtJsse_ECDH_ECDSA_AES128_BcKey", results);
        boolean cjEcdheAes256BcKey = testRequirement("subsubtestCrtJsse_ECDHE_ECDSA_AES256_BcKey", results);
        assertTrue(cjEcdhAes128EcKey && cjEcdheAes256EcKey && cjEcdheAes128EcKey && cjEcdhAes128BcKey && cjEcdheAes256BcKey,
                "all sub-subrequirements must succeed");
    }
    public void subsubtestCrtJsse_ECDH_ECDSA_AES128_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_ECDHE_ECDSA_AES256_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_ECDHE_ECDSA_AES128_tstKey() throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_ECDH_ECDSA_AES128_BcKey()   throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.CERTICOM); }
    public void subsubtestCrtJsse_ECDHE_ECDSA_AES256_BcKey()  throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.CERTICOM); }

    public void subtestEcc_TLS_1_0_RSA_JSSE(List<String> results) throws Exception {
        boolean cjEcdhAes128EcKey = testRequirement("subsubtestRsaJsse_ECDH_ECDSA_AES128_tstKey", results);
        boolean cjEcdheAes256EcKey = testRequirement("subsubtestRsaJsse_ECDHE_ECDSA_AES256_tstKey", results);
        boolean cjEcdheAes128EcKey = testRequirement("subsubtestRsaJsse_ECDHE_ECDSA_AES128_tstKey", results);
        boolean cjEcdhAes128BcKey = testRequirement("subsubtestRsaJsse_ECDH_ECDSA_AES128_BcKey", results);
        boolean cjEcdheAes256BcKey = testRequirement("subsubtestRsaJsse_ECDHE_ECDSA_AES256_BcKey", results);
        assertTrue(cjEcdhAes128EcKey && cjEcdheAes256EcKey && cjEcdheAes128EcKey && cjEcdhAes128BcKey && cjEcdheAes256BcKey,
                "all sub-subrequirements must succeed");
    }
    public void subsubtestRsaJsse_ECDH_ECDSA_AES128_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_ECDHE_ECDSA_AES256_tstKey()  throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_ECDHE_ECDSA_AES128_tstKey() throws Exception { sslTest(SslKey.ECC_CERT, "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_ECDH_ECDSA_AES128_BcKey()   throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", JsseProvider.RSA); }
    public void subsubtestRsaJsse_ECDHE_ECDSA_AES256_BcKey()  throws Exception { sslTest(SslKey.ECC_L7, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", JsseProvider.RSA); }

    //
    //
    // --- Utility methods and classes ---
    //
    //

    static class SkipTestException extends RuntimeException {
        SkipTestException(String message, Throwable cause) {
            super("skiptest: " + message, cause);
        }
    }

    // Represents a private key with its certificate chain (as with an entry parsed out of a PKCS#12 file).
    static class KeyEntry {
        final PrivateKey privateKey;
        final X509Certificate[] chain;

        KeyEntry(PrivateKey privateKey, X509Certificate[] chain) {
            this.privateKey = privateKey;
            this.chain = chain;
        }
    }

    // Represents an embedded PKCS#12 file.
    static class TestKeystore {
        final String keystoreBase64;
        final String alias;
        final char[] passphrase;
        final boolean expectEcc;

        TestKeystore(String alias, String passphrase, String base64) {
            this(alias, passphrase, true, base64);
        }

        TestKeystore(String alias, String passphrase, boolean expectEcc, String base64) {
            this.keystoreBase64 = base64;
            this.alias = alias;
            this.passphrase = passphrase.toCharArray();
            this.expectEcc = expectEcc;
        }

        KeyEntry parse(boolean parseUsingBouncyCastle) throws Exception {
            byte[] bytes = decodeBase64(keystoreBase64);
            KeyStore ks;
            try {
                ks = parseUsingBouncyCastle
                    ? KeyStore.getInstance("BCPKCS12", getBouncyCastleProvider())
                        : KeyStore.getInstance("PKCS12");
            } catch (ClassNotFoundException e) {
                throw new SkipTestException("No org.bouncycastle.jce.provider.BouncyCastleProvider in class path", null);
            }
            ks.load(new ByteArrayInputStream(bytes), passphrase);
            String al = alias != null ? alias : ks.aliases().nextElement();
            Certificate[] chain = ks.getCertificateChain(al);
            logger.fine("Read certificate: " + chain[0]);
            PrivateKey privateKey = (PrivateKey)ks.getKey(al, passphrase);
            if (expectEcc) {
                checkEcPublicKey(chain[0].getPublicKey());
                checkEcPrivateKey(privateKey);
            }
            X509Certificate[] x509chain = new X509Certificate[chain.length];
            System.arraycopy(chain, 0, x509chain, 0, chain.length);
            return new KeyEntry(privateKey, x509chain);
        }
    }

    static Provider getBouncyCastleProvider() throws Exception {
        return construct(Provider.class, "org.bouncycastle.jce.provider.BouncyCastleProvider");
    }

    /** Basic SecurityProvider that installs nothing, assuming the necessary Security providers are already installed. */
    static class SecurityProvider {
        final String shortname;
        final String[] classnames;
        final boolean addAsLeastPreference;
        String targetKeystoreType = "PKCS12";
        char[] targetKeystorePassphrase = "password".toCharArray();
        String targetKeystoreAlias = "entry";
        boolean targetKeystoreRequiresLoadPassword = false;
        boolean targetKeystoreRequiresStore = true;
        public boolean algNameEcdsa = false;

        public SecurityProvider(String shortname, boolean addAsLeastPreference, String... classnames) {
            this.shortname = shortname;
            this.classnames = classnames;
            this.addAsLeastPreference = addAsLeastPreference;
        }

        /** Install this provider. */
        public void install() throws Exception {
            for (String classname : classnames) {
                final Provider provider = construct(Provider.class, classname);
                if (addAsLeastPreference)
                    Security.addProvider(provider);
                else
                    Security.insertProviderAt(provider, 1);
            }
        }
    }

    /** A provider that uses Luna's native LunaJCE/LunaJCA providers. */
    static class LunaNativeSecurityProvider extends SecurityProvider {
        public LunaNativeSecurityProvider(String shortname, String... classnames) {
            super(shortname, false, classnames);
            targetKeystoreType = "Luna";
            targetKeystoreRequiresLoadPassword = true;
            targetKeystoreRequiresStore = false;
            algNameEcdsa = true;
        }

        public void install() throws Exception {
            String tokenPin = getRequiredSystemProperty("lunaTokenPin");
            final Object lunaTokenManager = invokeStaticMethod("com.chrysalisits.crypto.LunaTokenManager", "getInstance", new Class[0]);
            invokeMethod(lunaTokenManager, "Login", new Class[] { String.class }, tokenPin);
            super.install();
        }
    }

    static enum SslKey {
        /** Test RSA server cert, generated by Layer 7 using Bouncy Castle. */
        RSA_L7,

        /** The Certicom ECC demo cert and key. */
        ECC_CERT,

        /** Test ECC server cert generated by Layer 7 using Bouncy Castle. */
        ECC_L7,
    }

    /** A provider that uses the Luna via SunPKCS11 (if available). */
    static class LunaPkcs11SecurityProvider extends SecurityProvider {
        public LunaPkcs11SecurityProvider(String shortname) {
            super(shortname, false);
            targetKeystoreType = "PKCS11";
            targetKeystoreRequiresLoadPassword = true;
            targetKeystoreRequiresStore = false;
        }

        public void install() throws Exception {
            String tokenPin = getRequiredSystemProperty("lunaTokenPin");
            targetKeystorePassphrase = tokenPin.toCharArray();
            String libraryPath = getRequiredSystemProperty("lunaLibraryPath");
            String pkcs11Config = "name=Luna\n" +
            "library=" + libraryPath + "\n" +
            "description=Luna\n" +
            "attributes(*,CKO_PUBLIC_KEY,CKK_RSA) = {\n" +
            "  CKA_TOKEN = true\n" +
            "  CKA_ENCRYPT = true\n" +
            "  CKA_VERIFY = true\n" +
            "  CKA_WRAP = true\n" +
            "}\n" +
            "attributes(*,CKO_PRIVATE_KEY,CKK_RSA) = {\n" +
            "  CKA_TOKEN = true\n" +
            "  CKA_EXTRACTABLE = false\n" +
            "  CKA_DECRYPT = true\n" +
            "  CKA_SIGN = true\n" +
            "  CKA_UNWRAP = true\n" +
            "}\n" +
            "attributes(*,CKO_PUBLIC_KEY,CKK_EC) = {\n" +
            "  CKA_TOKEN = true\n" +
            "  CKA_VERIFY = true\n" +
            "  CKA_DERIVE = true\n" +
            "  CKA_WRAP = true\n" +
            "}\n" +
            "attributes(*,CKO_PRIVATE_KEY,CKK_EC) = {\n" +
            "  CKA_TOKEN = true\n" +
            "  CKA_EXTRACTABLE = false\n" +
            "  CKA_DERIVE = true\n" +
            "  CKA_SIGN = true\n" +
            "  CKA_UNWRAP = true\n" +
            "}\n";

            ByteArrayInputStream configStream = new ByteArrayInputStream(pkcs11Config.getBytes());
            Provider prov = construct(Provider.class, "sun.security.pkcs11.SunPKCS11",
                    new Class[] { InputStream.class },
                    new Object[] {configStream}
            );
            Security.addProvider(prov);

            // Log into token
            KeyStore token = KeyStore.getInstance("PKCS11");
            token.load(null, tokenPin.toCharArray());
        }
    }

    static class SingleEntryKeyManager implements X509KeyManager {
        final KeyEntry entry;
        public SingleEntryKeyManager(KeyEntry keyEntry) { entry = keyEntry; }
        public String[] getClientAliases(String kt, Principal[] principals) { return new String[0]; }
        public String chooseClientAlias(String[] kts, Principal[] principals, Socket socket) { return null; }
        public String[] getServerAliases(String kt, Principal[] principals) { return new String[] { "x" }; }
        public String chooseServerAlias(String kt, Principal[] principals, Socket socket) { return "x"; }
        public X509Certificate[] getCertificateChain(String s) { return entry.chain; }
        public PrivateKey getPrivateKey(String s) { return entry.privateKey; }
    }

    static class TrustEverythingTrustManager implements X509TrustManager {
        final X509Certificate[] serverCerts;
        TrustEverythingTrustManager(X509Certificate[] serverCerts) { this.serverCerts = serverCerts; }
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        public X509Certificate[] getAcceptedIssuers() { return serverCerts; }
    }

    private void sslTest(SslKey key, String ecSuite, JsseProvider jsseProvider) throws Exception {
        KeyEntry keyEntry;
        boolean expectEcc = true;
        try {
            switch (key) {
                case ECC_CERT:
                    keyEntry = getTestEccKeyEntry();
                    break;
                case ECC_L7:
                    try {
                        keyEntry = BC_SELFSIGNED_ECC.parse(false);
                    } catch (Exception e) {
                        keyEntry = BC_SELFSIGNED_ECC.parse(true);
                    }
                    break;
                case RSA_L7:
                    try {
                        keyEntry = BC_SELFSIGNED_RSA.parse(false);
                    } catch (Exception e) {
                        keyEntry = BC_SELFSIGNED_RSA.parse(true);
                    }
                    expectEcc = false;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown SslKey: " + key);
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't test this -- can't parse server cert and key", e);
        }

        if (expectEcc) {
            assertTrue(keyEntry.privateKey != null, "private key is null");
            assertTrue(keyEntry.privateKey.getAlgorithm().equals("EC"), "private key algorithm must be 'EC'");
            assertTrue(keyEntry.chain[0] != null, "cert chain is null");
            assertTrue(keyEntry.chain[0].getPublicKey().getAlgorithm().equals("EC"), "public key algorithm must be 'EC'");
        }

        testSslRoundTrip(keyEntry, ecSuite, jsseProvider.getProvider());
    }

    static enum JsseProvider {
        DEFAULT(null),
        RSA("com.rsa.jsse.JsseProvider"),
        CERTICOM("com.certicom.jsse.provider.CerticomJSSE");

        final String name;

        private JsseProvider(String provider) {
            this.name = provider;
        }

        public Provider getProvider() {
            Provider tlsProvider = null;
            if (name != null) {
                try {
                    tlsProvider = construct(Provider.class, name);
                } catch (Exception e) {
                    throw new SkipTestException("Can't test this -- no provider " + name + " available in classpath", null);
                }
            }
            return tlsProvider;
        }
    }

    static void testSslRoundTrip(KeyEntry keyEntry, String cipherSuite, Provider tlsProvider) throws Exception {

        SSLContext serverContext = createSslContext(tlsProvider);
        serverContext.init(new KeyManager[] { new SingleEntryKeyManager(keyEntry) },
                new TrustManager[] { new TrustEverythingTrustManager(new X509Certificate[] {keyEntry.chain[0]}) },
                SecureRandom.getInstance("SHA1PRNG"));
        SSLServerSocketFactory ssf = serverContext.getServerSocketFactory();
        assertTrue(Arrays.asList(ssf.getSupportedCipherSuites()).contains(cipherSuite),
                cipherSuite + " is not available from the default 'TLS' SSLContext with the current provider settings");
        final String[] eccSuites = {cipherSuite};

        SSLServerSocket ssock = null;
        SSLSocket csock = null;
        Thread serverThread = null;

        try {
            ssock = (SSLServerSocket) ssf.createServerSocket(13756, 5, InetAddress.getLocalHost());
            ssock.setEnabledCipherSuites(eccSuites);
            ssock.setReuseAddress(true);
            ssock.setNeedClientAuth(false);
            ssock.setWantClientAuth(false);
            ssock.setSoTimeout(30000);
            //ssock.setEnabledProtocols(new String[] {"TLSv1.0"});

            final AtomicReference<Throwable> serverException = new AtomicReference<Throwable>();
            final CountDownLatch latch = new CountDownLatch(1);
            final SSLServerSocket ssock1 = ssock;
            serverThread = new Thread("SSL echo server") {
                public void run() {
                    SSLSocket s = null;
                    try {
                        s = (SSLSocket) ssock1.accept();
                        logger.info("Server: incoming connection from " + s.getRemoteSocketAddress());
                        byte[] expected = PAYLOAD_DATA.getBytes("UTF-8");
                        byte[] buf = new byte[expected.length];
                        assertTrue(buf.length == s.getInputStream().read(buf), "Must read entire block");
                        String fromClient = new String(buf, "UTF-8");
                        logger.info("Server: Connected with " + s.getSession().getCipherSuite());
                        assertTrue(PAYLOAD_DATA.equals(fromClient), "server did not recieve the expected data");
                        s.getOutputStream().write(fromClient.getBytes("UTF-8"));
                        s.getOutputStream().flush();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Exception from server side of SSL test: " + e.getMessage(), e);
                        serverException.set(e);
                    } finally {
                        latch.countDown();
                        if (s != null)
                            closeQuietly(s);
                    }
                }
            };
            serverThread.start();

            // See if server dies early
            latch.await(1, TimeUnit.SECONDS);
            if (serverException.get() != null)
                throw new RuntimeException(serverException.get());

            // Open client connection
            SSLContext clientContext = createSslContext(tlsProvider);
            clientContext.init(null,
                    new TrustManager[] { new TrustEverythingTrustManager(new X509Certificate[] {keyEntry.chain[0]}) },
                    SecureRandom.getInstance("SHA1PRNG"));
            SSLSocketFactory csf = clientContext.getSocketFactory();
            csock = (SSLSocket) csf.createSocket(InetAddress.getLocalHost(), 13756);
            csock.setReuseAddress(true);
            csock.setEnabledCipherSuites(eccSuites);
            csock.setSoTimeout(30000);
            //csock.setEnabledProtocols(new String[] {"TLSv1.0"});

            // Write test data
            final OutputStream out = csock.getOutputStream();
            out.write(PAYLOAD_DATA.getBytes("UTF-8"));

            // Read it back
            String fromServer = new String(slurpStream(csock.getInputStream()), "UTF-8");
            assertTrue(PAYLOAD_DATA.equals(fromServer), "data echoed back from server doesn't match");

            csock.close();

            // Propagate any server exception
            if (serverException.get() != null)
                throw new RuntimeException(serverException.get());
        } finally {
            closeQuietly(csock);
            closeQuietly(ssock);
            if (serverThread != null) {
                serverThread.interrupt();
                serverThread.join(10000);
            }
        }
    }

    static SSLContext createSslContext(Provider tlsProvider) throws NoSuchAlgorithmException {
        return tlsProvider == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", tlsProvider);
    }

    static void assertTrue(boolean b, String req) {
        if (!b) throw new AssertionError("Requirement not met: " + req);
    }

    static void skip(String why) {
        skip(why, null);
    }

    static void skip(String why, Throwable cause) {
        throw new SkipTestException(why, cause);
    }

    static void checkEcPrivateKey(PrivateKey privateKey) {
        assertTrue(privateKey.getAlgorithm().equals("EC") || privateKey.getAlgorithm().equals("ECDSA"),
                "algorithm must be EC or ECDSA");
    }

    static void checkEcPublicKey(PublicKey publicKey) {
        assertTrue(publicKey.getAlgorithm().equals("EC") || publicKey.getAlgorithm().equals("ECDSA"),
                "algorithm must be EC or ECDSA");
        if (publicKey.getAlgorithm().equals("EC"))
            assertTrue(publicKey.getFormat().equals("X.509") || publicKey.getFormat().equals("X509"), "encoding of EC public key must be X.509 or X509");
        assertTrue(publicKey.getEncoded() != null, "public key must return non-null encoding");
    }

    static String getRequiredSystemProperty(String prop) {
        String val = System.getProperty(prop, null);
        if (val == null)
            throw new IllegalStateException("The system property \"" + prop + "\" must be set for this provider.");
        return val;
    }

    /** Stores the specified key entry to the target keystore. */
    private void storeKeyEntry(KeyEntry entry) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(securityProvider.targetKeystoreType);
        final char[] pass = securityProvider.targetKeystorePassphrase;
        ks.load(null, securityProvider.targetKeystoreRequiresLoadPassword ? pass : null);
        ks.setKeyEntry(securityProvider.targetKeystoreAlias, entry.privateKey, pass, entry.chain);
        if (securityProvider.targetKeystoreRequiresStore)
            ks.store(new ByteArrayOutputStream(), pass);
    }

    static byte[] slurpStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyStream(in, out);
        return out.toByteArray();
    }

    static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int got;
        while ((got = in.read(buf)) > 0)
            out.write(buf, 0, got);
    }

    KeyPair makeSigKeypair() throws Exception {
        String[] methodNames = {
                "subtestGenerateEcKey_standardJava5",
                "subtestGenerateEcKey_lunaSpecific",
                "subtestGenerateEcKey_certicomSpecific",
        };
        for (String methodName : methodNames) {
            Method method = getClass().getMethod(methodName);
            try {
                return (KeyPair) method.invoke(this);
            } catch (Exception e) {
                logger.log(Level.FINE, "EC keypair generation strategy " + methodName + " failed; trying next one", e);
            }
        }
        throw new RuntimeException("Can't test this -- no way to generate an EC keypair");
    }

    byte[] doSignature(final PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA1withECDSA");
        sig.initSign(privateKey);
        sig.update(PAYLOAD_DATA.getBytes());
        byte[] signatureBytes = sig.sign();
        assertTrue(signatureBytes != null && signatureBytes.length > 0, "signature must not be null or empty");
        return signatureBytes;
    }

    static KeyEntry getTestEccKeyEntry() throws Exception {
        X509Certificate[] chain = getTestEccCert();
        PrivateKey privateKey = getTestEccPrivateKey();
        return new KeyEntry(privateKey, chain);
    }

    static PrivateKey getTestEccPrivateKey() throws Exception {
        KeySpec spec = new PKCS8EncodedKeySpec(decodeBase64(CERTICOM_KEY_COMPRESSED_FORMAT));
        KeyFactory kf = KeyFactory.getInstance("EC", getBouncyCastleProvider());
        PrivateKey privateKey = kf.generatePrivate(spec);
        assertTrue(privateKey.getAlgorithm().equals("EC") || privateKey.getAlgorithm().equals("ECDSA"), "private key type must be EC or ECDSA");
        return privateKey;
    }

    static X509Certificate[] getTestEccCert() throws CertificateException {
        Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(
                new ByteArrayInputStream(CERTICOM_KEY_COMPRESSED_FORMAT_CERT_CHAIN.getBytes()));
        X509Certificate[] chain = certs.toArray(new X509Certificate[certs.size()]);
        assertTrue(chain != null, "certificate factory failed to produce a certificate chain");
        assertTrue(chain.length > 0, "certificate factory produced an empty certificate chain");
        final String keyAlg = chain[0].getPublicKey().getAlgorithm();
        assertTrue(keyAlg.equals("EC") || keyAlg.equals("ECDSA"), "cert public key type must be EC or ECDSA");
        return chain;
    }

    public static byte[] decodeBase64(String in) throws IOException {
        return new BASE64Decoder().decodeBuffer(in);
    }

    static void closeQuietly(Socket s) {
        try {
            if (s != null)
                s.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception closing socket", e);
        }
    }

    static void closeQuietly(ServerSocket s) {
        try {
            if (s != null)
                s.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception closing socket", e);
        }
    }

    /** Instantiates a class reflectively, to avoid compile-time dependencies. */
    static <T> T construct(Class<T> iface, String classname) throws Exception {
        return construct(iface, classname, new Class[0], new Object[0]);
    }

    /** Instantiates a class reflectively, to avoid compile-time dependencies. */
    static <T> T construct(Class<T> iface, String classname, Class[] argtypes, Object[] args) throws Exception {
        Object obj = Class.forName(classname).getConstructor(argtypes).newInstance(args);
        if (iface.isAssignableFrom(obj.getClass())) {
            //noinspection unchecked
            return (T)obj;
        }
        throw new ClassCastException(classname + " is not a subclass of " + iface.getName());
    }

    /** Invokes a static method reflectively, to avoid compile-time dependencies. */
    static Object invokeStaticMethod(String classname, String methodname, Class[] argtypes, Object... args) throws Exception {
        return Class.forName(classname).getMethod(methodname, argtypes).invoke(null, args);
    }

    /** Invokes a method reflectively, to avoid compile-time dependencies. */
    static Object invokeMethod(Object instance, String methodname, Class[] argtypes, Object... args) throws Exception {
        return instance.getClass().getMethod(methodname, argtypes).invoke(instance, args);
    }


    //
    //
    // --- Test data ---
    //
    //

    /** A PKCS#12 file containing a single entry consisting of a self-signed RSA cert generated using Bouncy Castle. */
    static final TestKeystore BC_SELFSIGNED_RSA = new TestKeystore(null, "password", false,
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA+gwgDCABgkqhkiG9w0BBwGggCSABIIDDTCCAwkwggMF\n" +
            "BgsqhkiG9w0BDAoBAqCCArIwggKuMCgGCiqGSIb3DQEMAQMwGgQULF6vVC6fCvwmsX6HaJ9yTpKT\n" +
            "BSACAgQABIICgKmfQn9YNsmdOseCLajJ52XlSmfRsiQIdRwsXuG/uL8x+sPWuVy+oLtu6SdHjrJz\n" +
            "0bFR+6eoO48NyGFFbgBX+22RBJG0HLZ6c351bRU6QiTifBpbucOjjwJYxATfI2OxeWPFjJGp8ApA\n" +
            "EF3Fa3q3TlxyBC1Jq6DSrUC78Og0Ei7ESoshiCJgH9QgzNgC/2UQQW87K4WrdIS59V4IdnPY4bsS\n" +
            "E/oKbexT2KIgobsnO2p0hSPZYEIV4rKQACWQ4oJCJCTBvzDm9Rs7o0rShMNxXm3pT5QLP00HN1So\n" +
            "YZtJIcnJ/vtqyELbpjuiVsGPFA8WIutYru34fTaw1ece69jaRpGUiBY2nXc2HAocJqJBwbfGTVCY\n" +
            "kUi9XtJuoXKvSSatHG/h6HHB69kwbAxXzMGMQGoWnIbWrcmNxZGmyhFn6mXLRxq5ziW7W4EelVl7\n" +
            "5ZTkFmYayJ4GHmC0O6IWczSAurYvYOMtT4A6GGie2+DHbgOWpFHZmn0cVeGKMb0dHm3CkfjsMfKy\n" +
            "98srSEwFRH68gUzvQiMb4mscygGSECmQVB81DSNZaxyX08lLjTXBQIaMgl1R10baYPiX+gqJ5xTQ\n" +
            "yJ32le5i2j3atoo2vb/wySFzqT1us8Oes0cUztIoP0Yb/G0I2EVFJxCL/s/OlDZRz4SxKcYmgfxw\n" +
            "6yHEflQ7+RrGco4saFz5VKhO9obG0Cl/jZ7Sy/NPR1VZ2JGmZP+XQbADz3+ZPidx7xVJOck4XegB\n" +
            "WtQccMl8Q3yHijsAkajbxs+qYWfdHiPodoNadZD4cct+58vVK3dWqPfHYwdu0WMBymbEdgaJaGII\n" +
            "Dm1NAdtZjXwMkVhqemTQMRz7ObSScooxQDAZBgkqhkiG9w0BCRQxDB4KAGUAbgB0AHIAeTAjBgkq\n" +
            "hkiG9w0BCRUxFgQUtasXfGTRK+rGIN8lSfQOkTVBfsQAAAAAAAAwgAYJKoZIhvcNAQcGoIAwgAIB\n" +
            "ADCABgkqhkiG9w0BBwEwKAYKKoZIhvcNAQwBBjAaBBRf0IAEGvw/tKT5wJi1LItAjyYOrQICBACg\n" +
            "gASCAiiY2pkOrtA+9AaszrYYg1RNH2+lYPQZpssR4I0DyvrEAcX7r7VgGl5gv9s9L1StQkGMv/Ld\n" +
            "pxJcy/G90ChV+hKz+z7G4gL6obk4N561+jUPf/+iGsxPonqaXDhDIrmw0Nt4JOf05JRoamKXASbU\n" +
            "BIIBxwJSxGwEQTqWDoMYNtDskHYVP+Khcn36q3MXCshJ2fU3csiZu6l7wZNqGcd23OQegbExtRmz\n" +
            "sQ7ZHfAOrdpLkaw1K3ijutucmgPCwdvWdGhTk1/hyyaPUPMll+Qxq5zPsKTYYPUlHtFHM6QR5ipo\n" +
            "fgBlGsq0SZLyXn6xEBNu4k8yxdxDmZ3Zg3mNhjqqSGixej6t3QUawLsiXiqivuYdUSy+6Eu7R2in\n" +
            "VmSd5lqfCxZVbsSRv8XXeC/pXLZAc/VCtyOsvjDQYVKiLMdfwLM6QpMJUTeyP87OkzgJ6tWwTejL\n" +
            "dYrY1R7k+8+FL1hE19/djwJB7M+Cbye7r+acmp96OwLZtuS8gNZkl1mOPWqMesD+SqgW0HX1lXve\n" +
            "1vu3galGkHYfSqEuHOvbCx+gvzHyWBqK7nw4/wuKVBNIbmSbRC2YtFPpDabbZ9BAG2n9LD50ukMu\n" +
            "DkggpAFl8lh8kNtBpoJ2w+B4zwVizTZ8IAtowDz8ezGaBTbLg7RFYcMTrHtOHgTu0PNLzIzbtLCl\n" +
            "8nlrGfZQkcQcVY0KKnrzjjBrH7JFhY3k2Lzk7P0fODMtkNJRXYZkC0ialCiCuwnyAAAAAAAAAAAA\n" +
            "AAAAAAAAAAAAMD0wITAJBgUrDgMCGgUABBSD0dLvfMNocQlwH9y5/8Vppp72kAQUZDwFIgF1UeBf\n" +
            "qw1384lyXXbpq5MCAgQAAAA=");

    /** A PKCS#12 file containing a single entry consisting of a self-signed ECC cert generated by Certicom. */
    static final TestKeystore CERTICOM_SELFSIGNED_ECC = new TestKeystore("entry", "password",
            "MIIDpQIBAzCCA18GCSqGSIb3DQEHAaCCA1AEggNMMIIDSDCB9gYJKoZIhvcNAQcBoIHoBIHlMIHi\n" +
            "MIHfBgsqhkiG9w0BDAoBAqCBjzCBjDAoBgoqhkiG9w0BDAEDMBoEFO9rizU+Sx3wPimpN4MwYdT1\n" +
            "fLdQAgIEAARgrJAg0OWgYLSwROdL+6O2t233jcN0pSxn/NpNNK/3QX61yjx0KcoFgKoMqnWevdkc\n" +
            "miK3puyhdIRR0qV1KMnYI/ta15e1mczDlHiKZLXiHA+CRMENJF3O9q+LMBgA/5VFMT4wGQYJKoZI\n" +
            "hvcNAQkUMQweCgBlAG4AdAByAHkwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTIzOTIzMzA1MDQ4MTCC\n" +
            "AksGCSqGSIb3DQEHBqCCAjwwggI4AgEAMIICMQYJKoZIhvcNAQcBMCgGCiqGSIb3DQEMAQYwGgQU\n" +
            "C2NO9oPAaXLpSobKCSK8QSCf5N0CAgQAgIIB+LTWmPam7FUhh0B4oFjON8bCxzF8CffjPHoY6FNO\n" +
            "2NDpEwvWSY1VF/azvVFzA2nJDJAQl1pmn2nvXoJ2b35gc8VeyVhaUVYKmt6Rgdx7ukmKSQM9Hs8g\n" +
            "Eo+l0aJwRWJ+ntZ6iPE5tegYZxQmk9sYkuM94TSca5YnZcF+4KMKA338w4ndOFy6tsCDLZCnqCOJ\n" +
            "MRcgWg4+VLs0UTVXv+KCGrHhBTsCd6Pj1/5ifwQwpdiz4zPeYmAPFaqyHB2nqaTU9icUZdeHoeA8\n" +
            "4To0AnbwDOT+8XdYkPhOVLOuKyvNos5wPSg2/b/TVsX06ydh3zYJyEEvS9d9z6Ey02juf5rlbrCb\n" +
            "EMcblZMFLrl+FJmEegG9Wu7BCWdX9KHLFWTthAKfNM6HZaHUWUDETf3gVcjv1RKten1ZjAzt0ZKa\n" +
            "T17HjVXmqaCNcLUS+5Jmr+BtBYt3httMDmuL8mOFr5Mgcze7tqPaD0uMx1ISKxzFYZr4ODmboLE3\n" +
            "0kDWfPsd3Vle/hUSkysKO2u4lOx1VG9FhSXNV6Ae/KthcY/7luLzWJjL5bNxDWx6FeHIUMduIHFn\n" +
            "hDkfB7583RHVddyHm8y+ZRcAaKZXLVlK9nCtNazGxJpVXQQuJCfXVPC5wtuWIlNV2NUyHjveR73N\n" +
            "370SZk7UokVck/rzVRLmQp1nTjA9MCEwCQYFKw4DAhoFAAQUOuHQZ84xqoJEz0nOlNSgSmkNlVYE\n" +
            "FNhY4sYbxBNm/xjA7v8/elGGPJN8AgIEAA==");

    /** A PKCS#12 file containing a single entry consisting of a self-signed ECC cert generated using Bouncy Castle. */
    static final TestKeystore BC_SELFSIGNED_ECC = new TestKeystore("entry", "password",
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA+gwgDCABgkqhkiG9w0BBwGggCSABIICPTCCAjkwggI1\n" +
            "BgsqhkiG9w0BDAoBAqCCAeIwggHeMCgGCiqGSIb3DQEMAQMwGgQUTwhf/cGoYB8sRvc34ryYZz6+\n" +
            "5aUCAgQABIIBsMmQqse4P0wkJVwpWUZ6F2eAtPLzPej89Ch7ayrs6m1yqckxquV0pLqUru3FZCBT\n" +
            "rGR8CdNTVkvD0qNFxEI5YlIoddZUPPlsrRwWkclG1/12h6qfCqsFE5ZHbo+5tF4mA8ir3bglVxi0\n" +
            "leFXYxG+ddY6asXUdKyZfbLSQViaqQaFih70B+oZmh1JTfU3J3aiozmRog+3xaRIHtv+IFlJd22U\n" +
            "L8neozt2tl22hkVxROIDxG+judbLzzgpdz5472BL5pj3VjgIzqYx9b163GLPSWZPMVyXiwhkFfYk\n" +
            "qdOdy6kGR6xjIz1PF4oYPnggJGN+InMRlZy3tYouzVC+8e3g9KMc/eTyfm5o3+pF5t8ANMwjyKet\n" +
            "Bzb4/cyqRGiDFMIGbrl6RJEc8xg9qnhN5bYNQl8pNiBz21WG6V/6hTg2OfpbktZ5Vzh+9q8/raXy\n" +
            "b7nfU8/LzaUGKi0DEK/NgsBou9e+z6TplC5PqwuRNCcRkpaaT4b8TUtXv/BZk21dhUT12Oe0rohe\n" +
            "EcW+rhl215x5Yv9HBnurzHDVQv4Th8EaKEAZuhfA9SMET9cZh33lCMdTezFAMBkGCSqGSIb3DQEJ\n" +
            "FDEMHgoAZQBuAHQAcgB5MCMGCSqGSIb3DQEJFTEWBBSYPFS/55FMqJEa8y0zy8Vxr3YflQAAAAAA\n" +
            "ADCABgkqhkiG9w0BBwaggDCAAgEAMIAGCSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFMBMFYeK\n" +
            "vVJJ8zDgJWHp5LmjHz9dAgIEAKCABIICEDGxvSKzgmAgBffxTC8fTvOw1Wj5Y5NFFSy+S734FO6C\n" +
            "CPTwuXXEh7oQeiL0/dZH4HGOppseEzlUnG928MdZJhkDiA1feduaUH+8U/1swWGFKEsZEpil22Hc\n" +
            "v2Wfz8FIIDTySso5c1e0A2Bvh8QoQeAUdOXq8Dbhu4wqDrb5qc7nM99pqkEmjrFV/bheTzayNvzV\n" +
            "D0WE4ve3jf9zwxhIslOgoyhMHCknBKhydvCjvqI9JAhnS/Nh02RqraAJlh7k2WDPBy0JMMyr+qof\n" +
            "vh2XA9bfFLf6RGdZqtPWYFB/jVoUVZ6dXL/Vt1ht9ZE89UfvxqW32k/KWoUzRfYqy4L1MQmVgAAm\n" +
            "4yGM0aSw4Os9PcN4o0nBgk3cXqTteB1hoG0toK8D1R6JBE4r2/XT0t/Fo+BLbAOPpRwtBNivfzPl\n" +
            "BIHfB6DE0h5Ubaq/my7+tQThaTXchX44vZIYSmSYnSYUdqCCn/cizWg2p266wKDTDSzqp7eDP4mX\n" +
            "7lj0BMgF+fNjpKd3WtpAvy0YD3MTa8aVgaXcme+CV6yp0SiYBFwVlEyITsWPVHAZG7HvSGQK5eUg\n" +
            "5OEbyzr3sB87oxr61Q0xHAViGHuo23tO0seeTYEPvWgWhBOQnybJh2JB2W/7Prga0LawurTkaPkU\n" +
            "XZaeXdwGkmErWb/7hBcNjTPAzYbNhneNRzDS0h4T77V8rGgOMUmFhJtAIQAAAAAAAAAAAAAAAAAA\n" +
            "AAAAADA9MCEwCQYFKw4DAhoFAAQU5ckKUAm2a3/V/Nl+77zgIAniAwMEFPrV3w6AJSOODXUYj3XC\n" +
            "cVVS+FIEAgIEAAAA");

    static final TestKeystore TEST_CERT_ECC = new TestKeystore("entry", "password",
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA44wgDCABgkqhkiG9w0BBwGggCSABIIBfDCCAXgwggF0\n" +
            "BgsqhkiG9w0BDAoBAqCCASEwggEdMCgGCiqGSIb3DQEMAQMwGgQUAGfYFdTdn1rWVQJYZGWy50/H\n" +
            "VtQCAgQABIHwTIgo0/L7JtFLtaeICOmlFWCcfyxtys/qeRXgy2P+YqW5XseZx0XpLFnjJa62xErH\n" +
            "f9guzCd31XbbfCZSQiYWxzMV+FHqunRlwuVszEfVOm/wiim1YNTLA32ojVGwgwKxKr7zE7ULN+0b\n" +
            "XmsxmvI54LBB2id7qhiY4ogHUB895C+kuENZBRQHJ5/65Q56b6acyZlCt1waVZC2d1y0Slb3jIUR\n" +
            "mEdc1DNmbGLmgVovTKBFDXeGc3IaThH9jcY5y17S0D9bE0Pj6sj4EUJKAujHGBfyawNI+Lb4wErB\n" +
            "iyCzIqbldzS1QFxbtfhBdPZewPOFMUAwGQYJKoZIhvcNAQkUMQweCgBlAG4AdAByAHkwIwYJKoZI\n" +
            "hvcNAQkVMRYEFHOEzyjrfvzqIkN1NMM88ENiu+osAAAAAAAAMIAGCSqGSIb3DQEHBqCAMIACAQAw\n" +
            "gAYJKoZIhvcNAQcBMCgGCiqGSIb3DQEMAQYwGgQUi9agk0Z6W5pDR4SA8nU9LxZUCFgCAgQAoIAE\n" +
            "ggGYzb2pg0W+RWUZ7W+kdgW6aGe1+PY+DumzevD1zxb6NHhlQBTwyK/qtS4pQ0kSCv4lgNWHm1G8\n" +
            "pYhDCajn5F8QeYjLva8+KdM8wlsWWsL1dh3abBXFqcYIMTVbDjLyMjnW3+DU5yLpe2gUX/sBx4P/\n" +
            "0/DRhxKxOCapw5umQJDxC3gC/NmFO0WTRkS8BfJJljAAu/PjMZ+Y4B9kTG5/gP6IS5WujEMq7AN5\n" +
            "qlGppivzAWPV+bMgRSsoKM650e26iMETj77Vp0dWXrqZKrHBkn8kkmaFATXwgNIe448lLF+c+2XS\n" +
            "tiWkgzkBsKhtikkGq/UqyORQrI75Yky+7CVHBpiChsXYMN+eXTaTEhdAqMC/hRBfQSS8weYEKcea\n" +
            "ywRbE1FLiOfgBxVUEtdaFmuH9eUoddd5U6PKqk4IML4armG8SsnRG3cuuHT4f4ASmG5ngkGeSVeO\n" +
            "NBI5PyPOpet3EhGGmpZ+veJwyJOn16r7aEcL9UFN7J/RDapOZI3BNC/PrwXTONhStibTRzAs/S31\n" +
            "yFoutRvHbzjeCwlJAAAAAAAAAAAAAAAAAAAAAAAAMD0wITAJBgUrDgMCGgUABBSAQWtE+TAJiYOi\n" +
            "2akG8uSLZpmIzwQUN6jNz/ih6UaQdkSByOhFQM0e9pMCAgQAAAA=");

    static final TestKeystore VIN_ECC_TEST_7 = new TestKeystore("entry", "password",
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA6YwgDCABgkqhkiG9w0BBwGggCSABIIBfDCCAXgwggF0\n" +
            "BgsqhkiG9w0BDAoBAqCCASEwggEdMCgGCiqGSIb3DQEMAQMwGgQUmR5OnZ11+YaSKaujM4rsJdHx\n" +
            "d3QCAgQABIHwHwiouPOmuZTT0O7WrofahX3cxxwfDdduAm7//UBazxOsSdzzkEo828LTF9M+DzcX\n" +
            "7P8Lc+Ups11FVL7SjlIuScWHOmDWlVPityZo3ga9OWxuln1MXuUCRh4E7EPBdn9yCp0zChAWWTI5\n" +
            "9FSw57lThWk8luJSSXMwuUZ3YzfvsjDGq1KQofxs5vFqXR5lroF/IwlSxSBSILaA523HpcUV7hzv\n" +
            "rZNumgl9b5ngpHDuI6PqpKImcC46Qsk2A5K0O2ypJqOKWFRvP+pxPpRxEWLxFm8iuTUQHUO7tbCl\n" +
            "k8GMXrETAnK3rZ4e1v7cdQg69h0bMUAwGQYJKoZIhvcNAQkUMQweCgBlAG4AdAByAHkwIwYJKoZI\n" +
            "hvcNAQkVMRYEFHzWGhaWtGPPQAaOdWkXYdY2IM4dAAAAAAAAMIAGCSqGSIb3DQEHBqCAMIACAQAw\n" +
            "gAYJKoZIhvcNAQcBMCgGCiqGSIb3DQEMAQYwGgQUL0rR63uVzeHfiq0fICnFl+QYOboCAgQAoIAE\n" +
            "ggGwsikD/CyHwSZw5BPZebVBjmzz5D5jCYgHrI5yyOZXOVKLvO+VNs9tSrM95UoRR1JlcugQyvC/\n" +
            "8Pd/buZPqpwv1RzvMUVsdGfp9w00VxlYgoa+t6lKUhWiCaJKe067AoTbh7fjqbZPY/ZLl/5s0S6L\n" +
            "hiteqXjy4ow93FuSsf3w1YiIRg3QqBSawQmVO+qSDU2AKMzvQpXWwQfBmCMKzjaW44oAnuO2DlDI\n" +
            "NFfJ4uswA4uIyriTqU+eX8QUFeozcQTuP6tsHe0kzLoI+HU9moej4u4BDr0XnwqG/o8stLX4SstQ\n" +
            "lagjp2btXPL2a82foOmOVEdsfnc+Ic2scIYlVoK/+3S4FES//cM0Jrig5vjANh6MEx8xF79F6M/h\n" +
            "PDjLQr4JVG/sbHyM8RXsmbJu7bkCWXISs5jHUDa/Balvu5joPpzw7S2CVZ0iiM86Hzlx+rd+2/Dq\n" +
            "DMZ2LiCGhnaLEV9A6Gx6wkcJ11XK0dAsHBmDURw9yeFN724Xyunkji2VDvLBISmP+4iqYIDg3N4w\n" +
            "UL4YtONjSxceMzFAlgZh1BZ/+yopy/bTZwqPla4tecemxh+JAAAAAAAAAAAAAAAAAAAAAAAAMD0w\n" +
            "ITAJBgUrDgMCGgUABBR+bt2Kz6fRIdJleWuIKvWJpAiyqwQUBeiabbrIgAkPTKxH48fJMF+ENDcC\n" +
            "AgQAAAA=");

    static final TestKeystore CERTICOM_DEMO_CHAIN = new TestKeystore("entry", "password",
            "MIIHowIBAzCCB10GCSqGSIb3DQEHAaCCB04EggdKMIIHRjCB1AYJKoZIhvcNAQcBoIHGBIHDMIHA\n" +
            "MIG9BgsqhkiG9w0BDAoBAqBuMGwwKAYKKoZIhvcNAQwBAzAaBBSIhC6yiAuLJuhDhIGmft3WMYiw\n" +
            "UQICBAAEQFPk0qAfCqkQXwHfjM/wFVYb28uA9+WAUJGiIEa+a5/VLtIMT7IqNvY82/aLegcFsEg2\n" +
            "E7pmzn6xqsmpxnqo6QgxPjAZBgkqhkiG9w0BCRQxDB4KAGUAbgB0AHIAeTAhBgkqhkiG9w0BCRUx\n" +
            "FAQSVGltZSAxMjQwODY5ODg3Njg1MIIGawYJKoZIhvcNAQcGoIIGXDCCBlgCAQAwggZRBgkqhkiG\n" +
            "9w0BBwEwKAYKKoZIhvcNAQwBBjAaBBQlus3HuSxLv7FnC308xCKAsp9KHwICBACAggYYQCIf0XWt\n" +
            "uLRDQKemar0/6AXVeQngcSi4C8PysbQBHdBlIZYVjD5Gbf0uR0HwlVgnORlKcAA88BwTjOxWQq6i\n" +
            "yFJI9Xv0sbNkkDF0yweuyURkzhKAp8C0jnJ0+nOWacwH0Nh6yI8aL+DK8nkPEq6Q1vZz5I7o3Bm0\n" +
            "vSU54/voYsfD9KnYd9GhzBizE31dOrJkozswKQGzLkl1ppEfQej/F347pPCoCadtA+vlRcbwrhZ3\n" +
            "TCLuGU93mgZE1Q+K7mSO86kxeKNjLZ/5rHVR0bn9wN/eemeb44AUPmpbdREnYmQoqscTM2ATPiiF\n" +
            "T2HtNO7khNK3WEsfCgdL9iGODEwLuzkds00QbhnKjmfIfdVrD2z5++eHNecC4Pz/bw4JnAQmtFND\n" +
            "JoVlrS+9j3oKuXCLiVzeE+jABFLZf2CSoxaK8+yfaQyKpkICYIEpoTeVh2iIrFIPb6Cgxypt0qJw\n" +
            "Gh2FJ6iZuNI7Bfom3n2tKXFRBCmBjX50iUXC8fEA0DudY136rCm3moat77okgSCKNSySt70xq0ff\n" +
            "32v7aYrw1NSu77MJT455DBNybTg+u5GSdDL7Nu18NJTGQ5MDJL5VmFeWyhybrtR9/ZlMgGi7jsXf\n" +
            "J4JpShx+VAPMWQceteJDURnt8ns4dbOhdMZkZdDG19W8WGZNDmVu02BX0WIdR9AnAfcF3g/8JQF9\n" +
            "8+F2qX4mXwS1iuu2jW/ub0cfuVJSHvRgjukPaCu9vJ9JlPd2sBhY1NSYVpBWjkUVtgQNL9Mb7jjk\n" +
            "kW9YCaVfitLMZFbcle08XrnwfC69ZKmKdyZBMaeMMHzST4MNJysv/705iKj7MZ0/Qut1xux/gnd3\n" +
            "DScBH9nBsxmgT3pQP2x3ikXDIKbY27ZT66hRO2353DfQaJR9sqz4IFm0I+tmWq5rkAcHt+9dzdLI\n" +
            "fBxgK4qmnAeh9jxqWdAWvPfdt907AGKfWJJbMTiebByzaf2uH26nm7O+oyWBEdCgapZFZ2YpOAdM\n" +
            "n2eb70mKdJQz0zN9JbFPhFTGsD2bbMycqCSbKNhrv5KrROfLvx6cEekMe9QPctqo1p4oXeRHJH2A\n" +
            "8rWAwje01HJajf9CCyqUx8W7PyZ8Tcn6tYazQHNipBuzpJcfpHiSM0y+KDMUjfDJAiUc4+/LdVMC\n" +
            "yhjouUdE1WbwhZUKe9SJ7H0qPQiVpBteeN+1OlPCBGiCrB9IeWGoSmqy0ljAd87WcA6ZnVVWeq4F\n" +
            "cuIWlyBJM413fjBW5nS61HXcbyRVSL75iIxburpZeXALASgSIlldoL58k+cUriDDkb6Zn5imwZY3\n" +
            "FKtgO3I18KvpfTnjn6wonc8E2Vfo7l6GWhPhhqVSQSUVJH1jyK1DQhZKUii7tqLQsBGqJ7qsjmrT\n" +
            "HqIcpWoxkpIBWSU1Wdp28prmfA9B82CyhTtbCwUngmWrMTJn9QQgxls9kmEXjm6bSIYL4C1QUAoa\n" +
            "TyURCTpWyYJBXgKJuv2No7ZZy1vqmGUzZkfOUHyQTNWYrZGOgjbPsn4ZNT86LHgpIaoV37dRioKf\n" +
            "BkDrGT/3xSIeCyuhJxWwcaipZE4VZeqgA60ZtzaIerNum3xLvXBlXSuB44wmwBxkDSHgo5uljU5T\n" +
            "ckyp7JgUgUDZJDxV+nVzwOOiB6GhSrdGQwHMO2NwOSTosWozOHnAVjEq4a8o1ig6wNSJSDKY0zD5\n" +
            "/CnB0EtnRM9M8HFw3EH2RdEVXtWxozL0OfhFFkszshCjqvRlDcJK6bDmZTe4pVFqk9Y6tFsDgj9/\n" +
            "YpXAdyKB4sk8jL0lszFEV2qiO1gING+wxrFctfJ7r0ysIXo3UXzR9gri98RuRrpdC+yycb66Q0gC\n" +
            "vK+TgJkyqTtRncrbTCPwKeQuY1dxLN8Wj6rzAlyyo4fqYHAwB68IldoU6lhSIwyIHA6rVLl6KyCB\n" +
            "OJGn+/okh0AV/u4G3SX+CS07EU5cWxut9CJ4EUuTkjsLYgVk4g/5CXmQtj9XWihv0ySSO/3mh5Ol\n" +
            "USX6CDFFpr70FuFgWFHu0bK4OllqUY2n5cEY7/VJPUgcqzhffoFWO+xXQGhTKqRp+PmoKG0yEd9p\n" +
            "ebptgkG91o3NHiHntMsKMD0wITAJBgUrDgMCGgUABBRWjntPaly/wxOW7GVJkhBTTs/1vAQUnXLA\n" +
            "iRfKZ/+m3lf5L/vahSx7GokCAgQA");

    static final String CERTICOM_KEY_COMPRESSED_FORMAT =
            "MGICAQAwEAYHKoZIzj0CAQYFK4EEAAEESzBJAgEBBBRQieICGuF1jSJ4tU3R9yCh\n" +
            "NV8g5qEuAywABAE814kv8euQhqkuB/zb93UYu4NS5AV1c/4SU86koMXBQL1pNymX\n" +
            "gAVp9w==";

    static final String CERTICOM_KEY_COMPRESSED_FORMAT_CERT_CHAIN =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIB4TCCAZ6gAwIBAgIBAjALBgcqhkjOPQQBBQAwgY8xCzAJBgNVBAYTAkNBMRAw\n" +
            "DgYDVQQIEwdPbnRhcmlvMRAwDgYDVQQHEwdUb3JvbnRvMRcwFQYDVQQKEw5DZXJ0\n" +
            "aWNvbSBDb3JwLjEUMBIGA1UECxMLU0FNUExFIE9OTFkxLTArBgNVBAMTJFNTTCBQ\n" +
            "bHVzIFNhbXBsZSBFQ0RTQSBDQSBDZXJ0aWZpY2F0ZTAeFw0wNTAzMjgyMTU2MzRa\n" +
            "Fw0xNDAzMjgyMTU2MzRaMIGTMQswCQYDVQQGEwJDQTEQMA4GA1UECBMHT250YXJp\n" +
            "bzEQMA4GA1UEBxMHVG9yb250bzEXMBUGA1UEChMOQ2VydGljb20gQ29ycC4xFDAS\n" +
            "BgNVBAsTC1NBTVBMRSBPTkxZMTEwLwYDVQQDEyhTU0wgUGx1cyBTYW1wbGUgRUNE\n" +
            "U0EgU2VydmVyIENlcnRpZmljYXRlMCswEAYHKoZIzj0CAQYFK4EEAAEDFwACATzX\n" +
            "iS/x65CGqS4H/Nv3dRi7g1LkoxIwEDAOBgNVHQ8BAf8EBAMCA4gwCwYHKoZIzj0E\n" +
            "AQUAAzAAMC0CFBrN8h6Om95rkdem2EHXKA/yPADZAhUAvVrPmzlVCRTHA9pkEGa8\n" +
            "PlrQUNU=\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIB7jCCAaugAwIBAgIBATALBgcqhkjOPQQBBQAwgY8xCzAJBgNVBAYTAkNBMRAw\n" +
            "DgYDVQQIEwdPbnRhcmlvMRAwDgYDVQQHEwdUb3JvbnRvMRcwFQYDVQQKEw5DZXJ0\n" +
            "aWNvbSBDb3JwLjEUMBIGA1UECxMLU0FNUExFIE9OTFkxLTArBgNVBAMTJFNTTCBQ\n" +
            "bHVzIFNhbXBsZSBFQ0RTQSBDQSBDZXJ0aWZpY2F0ZTAeFw0wNTAzMTgyMDI1MDda\n" +
            "Fw0xNTAzMTgyMDI1MDdaMIGPMQswCQYDVQQGEwJDQTEQMA4GA1UECBMHT250YXJp\n" +
            "bzEQMA4GA1UEBxMHVG9yb250bzEXMBUGA1UEChMOQ2VydGljb20gQ29ycC4xFDAS\n" +
            "BgNVBAsTC1NBTVBMRSBPTkxZMS0wKwYDVQQDEyRTU0wgUGx1cyBTYW1wbGUgRUNE\n" +
            "U0EgQ0EgQ2VydGlmaWNhdGUwKzAQBgcqhkjOPQIBBgUrgQQAAQMXAAMDbRQ6oDC1\n" +
            "BhglM8sPLTMWsYGr/WCjIzAhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQD\n" +
            "AgEGMAsGByqGSM49BAEFAAMwADAtAhQ+JPKNNm1YzWuR9C828e5NxPlmRgIVAtU6\n" +
            "bFQi9PAMnBB/dLFrMANvSCvK\n" +
            "-----END CERTIFICATE-----";

    static final String TEST_KEY2 =
            "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDC3OXy5j5VeLqqzLwqBQRddKxxiVjUKmRFp\n" +
            "i4xXe50Cx4zI7h9wMhqlcsRAy2KbkI8=";

    static final String TEST_CERT2 =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBZjCB7KADAgECAgkA3OkNFsXD6ucwCgYIKoZIzj0EAwMwGjEYMBYGA1UEAwwPZGF0YS5sN3Rl\n" +
            "Y2guY29tMB4XDTA5MDQyNTAyNTQyNVoXDTI5MDQyMDAyNTQyNVowGjEYMBYGA1UEAwwPZGF0YS5s\n" +
            "N3RlY2guY29tMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE5LMqX1OV0JnsZP5Ex6oOicHSzAudFWVI\n" +
            "gngPRzZ7+NAYM87/arBaxoFu3lI9mhbk0DyVDRxrP3/UmCjFYuDhprA9xniCLtnwnOCqFI1Upp7v\n" +
            "y4r9n1B4cbGXrs8NZa07MAoGCCqGSM49BAMDA2kAMGYCMQDkRKd6u4p+8CoZEG55RzPOtmuZjAem\n" +
            "VqpwJFHfaLcQrtwTrl9De68+4YNp4btsKEgCMQC783Rz1TcxEIUTZpPp+s9UkaBTmi3XRAH+2kni\n" +
            "xgfpipz76LAblb0N8t0BqM6LT54=\n" +
            "-----END CERTIFICATE-----";

    static final String TEST_SIGNATURE_PUBKEY =
            "MHgwEAYHKoZIzj0CAQYFK4EEACIDZAAEYQQxvqTkqdxDNYlvlQamj00pk6cs3bNAjyXuyS17Xcsu\n" +
            "4SoYE4N2pDKf0+HCmduzhZCoL+qA7KFgJMPL5noc9IML6Dw1SyI4SVbtDJ/jActZAesFOnZjwt1o\n" +
            "qu+sHaBJ1So=";

    static final String TEST_SIGNATURE_SIGVALUE =
            "MGUCMD87vSDKvl+ObdUPl3eWKa6Vbd1cntT/9LW5AEamptssZay5iClG9rIon93T3YBGFwIxAPNZ\n" +
            "L/Q3GJ+1uKi4m3tRcUrZhBWTNklmfkOXYo+9PU+ZxlUaPTuAWnCmAjzmkcqQ/A==";

    static final String PAYLOAD_DATA =
            "This is some test data to use as a payload for signatures/SSL/etc.\n\n" +
            "I'd like it to be fairly long -- about a paragraph, say.  It doesn't really matter what's in it.\n" +
            "One more short line ought to do it -- there we go.";

    static final SecurityProvider DEFAULT_SECURITY_PROVIDER = new SecurityProvider("default", false);

    static final SecurityProvider[] SECURITY_PROVIDERS = {
        DEFAULT_SECURITY_PROVIDER,
        new LunaNativeSecurityProvider("luna", "com.chrysalisits.cryptox.LunaJCEProvider", "com.chrysalisits.crypto.LunaJCAProvider"),
        new LunaPkcs11SecurityProvider("lunapkcs11"),
        new SecurityProvider("bc", false, "org.bouncycastle.jce.provider.BouncyCastleProvider"),
        new SecurityProvider("bclowpri", true, "org.bouncycastle.jce.provider.BouncyCastleProvider"),
        new SecurityProvider("certicom", false, "com.certicom.ecc.jcae.Certicom"),
        new SecurityProvider("rsa", false, "com.rsa.jsafe.provider.JsafeJCE"),
    };


    //
    //
    // --- Entry point ---
    //
    //

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0] == null || args[0].contains("?") || args[0].contains("help")) {
            showUsage();
            System.exit(11);
        }

        SecurityProvider prov = lookupSecurityProvider(args[0]);
        if (prov == null) {
            System.out.println(MessageFormat.format("Unrecognized provider nickname: {0}\n", args[0]));
            showUsage();
            System.exit(11);
        }

        String testname = args.length > 1 ? args[1] : null;

        logger.log(Level.INFO, "Installing provider: " + prov.shortname);
        prov.install();

        // Run test
        final EllipticCurveProviderTester test = new EllipticCurveProviderTester(prov);
        boolean result = testname == null ? test.checkAllRequirements() : test.checkSpecificRequirements(new String[] { testname });
        System.exit(result ? 0 : 1);
    }

    private static void showUsage() {
        System.out.print("Usage: java EllipticCurveProviderTester <provname> [<test>]\n\n" +
                           "  provname   one of: ");
        for (SecurityProvider sp : SECURITY_PROVIDERS)
            System.out.print(sp.shortname + " ");
        System.out.println("\n  test       name of a single test, subtest, or sub-subtest");
        System.out.println("\nExample: java -Xbootclasspath/p:lib/jdk-ECParameters-patch.jar -DlunaTokenPin=tH3S-HpW3-sCFK-p7E9 -DlunaLibraryPath=cryptoki.dll EllipticCurveProviderTester lunapkcs11 subsubtestCrtJsse_ECDH_ECDSA_AES128_BcKey");
    }

    static SecurityProvider lookupSecurityProvider(String provname) {
        for (SecurityProvider sp : SECURITY_PROVIDERS)
            if (sp.shortname.equalsIgnoreCase(provname))
                return sp;
        return null;
    }

    static void enableLogging() {
        try {
            invokeStaticMethod("com.certicom.tls.util.SSLLog", "setLogVerboseLevel", new Class[] { int.class }, 3);
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to enable logging for Certicom SSL: " + e.getClass() + ": " + e.getMessage());
        }
    }
}
