package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import java.text.ParseException;

import com.l7tech.util.Charsets;
import com.l7tech.util.MasterPasswordManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class SecretsEncryptorTest {

    private String passphrase = "Password";

    @Test
    public void testRoundtrip() throws Exception {
        String secret = "Super Secret";

        SecretsEncryptor encryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(mpm.looksLikeEncryptedPassword(encrypted));
        assertTrue(mpm.looksLikeEncryptedPassword(wrappedBundleKey));

        SecretsEncryptor decryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String decrypted = decryptor.decryptSecret(encrypted,wrappedBundleKey);

        assertEquals(secret, decrypted);

    }

    @Test
    public void testBadDecryptingPassphrase() throws Exception{
        String secret = "Super Secret";

        SecretsEncryptor encryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(mpm.looksLikeEncryptedPassword(encrypted));
        assertTrue(mpm.looksLikeEncryptedPassword(wrappedBundleKey));

        SecretsEncryptor decryptor = new SecretsEncryptor("bad".getBytes(Charsets.UTF8));
        try {
            decryptor.decryptSecret(encrypted, wrappedBundleKey);
        }catch (ParseException e) {
            return;
        }
        fail("Should throw error");
    }

    @Test
    public void testDecrypting() throws Exception{
        SecretsEncryptor decryptor = new SecretsEncryptor("bad".getBytes(Charsets.UTF8));
        try {
            decryptor.decryptSecret(null, null);
        }catch (ParseException e) {
            return;
        }
        fail("Should throw error");
    }

}
