package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;

/**
 * For creating secrets encryptor
 */
public class SecretsEncryptorFactory {

    private final char[] clusterPassphrase;

    public SecretsEncryptorFactory(char[] clusterPassphrase) {
        this.clusterPassphrase = clusterPassphrase;
    }

    /**
     * Creates an instance of the SecretsEncryptor to use with the passphrase.
     *
     * @param base64encodedKeyPassphrase The base-64 UTF-8 encoded passphrase to use for the encryption key when encrypting passwords, if null uses cluster passphrase.
     * @return an instance of the SecretsEncryptor
     * @throws java.io.FileNotFoundException
     * @throws java.security.GeneralSecurityException
     */
    @NotNull
    public SecretsEncryptor createSecretsEncryptor(@Nullable final String base64encodedKeyPassphrase) throws FileNotFoundException, GeneralSecurityException {
        if (base64encodedKeyPassphrase != null) {
            return new SecretsEncryptor(HexUtils.decodeBase64(base64encodedKeyPassphrase));
        } else {
            // use cluster passphrase
            return new SecretsEncryptor(new String(clusterPassphrase).getBytes(Charsets.UTF8));
        }
    }
}
