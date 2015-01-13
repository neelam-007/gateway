package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

/**
 * Utility to encrypt secrets in a resource
 */
public class SecretsEncryptor {

    private MasterPasswordManager secretsEncryptor;
    private boolean encryptingInitialized = false;
    private String wrappedBundleKey;
    private MasterPasswordManager bundleKeyEncryptor;

    public SecretsEncryptor(byte[] passphrase) throws GeneralSecurityException {

        // Convert bundle passphrase to bundle passphrase key bytes
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");
        SecretKey secretKey = skf.generateSecret(new PBEKeySpec(new String(passphrase, Charsets.UTF8).toCharArray()));
        byte[] bundlePassphraseKeyBytes = secretKey.getEncoded();
        bundleKeyEncryptor = MasterPasswordManager.createMasterPasswordManager( bundlePassphraseKeyBytes, false, false );
    }

    /**
     * Creates an instance of the SecretsEncryptor to use with the passphrase.
     *
     * @param base64encodedKeyPassphrase The base-64 UTF-8 encoded passphrase to use for the encryption key when encrypting passwords, if null uses cluster passphrase.
     * @return an instance of the SecretsEncryptor
     * @throws FileNotFoundException
     * @throws GeneralSecurityException
     */
    @Nullable
    public static SecretsEncryptor createSecretsEncryptor(final String base64encodedKeyPassphrase) throws FileNotFoundException, GeneralSecurityException {
        if (base64encodedKeyPassphrase != null) {
            return new SecretsEncryptor(HexUtils.decodeBase64(base64encodedKeyPassphrase));
        } else {
            final String confDir = SyspropUtil.getString("com.l7tech.config.path", "../node/default/etc/conf");
            final File ompDat = new File(new File(confDir), "omp.dat");
            if (ompDat.exists()) {
                return new SecretsEncryptor(new DefaultMasterPasswordFinder(ompDat).findMasterPasswordBytes());
            } else {
                throw new FileNotFoundException("Cannot encrypt because " + ompDat.getAbsolutePath() + " does not exist");
            }
        }
    }

    /**
     * Encrypts the secret
     *
     * @param secret the secret to encrypt
     * @return the encrypted secret
     */
    public String encryptSecret(String secret) {
        if (!encryptingInitialized) {
            setupEncrypting();
        }
        return secretsEncryptor.encryptPassword(secret.toCharArray());
    }

    /**
     * The key to store in the resource.
     *
     * @return the bundle key to store
     */
    public String getWrappedBundleKey() {
        if (!encryptingInitialized) {
            setupEncrypting();
        }
        return wrappedBundleKey;
    }

    private void setupEncrypting() {
        // lazily create encrypting objects, not needed for decrypting

        // Create a new random bundle key
        final byte[] bundleKeyBytes = new byte[32];
        RandomUtil.nextBytes(bundleKeyBytes);

        // setup secrets encryptor
        secretsEncryptor = MasterPasswordManager.createMasterPasswordManager( bundleKeyBytes, true, false );

        // save wrapped key
        String bundleKeyBase64 = HexUtils.encodeBase64(bundleKeyBytes);
        wrappedBundleKey = bundleKeyEncryptor.encryptPassword(bundleKeyBase64.toCharArray());

        encryptingInitialized = true;
    }

    /**
     * Decrypts the secret
     *
     * @param secret        the encrypted secret
     * @param encryptedKey  the key in the resource
     * @return  the decrypted secret
     */
    public String decryptSecret(@Nullable final String secret, @Nullable final String encryptedKey) throws ParseException {

        MasterPasswordManager secretsDecryptor = new MasterPasswordManager(HexUtils.decodeBase64(new String(bundleKeyEncryptor.decryptPassword(encryptedKey))),true);
        return new String(secretsDecryptor.decryptPassword(secret));
    }

}
