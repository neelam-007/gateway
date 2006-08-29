package com.l7tech.server.config.beans;

import com.l7tech.server.config.ClusteringType;
import com.l7tech.server.config.KeystoreType;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 1:39:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeystoreConfigBean extends BaseConfigurationBean {
    private final static String NAME = "Keystore Configuration";
    private final static String DESCRIPTION = "Configures the keystore for the SSG";

    private char[] ksPassword;
    boolean doBothKeys;
    String hostname;

    private static final String DO_BOTH_KEYS_INFO = "Creating CA and SSL keys";
    private static final String SKIP_CA_KEY_INFO = "Skipping CA keys creation";
    private static final String USING_HOSTNAME_INFO = "Generating keys using hostname: ";
    private static final String SKIPPING_KEYSTORE_CONFIG_INFO = "Skipping keystore configuration";

    private String lunaJspPath;
    private String lunaInstallationPath;
    private boolean doKeystoreConfig;
    private ClusteringType clusteringType;
    private KeystoreType keyStoreType;


    public KeystoreType getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(KeystoreType keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public KeystoreConfigBean() {
        super(NAME, DESCRIPTION);
        init();
    }

    private void init() {
        ELEMENT_KEY = this.getClass().getName();
    }

    public void reset() {
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (isDoKeystoreConfig()) {
            explanations.add(insertTab + "Create " + getKeyStoreType());
            if (getKeyStoreType() == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                if (isDoBothKeys()) {
                    explanations.add(insertTab + DO_BOTH_KEYS_INFO);
                } else {
                    explanations.add(insertTab + SKIP_CA_KEY_INFO);
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

    public void setClusterType(ClusteringType theClusteringType) {
        this.clusteringType = theClusteringType;
    }

    public ClusteringType getClusteringType() {
        return clusteringType;
    }
}
