package com.l7tech.security.cert;

import com.l7tech.util.Pair;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Generates test data to populate TestKeys.
 */
public class TestKeysGenerator {
    public static void main(String[] args) throws Exception {
        generateAndOutputRsa(512);
        generateAndOutputRsa(768);
        generateAndOutputRsa(1024);
        generateAndOutputRsa(1536);
        generateAndOutputRsa(2048);

        generateAndOutputEcc("secp256r1");
        generateAndOutputEcc("secp384r1");
        generateAndOutputEcc("secp521r1");

        generateAndOutputDsa(1024);
    }

    private static void generateAndOutputRsa(int bits) throws Exception {
        output("RSA_" + bits, new TestCertificateGenerator().subject("cn=test_rsa_" + bits).keySize(bits).daysUntilExpiry(365 * 25).generateWithKey());
    }

    private static void generateAndOutputDsa(int bits) throws Exception {
        output("DSA_" + bits, new TestCertificateGenerator().subject("cn=test_dsa_" + bits).dsaKeySize(bits).daysUntilExpiry(365 * 25).generateWithKey());
    }

    private static void generateAndOutputEcc(String curveName) throws Exception {
        output("EC_" + curveName, new TestCertificateGenerator().subject("cn=test_ec_" + curveName).curveName(curveName).daysUntilExpiry(365 * 25).generateWithKey());
    }

    private static void output(String name, Pair<X509Certificate, PrivateKey> k) throws Exception {
        outputValue(name + "_CERT_X509_B64", k.left.getEncoded());
        outputValue(name + "_KEY_PKCS8_B64", KeyFactory.getInstance(k.right.getAlgorithm()).getKeySpec(k.right, PKCS8EncodedKeySpec.class).getEncoded());
    }

    private static void outputValue(String name, byte[] bytes) throws Exception {
        final String b64 = HexUtils.encodeBase64(bytes);
        final String b64WithLineBreaks = TextUtils.breakOnMultipleLines(b64, 72);
        final String b64StringLiteral = "        \"" + b64WithLineBreaks.replaceAll("[\\r\\n]", "\" + \n        \"") + "\";\n";

        System.out.println("    public static final String " + name + " = ");
        System.out.println(b64StringLiteral);
    }
}
