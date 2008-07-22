package com.l7tech.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for encryption/decryption routines.
 *
 * @author $Author$
 * @version $Revision$
 */
public class EncryptionUtil {

    //- PUBLIC

    /**
     * Encrypt the given data.
     *
     * @param data the text to encrypt
     * @param keyMod the text used to obscure the hard-coded key
     * @return the encoded text (base64)
     * @throws IllegalArgumentException on error
     */
    public static String encrypt(String data, String keyMod) {
        if(keyMod==null) throw new IllegalArgumentException("keyMod must not be null");
        if(keyMod.length()==0) throw new IllegalArgumentException("keyMod must not be empty");
        return encrypt(data, generatedModifedKey(DEFAULT_KEY, keyMod));
    }

    /**
     * Decrypt the given data.
     *
     * @param data the (base64) text to decrypt
     * @param keyMod the text used to obscure the hard-coded key
     * @return the decrypted text
     * @throws IllegalArgumentException on error
     */
    public static String decrypt(String data, String keyMod) {
        if(keyMod==null) throw new IllegalArgumentException("keyMod must not be null");
        if(keyMod.length()==0) throw new IllegalArgumentException("keyMod must not be empty");
        return decrypt(data, generatedModifedKey(DEFAULT_KEY, keyMod));
    }

    /**
     * Encrypt the given data.
     *
     * @param data the text to encrypt
     * @param key the key data (16 bytes, 128 bits)
     * @return the encrypted text (base64)
     * @throws IllegalArgumentException on error
     */

    public static String encrypt(String data, byte[] key) {
        if(data==null) throw new IllegalArgumentException("data must not be null");
        try {
            byte[] dataBytes = data.getBytes(DEFAULT_ENCODING);
            byte[] encrypted = encrypt(dataBytes, key);
            return HexUtils.encodeBase64(encrypted);
        }
        catch(UnsupportedEncodingException uue) {
            throw new RuntimeException("Platform must support default encoding.", uue);
        }
    }

    /**
     * Decrypt the given data.
     *
     * @param data the base64 text to decrypt
     * @param key the key data (16 bytes, 128 bits)
     * @return the decrypted string
     * @throws IllegalArgumentException on error
     */
    public static String decrypt(String data, byte[] key) {
        if(data==null) throw new IllegalArgumentException("data must not be null");
        try {
            byte[] encrypted = HexUtils.decodeBase64(data);
            byte[] dataBytes = decrypt(encrypted, key);
            //return new String(dataBytes, DEFAULT_ENCODING); // does not handle invalid bytes
            Charset utf8Charset = Charset.forName(DEFAULT_ENCODING);
            CharsetDecoder decoder = utf8Charset.newDecoder();
            return decoder.decode(ByteBuffer.wrap(dataBytes)).toString();
        }
        catch(UnsupportedEncodingException uue) {
            throw new RuntimeException("Platform must support default encoding.", uue);
        }
        catch(CharacterCodingException cce) {
            throw new IllegalArgumentException("Cannot decode data.", cce);
        }
        catch(IOException ioe) {
            throw new IllegalArgumentException("Cannot decode data.", ioe);
        }
    }

    /**
     * Encrypt the given data.
     *
     * @param data the data to encrypt
     * @param key the key data (16 bytes, 128 bits)
     * @return the encrypted data
     * @throws IllegalArgumentException on error
     */
    public static byte[] encrypt(byte[] data, byte[] key) {
        return crypter(data, key, DEFAULT_ALGORITHM, Cipher.ENCRYPT_MODE);
    }

    /**
     * Decrypt the given encrypted data.
     *
     * @param data the data to decrypt
     * @param key the key data (16 bytes, 128 bits)
     * @return the decrypted data
     * @throws IllegalArgumentException on error
     */
    public static byte[] decrypt(byte[] data, byte[] key) {
        return crypter(data, key, DEFAULT_ALGORITHM, Cipher.DECRYPT_MODE);
    }

    private static final String RSACIPHER = "RSA/ECB/PKCS1Padding";

    /**
     * RSA encrypts and base 64 data
     * @param toEncrypt the data to encrypt
     * @param key the key to encrypt with
     * @return a base 64 encoded version of the encrypted data
     * @throws NoSuchAlgorithmException on error
     * @throws NoSuchPaddingException on error
     * @throws InvalidKeyException on error
     * @throws BadPaddingException on error
     * @throws IllegalBlockSizeException on error
     */
    public static String rsaEncAndB64(byte[] toEncrypt, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                                  InvalidKeyException, BadPaddingException,
                                                                  IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSACIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] tmp = cipher.doFinal(toEncrypt);
        return HexUtils.encodeBase64(tmp);
    }

    /**
     * Base 64 decode incoming data and decrypt using RSA
     * @param b64edEncedData incoming data to process
     * @param key the key used for decryption
     * @return the decrypted key
     * @throws IOException on error
     * @throws NoSuchAlgorithmException on error
     * @throws NoSuchPaddingException on error
     * @throws InvalidKeyException on error
     * @throws BadPaddingException on error
     * @throws IllegalBlockSizeException on error
     */
    public static byte[] deB64AndRsaDecrypt(String b64edEncedData, Key key) throws IOException, NoSuchAlgorithmException,
                                                                      NoSuchPaddingException, InvalidKeyException,
                                                                      BadPaddingException, IllegalBlockSizeException {
        byte[] tmp = HexUtils.decodeBase64(b64edEncedData);
        Cipher cipher = Cipher.getInstance(RSACIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(tmp);
    }

    /**
     * @param key the key for which to generate an id
     * @return a string of length 28 to be used to uniquely identify this public key
     */
    public static String computeCustomRSAPubKeyID(RSAPublicKey key) {
        String keyIDNotHashed = key.getPublicExponent().toString() + key.getModulus().toString();
        try {
            return HexUtils.encodeBase64(HexUtils.getSha1Digest(keyIDNotHashed.getBytes(DEFAULT_ENCODING)));
        } catch (UnsupportedEncodingException e) {
            // wont happen
            throw new RuntimeException(e);
        }
    }

    //- PRIVATE

    // Use AES encryption
    private static final String DEFAULT_ALGORITHM = "AES";

    // Encode strings using UTF-8
    private static final String DEFAULT_ENCODING = "UTF-8";

    // Random default key
    private static final byte[] DEFAULT_KEY = new byte[]{(byte)0xa2, (byte)0xb5, (byte)0xfb, (byte)0x9c
                                                        ,(byte)0x11, (byte)0x91, (byte)0x3a, (byte)0x82
                                                        ,(byte)0xea, (byte)0x5e, (byte)0xd3, (byte)0xba
                                                        ,(byte)0xc4, (byte)0x4d, (byte)0xfb, (byte)0xae};

    /**
     * Do the work.
     */
    private static byte[] crypter(byte[] data, byte[] key, String algorithm, int mode) {
        if(data==null) throw new IllegalArgumentException("data must not be null");
        if(key==null) throw new IllegalArgumentException("key must not be null");
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(mode, new SecretKeySpec(key, algorithm));
            return cipher.doFinal(data);
        }
        catch(NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Platform must support default algorithm.", nsae);
        }
        catch(NoSuchPaddingException nspe) {
            throw new RuntimeException("Platform must support default algorithm.", nspe);
        }
        catch(InvalidKeyException ike) {
            throw new IllegalArgumentException("Invalid key for encryption/decryption.", ike);
        }
        catch(IllegalBlockSizeException ibse) {
            throw new IllegalArgumentException("Encryption error", ibse); // should not happen
        }
        catch(BadPaddingException bpe) {
            throw new IllegalArgumentException("Decryption error (wrong key?)", bpe);
        }
    }

    /**
     * Obscure the given key
     */
    private static byte[] generatedModifedKey(byte[] key, String keyMod) {
        byte[] modded = new byte[key.length];
        System.arraycopy(key, 0, modded, 0, key.length);

        int length = keyMod.length();
        byte[] rotxor = intToByteArray(null,keyMod.hashCode());

        for(int m=0; m<modded.length; m++) {
            modded[m] ^= rotxor[m%4];
        }

        for(int m=0; m<modded.length; m++) {
            char character = keyMod.charAt(m%length);
            modded[m] ^= (byte)(character>>8);
            modded[m] ^= (byte)(character);
        }

        return modded;
    }

    /**
     * Convert an int to a byte[]
     */
    private static byte[] intToByteArray(byte[] bytes, int value )
    {
        if(bytes ==null) bytes = new byte[4];

        bytes[0] = (byte)(value & 0xFF);
        value >>= 8;
        bytes[1] = (byte)(value & 0xFF);
        value >>= 8;
        bytes[2] = (byte)(value & 0xFF);
        value >>= 8;
        bytes[3] = (byte)(value & 0xFF);

        return bytes;
    }

    /**
     * Convert a byte[] to an int
     */
    private static int byteArrayToInt(byte[] bytes)
    {
        int value = ( bytes[ 0 ] & 0xFF);
        value    |= ((bytes[ 1 ] & 0xFF) << 8);
        value    |= ((bytes[ 2 ] & 0xFF) << 16);
        value    |= ((bytes[ 3 ] & 0xFF) << 24);

        return value;
    }
}
