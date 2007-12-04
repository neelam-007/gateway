package com.l7tech.server.config.beans;

import com.l7tech.server.config.KeystoreType;
import com.l7tech.server.config.SharedWizardInfo;

/**
 * User: megery
 * Date: Aug 17, 2005
 */
public class KeystoreConfigBean extends BaseConfigurationBean {
    public static final String MASTERKEY_MANAGE_SCRIPT = "appliance/libexec/masterkey-manage.pl";

    private final static String NAME = "Keystore Configuration";
    private final static String DESCRIPTION = "Configures the keystore for the SSG";

    private char[] ksPassword;
    private boolean doBothKeys;
    private String hostname;

    private static final String DO_BOTH_KEYS_INFO = "Creating CA and SSL keys";
    private static final String SKIP_CA_KEY_INFO = "Skipping CA keys creation";
    private static final String USING_HOSTNAME_INFO = "Generating keys using hostname: ";
    private static final String SKIPPING_KEYSTORE_CONFIG_INFO = "Skipping keystore configuration";

    private String lunaJspPath;
    private String lunaInstallationPath;
    private boolean doKeystoreConfig;
    private KeystoreType keyStoreType;
    private boolean initializeHSM;
    private SharedWizardInfo sharedWizardInfo;

    private byte[] clusterSharedKey;
    private byte[] sharedKeyData;
    private boolean shouldBackupMasterKey;
    private char[] masterKeyBackupPassword;

    public static final String[] DEFAULT_SECURITY_PROVIDERS =
            {
                "sun.security.provider.Sun",
                "sun.security.rsa.SunRsaSign",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE",
                "sun.security.jgss.SunProvider",
                "com.sun.security.sasl.Provider"
            };

    public static final String PKCS11_CFG_FILE = "/ssg/appliance/pkcs11.cfg";
    public static final String[] HSM_SECURITY_PROVIDERS =
            {
                "sun.security.pkcs11.SunPKCS11 " + PKCS11_CFG_FILE,
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE"
            };


    public KeystoreType getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(KeystoreType keyStoreType) {
        this.keyStoreType = keyStoreType;
        sharedWizardInfo.setKeystoreType(keyStoreType);
    }

    public KeystoreConfigBean() {
        super(NAME, DESCRIPTION);
        init();
    }

    private void init() {
        ELEMENT_KEY = this.getClass().getName();
        sharedWizardInfo = SharedWizardInfo.getInstance();
    }

    public void reset() {
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (isDoKeystoreConfig()) {
            KeystoreType type = getKeyStoreType();
            if (type == KeystoreType.DEFAULT_KEYSTORE_NAME || type == KeystoreType.LUNA_KEYSTORE_NAME) {
                explanations.add(insertTab + "Create " + getKeyStoreType());
                if (type == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                    if (isDoBothKeys()) {
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

    public void doKeystoreConfig(boolean b) {
        this.doKeystoreConfig = b;
    }

    public boolean isDoKeystoreConfig() {
        return doKeystoreConfig;
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

    public void setShouldBackupMasterKey(boolean b) {
        shouldBackupMasterKey = b;
    }


    public boolean isShouldBackupMasterKey() {
        return shouldBackupMasterKey;
    }

    public void setMasterKeyBackupPassword(char[] backupPassword) {
        this.masterKeyBackupPassword = backupPassword;
    }


    public char[] getMasterKeyBackupPassword() {
        return masterKeyBackupPassword;
    }
}
