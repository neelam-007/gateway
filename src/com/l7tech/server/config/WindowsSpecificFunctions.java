package com.l7tech.server.config;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 2:23:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class WindowsSpecificFunctions extends OSSpecificFunctions {

    public WindowsSpecificFunctions() {
        super();
    }

    void makeOSSpecificFilenames() {
        ssgInstallFilePath = "c:/program files/Layer 7/SSG/SSG_INSTALL";
        hostsFile = "c:/windows/system32/drivers/etc/hosts";
        if (StringUtils.isEmpty(installRoot)) {
            installRoot = "c:/program files/Layer 7/SSG";
        }
        lunaInstallDir = "C:/Program Files/LunaSA/";
        lunaJSPDir = "C:/Program Files/LunaSA/JSP";
        lunaCmuPath = "cmu.exe";
        pathToJdk = "jdk/";
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeyStoreConstants.DEFAULT_KEYSTORE_NAME,
            KeyStoreConstants.LUNA_KEYSTORE_NAME
        };
    }

    public boolean isWindows() {
        return true;
    }
}
