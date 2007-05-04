package com.l7tech.server.config;

public class LinuxSpecificFunctions extends UnixSpecificFunctions {

    public LinuxSpecificFunctions(String osname) {
        super(osname);
    }

    public LinuxSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    void makeOSSpecificFilenames() {
        if (isEmptyString(installRoot)) {
            installRoot = "/ssg/";
        }
        lunaInstallDir = "/usr/lunasa";
        lunaJSPDir = "/usr/lunasa/jsp";
        lunaCmuPath = "bin/cmu";
        pathToJdk = "jdk/";
        partitionControlScriptName = "partitionControl.sh";
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeystoreType.DEFAULT_KEYSTORE_NAME.getName(),
            KeystoreType.LUNA_KEYSTORE_NAME.getName()
        };
    }

    public String getOriginalPartitionControlScriptName() {
        return getSsgInstallRoot() + "bin/" + partitionControlScriptName;
    }

    public String getSpecificPartitionControlScriptName() {
        return getPartitionBase() + getPartitionName() + "/" + partitionControlScriptName;
    }

    public String getNetworkConfigurationDirectory() {
        return "/etc/sysconfig/network-scripts";
    }

    public String getUpgradedFileExtension() {
        return "rpmnew";
    }


    public boolean isUnix() {
        return true;
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isLinux() {
        return true;
    }
}
