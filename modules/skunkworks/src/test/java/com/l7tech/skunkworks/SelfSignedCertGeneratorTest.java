package com.l7tech.skunkworks;

import org.junit.Test;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.*;

/**
 * Tests for SelfSignedCertGenerator.
 */
public class SelfSignedCertGeneratorTest {

    @Test
    public void testDnSanitizer() throws Exception {
        assertEquals( "Should allow Unicode alphanum, plus ASCII dash dot and underscore",
                "__Blah_123-1.1__", SelfSignedCertGenerator.sanitizeCn( "\"!Blah_123-1.1$\"" ) );

        String hirohito = ub( "5pit5ZKM5aSp55qH" );
        assertEquals( "Japanese letter characters should be permitted",
                hirohito, SelfSignedCertGenerator.sanitizeCn( hirohito ) );

        String ocean = ub( "5aSq5bmz5rSL" );
        assertEquals( "Chinese letter characters should be permitted",
                ocean, SelfSignedCertGenerator.sanitizeCn( ocean ) );
    }

    static String ub( String utf8Base64 ) throws Exception {
        return new String( new BASE64Decoder().decodeBuffer( utf8Base64 ), "UTF-8" );
    }

    @Test
    public void testGenerateCert() throws Exception {
        SelfSignedCertGenerator gen = new SelfSignedCertGenerator( "joe" );

        // -----
        // Check generated cert
        // -----

        X509Certificate cert = gen.getCertificate();
        assertNotNull( cert );
        assertEquals( "CN=joe", cert.getSubjectDN().getName() );
        assertEquals( "RSA", cert.getPublicKey().getAlgorithm() );

        // Note: the following assertion should be pretty safe but might not be true when using an HSM
        assertTrue( cert.getPublicKey() instanceof RSAPublicKey );
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
        assertTrue( "RSA key size must be strong",
                publicKey.getModulus().toString( 16 ).length() + 1 >= ( 2048 / 8 ) );


        // -----
        // Check generated private key
        // -----

        // Note: the following assertion won't generally be true when using an HSM
        assertTrue( gen.getPrivateKey() instanceof RSAPrivateKey );
        RSAPrivateKey privateKey = (RSAPrivateKey)gen.getPrivateKey();
        assertNotNull( privateKey );
        assertEquals( "public and private keys must be related",
                publicKey.getModulus(), privateKey.getModulus() );

        // -----
        // Test export as PKCS#12 key store
        // -----

        byte[] keystoreBytes = gen.toKeyStoreFileBytes( "sekrit".toCharArray(), "user" );
        KeyStore ks = KeyStore.getInstance( "PKCS12" );
        ks.load( new ByteArrayInputStream( keystoreBytes ), "sekrit".toCharArray() );
        assertEquals( "user", ks.aliases().nextElement() );

        // Check key
        Key gotKey = ks.getKey( "user", "sekrit".toCharArray() );
        assertNotNull( gotKey );
        assertTrue( gotKey instanceof RSAPrivateKey );
        RSAPrivateKey gotRsaPrivateKey = (RSAPrivateKey) gotKey;
        assertEquals( privateKey.getPrivateExponent(), gotRsaPrivateKey.getPrivateExponent() );
        assertEquals( privateKey.getModulus(), gotRsaPrivateKey.getModulus() );

        // Check cert chain
        Certificate[] gotCertChain = ks.getCertificateChain( "user" );
        assertNotNull( gotCertChain );
        assertEquals( 1, gotCertChain.length );
        assertTrue( gotCertChain[0] instanceof X509Certificate );
        X509Certificate gotCert = (X509Certificate) gotCertChain[0];
        assertArrayEquals( cert.getEncoded(), gotCert.getEncoded() );
    }
}
