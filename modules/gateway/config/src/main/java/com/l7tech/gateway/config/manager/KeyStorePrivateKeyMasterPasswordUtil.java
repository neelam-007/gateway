package com.l7tech.gateway.config.manager;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertificateGeneratorException;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.KeyStorePrivateKeyMasterPasswordFinder;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extends KeyStorePrivateKeyMasterPasswordFinder to add the ability to edit the configuration.
 */
public class KeyStorePrivateKeyMasterPasswordUtil extends KeyStorePrivateKeyMasterPasswordFinder {
    private static final Logger logger = Logger.getLogger(KeyStorePrivateKeyMasterPasswordUtil.class.getName());

    public KeyStorePrivateKeyMasterPasswordUtil(File path) throws IOException {
        super(path);
    }

    protected KeyStorePrivateKeyMasterPasswordUtil(File path, Properties properties) throws IOException {
        super(path, properties);
    }

    /**
     * Generate a new master password (and/or keystore and/or keypair if necessary), and encrypt it using the current properties.
     * The modified properties are not saved to disk -- to cause this to happen, call {@link #saveProperties()}.
     * <p/>
     * If the current properties contain a non-empty {@link #PROP_KEYSTORE_CONTENTS_BASE64} value, the new keystore
     * data will be saved there.  Otherwise, it will be saved to the currently-set keystore file path.
     * <p/>
     * The replacement of the properties file will use the FileUtils saveFileSafely protocol.
     * <p/>
     * The caller is responsible for dealing with the implications of setting a new master password.  Most obviously,
     * any passwords encrypted using the old master password will need to be reencryped with the new one or else
     * reentered.
     * <p/>
     * If the keystore currently does not exist, this method will create a new keystore.
     * <p/>
     * If the keystore currently does not contain a private key entry with the expected alias, this method
     * will generate one (with a new keypair and self-signed cert).
     *
     * @throws Exception if an error occurs while generating and saving the new master password.
     */
    public void generateNewMasterPassword() throws GeneralSecurityException, IOException {
        init();

        final PublicKey publicKey = findOrCreatePublicKey();

        final byte[] masterPasswordPlaintextBytes;
        try {
            masterPasswordPlaintextBytes = newMasterPasswordBytes();
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to generate new master passphrase bytes: " + ExceptionUtils.getMessage(e), e);
        }

        final String masterPasswordCiphertextBase64;
        try {
            Cipher rsa = newRsaCipher();
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] masterPasswordCiphertextBytes = rsa.doFinal(masterPasswordPlaintextBytes);
            masterPasswordCiphertextBase64 = HexUtils.encodeBase64(masterPasswordCiphertextBytes, true);
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to encrypt new master passphrase: " + ExceptionUtils.getMessage(e), e);
        }

        properties.remove(PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64);
        origProperties.setProperty(PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64, masterPasswordCiphertextBase64);

        saveProperties();
    }

    void init() throws GeneralSecurityException {
        maybeSetSystemProperties();
        maybeInstallProviders();
    }

    /**
     * Save current properties back into place over top of the existing kmp.properties file (using FileUtils.saveFileSafely).
     *
     * @throws IOException if the save fails.
     */
    public void saveProperties() throws IOException {
        FileUtils.saveFileSafely(findPropertiesFile(dir).getAbsolutePath(), true, new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                origProperties.store(fos, "Edited by KeyStorePrivateKeyMasterPassWordUtil at " + System.currentTimeMillis());
            }
        });
    }

    private PublicKey findOrCreatePublicKey() throws GeneralSecurityException {
        KeyStore keyStore = findOrCreateKeyStore();

        final PublicKey publicKey;
        final String alias = findKeyStoreAlias();
        final char[] entryPass = findKeyStoreEntryPassword();
        if (isValidPrivateKeyEntry(keyStore, alias, entryPass)) {
            publicKey = keyStore.getCertificateChain(alias)[0].getPublicKey();
        } else {
            publicKey = createNewKeyEntry(keyStore, alias, entryPass);
            storeKeyStore(keyStore);
        }
        return publicKey;
    }

    /**
     * Store the keystore to whereever it shall be persisted.
     * <p/>
     * Currently this always stores the keystore to the properties, as {@link #PROP_KEYSTORE_CONTENTS_BASE64}.
     * Caller is responsible for ensuring that the properties eventually get saved back out.
     * @param keyStore the key store to persist.  Required.
     * @throws java.security.GeneralSecurityException if there is an error while accessing the keystore.
     */
    private void storeKeyStore(KeyStore keyStore) throws GeneralSecurityException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            keyStore.store(baos, findKeyStorePassword());
        } catch (IOException e) {
            // Can't happen
            throw new GeneralSecurityException("Unable to store keystore: " + ExceptionUtils.getMessage(e), e);
        }
        String keystoreb64 = HexUtils.encodeBase64(baos.toByteArray(), true);
        properties.remove(PROP_KEYSTORE_CONTENTS_BASE64);
        origProperties.setProperty(PROP_KEYSTORE_CONTENTS_BASE64, keystoreb64);
    }

    private KeyStore findOrCreateKeyStore() throws GeneralSecurityException {
        KeyStore keyStore = tryLoadExistingKeyStore();
        if (keyStore == null) {
            keyStore = createEmptyKeyStore();
        }
        return keyStore;
    }

    /**
     * Generate a new keypair and self-signed certificate and save it into the specified keystore under the
     * specified alias.
     * <p/>
     * This method will use the currently configured secure random provider for generating the keypair.
     * It assumes the currently-configured RSA Cipher provider also provides an RSA KeyPairGenerator (and
     * will fail if this is not the case).
     * <p/>
     * Caller remains responsible for persisting the keystore itself.
     *
     * @param keyStore the keystore in which to save the new entry.  Required.
     * @param alias  alias to use for new entry.  Required.
     * @param entryPass  entry-specific passphrase for the new keystore entry, or null to pass null.
     * @return the public key from the new self-signed certificate.  Never null.
     * @throws GeneralSecurityException if there is a problem generating the new key entry.
     */
    public PublicKey createNewKeyEntry(KeyStore keyStore, String alias, char[] entryPass) throws GeneralSecurityException {
        final SecureRandom secureRandom;
        final Provider rsaProvider;
        final String rsaProvName;
        try {
            secureRandom = newSecureRandom();
            rsaProvider = findRsaProvider();
            rsaProvName = rsaProvider == null ? null : rsaProvider.getName();
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to generate new master passphrase keypair: " + ExceptionUtils.getMessage(e), e);
        }

        // Create new keypair
        final KeyPair keyPair;
            final KeyPairGenerator kpg = rsaProvider == null ? KeyPairGenerator.getInstance("RSA") : KeyPairGenerator.getInstance("RSA", rsaProvider);
            kpg.initialize(intprop(PROP_PRIVATE_KEY_SIZE, DEFAULT_PRIVATE_KEY_SIZE));
            keyPair = kpg.generateKeyPair();

        // Create new cert
        final X509Certificate cert;
        try {
            CertGenParams certGenParams = new CertGenParams();
            certGenParams.disableAllExtensions();
            certGenParams.useUserCertDefaults();
            certGenParams.setSubjectDn(new X500Principal("cn=" + alias.replaceAll("[^a-zA-Z0-9]", "_")));
            cert = new ParamsCertificateGenerator(certGenParams, secureRandom, rsaProvName).generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), null);
        } catch (CertificateGeneratorException e) {
            throw new GeneralSecurityException("Unable to generate new master passphrase certificate: " + ExceptionUtils.getMessage(e), e);
        }

        keyStore.setKeyEntry(alias, keyPair.getPrivate(), entryPass, new X509Certificate[] { cert });
        return keyPair.getPublic();
    }

    public KeyStore getExistingKeyStore() throws Exception {
        init();
        return tryLoadExistingKeyStore();
    }

    private boolean isValidPrivateKeyEntry(KeyStore keyStore, String alias, final char[] entryPassword) {
        try {
            // Ensure the keystore contains a key entry with this alias that contains a non-empty cert
            // chain with a subject cert that uses the same algorithm.
            if (!keyStore.containsAlias(alias) || !keyStore.isKeyEntry(alias)) {
                logger.info("Master passphrase keystore does not contain any entry with alias '" + alias + "'");
                return false;
            }
            Key key = keyStore.getKey(alias, entryPassword);
            if (!(key instanceof PrivateKey)) {
                logger.info("Master passphrase keystore entry is not a PrivateKey");
                return false;
            }
            if (!"RSA".equals(key.getAlgorithm())) {
                logger.info("Master passphrase keystore entry is not an RSA key");
                return false;
            }
            Certificate[] cert = keyStore.getCertificateChain(alias);
            if (cert == null || cert.length < 1 || cert[0] == null) {
                logger.info("Master passphrase keystore entry does not include a subject certificate");
                return false;
            }
            PublicKey publicKey = cert[0].getPublicKey();
            String publicAlg = publicKey.getAlgorithm();
            return publicAlg != null && publicAlg.equals(key.getAlgorithm());

        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "Unable to check for existing master passphrase private key entry: " + ExceptionUtils.getMessage(e), e);
            return false;
        } catch (UnrecoverableKeyException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "Unable to access existing master passphrase private key entry: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        } catch (NoSuchAlgorithmException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "Unable to make use of existing master passphrase private key entry: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    protected KeyStore tryLoadExistingKeyStore() throws GeneralSecurityException {
        try {
            // Try to use existing keystore
            KeyStore keyStore = newKeyStore();
            keyStore.load(newKeyStoreInputStream(), findKeyStorePassword());
            return keyStore;
        } catch (FileNotFoundException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINE, "No existing keystore -- will create new one: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new GeneralSecurityException("Unable to load existing master passphrase keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected KeyStore createEmptyKeyStore() throws GeneralSecurityException {
        try {
            // Create new keystore
            KeyStore keyStore = newKeyStore();
            keyStore.load(null, findKeyStoreCreationPassword());
            return keyStore;
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to create new master passphrase keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Generate a new master password byte array using the length and SecureRandom specified in the
     * current properties.
     *
     * @return a new byte array.  Never null.
     * @throws Exception if SecureRandom could not be instantiated with the specified provider or algorithm name.
     */
    public byte[] newMasterPasswordBytes() throws Exception {
        int size = intprop(PROP_MASTER_PASSPHRASE_LENGTH, DEFAULT_MASTER_PASSPHRASE_LENGTH);
        byte[] ret = new byte[size];
        newSecureRandom().nextBytes(ret);
        return ret;
    }

    protected SecureRandom newSecureRandom() throws Exception {
        String randomName = prop(PROP_SECURERANDOM_NAME);
        if (randomName.length() < 1)
            return new SecureRandom();

        Provider provider = findSecureRandomProvider();
        return provider == null ? SecureRandom.getInstance(randomName) : SecureRandom.getInstance(randomName, provider);
    }

    protected char[] findKeyStoreCreationPassword() {
        char[] pass = prop(PROP_KEYSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD).toCharArray();
        if (pass == null || pass.length < 1)
            pass = prop(PROP_KEYSTORE_CREATION_PASSWORD, DEFAULT_KEYSTORE_CREATION_PASSWORD).toCharArray();
        return specialPass(pass);
    }

    protected int intprop(String propname, int defaultValue) {
        try {
            return Integer.parseInt(prop(propname, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected Provider findSecureRandomProvider() throws Exception {
        return findProvider(PROP_SECURERANDOM_PROVIDER_CLASSNAME);
    }
}
