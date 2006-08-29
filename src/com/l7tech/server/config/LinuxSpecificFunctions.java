package com.l7tech.server.config;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 2:23:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinuxSpecificFunctions extends OSSpecificFunctions {

    public LinuxSpecificFunctions(String osname) {
        super(osname);
    }

    void makeOSSpecificFilenames() {
        ssgInstallFilePath = "/etc/SSG_INSTALL";
        hostsFile = "/etc/hosts";
        if (StringUtils.isEmpty(installRoot)) {
            installRoot = "/ssg";
        }
        lunaInstallDir = "/usr/lunasa";
        lunaJSPDir = "/usr/lunasa/jsp";
        lunaCmuPath = "bin/cmu";
        pathToJdk = "jdk/";
        //pathToJavaLibPath = "jre/lib/i386/";
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeystoreType.DEFAULT_KEYSTORE_NAME.getName(),
            KeystoreType.LUNA_KEYSTORE_NAME.getName()
        };
    }

    public String getNetworkConfigurationDirectory() {
        return "/etc/sysconfig/network-scripts";
    }

    public String getUpgradedFileExtension() {
        return "rpmnew";
    }

    public boolean isLinux() {
        return true;
    }
}
