package com.l7tech.server.config.beans;

import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.OSSpecificFunctions;

import java.util.ArrayList;

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
    private static final String SKIP_CA_KEY_INFO = "Skipping CA leys creation";
    private static final String USING_HOSTNAME_INFO = "Using hostname: ";
    private static final String SKIPPING_KEYSTORE_CONFIG_INFO = "Skipping keystore configuration";

    private String lunaJspPath;
    private String lunaInstallationPath;
    private boolean doKeystoreConfig;
    private int clusteringType;


    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    private String keyStoreType;

    public KeystoreConfigBean(OSSpecificFunctions osFunctions) {
        super(NAME, DESCRIPTION, osFunctions);
        init();
    }

    private void init() {
        ELEMENT_KEY = this.getClass().getName();
    }

    void reset() {

    }

    public String[] explain() {
        ArrayList explanations = new ArrayList();
        explanations.add(getName() + " - " + getDescription());
        if (isDoKeystoreConfig()) {
            explanations.add(insertTab + "Create " + getKeyStoreType());
            if (getKeyStoreType().equalsIgnoreCase(KeyStoreConstants.DEFAULT_KEYSTORE_NAME)) {
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
        return (String[]) explanations.toArray(new String[explanations.size()]);
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

    public void setClusterType(int theClusteringType) {
        this.clusteringType = theClusteringType;
    }

    public int getClusteringType() {
        return clusteringType;
    }
}
