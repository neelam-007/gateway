package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Closeable;


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


    protected SecretsEncryptor(@NotNull byte[] passphrase) throws GeneralSecurityException {
        this.passphrase = passphrase;
        bundleKeyEncryptor = MasterPasswordManager.createMasterPasswordManager( passphrase, false, false );
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
        Arrays.fill( bundleKeyBytes, (byte)0 );
        return decrypted;
    }

}