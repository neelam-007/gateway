package com.l7tech.server.config;

import org.apache.commons.lang.StringUtils;
import com.l7tech.server.config.beans.KeystoreConfigBean;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 2:23:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinuxSpecificFunctions extends OSSpecificFunctions {

    public LinuxSpecificFunctions() {
        super();
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
    }

    public String[] getKeystoreTypes() {
        return new String[]
        {
            KeyStoreConstants.DEFAULT_KEYSTORE_NAME,
            KeyStoreConstants.LUNA_KEYSTORE_NAME
        };
    }

    public boolean isLinux() {
        return true;
    }
}
