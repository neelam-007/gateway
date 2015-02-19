package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The legacy "$L7C$" password encryption format.
 * <p/>
 * This format is weird and idiosyncratic, featuring among other things an IV-dependent key.
 * It uses a simple double-hashing to convert the raw encryption key (which may be a passphrase)
 * into an AES key, instead of using a standard KDF.  The "salt" is included in the hash used to produce the key,
 * and is also used as the CBC IV.  There is no integrity protection of the ciphertext so the system is vulnerable
 * to attacks on the CBC decryption (though this was normally not a fatal problem in its original use case
 * of obscuring passwords in local config files).
 * <p/>
 * Use L7C2SecretEncryptor instead when possible.
 */
public class L7CSecretEncryptor implements SecretEncryptor {
    private static final Logger logger = Logger.getLogger( L7CSecretEncryptor.class.getName() );
    private static final String ENCRYPTION_TAG = "L7C";
    private static final String ENCRYPTION_PREFIX = "$" + ENCRYPTION_TAG + "$";

    /**
     * Test if the specified string looks like it might be an encrypted password.
     * <p/>
     * This method considers a string to look like an encrypted password if it starts with the {@link #ENCRYPTION_PREFIX},
     * "$L7C$".
     *
     * @param possiblyEncryptedSecret a password that might be encrypted.  Required.
     * @return true if this password looks like an encrypted password.
     */
    @Override
    public boolean looksLikeEncryptedSecret( String possiblyEncryptedSecret ) {
        return !( possiblyEncryptedSecret == null || possiblyEncryptedSecret.length() < 1) &&
                possiblyEncryptedSecret.startsWith(ENCRYPTION_PREFIX);
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
    public String encryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, boolean bypassKdf, @NotNull byte[] passwordBytes ) {
        String salt = generateSalt();
        SecretKey key = getKey( salt, keyFinder );
        if ( key == null )
            throw new RuntimeException( "Unable to obtain key for encryption" );

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);

            Cipher aes = getAes();
            aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(saltBytes));
            byte[] ciphertextBytes = aes.doFinal( passwordBytes );

            return ENCRYPTION_PREFIX + salt + "$" + HexUtils.encodeBase64(ciphertextBytes, true);

        } catch ( GeneralSecurityException e ) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        }
    }

    @NotNull
    @Override
    public byte[] decryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, @NotNull String encryptedPassword ) throws ParseException {
        if (!looksLikeEncryptedSecret( encryptedPassword ))
            throw new ParseException("Not an encrypted password", 0);

        String[] components = encryptedPassword.split("\\$", 10);
        if (components.length != 4)
            throw new ParseException("Encrypted password does not have correct format: expected 4 components, found " + components.length, 0);
        if (components[0].length() > 0)
            throw new ParseException("Encrypted password does not have correct format: first component not empty", 0); // can't happen
        if (!ENCRYPTION_TAG.equals(components[1]))
            throw new ParseException("Encrypted password does not have correct format: second component not " + ENCRYPTION_TAG, 0);
        String salt = components[2];
        String ciphertextBase64 = components[3];
        if (salt.length() < 1)
            throw new ParseException("Encrypted password does not have correct format: no salt", 0);
        if (ciphertextBase64.length() < 1)
            throw new ParseException("Encrypted password does not have correct format: no ciphertext", 0);
        SecretKey ourkey = getKey( salt, keyFinder );
        if (ourkey == null)
            throw new IllegalStateException( "Password decryption key is not currently available" );

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);
            byte[] ciphertextBytes = HexUtils.decodeBase64(ciphertextBase64);

            Cipher aes = getAes();
            aes.init(Cipher.DECRYPT_MODE, ourkey, new IvParameterSpec(saltBytes));

            return aes.doFinal(ciphertextBytes);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new ParseException("Encrypted password does not have correct format: " + ExceptionUtils.getMessage(e), 0); // shouldn't be possible
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to decrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (InvalidAlgorithmParameterException e) {
            throw new ParseException("Encrypted password does not have correct salt format: " + ExceptionUtils.getMessage(e), 0);
        }
    }

    /**
     * Get the symmetric key that will be used to encrypt and decrypt the encrypted passwords.
     * This key will be built out of the master password.
     * Uncached -- this will always look up the master password.
     * <P/>
     * The salt and the master password are encoded as UTF-8 byte streams and then key is produced as follows:
     * <pre>  sha512(masterPass, salt, sha512(masterPass, salt, masterPass)) </pre>
     *
     * @param salt the salt string from the encrypted password, or the salt string to use for encrypting a new password
     * @return the symmetric key to use for encryption/decryption, or null if no key is available
     */
    @Nullable
    private SecretKey getKey( String salt, SecretEncryptorKeyFinder keyFinder ) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            byte[] mpBytes = getMasterPasswordBytes( keyFinder );
            if (mpBytes == null)
                return null;
            byte[] saltBytes = salt.getBytes(Charsets.UTF8);

            sha.reset();
            sha.update(mpBytes);
            sha.update(saltBytes);
            sha.update(mpBytes);
            byte[] stage1 = sha.digest();

            sha.reset();
            sha.update(mpBytes);
            sha.update(saltBytes);
            sha.update(stage1);
            byte[] keybytes = sha.digest();

            if ( keybytes.length > 32 ) {
                byte[] trimmedBytes = new byte[32];
                System.arraycopy( keybytes, 0, trimmedBytes, 0, 32 );
                keybytes = trimmedBytes;
            }

            return new SecretKeySpec( keybytes, "AES" );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No SHA-512 implementation configured", e); // shouldn't happen
        }
    }

    /** @return a base64-ed random Salt string. */
    private String generateSalt() {
        Cipher aes = getAes();
        int blocksize = aes.getBlockSize();
        byte[] saltBytes = new byte[blocksize];
        RandomUtil.nextBytes(saltBytes);
        return HexUtils.encodeBase64(saltBytes, true);
    }

    private Cipher getAes() {
        try {
            return Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No AES implementation available", e); // can't happen
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("No AES implementation available with PKCS5Padding", e); // can't happen
        }
    }

    /**
     * @return the master password converted to key bytes, or null if the master password is unavailable.
     */
    @Nullable
    byte[] getMasterPasswordBytes( @NotNull SecretEncryptorKeyFinder finder ) {
        byte[] ret = null;
        Throwable t = null;
        try {
            ret = finder.findMasterPasswordBytes();
        } catch (Exception e) {
            /* FALLTHROUGH and log it */
            t = e;
        }
        if (ret == null) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to find master password: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
        }
        return ret;
    }
}
