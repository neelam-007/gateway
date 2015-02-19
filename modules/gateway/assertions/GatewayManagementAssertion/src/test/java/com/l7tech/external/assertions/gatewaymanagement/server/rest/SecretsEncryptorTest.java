package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.L7C2SecretEncryptor;
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
    final private L7C2SecretEncryptor l7C2SecretEncryptor = new L7C2SecretEncryptor();
    private String passphraseBase64encoded = HexUtils.encodeBase64("Password".getBytes(Charsets.UTF8));
    private SecretsEncryptorFactory secretsEncryptorFactory = new SecretsEncryptorFactory("clusterPassphase".toCharArray());

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
        final SecretsEncryptor secretsEncryptor = secretsEncryptorFactory.createSecretsEncryptor(null);
        final byte[] toEncrypt = "foo".getBytes(Charsets.UTF8);
        final String encrypted = secretsEncryptor.encryptSecret(toEncrypt);
        final String wrappedBundleKey = secretsEncryptor.getWrappedBundleKey();
        final byte[] decrypted = secretsEncryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertArrayEquals(toEncrypt, decrypted);
    }

    @Test
    public void testIsUsingClusterPassphrase() throws Exception {
        final SecretsEncryptor secretsEncryptor = secretsEncryptorFactory.createSecretsEncryptor(null);
        final byte[] toEncrypt = "foo".getBytes(Charsets.UTF8);
        final String encrypted = secretsEncryptor.encryptSecret(toEncrypt);
        final String wrappedBundleKey = secretsEncryptor.getWrappedBundleKey();

        final SecretsEncryptor secretsDecryptor = secretsEncryptorFactory.createSecretsEncryptor(HexUtils.encodeBase64("clusterPassphase".getBytes(Charsets.UTF8)));
        final byte[] decrypted = secretsDecryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertArrayEquals(toEncrypt, decrypted);
    }

    @Test
    public void testRoundtrip() throws Exception {
        byte[] secret = "Super Secret".getBytes(Charsets.UTF8);

        SecretsEncryptor encryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded);
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();
        encryptor.close();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(l7C2SecretEncryptor.looksLikeEncryptedSecret(encrypted));
        assertTrue(l7C2SecretEncryptor.looksLikeEncryptedSecret(wrappedBundleKey));

        byte[] decrypted;
        try(SecretsEncryptor decryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded)) {
            decrypted = decryptor.decryptSecret(encrypted, wrappedBundleKey);
        }
        assertArrayEquals(secret, decrypted);
    }

    @Test(expected = ParseException.class)
    public void testBadDecryptingPassphrase() throws Exception{
        byte[] secret = "Super Secret".getBytes(Charsets.UTF8);

        SecretsEncryptor encryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded);
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();
        encryptor.close();

        assertTrue(new L7C2SecretEncryptor().looksLikeEncryptedSecret(encrypted));
        assertTrue(new L7C2SecretEncryptor().looksLikeEncryptedSecret(wrappedBundleKey));

        try(SecretsEncryptor decryptor = secretsEncryptorFactory.createSecretsEncryptor(HexUtils.encodeBase64("bad".getBytes(Charsets.UTF8)))) {
            decryptor.decryptSecret(encrypted, wrappedBundleKey);
        }
    }

    @Test(expected = ParseException.class)
    public void testPassphraseRevoked() throws Exception{
        byte[] secret = "Super Secret".getBytes(Charsets.UTF8);

        SecretsEncryptor encryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded);
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();
        encryptor.close();

        encryptor.decryptSecret(encrypted,wrappedBundleKey);
    }

    @Test
    public void testMultiDecrypt() throws Exception {
        byte[] secret = "Super Secret".getBytes(Charsets.UTF8);

        SecretsEncryptor encryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded);
        String encrypted = encryptor.encryptSecret(secret);
        String wrappedBundleKey = encryptor.getWrappedBundleKey();
        encryptor.close();

        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(encrypted.startsWith("$L7"));
        assertTrue(l7C2SecretEncryptor.looksLikeEncryptedSecret(encrypted));
        assertTrue(l7C2SecretEncryptor.looksLikeEncryptedSecret(wrappedBundleKey));

        byte[] decrypted;
        SecretsEncryptor decryptor = secretsEncryptorFactory.createSecretsEncryptor(passphraseBase64encoded);

        decrypted = decryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertArrayEquals(secret, decrypted);

        decrypted = decryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertArrayEquals(secret, decrypted);

        decrypted = decryptor.decryptSecret(encrypted, wrappedBundleKey);
        assertArrayEquals(secret, decrypted);

        decryptor.close();
    }
}