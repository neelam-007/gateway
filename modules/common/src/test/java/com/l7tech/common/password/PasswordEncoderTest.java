package com.l7tech.common.password;

import com.l7tech.test.BugId;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for PasswordEncoder.
 */
public class PasswordEncoderTest {

    @Test
    public void testBase64url() throws Exception {
        assertEquals( "", PasswordEncoder.base64url( b( "" ) ) );
        assertEquals( "YQ", PasswordEncoder.base64url( b( "a" ) ) );
        assertEquals( "YXNkZg", PasswordEncoder.base64url( b( "asdf" ) ) );
        assertEquals( "b2l1aHBvaXVoZWg5MjhoYXNiMzlyYg", PasswordEncoder.base64url( b( "oiuhpoiuheh928hasb39rb" ) ) );
        assertEquals( "YWl1Z2FsZWlyZ2hhZWlydWdoYWVybGl1Z2hhZWxyaXVnYWVyZ2l1aGFlcmlndWFoYWU",
                PasswordEncoder.base64url( b( "aiugaleirghaeirughaerliughaelriugaergiuhaeriguahae" ) ) );
    }

    @Test
    public void testUnbase64url() throws Exception {
        assertTrue( Arrays.equals( b( "" ), PasswordEncoder.unbase64url( "" ) ) );
        assertTrue( Arrays.equals( b( "a" ), PasswordEncoder.unbase64url( "YQ" ) ) );
        assertTrue( Arrays.equals( b( "asdf" ), PasswordEncoder.unbase64url( "YXNkZg" ) ) );
        assertTrue( Arrays.equals( b( "oiuhpoiuheh928hasb39rb" ), PasswordEncoder.unbase64url( "b2l1aHBvaXVoZWg5MjhoYXNiMzlyYg" ) ) );
        assertTrue( Arrays.equals( b( "aiugaleirghaeirughaerliughaelriugaergiuhaeriguahae" ),
                PasswordEncoder.unbase64url( "YWl1Z2FsZWlyZ2hhZWlydWdoYWVybGl1Z2hhZWxyaXVnYWVyZ2l1aGFlcmlndWFoYWU" ) ) );
    }

    @Test
    public void testBase64UrlRoundTrip() throws Exception {
        Random r = new Random( 483L );

        for ( int i = 0; i < 5000; ++i ) {
            int len = r.nextInt( 200 );
            byte[] b = new byte[ len ];
            r.nextBytes( b );
            byte[] out =
                    PasswordEncoder.unbase64url(
                            PasswordEncoder.base64url(
                                    PasswordEncoder.unbase64url(
                                            PasswordEncoder.base64url( b ) ) ) );
            assertEquals( "Length mismatch for " + HexUtils.encodeBase64( b ), b.length, out.length );
            assertTrue( Arrays.equals( b, out ) );
        }
    }

    @Test
    public void testGenerateKey() throws Exception {
        Key k = PasswordEncoder.generateKey( "blah" );
        byte[] expected = HexUtils.unHexDump( "92f64099a7d93bddbbea1b5dac5147e7" );
        assertTrue( Arrays.equals( expected, k.getEncoded() ) );

        k = PasswordEncoder.generateKey( "other" );
        expected = HexUtils.unHexDump( "8b844a55d1f4bb3b9506ab0415ccd8fb" );
        assertTrue( Arrays.equals( expected, k.getEncoded() ) );
    }

    @Test
    public void testAes256cbc_encrypt() throws Exception {
        byte[] iv = HexUtils.unHexDump( "4a7df577978649cc6c3745475f376938" );
        SecretKeySpec key = new SecretKeySpec( HexUtils.unHexDump( "92f64099a7d93bddbbea1b5dac5147e74a7df577978649cc6c3745475f376938" ), "AES" );
        byte[] got = PasswordEncoder.aes256cbc_encrypt( key, iv, b( "sekrit" ) );
        assertEquals( 16, got.length );
    }

    @Test
    public void testAes256cbc_decrypt() throws Exception {
        byte[] iv = HexUtils.unHexDump( "4a7df577978649cc6c3745475f376938" );
        SecretKeySpec key = new SecretKeySpec( HexUtils.unHexDump( "92f64099a7d93bddbbea1b5dac5147e74a7df577978649cc6c3745475f376938" ), "AES" );
        byte[] cipherText = HexUtils.unHexDump( "0ca73306af557aabe5c7b9d71c9ed22e" );
        byte[] got = PasswordEncoder.aes256cbc_decrypt( key, iv, cipherText );
        assertTrue( Arrays.equals( b( "sekrit" ), got ) );
    }

    @Test
    public void testAesRoundTrip() throws Exception {

        Random r = new Random( 48411L );

        for ( int i = 0; i < 1000; ++i ) {
            int len = r.nextInt( 200 );
            byte[] plaintext = new byte[ len ];
            r.nextBytes( plaintext );

            byte[] iv = new byte[16];
            r.nextBytes( iv );

            byte[] keyBytes = new byte[32];
            r.nextBytes( keyBytes );
            SecretKeySpec key = new SecretKeySpec( keyBytes, "AES" );

            byte[] cipher1 = PasswordEncoder.aes256cbc_encrypt( key, iv, plaintext );
            byte[] plain2 = PasswordEncoder.aes256cbc_decrypt( key, iv, cipher1 );
            byte[] cipher2 = PasswordEncoder.aes256cbc_encrypt( key, iv, plain2 );
            byte[] plain3 = PasswordEncoder.aes256cbc_decrypt( key, iv, cipher2 );

            assertEquals( plaintext.length, plain3.length );
            assertTrue( Arrays.equals( plaintext, plain3 ) );
        }
    }

    @Test
    public void testEncodePassword() throws Exception {
        String encoded = PasswordEncoder.encodePassword( b( "" ) );
        assertTrue( encoded.length() > 30 );
        assertTrue( encoded.indexOf( '.' ) > 1 );

        encoded = PasswordEncoder.encodePassword( b( "sekrit" ) );
        assertTrue( encoded.length() > 30 );
        assertTrue( encoded.indexOf( '.' ) > 1 );
    }

    @Test
    public void testDecodePassword() throws Exception {
        byte[] decoded = PasswordEncoder.decodePassword( "FEH78QMaVA0.0m8NV0uBiF_QgdgOypZ_UA" );
        assertEquals( "", new String( decoded, "UTF-8" ) );

        decoded = PasswordEncoder.decodePassword( "Q2jRqo3Gl5g.uC5Omhl4s0Nhg1SsSfaWfg" );
        assertEquals( "sekrit", new String( decoded, "UTF-8" ) );
    }

    @Test
    public void testDecodePassword_empty() throws Exception {
        try {
            PasswordEncoder.decodePassword( "" );
            fail( "expected exception not thrown" );
        } catch ( IOException e ) {
            assertEquals( "Encoded password did not contain two dot-delimited components", e.getMessage() );
        }
    }

    @Test
    public void testDecodePassword_noDot() throws Exception {
        try {
            PasswordEncoder.decodePassword( "cUlSZjf7BigjwKfburLztN-JNVmhg7zGw" );
            fail( "expected exception not thrown" );
        } catch ( IOException e ) {
            assertEquals( "Encoded password did not contain two dot-delimited components", e.getMessage() );
        }
    }

    @Test
    public void testDecodePassword_extraDot() throws Exception {
        try {
            PasswordEncoder.decodePassword( "cUlSZjf7Big.jwKfburLztN-J.NVmhg7zGw" );
            fail( "expected exception not thrown" );
        } catch ( IOException e ) {
            assertEquals( "Encoded password did not contain two dot-delimited components", e.getMessage() );
        }
    }

    @Test
    public void testDecodePassword_noSalt() throws Exception {
        try {
            PasswordEncoder.decodePassword( ".cUlSZjf7BigjwKfburLztN-JNVmhg7zGw" );
            fail( "expected exception not thrown" );
        } catch ( IOException e ) {
            assertEquals( "Encoded password contained an empty salt", e.getMessage() );
        }
    }

    @Test
    public void testDecodePassword_tooShort() throws Exception {
        try {
            PasswordEncoder.decodePassword( "cUlSZjf7Big.jwKfburLztN-JNVmhg7z" );
            fail( "expected exception not thrown" );
        } catch ( IOException e ) {
            assertEquals( "Encoded password is too short", e.getMessage() );
        }
    }

    @Test
    public void testEncodePasswordRoundTrip() throws Exception {

        Random r = new Random( 48411L );

        for ( int i = 0; i < 1000; ++i ) {
            int len = r.nextInt( 200 );
            byte[] passwordBytes = new byte[ len ];
            r.nextBytes( passwordBytes );

            String encoded1 = PasswordEncoder.encodePassword( passwordBytes );
            byte[] plain2 = PasswordEncoder.decodePassword( encoded1 );
            String encoded2 = PasswordEncoder.encodePassword( plain2 );
            byte[] plain3 = PasswordEncoder.decodePassword( encoded2 );

            assertEquals( len, plain3.length );
            assertTrue( Arrays.equals( passwordBytes, plain3 ) );
        }
    }

    @Test
    @BugId( "SSG-10060" )
    public void testKeySizeLimitedTo128Bits() throws Exception {
        Key key = PasswordEncoder.generateKey( "blah" );
        assertEquals( "Key size must be limited to 128 bits in order to work with a JDK lacking unlimited strength crypto policy files",
                16, key.getEncoded().length );
    }

    private byte[] b( String s ) {
        try {
            return s.getBytes( "UTF-8" );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

}
