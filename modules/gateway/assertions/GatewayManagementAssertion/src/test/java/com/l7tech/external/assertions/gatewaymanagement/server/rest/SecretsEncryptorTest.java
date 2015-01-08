package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 */
public class SecretsEncryptorTest {

    private SecretsEncryptor encryptor = new SecretsEncryptor("password".getBytes());
    @Test
    public void testRoundtrip(){
        byte[] secret = "super secret".getBytes();
        byte[] encrypted = encryptor.encryptSecret(secret);
        byte[] key = encryptor.getEncryptedKey();
        byte[] decrypted = encryptor.decryptSecret(encrypted,key);

        assertArrayEquals(secret, decrypted);
    }
}
