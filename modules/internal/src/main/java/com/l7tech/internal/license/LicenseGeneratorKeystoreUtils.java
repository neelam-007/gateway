package com.l7tech.internal.license;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * @author jwilliams@layer7tech.com
 */
public class LicenseGeneratorKeystoreUtils {

    public static final String PROPERTY_KEYSTORE_PATH = "licenseGenerator.keystorePath";
    public static final String PROPERTY_KEYSTORE_PASSWORD = "licenseGenerator.keystorePass";
    public static final String PROPERTY_KEYSTORE_TYPE = "licenseGenerator.keystoreType";
    public static final String PROPERTY_KEYSTORE_ALIAS = "licenseGenerator.keystoreAlias";
    public static final String PROPERTY_KEYSTORE_ALIAS_PASSWORD = "licenseGenerator.keystoreAliasPass";
    public static final String KEYSTORE_MESSAGE = "No keystore is configured.  Please set these system properties:\n" +
            "\n  " + PROPERTY_KEYSTORE_PATH +
            "\n  " + PROPERTY_KEYSTORE_PASSWORD +
            "\n  " + PROPERTY_KEYSTORE_ALIAS +
            "\n  " + PROPERTY_KEYSTORE_TYPE;

    public static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";

    public static KeyStore loadKeyStore() throws GeneralSecurityException, IOException
    {
        String storeType = SyspropUtil.getString(PROPERTY_KEYSTORE_TYPE, DEFAULT_KEYSTORE_TYPE);
        if (storeType == null || storeType.length() < 1) storeType = DEFAULT_KEYSTORE_TYPE; // not needed

        String storePath = SyspropUtil.getProperty(PROPERTY_KEYSTORE_PATH);
        if (storePath == null || storePath.length() < 1)
            throw new RuntimeException(KEYSTORE_MESSAGE);

        String storePass = SyspropUtil.getString(PROPERTY_KEYSTORE_PASSWORD, "");

        KeyStore ks = KeyStore.getInstance(storeType);

        final FileInputStream fis = new FileInputStream(storePath);

        try {
            ks.load(fis, storePass.toCharArray());
        } catch (IOException e) {
            throw (IOException) new IOException("Unable to load key store " +
                    storePath + ": " + ExceptionUtils.getMessage(e)).initCause(e);
        } finally {
            //noinspection EmptyCatchBlock
            try { fis.close(); } catch (IOException e) {}
        }

        return ks;
    }

    private static String getAlias(KeyStore keyStore, String storePath) throws KeyStoreException {
        String alias = SyspropUtil.getProperty(PROPERTY_KEYSTORE_ALIAS);

        if (alias == null || alias.length() < 1) {
            Enumeration aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String a = (String) aliases.nextElement();
                if (alias == null) {
                    if (keyStore.isKeyEntry(a))
                        alias = a;
                }
            }
        }

        if (alias == null) {
            throw new KeyStoreException("No alias configured: please set the system property " + PROPERTY_KEYSTORE_ALIAS +
                    "\nto the alias of the signing cert (using key store: " + storePath + ")");
        }

        return alias;
    }

    public static X509Certificate getSignerCert(KeyStore keyStore) throws GeneralSecurityException, IOException
    {
        String storePath = SyspropUtil.getProperty(PROPERTY_KEYSTORE_PATH);
        String alias = getAlias(keyStore, storePath);

        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null || chain.length < 1 || chain[0] == null)
            throw new KeyStoreException("The alias " + alias + " has no certificate chain (using key store: " + storePath + ".");

        if (!(chain[0] instanceof X509Certificate))
            throw new KeyStoreException("The alias " + alias + " certificate isn't X.509 (using key store: " + storePath + ".");

        return (X509Certificate) chain[0];
    }

    public static PrivateKey getSignerKey(KeyStore keyStore) throws GeneralSecurityException, IOException
    {
        String storePath = SyspropUtil.getProperty(PROPERTY_KEYSTORE_PATH);
        String alias = getAlias(keyStore, storePath);
        String storePass = SyspropUtil.getString(PROPERTY_KEYSTORE_PASSWORD, "");
        String keyPass = SyspropUtil.getString(PROPERTY_KEYSTORE_ALIAS_PASSWORD, storePass);

        Key key = keyStore.getKey(alias, keyPass.toCharArray());

        if (key == null)
            throw new KeyStoreException("The alias " + alias + " was not found in the key store " + storePath + ".");

        if (!(key instanceof PrivateKey))
            throw new KeyStoreException("The alias " + alias + " is not a private key (using key store: " + storePath + ".");

        return (PrivateKey) key;
    }
}
