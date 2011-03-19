package com.l7tech.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MasterPasswordFinder that keeps a master password in a properties file encrypted for a private key
 * stored in a keystore.
 */
public class KeyStorePrivateKeyMasterPasswordFinder implements MasterPasswordManager.MasterPasswordFinder {
    private static final Logger logger = Logger.getLogger(KeyStorePrivateKeyMasterPasswordFinder.class.getName());

    public static final String DEFAULT_PROPS_FILE_PATH = "kmp.properties";
    public static final String SYSPROP_PROPS_FILE_PATH = "com.l7tech.kmp.properties";

    public static final String PROP_PROFILE = "config.profile";
    public static final String DEFAULT_KEYSTORE_PATH = "kmp.keystore";
    public static final String PROP_KEYSTORE_PATH = "keystore.path";
    public static final String PROP_KEYSTORE_TYPE = "keystore.type";
    public static final String PROP_KEYSTORE_PROVIDER_CLASSNAME = "keystore.provider.classname";
    public static final String PROP_KEYSTORE_CONTENTS_BASE64 = "keystore.contents.base64";
    public static final String DEFAULT_KEYSTORE_ALIAS = "masterkey";
    public static final String PROP_KEYSTORE_ALIAS = "keystore.alias";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "NULL";
    public static final String PROP_KEYSTORE_PASSWORD = "keystore.password";
    public static final String PROP_KEYSTORE_ENTRY_PASSWORD = "keystore.entry.password";
    public static final String PROP_NCIPHER_PROTECT = "ncipher.protect";
    public static final String PROP_PREPEND_PROVIDERS = "prepend.providers";
    public static final String PROP_APPEND_PROVIDERS = "append.providers";
    public static final String PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64 = "master.passphrase.ciphertext.base64";

    public static final String DEFAULT_RSA_CIPHER_NAME = "RSA/ECB/PKCS1Padding";
    public static final String PROP_RSA_CIPHER_NAME = "rsa.cipher.name";
    public static final String PROP_RSA_PROVIDER_CLASSNAME = "rsa.provider.classname";

    // Properties used only by our subclass that creates the keystore, KeyStorePrivateKeyMasterPasswordUtil 
    public static final String DEFAULT_KEYSTORE_CREATION_PASSWORD = ""; // default to same as keystore password
    public static final String PROP_KEYSTORE_CREATION_PASSWORD = "keystore.creation.password";
    public static final String PROP_SECURERANDOM_NAME = "securerandom.name";
    public static final String PROP_SECURERANDOM_PROVIDER_CLASSNAME = "securerandom.provider.classname";
    public static final int DEFAULT_MASTER_PASSPHRASE_LENGTH = 32;
    public static final String PROP_MASTER_PASSPHRASE_LENGTH = "master.passphrase.length"; // length in bytes for when generating a new master passphrase byte array
    public static final int DEFAULT_PRIVATE_KEY_SIZE = 2048;
    public static final String PROP_PRIVATE_KEY_SIZE = "privatekey.bits"; // size in bits of RSA keypair to generate

    /** Provider to use when no more-specific provider is specified. */
    public static final String PROP_PROVIDER = "provider";

    static final String CONFIG_PROFILE_NCIPHER_SWORLD_RSA = "ncipher.sworld.rsa";
    private static final String PROV_NCIPHER_CLASSNAME = "com.ncipher.provider.km.nCipherKM";
    protected static final Map<String, String> CONFIG_PROFILE_NCIPHER_RSA;
    static {
        Map<String, String> p = new LinkedHashMap<String, String>();
        p.put(PROP_KEYSTORE_TYPE, "nCipher.sworld");
        p.put(PROP_PREPEND_PROVIDERS, PROV_NCIPHER_CLASSNAME);
        p.put(PROP_NCIPHER_PROTECT, "module");
        p.put(PROP_KEYSTORE_PASSWORD, "NULL");
        p.put(PROP_KEYSTORE_ENTRY_PASSWORD, "NULL");
        p.put(PROP_KEYSTORE_CREATION_PASSWORD, "NULL");
        p.put(PROP_RSA_CIPHER_NAME, "RSA/ECB/PKCS1Padding");
        //p.put(PROP_RSA_PROVIDER_CLASSNAME, PROV_NCIPHER_CLASSNAME);
        p.put(PROP_SECURERANDOM_NAME, "RNG");
        //p.put(PROP_SECURERANDOM_PROVIDER_CLASSNAME, PROV_NCIPHER_CLASSNAME);
        CONFIG_PROFILE_NCIPHER_RSA = p;
    }

    protected static final Map<String, Map<String,String>> CONFIG_PROFILES;
    static {
        Map<String, Map<String,String>> p = new HashMap<String, Map<String,String>>();
        p.put(CONFIG_PROFILE_NCIPHER_SWORLD_RSA, CONFIG_PROFILE_NCIPHER_RSA);
        CONFIG_PROFILES = p;
    }

    protected final File dir;
    protected final Properties origProperties; // Unexpected, as appears on disk, and as may be written back to disk by Util subclass
    protected final Properties properties;     // Expanded, after any config.profile is applied.

    private final AtomicReference<DecryptionBag> bagHolder = new AtomicReference<DecryptionBag>();

    /** Cache everything up to but not quite including the master passphrase plaintext since some providers are quite slow to initialize. */
    private static final class DecryptionBag {
        private DecryptionBag(PrivateKey privateKey, Cipher cipher) {
            this.privateKey = privateKey;
            this.cipher = cipher;
        }
        final PrivateKey privateKey;
        final Cipher cipher;

        synchronized byte[] decrypt(byte[] ciphertext) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(ciphertext);
        }
    }

    public KeyStorePrivateKeyMasterPasswordFinder(File path) throws IOException {
        this.dir = path.isDirectory() ? path : path.getParentFile();
        this.origProperties = loadProperties(findPropertiesFile(dir));
        this.properties = new Properties(origProperties);
        applyConfigProfile();
    }

    protected KeyStorePrivateKeyMasterPasswordFinder(File path, Properties properties) throws IOException {
        this.dir = path.isDirectory() ? path : path.getParentFile();
        this.origProperties = new Properties();
        origProperties.putAll(properties);
        this.properties = new Properties(origProperties);
        applyConfigProfile();
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


    @Override
    public byte[] findMasterPasswordBytes() {
        try {
            DecryptionBag bag = bagHolder.get();
            if (bag == null) {
                synchronized (bagHolder) {
                    bag = bagHolder.get();
                    if (bag == null) {
                        bag = createDecryptionBag();
                        bagHolder.set(bag);
                    }
                }
            }

            final byte[] ciphertextBytes = findMasterPassphraseCiphertextBytes();
            return bag.decrypt(ciphertextBytes);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt master passphrase: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private DecryptionBag createDecryptionBag() throws Exception {
        InputStream is = null;
        try {
            maybeSetSystemProperties();
            maybeInstallProviders();
            providerSanityCheck();
            KeyStore keyStore = newKeyStore();
            keyStore.load(is = newKeyStoreInputStream(), findKeyStorePassword());
            final String alias = findKeyStoreAlias();
            Key key = keyStore.getKey(alias, findKeyStoreEntryPassword());
            if (key == null)
                throw new RuntimeException("Unable to decrypt master passphrase: no such alias '" + alias + "' in kmp keystore");
            final Cipher cipher = newRsaCipher();
            return new DecryptionBag((PrivateKey)key, cipher);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    protected void applyConfigProfile() {
        String profileName = prop(PROP_PROFILE, "");
        if (profileName != null && profileName.length() > 0) {
            Map<String, String> configProps = CONFIG_PROFILES.get(profileName);
            if (configProps == null)
                throw new RuntimeException("Unknown kmp config profile name: " + profileName);
            for (Map.Entry<String, String> entry : configProps.entrySet()) {
                final String key = entry.getKey();
                if (!properties.containsKey(key) && !origProperties.containsKey(key)) {
                    properties.setProperty(key, entry.getValue());
                }
            }
        }
    }

    protected void maybeInstallProviders() throws GeneralSecurityException {
        installProviders(PROP_APPEND_PROVIDERS, false, false);
        installProviders(PROP_PREPEND_PROVIDERS, true, true);
    }

    private void installProviders(String propname, boolean removeExisting, boolean prepend) throws GeneralSecurityException {
        String newprovs = prop(propname, "");
        if (newprovs.length() < 1)
            return;

        providerSanityCheck();

        String[] provclasses = newprovs.split("\\s+,\\s+");
        for (String provclass : provclasses) {
            final Provider prov;
            try {
                prov = loadProvider(provclass);
            } catch (Exception e) {
                throw new GeneralSecurityException("Unable to instantiate security provider: " + ExceptionUtils.getMessage(e), e);
            }
            if (prov != null) {
                String provName = prov.getName();
                Provider existing = Security.getProvider(provName);
                if (existing != null && removeExisting) {
                    Security.removeProvider(provName);
                    existing = null;
                }

                if (existing == null) {
                    if (prepend)
                        Security.insertProviderAt(prov, 1);
                    else
                        Security.addProvider(prov);
                }
            }
        }
    }

    // Bug work-around hack: Force various security jarfiles to load under controlled conditions in the hope
    // of avoiding deadlock if provider classes need to be loaded, using the JarVerifier, while we are currently
    // attempting to retrieve a security implementation via something like Cipher.getInstance().
    private void providerSanityCheck() {
        try {
            MessageDigest.getInstance("SHA-1");
            Signature.getInstance("SHA1withRSA");
            Cipher.getInstance("RSA");
            Cipher.getInstance("AES");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Provider sanity check failed: " + ExceptionUtils.getMessage(e), e);
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
        return new FileInputStream(findKeyStoreFile());
    }

    protected Cipher newRsaCipher() throws Exception {
        String cipherName = prop(PROP_RSA_CIPHER_NAME, DEFAULT_RSA_CIPHER_NAME);
        Provider provider = findRsaProvider();
        return provider == null
                ? Cipher.getInstance(cipherName)
                : Cipher.getInstance(cipherName, provider);
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
        return loadProvider(providerClassname);
    }

    protected Provider loadProvider(String providerClassname) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return providerClassname.length() < 1 ? null : (Provider) Thread.currentThread().getContextClassLoader().loadClass(providerClassname).newInstance();
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
            props.load(fis = new FileInputStream(propsFile.getAbsolutePath()));
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return props;
    }
}
