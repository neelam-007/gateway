package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A cleaner password encryption scheme that includes an HMAC.
 */
public class L7C2SecretEncryptor implements SecretEncryptor {
    private static final Logger logger = Logger.getLogger( L7C2SecretEncryptor.class.getName() );
    private static final String ENCRYPTION_TAG = "L7C2";
    private static final String ENCRYPTION_PREFIX = "$" + ENCRYPTION_TAG + "$";
    private static final Pattern PARSE_REGEX = Pattern.compile( "^\\$L7C2\\$([0-9a-fA-F]+),([^$]+)\\$([^$]+)$" );

    // Crypto free parameters
    private static final String PROP_PBKDF_ITERATION_COUNT = "com.l7tech.util.L7C2SecretEncryptor.pbkdf2.iterationCount";
    static final int DEFAULT_PBKDF_ITERATION_COUNT = 0x1bead; // 114349, maybe even a bit low for 2014
    private static final String KDF_ALG = "PBKDF2WithHmacSHA1";
    private static final String MAC_ALG = "HmacSHA256";
    private static final String CIPHER_KEY_ALG = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int CIPHER_KEY_SIZE_BITS = 256; // hardcoding (instead of using Cipher.getMaxAllowedKeyLength()) to stay compatible in case someone someday defines a spec for AES with a bigger key

    // Crypto derived parameters
    private static final int CIPHER_KEY_SIZE_BYTES = CIPHER_KEY_SIZE_BITS / 8;
    private static final int CIPHER_BLOCK_LEN = findCipherBlockLen( CIPHER_TRANSFORMATION );
    private static final int MAC_LEN = findMacLen( MAC_ALG );

    @Override
    public boolean looksLikeEncryptedSecret( String possiblyEncryptedSecret ) {
        return possiblyEncryptedSecret != null && possiblyEncryptedSecret.startsWith( ENCRYPTION_PREFIX );
    }

    @NotNull
    @Override
    public String getName() {
        return ENCRYPTION_TAG;
    }

    @NotNull
    @Override
    public String getPrefix() {
        return ENCRYPTION_PREFIX;
    }

    @NotNull
    @Override
    public String encryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, boolean bypassKdf, @NotNull byte[] secretBytes ) {
        byte[] key = keyFinder.findMasterPasswordBytes();
        if ( key == null || key.length < 1 )
            throw new RuntimeException( "Unable to obtain key for encryption" );

        SecretKey secretKey = null;
        try {
            // Generate new salt and iteration count
            int iterationCount = bypassKdf ? 1 : ConfigFactory.getIntProperty( PROP_PBKDF_ITERATION_COUNT, DEFAULT_PBKDF_ITERATION_COUNT );
            byte[] salt = new byte[32];
            RandomUtil.nextBytes( salt );

            // Generate symmetric key from raw input key material
            secretKey = deriveSecretKey( key, iterationCount, salt );

            // Encrypt first, then MAC
            Cipher aes = Cipher.getInstance( CIPHER_TRANSFORMATION );
            byte[] iv = new byte[ CIPHER_BLOCK_LEN ];
            RandomUtil.nextBytes( iv );
            aes.init( Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec( iv ) );

            byte[] ciphertext = aes.doFinal( secretBytes );

            // Compute MAC after encryption so recipient must check it before attempting to decrypt
            byte[] mac = computeMac( secretKey, iv, ciphertext );

            // Concatenate IV + ciphertext + MAC
            byte[] encryptedBytes = ArrayUtils.concatMulti( iv, ciphertext, mac );

            // Encode everything into dollar-delimited printable output string
            return ENCRYPTION_PREFIX + Integer.toHexString( iterationCount ) + "," + HexUtils.encodeBase64( salt, true ) + '$' + HexUtils.encodeBase64( encryptedBytes, true );

        } catch ( GeneralSecurityException e ) {
            throw new RuntimeException( "Unable to encrypt password: " + ExceptionUtils.getMessage( e ), e );
        } finally {
            Arrays.fill( key, (byte)'X' );
            destroyEphemeralKey( secretKey );
        }
    }

    @NotNull
    @Override
    public byte[] decryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, @NotNull String encryptedPassword ) throws ParseException {
        Matcher matcher = PARSE_REGEX.matcher( encryptedPassword );
        if ( !matcher.matches() )
            throw new ParseException("Not a valid encrypted password", 0);

        byte[] key = keyFinder.findMasterPasswordBytes();
        if ( key == null || key.length < 1 )
            throw new RuntimeException( "Password decryption key is not currently available" );

        String iterationCountStr = matcher.group( 1 );
        String saltStr = matcher.group( 2 );
        String encryptedBytesStr = matcher.group( 3 );

        SecretKey secretKey = null;
        try {
            // Find existing salt and iteration count
            int iterationCount = Integer.parseInt( iterationCountStr, 16 );
            byte[] salt = HexUtils.decodeBase64( saltStr );

            // Pull apart encrypted data into IV + ciphertext + MAC
            int ivLen = CIPHER_BLOCK_LEN;
            int macLen = MAC_LEN;
            byte[] encryptedBytes = HexUtils.decodeBase64( encryptedBytesStr );
            int ciphertextLen = encryptedBytes.length - ivLen - macLen;
            if ( ciphertextLen < 0 )
                throw new ParseException( "Invalid encrypted password -- not enough encrypted data", 0 );
            byte[][] unpacked = ArrayUtils.unpack( encryptedBytes, ivLen, ciphertextLen, macLen );
            byte[] iv = unpacked[0];
            byte[] ciphertext = unpacked[1];
            byte[] mac = unpacked[2];

            // Generate symmetric key from raw input key material
            secretKey = deriveSecretKey( key, iterationCount, salt );

            // Check MAC first, then decrypt
            byte[] expectedMac = computeMac( secretKey, iv, ciphertext );
            if ( !ArrayUtils.compareArraysConstantTime( expectedMac, mac ) )
                throw new ParseException( "Unable to decrypt password: bad mac value", 0 );

            // Decrypt, now that MAC is OK
            Cipher aes = Cipher.getInstance( CIPHER_TRANSFORMATION );
            aes.init( Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec( iv ) );
            return aes.doFinal( ciphertext );

        } catch ( NoSuchAlgorithmException|NoSuchPaddingException|InvalidAlgorithmParameterException e ) {
            // These exceptions can't happen unless there is a bug in our code or the environment is misconfigured
            throw new RuntimeException( "Unable to decrypt password: " + ExceptionUtils.getMessage( e ), e );
        } catch ( NumberFormatException e ) {
            throw (ParseException) new ParseException( "Invalid encrypted password -- bad iteration count: " + ExceptionUtils.getMessage( e ), 0 ).initCause( e );
        } catch ( InvalidKeyException|InvalidKeySpecException e ) {
            throw (ParseException)new ParseException( "Unable to decrypt password: bad mac value: " + ExceptionUtils.getMessage( e ), 0 ).initCause( e );
        } catch ( IllegalBlockSizeException|BadPaddingException e ) {
            // These exceptions are most likely caused by an invalid ciphertext
            // Our MAC check makes it safe to just propagate these errors since it ensures that nobody can reach
            // the point where these errors can occur unless they already know the original key to produce a valid MAC
            throw (ParseException)new ParseException( "Invalid encrypted password: " + ExceptionUtils.getMessage( e ), 0 ).initCause( e );
        } finally {
            Arrays.fill( key, (byte)0 );
            destroyEphemeralKey( secretKey );
        }
    }

    /**
     * Transform raw input key bytes, which may be of unpredictable length of entropy content (like a passphrase)
     * into a symmetric key suitable for use with the cipher and MAC.
     *
     * @param keyBytes raw key bytes.  Required.
     * @param iterationCount  iteration cound for PBKDF2.  Should be high-ish (like, over 100,000) if the key material
     *                        might be some low-entropy user-chosen value like a password or passphrase.  Can be as low
     *                        as 1 only if the key material
     *                        is known to be at least 32 bytes of high-entropy key material (like secure random output, or
     *                        the output of another KDF).
     * @param salt salt for PBKDF2.  Required.
     * @return A new SecretKey instance.  Never null.
     * @throws NoSuchAlgorithmException if the required PBKDF2 variant is not available
     * @throws InvalidKeySpecException if the passed-in key is not a suitable length
     */
    @NotNull
    private static SecretKey deriveSecretKey( @NotNull final byte[] keyBytes, int iterationCount, @NotNull byte[] salt )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        char[] keyChars = IOUtils.decodeCharacters( Charsets.ISO8859, keyBytes );
        SecretKey tempKey = null;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance( KDF_ALG );
            KeySpec spec = new PBEKeySpec( keyChars, salt, iterationCount, CIPHER_KEY_SIZE_BITS );
            tempKey = skf.generateSecret( spec );
            return new SecretKeySpec( tempKey.getEncoded(), 0, CIPHER_KEY_SIZE_BYTES, CIPHER_KEY_ALG );

        } finally {
            Arrays.fill( keyChars, 'X' );
            destroyEphemeralKey( tempKey );
        }
    }

    /**
     * Compute a message authentication code for the specified IV and ciphertext.
     *
     * @param macKey  key to use for MAC.  Required.
     * @param iv  IV that was used to encrypt ciphertext.  Required.
     * @param ciphertext  the ciphertext.  Required.
     * @return the MAC.  Never null or empty.
     * @throws NoSuchAlgorithmException if required HMAC algorithm is unavailable
     * @throws InvalidKeyException if the provided key is unsuitable
     */
    @NotNull
    private static byte[] computeMac( @NotNull final SecretKey macKey, @NotNull final byte[] iv, @NotNull final byte[] ciphertext )
            throws NoSuchAlgorithmException, InvalidKeyException
    {
        Mac hm = Mac.getInstance( "HmacSHA256" );
        hm.init( macKey );
        hm.update( iv );
        hm.update( ciphertext );
        return hm.doFinal();
    }

    /**
     * Destroy key if it is Destroyable.
     *
     * @param key a key that may or may not implement Destroyable.  May be null.
     */
    private static void destroyEphemeralKey( @Nullable final Key key ) {
        // In JDK 8, SecretKeySpec will finally implement Destroyable, so we are ready to go when that happens
        if ( key instanceof Destroyable ) {
            Destroyable destroyable = (Destroyable) key;
            try {
                destroyable.destroy();
            } catch ( DestroyFailedException e ) {
                // Not much we can do about this at this point aside from log it
                logger.log( Level.WARNING, "Unable to destroy ephemeral key: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            }
        }
    }

    private static int findMacLen( String macAlg ) {
        try {
            return Mac.getInstance( macAlg ).getMacLength();
        } catch ( NoSuchAlgorithmException e ) {
            throw new RuntimeException( "Unable to get HMAC: " + ExceptionUtils.getMessage( e ), e );
        }
    }

    private static int findCipherBlockLen( String cipherTransformation ) {
        try {
            return Cipher.getInstance( cipherTransformation ).getBlockSize();
        } catch ( GeneralSecurityException e ) {
            throw new RuntimeException( "Unable to get Cipher: " + ExceptionUtils.getMessage( e ), e );
        }
    }
}
