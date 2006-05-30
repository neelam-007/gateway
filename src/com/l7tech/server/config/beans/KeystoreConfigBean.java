package com.l7tech.server.config.beans;

import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.OSSpecificFunctions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

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
    private static final String USING_HOSTNAME_INFO = "Generating keys using hostname: ";
    private static final String SKIPPING_KEYSTORE_CONFIG_INFO = "Skipping keystore configuration";

    private String lunaJspPath;
    private String lunaInstallationPath;
    private boolean doKeystoreConfig;
    private int clusteringType;
    private String keyStoreType;

    private static final String linuxLunaConfigCopy =
            "<li>" +
                "LUNA CONFIGURATION: Copy the etc/Chrystoki.conf file from the primary node to each SSG in the cluster" + eol +
                    "<dl><dt></dt></dl>" + eol +
            "</li>" + eol;

    private static final String windowsLunaConfigCopy =
            "<li>" + eol +
                "LUNA CONFIGURATION: Copy the LUNA_INSTALL_DIR/crystoki.ini file from the primary node to each SSG in the cluster" + eol +
                "<dl><dt></dt></dl>" + eol +
            "</li>" + eol;

    private static final String windowsLunaString =
            "<dl>" + eol +
                "<dt>[Misc]<br>" + eol +
                    "ApplicationInstance=HTTP_SERVER<br>" + eol +
                    "AppIdMajor=1<br>" + eol +
                    "AppIdMinor=1<br>" + eol +
                "</dt>" + eol +
            "</dl>" + eol +
            "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

    private static final String windowsUpdateCrystokiLine =
            "<li>LUNA CONFIGURATION: Append the following to the LUNA_INSTALL_DIR/crystoki.ini file:" + eol +
                windowsLunaString + eol +
            "</li>" + eol;

    private static final String linuxLunaString =
            "<dl>" + eol +
                "<dt>Misc = {</dt>" + eol +
                    "<dd>ApplicationInstance=HTTP_SERVER;</dd>" + eol +
                    "<dd>AppIdMajor=1;</dd>" + eol +
                    "<dd>AppIdMinor=1;</dd>" + eol +
                "<dt>}</dt>" + eol +
            "</dl>" + eol +
            "where AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

    private static final String linuxUpdateCrystokiLine =
            "<li>LUNA CONFIGURATION: Append the following to the etc/Chrystoki.conf file:" + eol +
                 linuxLunaString + eol +
            "</li>" + eol;

    String runSSgConfigLine =
            "<li>RUN THE SSG CONFIGURATION WIZARD: run the wizard on each of the <br> " +
                "members of the cluster to generate the keystores" + eol +
                "<dl>" + eol +
                    "<dt>Note:</dt>" + eol +
                        "<dd>Use the same password for the keystore on each of the members of the cluster</dd>" + eol +
                "</dl>" + eol +
            "</li>" + eol;

    String copykeysLine =
            "<li>COPY THE KEYS: copy the contents of the keystore directory on the first node<br> " + eol +
            "of the cluster to the keystore directory on the other SSGs in the cluster" + eol +
                "<dl>" + eol +
                    "<dt>Note:</dt>" + eol +
                        "<dd>The SSG keystore directory is: \"" + osFunctions.getKeystoreDir() + "\"</dd>" + eol +
                "</dl>" + eol +
            "</li>" + eol;

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
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
    }

    public List<String> getManualSteps() {
        List<String> steps = new ArrayList<String>();

        if (clusteringType != ClusteringConfigBean.CLUSTER_NONE) {

            if (clusteringType == ClusteringConfigBean.CLUSTER_JOIN) {
                if (StringUtils.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME, keyStoreType)) {
                    steps.add(osFunctions.isLinux()?linuxLunaConfigCopy:windowsLunaConfigCopy);
                } else {
                    steps.add(runSSgConfigLine);
                }
            } else if (clusteringType == ClusteringConfigBean.CLUSTER_NEW) {
                if (StringUtils.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME, keyStoreType)) {
                    //instructions for luna in a clustered environment
                    steps.add(osFunctions.isLinux()?linuxUpdateCrystokiLine:windowsUpdateCrystokiLine);
                }
            }
            steps.add("<br>");
        }
        return steps;
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
