package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.Charsets;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.text.ParseException;

import static org.junit.Assert.*;

/**
 */
public class SecretsEncryptorTest {

    private static final String CONF_SYS_PROP = "com.l7tech.server.configDirectory";
    private String passphrase = "Password";

    @Before
    public void setup() {
        final URL resourceConf = SecretsEncryptorTest.class.getResource("conf");
        SyspropUtil.setProperty(CONF_SYS_PROP, resourceConf.getPath());
    }

    @After
    public void teardown() {
        SyspropUtil.clearProperty(CONF_SYS_PROP);
    }

    @Test
    public void roundtripClusterPassphrase() throws Exception {
        final SecretsEncryptor secretsEncryptor = SecretsEncryptor.createSecretsEncryptor(null);
        final String toEncrypt = "foo";
        final String encrypted = secretsEncryptor.encryptSecret(toEncrypt);
        final String wrappedBundleKey = secretsEncryptor.getWrappedBundleKey();
        final String decrypted = secretsEncryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertEquals(toEncrypt, decrypted);
    }

    @Test
    public void testRoundtrip() throws Exception {
        String secret = "Super Secret";

        SecretsEncryptor encryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[]{2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(mpm.looksLikeEncryptedPassword(encrypted));
        assertTrue(mpm.looksLikeEncryptedPassword(wrappedBundleKey));

        SecretsEncryptor decryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String decrypted = decryptor.decryptSecret(encrypted, wrappedBundleKey);

        assertEquals(secret, decrypted);

    }

    @Test
    public void testBadDecryptingPassphrase() throws Exception {
        String secret = "Super Secret";

        SecretsEncryptor encryptor = new SecretsEncryptor(passphrase.getBytes(Charsets.UTF8));
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[]{2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(mpm.looksLikeEncryptedPassword(encrypted));
        assertTrue(mpm.looksLikeEncryptedPassword(wrappedBundleKey));

        SecretsEncryptor decryptor = new SecretsEncryptor("bad".getBytes(Charsets.UTF8));
        try {
            decryptor.decryptSecret(encrypted, wrappedBundleKey);
        } catch (ParseException e) {
            return;
        }
        fail("Should throw error");
    }

    @Test
    public void testDecrypting() throws Exception {
        SecretsEncryptor decryptor = new SecretsEncryptor("bad".getBytes(Charsets.UTF8));
        try {
            decryptor.decryptSecret(null, null);
        } catch (ParseException e) {
            return;
        }
        fail("Should throw error");
    }

}
