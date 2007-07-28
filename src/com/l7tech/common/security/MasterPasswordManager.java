package com.l7tech.common.security;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SyspropUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides password encryption/decryption services.
 */
public class MasterPasswordManager {
    protected static final Logger logger = Logger.getLogger(MasterPasswordManager.class.getName());
    public static final String PROP_FINDER = "com.l7tech.masterPasswordFinder";
    private static final String ENCRYPTION_TAG = "L7C";
    public static final String ENCRYPTION_PREFIX = "$" + ENCRYPTION_TAG + "$";
    private static final String PROP_FINDER_DEFAULT = DefaultMasterPasswordFinder.class.getName();

    private static final MasterPasswordManager INSTANCE = new MasterPasswordManager();
    private MasterPasswordFinder finder;


    public static MasterPasswordManager getInstance() {
        return INSTANCE;
    }

    /**
     * Create a MasterPasswordManager that will find the default master password finder.
     */
    public MasterPasswordManager() {
    }

    /**
     * Create a MasterPasswordManager that will use the specified master password finder.
     *
     * @param finder the MasterPasswordFinder to use.
     */
    public MasterPasswordManager(MasterPasswordFinder finder) {
        this.finder = finder;
    }

    /**
     * Get the master password from the MasterPasswordFinder.
     * Uncached -- this will always invoke the finder.
     *
     * @return the master password, or null if one could not be found.
     */
    private char[] getMasterPassword() {
        try {
            return getFinder().findMasterPassword();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to find master password -- assuming unencrypted passwords", e);
            return null;
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
     * @return the symmetric key to use for encryption/decryption
     */
    private AesKey getKey(String salt) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            char[] mp = getMasterPassword();
            byte[] mpBytes = new String(mp).getBytes("UTF-8");
            byte[] saltBytes = salt.getBytes("UTF-8");

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

            return new AesKey(keybytes, 256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No SHA-512 implementation configured", e); // shouldn't happen
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8 implementation configured", e); // can't happen
        }
    }

    /** @return a base64-ed random Salt string. */
    private String generateSalt() {
        Cipher aes = getAes();
        int blocksize = aes.getBlockSize();
        byte[] saltBytes = new byte[blocksize];
        new SecureRandom().nextBytes(saltBytes);
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
        AesKey key = getKey(salt);

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);
            byte[] plaintextBytes = new String(plaintextPassword).getBytes("UTF-8");

            Cipher aes = getAes();
            aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(saltBytes));
            byte[] ciphertextBytes = aes.doFinal(plaintextBytes);

            return ENCRYPTION_PREFIX + salt + "$" + HexUtils.encodeBase64(ciphertextBytes, true);

        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8 implementation configured", e); // can't happen
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        } catch (BadPaddingException e) {
            throw new RuntimeException("Unable to encrypt password: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        } catch (IOException e) {
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
     *         the original password's characters unchanged.
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public char[] decryptPasswordIfEncrypted(String possiblyEncryptedPassword) {
        if (!looksLikeEncryptedPassword(possiblyEncryptedPassword))
            return possiblyEncryptedPassword.toCharArray();

        try {
            return decryptPassword(possiblyEncryptedPassword);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Unable to decrypt encrypted password: " + ExceptionUtils.getMessage(e), e);
            return possiblyEncryptedPassword.toCharArray();
        }
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
        AesKey ourkey = getKey(salt);

        try {
            byte[] saltBytes = HexUtils.decodeBase64(salt);
            byte[] ciphertextBytes = HexUtils.decodeBase64(ciphertextBase64);

            Cipher aes = getAes();
            aes.init(Cipher.DECRYPT_MODE, ourkey, new IvParameterSpec(saltBytes));
            byte[] plaintextBytes = aes.doFinal(ciphertextBytes);

            return new String(plaintextBytes, "UTF-8").toCharArray();

        } catch (IllegalBlockSizeException e) {
            throw new ParseException("Encrypted password does not have correct format: " + ExceptionUtils.getMessage(e), 0); // shouldn't be possible
        } catch (IOException e) {
            throw new ParseException("Encrypted password does not have correct format: bad Base 64: " + ExceptionUtils.getMessage(e), 0);
        } catch (BadPaddingException e) {
            throw new ParseException("Encrypted password does not have correct format: " + ExceptionUtils.getMessage(e), 0);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to decrypt password: " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (InvalidAlgorithmParameterException e) {
            throw new ParseException("Encrypted password does not have correct salt format: " + ExceptionUtils.getMessage(e), 0);
        }
    }

    private synchronized MasterPasswordFinder getFinder() {
        if (finder != null)
            return finder;

        String finderClassname = SyspropUtil.getString(PROP_FINDER, PROP_FINDER_DEFAULT);
        try {
            Class finderClass = Class.forName(finderClassname);
            if (finderClass == null || !MasterPasswordFinder.class.isAssignableFrom(finderClass))
                throw new IllegalStateException("Class does not implement MasterPasswordFinder: " + finderClassname);
            return finder = (MasterPasswordFinder)finderClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find master password finder class: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate master password finder class: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to instantiate master password finder class: " + ExceptionUtils.getMessage(e), e);
        }
    }
}

