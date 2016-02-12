package com.l7tech.skunkworks;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.text.ParseException;

/**
 * Attempts to decrypt a value enrypted on iOS using the RNCryptor libarary, in order to validate that the key derivation,
 * MAC, and encryption is used in the expected way.
 */
public class TestRnCryptorDataFormat {

    public static void main( String[] args ) throws Exception {
        // Try to parse data out of ciphertext struct
        // Convert to arguments if you like
        String ciphertextB64 =
                "AwFz1Zr9CTtNznbOTnpXD85StCHOCvDHAnD5ERr4IePu9cyOCdmYbmovtJm5nOdGXotUmKZqMzs0AGS/gG7zwHuI0Q+Lhh5Asy8j0WcjndupL2z31weApDamAn8ETYxqEy0AOuTFahh//ltffJrimez7oe7SlvgQk2EBdulq2LFJxQJAEQM4GFumPX0QJ8Dz8jHrLgmHRClfmghtb+YgTEwiBmlfciKJ3EPGCGZE52R1IsEPAOXywGPp8/edm4lAAHCMI25/WEMyaWiOxWG7Vn1+Re33lLwyrVWLyIZ0bhF1OkqdRg8jGX4saHhSVS3dRwpLqtym+vYfebfOzRYd1cYT\n";
        String password = "BigMickPwd!";
        int pbkdf2IterationCount = 10000;

        byte[] cb = decodeBase64( ciphertextB64 );

        byte version = cb[0];
        byte options = cb[1];
        byte[] encSalt = new byte[8];
        System.arraycopy( cb, 2, encSalt, 0, 8 );
        byte[] macSalt = new byte[8];
        System.arraycopy( cb, 10, macSalt, 0, 8 );
        byte[] iv = new byte[16];
        System.arraycopy( cb, 18, iv, 0, 16 );

        int cipherlen = cb.length - 34 - 32;
        byte[] ciphertext = new byte[cipherlen];
        System.arraycopy( cb, 34, ciphertext, 0, cipherlen );

        byte[] mac = new byte[32];
        System.arraycopy( cb, 34 + cipherlen, mac, 0, 32 );

        System.out.println( "version=" + version );
        System.out.println( "options=" + hexDump( new byte[] { options } ) );
        System.out.println( "encSalt=" + hexDump( encSalt ) );
        System.out.println( "macSalt=" + hexDump( macSalt ) );
        System.out.println( "iv=" + hexDump( iv ) );
        System.out.println( "hmac=" + hexDump( mac ) );
        System.out.println( "ciphertext=" + hexDump( ciphertext ) );

        if ( 3 != version )
            throw new ParseException( "Unsupported version: " + version, 0 );
        if ( 1 != options )
            throw new ParseException( "Unexpected options: " + options, 1 );


        // Try to derive keys (encryption and hmac) from password and salts
        byte[] encryptionKey = pbkdf2( encSalt, pbkdf2IterationCount, password.toCharArray() );
        System.out.println( "encryptionKey=" + hexDump( encryptionKey ) );
        byte[] hmacKey = pbkdf2( macSalt, pbkdf2IterationCount, password.toCharArray() );
        System.out.println( "hmacKey=" + hexDump( hmacKey ) );


        // Check MAC of entire message (header plus ciphertext, but excluding mac)
        Mac hm = Mac.getInstance( "HmacSHA256" );
        hm.init( new SecretKeySpec( hmacKey, "RAW" ) );
        hm.update( cb, 0, 34 + cipherlen );
        byte[] gotMac = hm.doFinal();
        if ( !compareArraysConstantTime( mac, gotMac ) )
            throw new ParseException( "MAC mismatch", 0 );


        // Decrypt ciphertext
        Cipher aes = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
        aes.init( Cipher.DECRYPT_MODE, new SecretKeySpec( encryptionKey, "AES" ), new IvParameterSpec( iv ) );
        byte[] plaintext = aes.doFinal( ciphertext );

        System.out.println( "plaintext=" + hexDump( plaintext ) );
        System.out.println( "plaintext(String)=" + new String( plaintext, StandardCharsets.UTF_8 ) );
    }

    private static String hexDump( byte[] bytes ) {
        return DatatypeConverter.printHexBinary( bytes );
    }


    private static byte[] decodeBase64( String b64 ) throws IOException {
        return new BASE64Decoder().decodeBuffer( b64 );
    }


    static byte[] pbkdf2( byte[] salt, int iterationCount, char[] password ) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA1" );
        KeySpec spec = new PBEKeySpec( password, salt, iterationCount, 256 );
        return skf.generateSecret( spec ).getEncoded();
    }

    
    public static boolean compareArraysConstantTime( final byte[] a, final byte[] b ) {
        if ( a.length != b.length )
            return false;

        int c = 0;
        for ( int i = 0; i < a.length; i++ ) {
            c |= a[i] ^ b[i];
        }

        return 0 == c;
    }
}
