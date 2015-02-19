package com.l7tech.skunkworks;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;

/**
 * Stand-alone utility to obfuscate and un-obfuscate passwords. SKUNKWORKS -- NOT USED BY THE GATEWAY.
 */
public class PasswordEncoder {
    static final Charset ISO8859 = Charset.forName("ISO-8859-1");
    static final SecureRandom random = new SecureRandom();

    /**
     * Obfuscate a password.
     * <p/>
     * The password will be encrypted with AES 256 in CBC mode, using a key
     * derived from a randomly-chosen salt hashed with an internal constant string.  The IV
     * is derived from the salt (without the internal constant string).
     * <p/>
     * The output is in the format SALT + '.' + CIPHERTEXT where SALT is the base64url encoding
     * of the salt and CIPHERTEXT is the base64url encoding of the ciphertext.
     * <p/>
     * Base64url encoding is a variant of Base-64 where - is used in place of + and _ is used in place
     * of / and trailing = padding is omitted.  It lacks shell metacharacters or characters that will
     * cause problems when used on a command line.
     *
     * @param plaintextPassword plaintext password, already converted to bytes using some encoding
     * @return the encoded password, eg. "hSpA0doc9o8.jaT7Vf4TmFN4Uaic3QWTamwAXSjpfD0CtoEGTfjQr4U"
     */
    public static String encodePassword( byte[] plaintextPassword ) {
        try {
            byte[] saltBytes = new byte[8];
            random.nextBytes( saltBytes );
            String salt = base64url( saltBytes );
            Key key = generateKey( salt );
            byte[] iv = generateIv( salt );
            byte[] cipherText = aes256cbc_encrypt( key, iv, plaintextPassword );
            return salt + "." + base64url( cipherText );

        } catch ( GeneralSecurityException|IOException e ) {
            throw new RuntimeException( "Unable to encode password: " + e.getMessage(), e );
        }
    }

    /**
     * Decode a password string previously obfuscated using {@link #encodePassword(byte[])}.
     *
     * @param encodedPasswordWithSalt encoded password, eg. "hSpA0doc9o8.jaT7Vf4TmFN4Uaic3QWTamwAXSjpfD0CtoEGTfjQr4U"
     * @return plaintext password bytes
     * @throws IOException if the encoded password format is not valid
     */
    public static byte[] decodePassword( String encodedPasswordWithSalt ) throws IOException {
        try {
            String[] parts = encodedPasswordWithSalt.split( "\\." );
            if ( parts.length !=  2 )
                throw new IOException( "Encoded password did not contain two dot-delimited components" );
            String salt = parts[0];
            if ( salt.length() < 1 )
                throw new IOException( "Encoded password contained an empty salt" ) ;
            byte[] iv = generateIv( salt );
            String cipherTextB64 = parts[1];
            byte[] cipherText = unbase64url( cipherTextB64 );
            if ( cipherText.length < 16 )
                throw new IOException( "Encoded password is too short" );
            Key key = generateKey( salt );
            return aes256cbc_decrypt( key, iv, cipherText );

        } catch ( GeneralSecurityException e ) {
            throw new IOException( "Unable to decode password: " + e.getMessage(), e );
        }
    }

    static Key generateKey( String salt ) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // This is good enough for an obfuscator, but this is not a proper KDF and should not be used
        // to generate keys that are to be used for encrypting anything for real.
        MessageDigest md = MessageDigest.getInstance( "SHA-512" );
        md.update( salt.getBytes( ISO8859 ) );
        md.update( "q8sN-j>s<lktAK=z2DcF9".getBytes( ISO8859 ) );  // This our built-in hardcoded secret
        byte[] bytes = md.digest();
        return new SecretKeySpec( bytes, 0, 16, "AES" );
    }

    private static byte[] generateIv( String salt ) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // Since we aren't doing actual crypto here we can get away with just
        // generating an IV from the salt.  In real life we'd want a random IV.
        MessageDigest md = MessageDigest.getInstance( "SHA-512" );
        md.update( salt.getBytes( ISO8859 ) );
        byte[] bytes = md.digest();
        byte[] iv = new byte[16];
        System.arraycopy( bytes, 0, iv, 0, 16 );
        return iv;
    }

    static String base64url( byte[] bytes ) {
        return new BASE64Encoder().encode( bytes ).
                replace( '+', '-' ).
                replace( '/', '_' ).replaceAll( "\\s|=", "" );
    }

    static byte[] unbase64url( String value ) throws IOException {
        int npad = value.length() % 4;
        final String pad;
        switch ( npad ) {
            case 0:
                pad = "";
                break;
            case 1:
                // Not actually possible/valid
                pad = "===";
                break;
            case 2:
                pad = "==";
                break;
            default:
                pad = "=";
                break;
        }

        return new BASE64Decoder().decodeBuffer( value.replace( '-', '+' ).replace( '_', '/' ) + pad );
    }

    static Cipher newAesCbc() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance( "AES/CBC/PKCS5Padding" );
    }

    static byte[] aes256cbc_encrypt( Key key, byte[] iv, byte[] plaintext ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher aes = newAesCbc();

        IvParameterSpec spec = new IvParameterSpec(iv );
        aes.init( Cipher.ENCRYPT_MODE, key, spec, random );

        return aes.doFinal( plaintext );
    }

    static byte[] aes256cbc_decrypt( Key key, byte[] iv, byte[] cipherText ) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher aes = newAesCbc();

        IvParameterSpec spec = new IvParameterSpec(iv );
        aes.init( Cipher.DECRYPT_MODE, key, spec );

        return aes.doFinal( cipherText );
    }

}
