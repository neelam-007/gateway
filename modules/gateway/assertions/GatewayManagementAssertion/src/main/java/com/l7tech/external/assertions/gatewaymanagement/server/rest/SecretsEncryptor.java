package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Utility to encrypt secrets in a resource
 */
public class SecretsEncryptor implements Closeable {

    private MasterPasswordManager secretsEncryptor;
    private boolean encryptingInitialized = false;
    private String wrappedBundleKey;
    private MasterPasswordManager bundleKeyEncryptor;
    private byte[] passphrase;
    private byte[] bundleKeyBytes;

    private SecretsEncryptor(@NotNull byte[] passphrase) throws GeneralSecurityException {
        this.passphrase = passphrase;
        bundleKeyEncryptor = MasterPasswordManager.createMasterPasswordManager( passphrase, false, false );
    }

    /**
     * Creates an instance of the SecretsEncryptor to use with the passphrase.
     *
     * @param base64encodedKeyPassphrase The base-64 UTF-8 encoded passphrase to use for the encryption key when encrypting passwords, if null uses cluster passphrase.
     * @return an instance of the SecretsEncryptor
     * @throws FileNotFoundException
     * @throws GeneralSecurityException
     */
    @NotNull
    public static SecretsEncryptor createSecretsEncryptor(@Nullable final String base64encodedKeyPassphrase) throws FileNotFoundException, GeneralSecurityException {
        if (base64encodedKeyPassphrase != null) {
            return new SecretsEncryptor(HexUtils.decodeBase64(base64encodedKeyPassphrase));
        } else {
            final String confDir = ConfigFactory.getProperty("com.l7tech.server.configDirectory");
            if (confDir != null) {
                final File ompDat = new File(new File(confDir), "omp.dat");
                if (ompDat.exists()) {
                    return new SecretsEncryptor(new DefaultMasterPasswordFinder(ompDat).findMasterPasswordBytes());
                } else {
                    throw new FileNotFoundException("Cannot encrypt because " + ompDat.getAbsolutePath() + " does not exist");
                }
            } else {
                throw new FileNotFoundException("Cannot encrypt because unable to locate conf directory");
            }
        }
    }

    /**
     * Revokes access to the passphrase.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        Arrays.fill( passphrase, (byte)0 );
        if(bundleKeyBytes!=null) {
            Arrays.fill(bundleKeyBytes, (byte) 0);
        }
    }

    /**
     * Encrypts the secret
     *
     * @param secret the secret to encrypt
     * @return the encrypted secret
     */
    @NotNull
    public String encryptSecret(@NotNull byte[] secret) {
        if (!encryptingInitialized) {
            setupEncrypting();
        }
        return secretsEncryptor.encryptSecret(secret);
    }

    /**
     * The key to store in the resource.
     *
     * @return the bundle key to store
     */
    @NotNull
    public String getWrappedBundleKey() {
        if (!encryptingInitialized) {
            setupEncrypting();
        }
        return wrappedBundleKey;
    }

    private void setupEncrypting() {
        // lazily create encrypting objects, not needed for decrypting

        // Create a new random bundle key
        bundleKeyBytes = new byte[32];
        RandomUtil.nextBytes(bundleKeyBytes);

        // setup secrets encryptor
        secretsEncryptor = MasterPasswordManager.createMasterPasswordManager( bundleKeyBytes, true, false );

        // save wrapped key
        wrappedBundleKey = bundleKeyEncryptor.encryptSecret( bundleKeyBytes );

        encryptingInitialized = true;
    }

    /**
     * Decrypts the secret using the encrypted key from the resource
     *
     * @param secret        the encrypted secret
     * @param encryptedKey  the encrypted key in the resource
     * @return  the decrypted secret
     */
    @NotNull
    public byte[] decryptSecret(@NotNull final String secret, @NotNull final String encryptedKey) throws ParseException {
        byte[] bundleKeyBytes = bundleKeyEncryptor.decryptSecret(encryptedKey);
        MasterPasswordManager secretsDecryptor = MasterPasswordManager.createMasterPasswordManager(bundleKeyBytes , true, false );
        byte[] decrypted =  secretsDecryptor.decryptSecret(secret);
        Arrays.fill( passphrase, (byte)0 );
        return decrypted;
    }

}