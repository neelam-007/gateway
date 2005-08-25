package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.KeyStoreConstants;

import javax.swing.*;
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
    private boolean overwriteLunaCerts;
    private static final String DO_BOTH_KEYS_INFO = "Creating CA and SSL keys";
    private static final String SKIP_CA_KEY_INFO = "Skipping CA leys creation";
    private static final String USING_HOSTNAME_INFO = "Using hostname: ";
    private static final String OVERWRITE_LUNA_CERT_INFO = "Will overwrite existing certs";
    private static final String NO_OVERWRITE_LUNA_CERT_INFO = "Will not overwrite existing certs";

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

        explanations.add(insertTab + "create " + getKeyStoreType());
        if (getKeyStoreType().equalsIgnoreCase(KeyStoreConstants.DEFAULT_KEYSTORE_NAME)) {
            if (isDoBothKeys()) {
                explanations.add(insertTab + DO_BOTH_KEYS_INFO);

            } else {
                explanations.add(insertTab + SKIP_CA_KEY_INFO);
            }
        } else {
            if (isOverwriteLunaCerts()) {
                explanations.add(insertTab + OVERWRITE_LUNA_CERT_INFO);
            } else {
                explanations.add(insertTab + NO_OVERWRITE_LUNA_CERT_INFO);
            }
        }

        explanations.add(insertTab + USING_HOSTNAME_INFO + getHostname());

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

    public boolean isOverwriteLunaCerts() {
        return overwriteLunaCerts;
    }

    public void overwriteLunaCerts(boolean isOverwrite) {
        overwriteLunaCerts = isOverwrite;
    }
}
