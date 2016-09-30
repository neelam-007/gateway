package com.l7tech.skunkworks.crypto;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

/**
 * Standalone unit test, suitable for giving to CCJ support, demonstrating problem with compatibility with Sun's PBE impl.
 */
public class CcjReproPbeSha1DESedeCompat {
    public static void main(String[] args) throws Exception {
//        Security.insertProviderAt(new CryptoComplyFipsProvider(), 1);
//        Security.insertProviderAt(new JsafeJCE(), 1);

        // We can use either skf or unHexDum.
//        SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBEWithSHA1AndDESede" );
//        SecretKey sharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec( "blahblah".toCharArray() ));

        final byte[] sharedKeyDecryptionKeyBytes = unHexDump( "626c6168626c6168" );
//        SecretKey sharedKeyDecryptionKey = new SecretKeySpec( pkcs12Convert(sharedKeyDecryptionKeyBytes), "PBEWithSHA1AndDESede" );
        SecretKey sharedKeyDecryptionKey = new SecretKeySpec( sharedKeyDecryptionKeyBytes, "PBEWithSHA1AndDESede" );

        final byte[] saltbytes = unHexDump( "6984792b00654f12eeb15ef771c8aa96d7c565fd" );
        final int iterationCount = 1024;

        final String cipherbytesB64 = "JJnRqAH+oftEe+XwJ9Q/7pE8MFtDIThRFI3CXEIQW+7mEcxJNXMpVHJKswjBPKdEiErE6fzjOwyGjzI+YeqgnRm6ZDMtVvj2";
        final byte[] cipherbytes = new BASE64Decoder().decodeBuffer( cipherbytesB64 );

        Cipher cipher = Cipher.getInstance( "PBEWithSHA1AndDESede" );
        cipher.init(Cipher.DECRYPT_MODE, sharedKeyDecryptionKey, new PBEParameterSpec(saltbytes, iterationCount));
        System.out.println( "Provider: " + cipher.getProvider() );

        byte[] plaintextBytes = cipher.doFinal(cipherbytes);     // This line fails with CCJ, but works with Sun

        System.out.println( "Expected output: " + new BASE64Encoder().encode( plaintextBytes ) );

        /*
        Output when using SunJCE provider:
        Provider: SunJCE version 1.8
        Expected output: KsERfEBmzJHwwNcHAhpcN85/OxYUSLjd25jS8/cpJAfu9A0RcBDJTL0HknW9MpKTibWuF7gQPwRT
7JdXb5GMfA==
         */

        /*
        Output when using CCJ:
           Provider: CCJ version 0.9
Exception in thread "main" javax.crypto.BadPaddingException: Error finalising cipher data: pad block corrupted
	at com.safelogic.cryptocomply.jcajce.provider.BaseCipher.engineDoFinal(BaseCipher.java:951)
	at javax.crypto.Cipher.doFinal(Cipher.java:2165)
	at com.l7tech.skunkworks.crypto.CcjReproPbeSha1DESedeCompat.main(CcjReproPbeSha1DESedeCompat.java:35)
         */
    }

    /**
     * This is the helper method to solve CCJ issue with PBEWithSHA1AndDESede.
     */
    private static byte[] pkcs12Convert(byte[] in) {
        byte[] out = new byte[in.length * 2 + 2];

        for (int i = 0; i != in.length; i++) {
            out[ 1 + (i * 2)] = (byte)(in[i] & 0x7f);
        }

        return out;
    }

    private static byte[] unHexDump(String hexData) throws IOException {
        if ( hexData.length() % 2 != 0 ) throw new IOException( "String must be of even length" );
        byte[] bytes = new byte[hexData.length()/2];
        for ( int i = 0; i < hexData.length(); i+=2 ) {
            int b1 = nybble( hexData.charAt(i) );
            int b2 = nybble( hexData.charAt(i+1) );
            bytes[i/2] = (byte)((b1 << 4) + b2);
        }
        return bytes;
    }

    private static byte nybble( char hex ) throws IOException {
        if ( hex <= '9' && hex >= '0' ) {
            return (byte)(hex - '0');
        } else if ( hex >= 'a' && hex <= 'f' ) {
            return (byte)(hex - 'a' + 10 );
        } else if ( hex >= 'A' && hex <= 'F' ) {
            return (byte)(hex - 'A' + 10 );
        } else {
            throw new IOException( "Invalid hex digit " + hex );
        }
    }
}
