package com.l7tech.server.config.beans;

import com.l7tech.server.config.KeystoreType;
import com.l7tech.server.config.db.DBInformation;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Aug 17, 2005
 */
public class KeystoreConfigBean extends BaseConfigurationBean {
    private static final Logger logger = Logger.getLogger(KeystoreConfigBean.class.getName());

    private final static String NAME = "Keystore Configuration";
    private final static String DESCRIPTION = "Configures the keystore for the SSG";

    private char[] ksPassword;
    private boolean doBothKeys;
    private String hostname;
    private KeyStore.PrivateKeyEntry importedSslKey;

    private static final String DO_BOTH_KEYS_INFO = "Creating CA and SSL keys";
    private static final String SKIP_CA_KEY_INFO = "Skipping CA keys creation";
    private static final String GEN_CA_IMPORT_SSL = "Generating new CA key and importing SSL key: ";
    private static final String SKIP_CA_IMPORT_SSL = "Skipping CA keys creation and importing SSL key: ";
    private static final String USING_HOSTNAME_INFO = "Generating keys using hostname: ";
    private static final String SKIPPING_KEYSTORE_CONFIG_INFO = "Skipping keystore configuration";

    private String lunaJspPath;
    private String lunaInstallationPath;
    private boolean doKeystoreConfig;
    private KeystoreType keyStoreType;
    private String keystoreTypeName;
    private boolean initializeHSM;

    private byte[] clusterSharedKey;
    private byte[] sharedKeyData;
    private boolean shouldBackupMasterKey;
    private char[] masterKeyBackupPassword;

    private DBInformation dbInformation;

    public KeystoreConfigBean() {
        super(NAME, DESCRIPTION);
        init();
    }

    private void init() {
    }

    public void reset() {
    }

    public KeystoreType getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(KeystoreType keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeystoreTypeName() {
        return this.keystoreTypeName;
    }

    public void setKeystoreTypeName(String keyStoreTypeName) {
        this.keystoreTypeName = keyStoreTypeName;
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (isDoKeystoreConfig()) {
            KeystoreType type = getKeyStoreType();
            if (type == KeystoreType.DEFAULT_KEYSTORE_NAME || type == KeystoreType.LUNA_KEYSTORE_NAME) {
                explanations.add(insertTab + "Create " + getKeyStoreType());
                if (type == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                    if (getImportedSslKey() != null) {
                        String exp = isDoBothKeys() ? GEN_CA_IMPORT_SSL : SKIP_CA_IMPORT_SSL;
                        exp += ((X509Certificate)getImportedSslKey().getCertificate()).getSubjectDN().getName();
                        explanations.add(insertTab + exp);
                    } else if (isDoBothKeys()) {
                        explanations.add(insertTab + DO_BOTH_KEYS_INFO);
                    } else {
                        explanations.add(insertTab + SKIP_CA_KEY_INFO);
                    }
                }
            } else if (type == KeystoreType.SCA6000_KEYSTORE_NAME) {
                if (isInitializeHSM()) {
                    explanations.add(insertTab + "Initialize " + getKeyStoreType());
                    if (isShouldBackupMasterKey()) {
                        explanations.add(insertTab + "Backup Master HSM Key");
                    }
                } else {
                    explanations.add(insertTab + "Restore " + getKeyStoreType());
                }
            }

            explanations.add(insertTab + USING_HOSTNAME_INFO + getHostname());
        }
        else {
            explanations.add(insertTab + SKIPPING_KEYSTORE_CONFIG_INFO);
        }
    }

    public char[] getKsPassword() {
        return ksPassword;
    }

    public void setKsPassword(char[] ksPassword) {
        this.ksPassword = ksPassword;
    }

    public boolean isDoBothKeys() {
        return doBothKeys;
    }

    public void setDoBothKeys(boolean doBothKeys) {
        this.doBothKeys = doBothKeys;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getLunaJspPath() {
        return lunaJspPath;
    }

    public void setLunaJspPath(String lunaJspPath) {
        this.lunaJspPath = lunaJspPath;
    }

    public String getLunaInstallationPath() {
        return lunaInstallationPath;
    }

    public void setLunaInstallationPath(String lunaInstallationPath) {
        this.lunaInstallationPath = lunaInstallationPath;
        if (!this.lunaInstallationPath.endsWith("/")) {
            this.lunaInstallationPath = this.lunaInstallationPath + "/";
        }
    }

    public boolean isDoKeystoreConfig() {
        return doKeystoreConfig;
    }

    public void setDoKeystoreConfig(boolean doKeystoreConfig) {
        this.doKeystoreConfig = doKeystoreConfig;
    }

    public void setInitializeHSM(boolean shouldInitialise) {
        initializeHSM = shouldInitialise;
    }

    public boolean isInitializeHSM() {
        return initializeHSM;
    }

    public byte[] getClusterSharedKey() {
        return clusterSharedKey;
    }

    public void setClusterSharedKey(byte[] clusterSharedKey) {
        this.clusterSharedKey = clusterSharedKey;
    }

    public void setSharedKeyBytes(byte[] sharedKeyData) {
        this.sharedKeyData = sharedKeyData;
    }

    public byte[] getSharedKeyData() {
        return sharedKeyData;
    }

    public boolean isShouldBackupMasterKey() {
        return shouldBackupMasterKey;
    }

    public void setShouldBackupMasterKey(boolean shouldBackupMasterKey) {
        this.shouldBackupMasterKey = shouldBackupMasterKey;
    }

    public char[] getMasterKeyBackupPassword() {
        return masterKeyBackupPassword;
    }

    public void setMasterKeyBackupPassword(char[] masterKeyBackupPassword) {
        this.masterKeyBackupPassword = masterKeyBackupPassword;
    }

    public DBInformation getDbInformation() {
        return dbInformation;
    }

    public void setDbInformation(DBInformation dbInformation) {
        this.dbInformation = dbInformation;
    }

    public KeyStore.PrivateKeyEntry getImportedSslKey() {
        return importedSslKey;
    }

    public void setImportedSslKey(KeyStore.PrivateKeyEntry importedSslKey) {
        this.importedSslKey = importedSslKey;
    }
}
