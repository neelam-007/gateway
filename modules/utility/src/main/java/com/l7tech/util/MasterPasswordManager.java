package com.l7tech.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides password encryption/decryption services.
 */
public class MasterPasswordManager {
    private static final Logger logger = Logger.getLogger(MasterPasswordManager.class.getName());
    private static final String ENCRYPTION_TAG = "L7C";
    private static final String ENCRYPTION_PREFIX = "$" + ENCRYPTION_TAG + "$";

    private final MasterPasswordFinder finder;
    private static final SecureRandom rand = new SecureRandom();

    /**
     * @return the master password converted to key bytes, or null if the master password is unavailable.
     */
    byte[] getMasterPasswordBytes() {
        byte[] ret = null;
        Throwable t = null;
        try {
            ret = getFinder().findMasterPasswordBytes();
        } catch (Exception e) {
            /* FALLTHROUGH and log it */
            t = e;
        }
        if (ret == null) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to find master password -- assuming unencrypted passwords: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
        }
        return ret;
    }

    /**
     * Create a MasterPasswordManager that will use the specified master password finder.
     *
     * @param finder the MasterPasswordFinder to use.
     */
    public MasterPasswordManager(final MasterPasswordFinder finder) {
        this.finder = finder;
    }

    /**
     * Create a MasterPasswordManager that will use the specified bytes to produce the key for encrypting
     * and decrypting passwords.
     * <p/>
     * <strong>Important note:</strong> This constructor causes the key bytes to be kept in memory for the entire
     * lifetime of the MasterPasswordManager instance and should only be used by unit tests or short-lived processes.
     *
     * @param fixedKeyBytes a byte string that will be used to produce a key.  Will not be treated as characters.
     *                      No defensive copy will be made, so caller may revoke access to the key at a later time by zeroing the array, causing decryption to fail.
     */
    public MasterPasswordManager(final byte[] fixedKeyBytes) {
        this.finder = new MasterPasswordFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return fixedKeyBytes;
            }
        };
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
    private SecretKey getKey(String salt) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            byte[] mpBytes = getMasterPasswordBytes();
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
        rand.nextBytes(saltBytes);
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
     * Encrypt a plaintext password using a key derived from the master password and a random salt.
     *
     * @param plaintextPassword the password to encrypt.  Required.
     * @return the encrypted form of this password, in the form "$L7C$jasdjhfasdkj$asdkajsdhfaskdjfhasdkjfh".  Never null.
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public String encryptPassword(char[] plaintextPassword) {
        String salt = generateSalt();
        SecretKey key = getKey(salt);
        if (key == null)
            return new String(plaintextPassword);

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);
            byte[] plaintextBytes = new String(plaintextPassword).getBytes(Charsets.UTF8);

            Cipher aes = getAes();
            aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(saltBytes));
            byte[] ciphertextBytes = aes.doFinal(plaintextBytes);

            return ENCRYPTION_PREFIX + salt + "$" + HexUtils.encodeBase64(ciphertextBytes, true);

        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        } catch (BadPaddingException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        }
    }

    /**
     * Test if the specified string looks like it might be an encrypted password.
     * <p/>
     * This method considers a string to look like an encrypted password if it starts with the {@link #ENCRYPTION_PREFIX},
     * "$L7C$".
     *
     * @param possiblyEncryptedPassword a password that might be encrypted.  Required.
     * @return true if this password looks like an encrypted password.
     */
    public boolean looksLikeEncryptedPassword(String possiblyEncryptedPassword) {
        return !(possiblyEncryptedPassword == null || possiblyEncryptedPassword.length() < 1) &&
               possiblyEncryptedPassword.startsWith(ENCRYPTION_PREFIX);
    }

    /**
     * Decrypts a password if it looks like an encrypted password; otherwise returns it unchanged.
     * If errors occur while decrypting the password, this logs the errors at level WARNING but then
     * returns the password unchanged.
     *
     * @param possiblyEncryptedPassword a password that might be encrypted.  Required.
     * @return the decrypted version of the password if it was encrypted and decryption was possible, otherwise
     *         the original password's characters unchanged.  Possibly null if the input string was null.
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public char[] decryptPasswordIfEncrypted(String possiblyEncryptedPassword) {
        if (!looksLikeEncryptedPassword(possiblyEncryptedPassword))
            return possiblyEncryptedPassword == null ? null : possiblyEncryptedPassword.toCharArray();

        try {
            return decryptPassword(possiblyEncryptedPassword);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Unable to decrypt encrypted password: " + ExceptionUtils.getMessage(e), e);
            return possiblyEncryptedPassword.toCharArray();
        }
    }

    /**
     * Check if a key is currently available for encryption/decryption.
     * If no key is available, the encrypt and decrypt methods will return their input unchanged.
     *
     * @return true if a master key is currently available; false if no crypto will be done.
     */
    public boolean isKeyAvailable() {
        return getMasterPasswordBytes() != null;
    }

    /**
     * Decrypt an encrypted password.
     *
     * @param encryptedPassword an encrypted password, similar to "$L7C$jasdjhfasdkj$asdkajsdhfaskdjfhasdkjfh".  Required.
     * @return the plaintext of the password.  Never null, but may be empty.
     * @throws ParseException if the encrypted password was invalid and could not be decrypted
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public char[] decryptPassword(String encryptedPassword) throws ParseException {
        if (!looksLikeEncryptedPassword(encryptedPassword))
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
        SecretKey ourkey = getKey(salt);
        if (ourkey == null)
            return encryptedPassword.toCharArray();

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);
            byte[] ciphertextBytes = HexUtils.decodeBase64(ciphertextBase64);

            Cipher aes = getAes();
            aes.init(Cipher.DECRYPT_MODE, ourkey, new IvParameterSpec(saltBytes));
            byte[] plaintextBytes = aes.doFinal(ciphertextBytes);

            return new String(plaintextBytes, Charsets.UTF8).toCharArray();

        } catch (IllegalBlockSizeException e) {
            throw new ParseException("Encrypted password does not have correct format: " + ExceptionUtils.getMessage(e), 0); // shouldn't be possible
        } catch (BadPaddingException e) {
            throw new ParseException("Encrypted password does not have correct format: " + ExceptionUtils.getMessage(e), 0);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to decrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (InvalidAlgorithmParameterException e) {
            throw new ParseException("Encrypted password does not have correct salt format: " + ExceptionUtils.getMessage(e), 0);
        }
    }

    private synchronized MasterPasswordFinder getFinder() {
       return finder;
    }

    /**
     * Interface implemented by strategies for obtaining the master password.
     */
    public static interface MasterPasswordFinder {
        /**
         * Get the Master Password from wherever this MasterPasswordFinder gets it.
         *
         * @return the master password as a newly-allocated.  Never empty or null.  May contain arbitrary binary data (not necessarily a valid UTF-8 string).
         *         Caller should zero the array when finished with it.
         * @throws IllegalStateException if no master password can be found.
         */
        byte[] findMasterPasswordBytes();
    }
}

