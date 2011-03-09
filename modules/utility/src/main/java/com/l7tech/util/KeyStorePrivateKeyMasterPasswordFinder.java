package com.l7tech.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Properties;

/**
 * A MasterPasswordFinder that keeps a master password in a properties file encrypted for a private key
 * stored in a keystore.
 */
public class KeyStorePrivateKeyMasterPasswordFinder implements MasterPasswordManager.MasterPasswordFinder {
    public static final String DEFAULT_PROPS_FILE_PATH = "kmp.properties";
    public static final String SYSPROP_PROPS_FILE_PATH = "com.l7tech.kmp.properties";

    protected static final String DEFAULT_KEYSTORE_PATH = "kmp.keystore";
    protected static final String PROP_KEYSTORE_PATH = "keystore.path";
    protected static final String PROP_KEYSTORE_TYPE = "keystore.type";
    protected static final String PROP_KEYSTORE_PROVIDER_CLASSNAME = "keystore.provider.classname";
    protected static final String PROP_KEYSTORE_CONTENTS_BASE64 = "keystore.contents.base64";
    protected static final String DEFAULT_KEYSTORE_ALIAS = "masterkey";
    protected static final String PROP_KEYSTORE_ALIAS = "keystore.alias";
    protected static final String DEFAULT_KEYSTORE_PASSWORD = "NULL";
    protected static final String PROP_KEYSTORE_PASSWORD = "keystore.password";
    protected static final String DEFAULT_KEYSTORE_CREATION_PASSWORD = ""; // default to same as keystore password
    protected static final String PROP_KEYSTORE_CREATION_PASSWORD = "keystore.creation.password";
    protected static final String PROP_KEYSTORE_ENTRY_PASSWORD = "keystore.entry.password";
    protected static final String PROP_NCIPHER_PROTECT = "ncipher.protect";
    protected static final String PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64 = "master.passphrase.ciphertext.base64";
    protected static final String PROP_MASTER_PASSPHRASE_LENGTH = "master.passphrase.length"; // length in bytes for when generating a new master passphrase byte array

    protected static final String DEFAULT_RSA_CIPHER_NAME = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    protected static final String PROP_RSA_CIPHER_NAME = "rsa.cipher.name";
    protected static final String PROP_RSA_PROVIDER_CLASSNAME = "rsa.provider.classname";

    // Properties used only by our subclass that creates the keystore, KeyStorePrivateKeyMasterPasswordUtil 
    protected static final String PROP_SECURERANDOM_NAME = "securerandom.name";
    protected static final String PROP_SECURERANDOM_PROVIDER_CLASSNAME = "securerandom.provider.classname";

    /** Provider to use when no more-specific provider is specified. */
    protected static final String PROP_PROVIDER = "provider";

    protected final File dir;
    protected final Properties properties;

    public KeyStorePrivateKeyMasterPasswordFinder(File path) throws IOException {
        this.dir = path.isDirectory() ? path : path.getParentFile();
        this.properties = loadProperties(findPropertiesFile(dir));
    }

    /**
     * Figure out where "kmp.properties" would be, give then current setting of the "com.l7tech.kmp.properties" system
     * property and assuming relative paths should be resolved relative to the specified directory.
     *
     * @param dir the directory expected to contain kmp.properties (or a file within this directory).
     * @return a File pointing at kmp.properties (though not it may not actually exist).  Never null.
     */
    public static File findPropertiesFile(File dir) {
        dir = dir.isDirectory() ? dir : dir.getParentFile();
        return asAbsolute(dir, SyspropUtil.getString(SYSPROP_PROPS_FILE_PATH, DEFAULT_PROPS_FILE_PATH));
    }

    protected KeyStorePrivateKeyMasterPasswordFinder(File path, Properties properties) throws IOException {
        this.dir = path.isDirectory() ? path : path.getParentFile();
        this.properties = properties;
    }

    @Override
    public byte[] findMasterPasswordBytes() {
        InputStream is = null;
        try {
            maybeSetSystemProperties();
            KeyStore keyStore = newKeyStore();
            keyStore.load(is = newKeyStoreInputStream(), findKeyStorePassword());
            final String alias = findKeyStoreAlias();
            Key key = keyStore.getKey(alias, findKeyStoreEntryPassword());
            if (key == null)
                throw new RuntimeException("Unable to decrypt master passphrase: no such alias '" + alias + "' in kmp keystore");

            Cipher cipher = newRsaOaepCipher();
            cipher.init(Cipher.DECRYPT_MODE, key, newOaepParameterSpec());
            return cipher.doFinal(findMasterPassphraseCiphertextBytes());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt master passphrase: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    protected void maybeSetSystemProperties() {
        // This is a hack, but it prevents these system properties from needing to be added to the clutter in the Gateway's already-enormous command line
        String protect = prop(PROP_NCIPHER_PROTECT);
        if (protect.length() > 0 && !protect.equals(SyspropUtil.getProperty("protect")))
            SyspropUtil.setProperty("protect", protect);
    }

    protected KeyStore newKeyStore() throws Exception {
        String storetype = prop(PROP_KEYSTORE_TYPE, KeyStore.getDefaultType());
        Provider provider = findKeyStoreProvider();
        return provider == null ? KeyStore.getInstance(storetype) : KeyStore.getInstance(storetype, provider);
    }

    protected InputStream newKeyStoreInputStream() throws IOException {
        String keystoreContentsB64 = prop(PROP_KEYSTORE_CONTENTS_BASE64);
        if (keystoreContentsB64.equals("UNSET"))
            throw new FileNotFoundException("Keystore contents not yet set");
        if (keystoreContentsB64.length() > 0)
            return new ByteArrayInputStream(HexUtils.decodeBase64(keystoreContentsB64));
        return FileUtils.loadFileSafely(findKeyStoreFile().getAbsolutePath());
    }

    protected Cipher newRsaOaepCipher() throws Exception {
        String cipherName = prop(PROP_RSA_CIPHER_NAME, DEFAULT_RSA_CIPHER_NAME);
        Provider provider = findRsaProvider();
        return provider == null ? Cipher.getInstance(cipherName) : Cipher.getInstance(cipherName, provider);
    }

    protected OAEPParameterSpec newOaepParameterSpec() {
        return new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
    }

    protected Provider findKeyStoreProvider() throws Exception {
        return findProvider(PROP_KEYSTORE_PROVIDER_CLASSNAME);
    }

    protected Provider findRsaProvider() throws Exception {
        return findProvider(PROP_RSA_PROVIDER_CLASSNAME);
    }

    protected Provider findProvider(String propname) throws Exception {
        String providerClassname = prop(propname);
        if (providerClassname.length() < 1)
            providerClassname = prop(PROP_PROVIDER);
        return providerClassname.length() < 1 ? null : (Provider) Class.forName(providerClassname).newInstance();
    }

    protected File findKeyStoreFile() {
        return asAbsolute(dir, prop(PROP_KEYSTORE_PATH, DEFAULT_KEYSTORE_PATH));
    }

    protected String findKeyStoreAlias() {
        return prop(PROP_KEYSTORE_ALIAS, DEFAULT_KEYSTORE_ALIAS);
    }

    protected char[] findKeyStorePassword() {
        return specialPass(prop(PROP_KEYSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD).toCharArray());
    }

    protected char[] findKeyStoreEntryPassword() {
        String pass = prop(PROP_KEYSTORE_ENTRY_PASSWORD);
        return pass.length() < 1 ? findKeyStorePassword() : specialPass(pass.toCharArray());
    }

    protected byte[] findMasterPassphraseCiphertextBytes() {
        String b64 = prop(PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64);
        if (b64.length() < 1)
            throw new RuntimeException("Unable to decrypt master passphrase: No " + PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64 + " specified in kmp properties");
        return HexUtils.decodeBase64(b64);
    }

    // Recognize special passwords "NULL" and "EMPTY" and translate them appropriately
    protected char[] specialPass(char[] pass) {
        if (Arrays.equals("NULL".toCharArray(), pass))
            return null;
        if (Arrays.equals("EMPTY".toCharArray(), pass))
            return new char[0];
        return pass;
    }

    // get property or default
    protected String prop(String propname, String defaultValue) {
        return properties.getProperty(propname, defaultValue);
    }

    // get property or empty string (never null)
    protected String prop(String propname) {
        return prop(propname, "").trim();
    }

    protected static File asAbsolute(File dir, String path) {
        File file = new File(path);
        return file.isAbsolute() ? file : new File(dir, file.getName());
    }

    protected static Properties loadProperties(File propsFile) throws IOException {
        Properties props;
        InputStream fis = null;
        try {
            props = new Properties();
            props.load(fis = FileUtils.loadFileSafely(propsFile.getAbsolutePath()));
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return props;
    }
}
