package com.l7tech.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Properties;

import static com.l7tech.util.KeyStorePrivateKeyMasterPasswordFinder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for KeyStorePrivateKeyMasterPasswordFinder.
 */
public class KeyStorePrivateKeyMasterPasswordFinderTest {

    String alias = "testentry";
    char[] kspass = "ksPassword123".toCharArray();
    char[] entrypass = "entryPassword123".toCharArray();
    byte[] masterPassword = "SecureMasterPass including\000nul chars".getBytes();

    // Lazily populated
    KeyStore keyStore;
    PrivateKey privateKey;
    X509Certificate cert;

    @Before
    public void beforeTest() {
        clearProv();
    }

    @After
    public void afterTest() {
        clearProv();
    }

    private void clearProv() {
        Security.removeProvider("BC");
        Security.removeProvider("BC");
        Security.removeProvider("BC");
    }


    @Test
    public void testPrependProvider() throws Exception {
        assertNull("BC shall not be installed as a provider at the start of the test", Security.getProvider("BC"));

        byte[] ciphertext = makeCiphertext();
        Properties props = makeProps(ciphertext);
        props.setProperty(PROP_PREPEND_PROVIDERS, BouncyCastleProvider.class.getName());

        MasterPasswordManager.MasterPasswordFinder finder = new KeyStorePrivateKeyMasterPasswordFinder(new File("."), props);
        byte[] decrypted = finder.findMasterPasswordBytes();

        assertEquals("Decrypted master password must match original", HexUtils.hexDump(masterPassword), HexUtils.hexDump(decrypted));

        assertEquals("BC shall have been installed as most-preference provider", "BC", Security.getProviders()[0].getName());
    }

    @Test
    public void testUnwrapPrivateKey() throws Exception {
        byte[] ciphertext = makeCiphertext();
        Properties props = makeProps(ciphertext);

        MasterPasswordManager.MasterPasswordFinder finder = new KeyStorePrivateKeyMasterPasswordFinder(new File("."), props);
        byte[] decrypted = finder.findMasterPasswordBytes();

        assertEquals("Decrypted master password must match original", HexUtils.hexDump(masterPassword), HexUtils.hexDump(decrypted));

        decrypted = finder.findMasterPasswordBytes();

        assertEquals("Decrypted master password must match original", HexUtils.hexDump(masterPassword), HexUtils.hexDump(decrypted));
    }

    private byte[] makeCiphertext() throws Exception {
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, getCert().getPublicKey());
        return rsa.doFinal(masterPassword);
    }

    private Properties makeProps(byte[] ciphertext) throws Exception {
        Properties props = new Properties();
        props.setProperty(PROP_KEYSTORE_PROVIDER_CLASSNAME, getKeyStore().getProvider().getClass().getName());
        props.setProperty(PROP_KEYSTORE_ALIAS, alias);
        props.setProperty(PROP_KEYSTORE_PASSWORD, String.valueOf(kspass));
        props.setProperty(PROP_KEYSTORE_ENTRY_PASSWORD, String.valueOf(entrypass));
        props.setProperty(PROP_KEYSTORE_CONTENTS_BASE64, HexUtils.encodeBase64(getKeyStoreBytes()));
        props.setProperty(PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64, HexUtils.encodeBase64(ciphertext));
        props.setProperty(PROP_RSA_CIPHER_NAME, "RSA/ECB/PKCS1Padding");
        return props;
    }

    private byte[] getKeyStoreBytes() throws Exception {
        KeyStore ks = getKeyStore();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, kspass);
        return baos.toByteArray();
    }

    private KeyStore getKeyStore() throws Exception {
        if (keyStore != null)
            return keyStore;
        keyStore = KeyStore.getInstance("JKS"); // Use JKS for test rather than PKCS#12 to ensure handling of per-entry passwords is correct
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, getPrivateKey(), entrypass, new X509Certificate[] { getCert() });
        return keyStore;
    }

    private PrivateKey getPrivateKey() throws Exception {
        return privateKey != null ? privateKey : (privateKey =
                KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(HexUtils.decodeBase64(RSA_1024_KEY_PKCS8_B64, true)))
        );
    }

    private X509Certificate getCert() throws Exception {
        return cert != null ? cert : (cert =
                (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(HexUtils.decodeBase64(RSA_1024_CERT_X509_B64)))
        );
    }

    public static final String RSA_1024_CERT_X509_B64 =
        "MIICFDCCAX2gAwIBAgIJANA/LVIWYlZMMA0GCSqGSIb3DQEBDAUAMBgxFjAUBgNVBAMMDXRlc3Rf" +
        "cnNhXzEwMjQwHhcNMDkxMjE2MjM0MTE4WhcNMzQxMjEwMjM0MTE4WjAYMRYwFAYDVQQDDA10ZXN0" +
        "X3JzYV8xMDI0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCDh9hs9BnqyPvL7qoHARHjKwq9" +
        "ZwGeeWDU+oed9H4/Qjnw5PZ54ZgXfU+pEisDxADHfvHXnMrUNuSOXNaH1Lyg+EjOWwQVRW7EbQFK" +
        "paMP6d6FLy70/ErA616i1dPE+gmdtQEZiAqoe+5gch0oZVVu5V6cREFcjzVSv3K5Uo5PhQIDAQAB" +
        "o2YwZDAOBgNVHQ8BAf8EBAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQU3bG81B25" +
        "MHuoBRi9apZWR2bVqHMwHwYDVR0jBBgwFoAU3bG81B25MHuoBRi9apZWR2bVqHMwDQYJKoZIhvcN" +
        "AQEMBQADgYEADeL5oHQBkkqkojQ+GQBFOpYuDq6yi4QkAe1CKlt4ieXczmoPd1NmhWY8U+AyORdu" +
        "9I8H+N/OAwfCHNqS9a7xBjd55gObOJ1ZDYJEVXSJ/gx0vRwm166BY5A6hF/7F24Me5ItDiwQbK1c" +
        "J7t7E2C6q1B2qkLUujTACbCAyCpv5B4=";

    public static final String RSA_1024_KEY_PKCS8_B64 =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIOH2Gz0GerI+8vuqgcBEeMrCr1n" +
        "AZ55YNT6h530fj9COfDk9nnhmBd9T6kSKwPEAMd+8decytQ25I5c1ofUvKD4SM5bBBVFbsRtAUql" +
        "ow/p3oUvLvT8SsDrXqLV08T6CZ21ARmICqh77mByHShlVW7lXpxEQVyPNVK/crlSjk+FAgMBAAEC" +
        "gYAE60ix0nNBr6CTIOrk9ipIF60AJmEOHzX64R+/TYyHKx/lnXqGVmSMxFf9V7uaGXN6Aopi6O9A" +
        "/oiPtnMjg1ZGlP7ONFyaf0ZsaMs4jm7FAfDHtnaemEJkDEadYSvppN8oB1bPm1NYe6mAvaui3PiM" +
        "EGkkq+MSgms5j8RFHyUvBQJBANYoloJJ6hfhRGJiTSyP1TRYoKHrf2mVFnEyHxALhK32+TnasKZI" +
        "CkVLcAPhqfnNQKwn3nATfc0ZXeI9BMTwHNMCQQCdOoSZCNUFjslXOC1zS+XtoiE1znjtlIYqGR+h" +
        "TLWdJQkdyoB5ATOtL3uj+7muwMmKc7rmsW6imAqxxwDBmctHAkABMVatQRYhreqAlcWSQvbQBNJY" +
        "NISQJPlsBfhwUXAau+5laRdkxa/w9NuZ2e7lakQ68Tnm6+TeeI6yTN6y7hdrAkBmgWtHdomjSPcd" +
        "RQPkwlvSNLygHs+aXRWnRp/ngmJ5ZFbwNEDUIyN0yps6SvhA5XHAMTluA8nUeXmnc82batArAkEA" +
        "gW39CjSVbeUpgwEpt2CAz0qa08IQ56clJcyCHiwLQelfezLrQkwX+k6Lkf11JxOsf9a4A2ToElml" +
        "KyZu+sPE/g==";
}
