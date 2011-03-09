package com.l7tech.gateway.config.manager;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.KeyStorePrivateKeyMasterPasswordFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
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
     * Generate a new master password, encrypt it, and save it, using the current properties, and overwriting the
     * properties file when finished.
     * <p/>
     * If the current properties contain a non-empty {@link #PROP_KEYSTORE_CONTENTS_BASE64} value, the new keystore
     * data will be saved there.  Otherwise, it will be saved to the currently-set keystore file path.
     * <p/>
     * The replacement of the properties file will use the FileUtils saveFileSafely protocol.
     * <p/>
     * The caller is responsible for dealing with the implications of setting a new master password.  Most obviously,
     * any passwords encrypted using the old master password will need to be reenctyped with the new one or else
     * reentered.
     * <p/>
     * If the keystore currently does not exist, this method will create a new keystore.
     * <p/>
     * If the keystore currently does not contain a private key entry with the expected alias, this method
     * will generate one (with a new keypair and self-signed cert).
     *
     * @throws Exception if an error occurs while generating and saving the new master password.
     */
    public void generateAndSaveNewMasterPassword() throws GeneralSecurityException, IOException {
        KeyStore keyStore = tryLoadExistingKeyStore();
        if (keyStore == null) {
            keyStore = createEmptyKeyStore();
        }

        String alias = findKeyStoreAlias();
        if (keyStore.containsAlias(alias) && keyStore.isKeyEntry(alias)) {

        } else {

        }


        byte[] masterPasswordBytes;
        try {
            masterPasswordBytes = newMasterPasswordBytes();
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to generate new master passphrase bytes: " + ExceptionUtils.getMessage(e), e);
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

    protected byte[] newMasterPasswordBytes() throws Exception {
        int size = intprop(PROP_MASTER_PASSPHRASE_LENGTH, 32);
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
