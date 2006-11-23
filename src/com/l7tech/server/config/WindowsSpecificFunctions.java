package com.l7tech.server.config;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 2:23:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class WindowsSpecificFunctions extends OSSpecificFunctions {

    public WindowsSpecificFunctions(String osname) {
        super(osname);
    }

    public WindowsSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    void makeOSSpecificFilenames() {
        if (isEmptyString(installRoot)) {
            installRoot = "c:/Program Files/Layer 7 Technologies/SecureSpan Gateway/";
        }
        lunaInstallDir = "C:/Program Files/LunaSA/";
        lunaJSPDir = "C:/Program Files/LunaSA/JSP";
        lunaCmuPath = "cmu.exe";
        pathToJdk = "jdk/";
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeystoreType.DEFAULT_KEYSTORE_NAME.getName(),
            KeystoreType.LUNA_KEYSTORE_NAME.getName()
        };
    }

    public String getNetworkConfigurationDirectory() {
        throw new IllegalStateException("Cannot Configure the network on Windows.");
    }

    public String getUpgradedFileExtension() {
        return "new";
    }

    public boolean isWindows() {
        return true;
    }
}
