package com.l7tech.external.assertions.oauth.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.StringTokenizer;
import java.math.BigInteger;
import java.net.URLDecoder;

/**
 * Test the OAuthAssertion.
 */
public class ServerOAuthValidationAssertionTest {

    private static final Logger log = Logger.getLogger(ServerOAuthValidationAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    private PublicKey publicKey;
    private X509Certificate publicCert;
    private PrivateKey privateKey;
    private SecureRandom rand;

    @Before
    public void setUp() throws Exception {

        try {
            initKeypair();

            if (rand == null)
                rand = new SecureRandom((this.getClass().getName() + System.currentTimeMillis()).getBytes());

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Unable to initialize tests: " + ex);
            throw ex;
        }
    }

    @Test
    @Ignore("unknown")
    public void testKeyInitialized() throws Exception {

        /*
        Requestor=ClientIdentifier&
        Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&
        ExpiresOn=1277219129&
        HMACSHA256=AzyMK1Kgq4fezPRwYK89DD%2bFjpIx7J7XjmYz8wNprVsGPZut5epB%2fGkaQQUlAGYbgUzCdZkyvoNtmWmNiFhAYNpg
                %2bDnXqRBuARxJ0KUHP9uLBAMZxK2xkauY%2fdZxhLl2d3v5VbYwf9uv1pHdDuuzUKF3qHZ381NZ%2fsoNEkF%2ftO8%3d
         */
//        final byte[] signBytes =
//                "Requestor=ClientIdentifier&Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&ExpiresOn=1277219129".getBytes();

        final byte[] signBytes =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&ExpiresOn=12312010".getBytes();

        final byte[] signDigestBytes = null;

        try {
            assertNotNull("Public key was not initialized", this.publicKey);
            assertNotNull("Private key was not initialized", this.privateKey);

            // instantiate Signature instance
            Signature signer = Signature.getInstance("SHA256withRSA");
            Signature verifier = Signature.getInstance("SHA256withRSA");
            assertNotNull(signer);
            assertNotNull(verifier);

            System.out.println(MessageFormat.format("Initialized Signature instance algorithm({0}), provider({1})", signer.getAlgorithm(), signer.getProvider()));

            // Sign the text bytes
            signer.initSign(this.privateKey);
            signer.update(signBytes);
            byte[] dasSig = signer.sign();
            assertNotNull(dasSig);
            assertTrue(dasSig.length > 0);

            System.out.println(MessageFormat.format("Signature length: {0}; value: {1}", dasSig.length, HexUtils.encodeBase64(dasSig)));

            // Now verify the signed bytes
            verifier.initVerify(this.publicKey);
//            verifier.initVerify(publicCert);
            verifier.update(signBytes);
            if (verifier.verify(dasSig))
                System.out.println("Signature valid: TEST PASSED");
            else
                fail("Good signature verify failed - " + HexUtils.encodeBase64(dasSig));

            // Negative test
            byte[] badSig = new byte[dasSig.length];
            rand.nextBytes(badSig);
            if (verifier.verify(badSig))
                fail("Expected SignatureException, but passed with bad signature value instead - " + HexUtils.encodeBase64(badSig));
            else
                System.out.println("Bad signature verify failed: TEST PASSED");

        } catch (NoSuchAlgorithmException nsa) {
            nsa.printStackTrace();
            fail("No Signature Algorithm: " + nsa.getMessage());
        } catch (Exception ex) {
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    @Test
    @Ignore("unknown")
    public void testSignHash() throws Exception {

        /*
        Requestor=ClientIdentifier&
        Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&
        ExpiresOn=1277219129&
        HMACSHA256=AzyMK1Kgq4fezPRwYK89DD%2bFjpIx7J7XjmYz8wNprVsGPZut5epB%2fGkaQQUlAGYbgUzCdZkyvoNtmWmNiFhAYNpg
                %2bDnXqRBuARxJ0KUHP9uLBAMZxK2xkauY%2fdZxhLl2d3v5VbYwf9uv1pHdDuuzUKF3qHZ381NZ%2fsoNEkF%2ftO8%3d
         */
//        final byte[] signBytes =
//                "Requestor=ClientIdentifier&Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&ExpiresOn=1277219129".getBytes();

        final byte[] signBytes =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&ExpiresOn=12312010".getBytes();

        final byte[] signDigestBytes = null;

        try {
            assertNotNull("Public key was not initialized", this.publicKey);
            assertNotNull("Private key was not initialized", this.privateKey);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] sha256Digest = md.digest(signBytes);

            // instantiate Signature instance
            Signature signer = Signature.getInstance("SHA256withRSA");
            Signature verifier = Signature.getInstance("SHA256withRSA");
            assertNotNull(signer);
            assertNotNull(verifier);

            System.out.println(MessageFormat.format("Initialized Signature instance algorithm({0}), provider({1})", signer.getAlgorithm(), signer.getProvider()));

            // Sign the text bytes
            signer.initSign(this.privateKey);
//            signer.update(signBytes);
            signer.update(sha256Digest);
            byte[] dasSig = signer.sign();
            assertNotNull(dasSig);
            assertTrue(dasSig.length > 0);

            System.out.println(MessageFormat.format("Signature length: {0}; value: {1}", dasSig.length, HexUtils.encodeBase64(dasSig)));

            // Now verify the signed bytes
            verifier.initVerify(this.publicKey);
//            verifier.initVerify(publicCert);
            verifier.update(signBytes);
            if (verifier.verify(dasSig))
                System.out.println("Signature valid: TEST PASSED");
            else
                fail("Good signature verify failed - " + HexUtils.encodeBase64(dasSig));

            // Negative test
            byte[] badSig = new byte[dasSig.length];
            rand.nextBytes(badSig);
            if (verifier.verify(badSig))
                fail("Expected SignatureException, but passed with bad signature value instead - " + HexUtils.encodeBase64(badSig));
            else
                System.out.println("Bad signature verify failed: TEST PASSED");

        } catch (NoSuchAlgorithmException nsa) {
            nsa.printStackTrace();
            fail("No Signature Algorithm: " + nsa.getMessage());
        } catch (Exception ex) {
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    @Test
    @Ignore("unknown")
    public void testTokenParse() throws Exception
    {
        final String testToken =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2fServices%2fAuto%2fPolicy%2fSummary&ExpiresOn=12312010" +
                "&RSASHA256=Sj5pgnVXXh7aDqLmCJqkqARChQF1jGnPX/oxxqrBwb4JnUFXxzKYRZ4tqrI7vJLV6C2P31fv6PUFhyGRjWLXC6kX50J1y6jlbd8lLKdRv+O6AKdauQyLSh7lOJO6Y+aVAsG202lNeTzysA7eyaIgZK32hyEU2Ibqxii+n7FR1Pw=";

        try {
            StringTokenizer st = new StringTokenizer(testToken, "&");
            String[][] pairs = new String[st.countTokens()][2];

            int i=0;
            String item;
            while (st.hasMoreTokens()) {

                item = st.nextToken();
                pairs[i][0] = item.substring(0, item.indexOf("="));
                pairs[i][1] = item.substring(item.indexOf("=")+1);
                i++;
            }

            StringBuffer sb = new StringBuffer("<parsedToken>\n");
            for (String[] one : pairs)
                sb.append(MessageFormat.format("<name>{0}</name><value>{1}</value>", one[0], one[1])).append("\n");
            sb.append("</parsedToken>");
            System.out.println(sb.toString());
        } catch (Exception ex) {
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    @Test
    @Ignore("unknown")
    public void oldtestErieSampleEncrypted() throws Exception {

        final byte[] signBytes =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f0&ExpiresOn=1277477796".getBytes();

        final String sigparm = "NJ66NHuNqnDNrQNoJEN4fynEzgLNcoZhNzWWlEn1BW%2fMLLI9YBIbgByzwJ1BBf%2bD%2biK1k9vCbkTN0WJs%2bjBAVx83DaybDMoGO6j7z%2bY6rmVKN49XfIECjh07Dlo%2fx59tr5O2WKwBeEKJM5GkoNlMok6B%2fLHNEAZhgHojqIBjOkA%3d";
        final byte[] sigBytes =
//                HexUtils.decodeBase64("NJ66NHuNqnDNrQNoJEN4fynEzgLNcoZhNzWWlEn1BW/MLLI9YBIbgByzwJ1BBf+D+iK1k9vCbkTN0WJs+jBAVx83DaybDMoGO6j7z+Y6rmVKN49XfIECjh07Dlo/x59tr5O2WKwBeEKJM5GkoNlMok6B/LHNEAZhgHojqIBjOkA=");
                HexUtils.decodeBase64("AEwbF2O1oZfrYFkafMgDgV0e5iv9Ymm227hI9svPD6bWVwyJyYzgjiOC8xxnxS8k2SwoAqK/CTyBUZyycc2gAPnSxWr1x0HWsWmYVK0gbY0twZR0+9myns5RgcVeNu4+B++1JE7Kq++05pybuc5XRguoEb5vuZAWbdzyZRmgB3U=");
//        final String unescape = URLDecoder.decode(sigparm, "UTF-8");
//        final byte[] sigBytes =
//                HexUtils.decodeBase64(unescape);

        try {
            final String modulus = "uaBhE6x8EP9+dDAhOBmYRmrE1mZumH3e5DcxBp+bSeO+eBIuKxo1HKrPHqD2/rgWmACCPpa7ywGgeQIebTPtrnvZG480WQPQ3sfDkbwttbeUlr5itGhoWm+bGbQKxX3QMeyCZjZ61ViK0vZTID7Ki/w9KJb43GGQam01RHazkJE=";
            final String exponent = "AQAB";

            BigInteger modValue = new BigInteger(modulus.getBytes());
            BigInteger expValue = new BigInteger(exponent.getBytes());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modValue, expValue);
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(spec);
//            RSAPrivateKeySpec spec2 = new RSAPrivateKeySpec(modValue, expValue);
//            PrivateKey pub = KeyFactory.getInstance("RSA").generatePrivate(spec2);

            Cipher c1 = Cipher.getInstance("RSA");
            c1.init(Cipher.DECRYPT_MODE, pub);
            c1.update(sigBytes);
            byte[] d1 = c1.doFinal();
            System.out.println("cipher: " + c1.getProvider() + "; alg: " + c1.getAlgorithm());
            System.out.println("cryptext length: " + sigBytes.length);
            System.out.println("decrypted sigBytes (1) length: " + d1.length + "; b64: " + HexUtils.encodeBase64(d1));
//            System.out.println("try toString: " + new String(d1));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(signBytes);
            byte[] sha256Digest = md.digest();
            System.out.println("signBytes sha-256 length: " + sha256Digest.length + "; value: " + HexUtils.encodeBase64(sha256Digest));

//            RSAPrivateKeySpec spec2 = new RSAPrivateKeySpec(modValue, expValue);
//            PrivateKey priv = KeyFactory.getInstance("RSA").generatePrivate(spec2);
//
//            Cipher c2 = Cipher.getInstance("RSA/ECB/NoPadding");
//            c2.init(Cipher.DECRYPT_MODE, priv);
//            byte[] d2 = c2.doFinal(sigBytes);
//            System.out.println("decrypted sigBytes (2): " + HexUtils.encodeBase64(d2));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }

    }


    @Test
    @Ignore("unknown")
    public void testErieSampleEncrypted() throws Exception {

        /*
           Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f1&ExpiresOn=1277477798
           &RSASHA256=f%2bobJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu%2fdlrwNhKtG8DR0Ylcpveq%2fFhqjMudsW2ClwWJQRMBi45ytS9STO7Km4DsVlwgdFFB1DT%2bvSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf9LWv9HVVfruLXQpXL5A%3d
         */
        final byte[] signedBytes =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f1&ExpiresOn=1277477798".getBytes();

        final String sigparm =
//                "f%2bobJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu%2fdlrwNhKtG8DR0Ylcpveq%2fFhqjMudsW2ClwWJQRMBi45ytS9STO7Km4DsVlwgdFFB1DT%2bvSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf9LWv9HVVfruLXQpXL5A%3d";
                "f+obJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu/dlrwNhKtG8DR0Ylcpveq/FhqjMudsW2ClwWJQR\n" +
                "MBi45ytS9STO7Km4DsVlwgdFFB1DT+vSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf\n" +
                "9LWv9HVVfruLXQpXL5A=";
//        final String decoded = URLDecoder.decode(sigparm, "UTF-8");
        final byte[] sigBytes = HexUtils.decodeBase64(sigparm);

        try {
            PublicKey pub = getEriePublicKey();

//            Cipher c1 = Cipher.getInstance("RSA");
//            Cipher c1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Cipher c1 = Cipher.getInstance("RSA/ECB/NoPadding");
            c1.init(Cipher.DECRYPT_MODE, pub);
            c1.update(sigBytes);

            System.out.println("cipher: " + c1.getProvider() + "; alg: " + c1.getAlgorithm());
            System.out.println("cryptext length: " + sigBytes.length + "; value: " + sigparm);

            byte[] d1 = c1.doFinal();
            System.out.println("decrypted sigBytes (1) length: " + d1.length + "; b64: " + HexUtils.encodeBase64(d1));

//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            md.update(signedBytes);
//            byte[] sha256Digest = md.digest();
//            System.out.println("signBytes sha-256 length: " + sha256Digest.length + "; value: " + HexUtils.encodeBase64(sha256Digest));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }

    }

    @Test
    @Ignore("unknown")
    public void testCrypto() throws Exception {

        try {
            final String dataStr = "KZqTNL7DVgtkFqg9BQaP/QH3LT2WuhNeuStF+Nkrd+8=";
            System.out.println("data: " + dataStr);
            final byte[] data = HexUtils.decodeBase64(dataStr);

            Cipher c1 = Cipher.getInstance("RSA");
            c1.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] d1 = c1.doFinal(data);
            System.out.println("cipher: " + c1.getProvider() + "; alg: " + c1.getAlgorithm());
            System.out.println("encrypted sigBytes (1) length: " + d1.length + "; b64: " + HexUtils.encodeBase64(d1));

            Cipher dc = Cipher.getInstance("RSA");
            dc.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] p1 = dc.doFinal(d1);
            System.out.println("cipher: " + dc.getProvider() + "; alg: " + dc.getAlgorithm());
            System.out.println("decrypted sigBytes (1) length: " + p1.length + "; b64: " + HexUtils.encodeBase64(p1));

            assertEquals(dataStr, HexUtils.encodeBase64(p1));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    /*
        -- Sample Private Key Value from Erie --
        <RSAKeyValue>
           <Modulus>uaBhE6x8EP9+dDAhOBmYRmrE1mZumH3e5DcxBp+bSeO+eBIuKxo1HKrPHqD2/rgWmACCPpa7ywGgeQIebTPtrnvZG480WQPQ3sfDkbwttbeUlr5itGhoWm+bGbQKxX3QMeyCZjZ61ViK0vZTID7Ki/w9KJb43GGQam01RHazkJE=</Modulus>
           <Exponent>AQAB</Exponent>
           <P>9rXS5qag/ohcV+wfHLJHFl+awyC6AcfNtDNbLvs2gMCOKNmnb7wf9cVj8poy0B4yPL7QviZtdKd54UKerBQYWQ==</P>
           <Q>wJ28OOGXS61Yet8P9DKYRF8o+qWUpPZLzvSQDHLtfSnMtNvPa1bAdxclE3M117A3PqR1i8EiJ+o9NEKgKMqy+Q==</Q>
           <DP>g5E3pbWbCeKijGjptp4EdxQJLqJXT/fD5aO1rvpdeJA3v+VC+71xtdnMkWZWTtKrq+V+4MkRejKONukWQWJ+EQ==</DP>
           <DQ>NsQiomGyLPhKshT+akaAeOA9vJab3xOQlnV/B6sdz3q1E690GGcALlxMVG1rn7og2xTTvzrYxVSatmNDOpX3sQ==</DQ>
           <InverseQ>FN+fS3xf2reqWCWGkJ2KXa+hGROMGWbHtaVpSdtgr0IPyh7v4leeJjXUpekpqeHxfqhzLEeFkI5jlfAHnIrUYw==</InverseQ>
           <D>iRrSWQuvX4a2ye66uor9FBXMaWejDHL77KhvR6sfscXLazhSbXef/xqSfNb7WEx0M7U8fiorbtg2xOoxuwr94/wLgB0Dj/BHkQflZkuJMvvC22S6H9aZf/alZI+lcQvcoanG6IwlDpv9UZNsixu0A3AUfrfZrVBbp+9+jcpoM4E=</D>
        </RSAKeyValue>
     */
    private static final String[] ERIE_PRIVATE_KEY = {
            // modulus
            "uaBhE6x8EP9+dDAhOBmYRmrE1mZumH3e5DcxBp+bSeO+eBIuKxo1HKrPHqD2/rgWmACCPpa7ywGgeQIebTPtrnvZG480WQPQ3sfDkbwttbeUlr5itGhoWm+bGbQKxX3QMeyCZjZ61ViK0vZTID7Ki/w9KJb43GGQam01RHazkJE=",
            // Exponent
            "AQAB",
            // P
            "9rXS5qag/ohcV+wfHLJHFl+awyC6AcfNtDNbLvs2gMCOKNmnb7wf9cVj8poy0B4yPL7QviZtdKd54UKerBQYWQ==",
            // Q
            "wJ28OOGXS61Yet8P9DKYRF8o+qWUpPZLzvSQDHLtfSnMtNvPa1bAdxclE3M117A3PqR1i8EiJ+o9NEKgKMqy+Q==",
            // DP
            "g5E3pbWbCeKijGjptp4EdxQJLqJXT/fD5aO1rvpdeJA3v+VC+71xtdnMkWZWTtKrq+V+4MkRejKONukWQWJ+EQ==",
            // DQ
            "NsQiomGyLPhKshT+akaAeOA9vJab3xOQlnV/B6sdz3q1E690GGcALlxMVG1rn7og2xTTvzrYxVSatmNDOpX3sQ==",
            // InverseQ
            "FN+fS3xf2reqWCWGkJ2KXa+hGROMGWbHtaVpSdtgr0IPyh7v4leeJjXUpekpqeHxfqhzLEeFkI5jlfAHnIrUYw==",
            // D
            "iRrSWQuvX4a2ye66uor9FBXMaWejDHL77KhvR6sfscXLazhSbXef/xqSfNb7WEx0M7U8fiorbtg2xOoxuwr94/wLgB0Dj/BHkQflZkuJMvvC22S6H9aZf/alZI+lcQvcoanG6IwlDpv9UZNsixu0A3AUfrfZrVBbp+9+jcpoM4E="
    };

    @Test
    public void testCryptoWithErieKeys() throws Exception {

        final byte[] signBytes =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f1&ExpiresOn=1277477798".getBytes();

        try {
            byte[] crypttext = encrypt(signBytes);
            byte[] plaintext = decrypt(crypttext);
            assertTrue(crypttext.length > 0);
            assertTrue(plaintext.length > 0);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    @Test
    public void testDecryptErieToken() throws Exception {

        final String signData =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f1&ExpiresOn=1277477798";

//        final String encodedSig = "f%2bobJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu%2fdlrwNhKtG8DR0Ylcpveq%2fFhqjMudsW2ClwWJQRMBi45ytS9STO7Km4DsVlwgdFFB1DT%2bvSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf9LWv9HVVfruLXQpXL5A%3d";
        final String sigparm =
                "f+obJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu/dlrwNhKtG8DR0Ylcpveq/FhqjMudsW2ClwWJQR\n" +
                "MBi45ytS9STO7Km4DsVlwgdFFB1DT+vSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf\n" +
                "9LWv9HVVfruLXQpXL5A=";

        try {
            byte[] tokenBytes = HexUtils.decodeBase64(sigparm);
            byte[] plaintext = decrypt(tokenBytes);
            assertTrue(plaintext.length > 0);

            byte[] sha256Hash = sha256Digest(signData);
            assertTrue(sha256Hash.length > 0);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    @Test
    public void testVerifySig() throws Exception {

        final String signData =
                "Issuer=Erie&Audience=https%3a%2f%2fwww.erieinsurance.com%2ftesting%2fTest%2fResource%2f1&ExpiresOn=1277477798";

        final String encodedSig = "f%2bobJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu%2fdlrwNhKtG8DR0Ylcpveq%2fFhqjMudsW2ClwWJQRMBi45ytS9STO7Km4DsVlwgdFFB1DT%2bvSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf9LWv9HVVfruLXQpXL5A%3d";
        final String sigparm =
                URLDecoder.decode(encodedSig, "UTF-8");
//                "f+obJAUHvkBwkfICDwp4Mo7j5KEr9XCtN88iu/dlrwNhKtG8DR0Ylcpveq/FhqjMudsW2ClwWJQR\n" +
//                "MBi45ytS9STO7Km4DsVlwgdFFB1DT+vSZwNmfjIEffBOmTG7WbTroPQUh3XWie2HLzHpKK9sIyPf\n" +
//                "9LWv9HVVfruLXQpXL5A=";

        try {
            PublicKey publicKey = getEriePublicKey();
            Signature verify = Signature.getInstance("SHA256withRSA");
            verify.initVerify(publicKey);

            // check plaintext
            verify.update(signData.getBytes());
//            verify.update(sha256Digest(signData));

            // check Sig
            byte[] sigBytes = HexUtils.decodeBase64(sigparm);
            System.out.println("Verifying signature bytes length: ;" + sigBytes.length);
            assertTrue(verify.verify(sigBytes));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected Error occurred with exception: " + ex.getMessage());
        }
    }

    private byte[] encrypt(byte[] tobeEncrypted) throws Exception {

        PrivateKey privateKey = getEriePrivateKey(ERIE_PRIVATE_KEY);

        Cipher enc = Cipher.getInstance("RSA");
        enc.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] crypttxt = enc.doFinal(tobeEncrypted);
        System.out.println("cipher: " + enc.getProvider() + "; alg: " + enc.getAlgorithm());
        System.out.println("crypttext length: " + crypttxt.length + "; b64: " + HexUtils.encodeBase64(crypttxt));

        return crypttxt;
    }

    private byte[] decrypt(byte[] tobeDecrypted) throws Exception {

        PublicKey publicKey = getEriePublicKey();
        Cipher dec = Cipher.getInstance("RSA/ECB/NoPadding");
        dec.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] plaintxt = dec.doFinal(tobeDecrypted);
        System.out.println("cipher: " + dec.getProvider() + "; alg: " + dec.getAlgorithm());
        System.out.println("plaintext length: " + plaintxt.length + "; b64: " + HexUtils.encodeBase64(plaintxt));
        
        return plaintxt;
    }

    private byte[] sha256Digest(String toHash) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] result = md.digest(toHash.getBytes());
        System.out.println("digest: " + md.getProvider() + "; alg: " + md.getAlgorithm());
        System.out.println("digest length: " + result.length + "; b64: " + HexUtils.encodeBase64(result));
        return result;
    }

    private PublicKey getEriePublicKey() throws Exception {

        /*
             <RSAKeyValue>
                <Modulus>uaBhE6x8EP9+dDAhOBmYRmrE1mZumH3e5DcxBp+bSeO+eBIuKxo1HKrPHqD2/rgWmACCPpa7ywGgeQIebTPtrnvZG480WQPQ3sfDkbwttbeUlr5itGhoWm+bGbQKxX3QMeyCZjZ61ViK0vZTID7Ki/w9KJb43GGQam01RHazkJE=</Modulus>
                <Exponent>AQAB</Exponent>
             </RSAKeyValue>
         */
        final String modulus = "uaBhE6x8EP9+dDAhOBmYRmrE1mZumH3e5DcxBp+bSeO+eBIuKxo1HKrPHqD2/rgWmACCPpa7ywGgeQIebTPtrnvZG480WQPQ3sfDkbwttbeUlr5itGhoWm+bGbQKxX3QMeyCZjZ61ViK0vZTID7Ki/w9KJb43GGQam01RHazkJE=";
        final String exponent = "AQAB";

        // create the PublicKey

//        BigInteger modValue = new BigInteger(HexUtils.decodeBase64(modulus));
//        BigInteger expValue = new BigInteger(HexUtils.decodeBase64(exponent));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(base64ToBigInteger(modulus), base64ToBigInteger(exponent));
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(spec);
        assertNotNull(pub);
        return pub;
    }

    private PrivateKey getEriePrivateKey(String[] crtValues) throws Exception {

        RSAPrivateCrtKeySpec privKeySpec = new RSAPrivateCrtKeySpec(
                base64ToBigInteger(crtValues[0]),
                base64ToBigInteger(crtValues[1]),
                base64ToBigInteger(crtValues[2]),
                base64ToBigInteger(crtValues[3]),
                base64ToBigInteger(crtValues[4]),
                base64ToBigInteger(crtValues[5]),
                base64ToBigInteger(crtValues[6]),
                base64ToBigInteger(crtValues[7])
        );

        PrivateKey returnKey = KeyFactory.getInstance("RSA").generatePrivate(privKeySpec);
        assertNotNull(returnKey);
        return returnKey;
    }

    private BigInteger base64ToBigInteger(String encodeString) {

        byte[] theBytes = HexUtils.decodeBase64(encodeString);
//        return new BigInteger(theBytes);
        // alternatively
        return new BigInteger(HexUtils.hexDump(theBytes), 16);
    }

    private void initKeypair() throws IOException, CertificateException {

//        try {
//            Pair<X509Certificate,PrivateKey> g= new TestCertificateGenerator().generateWithKey();
//            this.privateKey = g.right;
//            this.publicCert = g.left;
//            this.publicKey = g.left.getPublicKey();
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }

        if (this.publicKey == null) {
            this.publicCert = CertUtils.decodeFromPEM(aliceCert);
            this.publicKey = publicCert.getPublicKey();
        }

        if (this.privateKey == null) {
            this.privateKey = CertUtils.decodeKeyFromPEM(aliceKey);
        }
    }

    private static final String aliceCert =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAw\n" +
            "MQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENB\n" +
            "MB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FT\n" +
            "SVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVB\n" +
            "bGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT\n" +
            "4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+\n" +
            "mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtp\n" +
            "jmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQs\n" +
            "MCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYD\n" +
            "VR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNV\n" +
            "HSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEA\n" +
            "BTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TT\n" +
            "pHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf\n" +
            "5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1C\n" +
            "Br6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO\n" +
            "35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQOD\n" +
            "ZE9l4ATGy9s9hNVwryOJTw==\n" +
            "-----END CERTIFICATE-----";

    private static final String aliceKey =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIICXAIBAAKBgQCiqL30HLVVijRoeuQoI1PgOQiA8v9KBqFt4p0aGtu7crZcbtqt\n" +
            "8P22ogEgox+tfgan3Q0lrzkBXXKI2wTK+r6ZScyk1wqctcs6OROYwKoNhtuR6EmV\n" +
            "c+dpYvO976ZbSkl0/G1/etsPg9L9wFAi+2mOasMBTxWxkpPwM3krLP4RmwIDAQAB\n" +
            "AoGAY+fazB357rE1YVrh2hlgwh6lr3YRASmzaye+MLOAdNCPW5Sm8iFL5Cn7IU2v\n" +
            "/kKi2eW21oeaLtFzsMU9W2LJP6h33caPcMr/1F3wsiHRCBSZiRLgroYnryJ2pWRq\n" +
            "B8r6/j1mCKzNkoxwspUS1tPFIT0yJB4L/bQGMIvnoM4v5aECQQDX2hBKRbsQYSgL\n" +
            "xZmqx/KJG7+rcpjYXBcztcO09sAsJ+tJe7FPKoKB1CG/KWqj8KQn69blXxhKRDTp\n" +
            "rPZLiU7RAkEAwOnfR+dwLbnNGTuafvvbWE1d0CCa3YGooCrrCq4Af7D5jv9TZXDx\n" +
            "yOIZsHzQH5U47e9ht2JvYllbTlMhirKsqwJBAKbyAadwRz5j5pU0P6XW/78LtzLj\n" +
            "b1Pn5goYi0VrkzaTqWcsQ/b26fmAGJnBbrldZZl6zrqY0jCekE4reFLz4AECQA7Y\n" +
            "MEFFMuGh4YFmj73jvX4u/eANEj2nQ4WHp+x7dTheMuXpCc7NgR13IIjvIci8X9QX\n" +
            "Toqg/Xcw7xC43uTgWN8CQF2p4WulNa6U64sxyK1gBWOr6kwx6PWU29Ay6MPDPAJP\n" +
            "O84lDgb5dlC1SGE+xHUzPPN6E4YFI/ECawOHNrADEsE=\n" +
            "-----END RSA PRIVATE KEY-----";
}