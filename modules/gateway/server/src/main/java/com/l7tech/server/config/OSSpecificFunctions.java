package com.l7tech.server.config;

import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.server.config.exceptions.UnsupportedOsException;
import com.l7tech.util.OSDetector;

import java.util.List;
import java.net.SocketException;

/**
 * A class that encapsulates the configuraration locations for an SSG on a supported platform.
 *
 * Written By: megery
 * Date: Aug 12, 2005
 * Time: 3:06:33 PM
 */
public abstract class OSSpecificFunctions {
    protected String osName;

    // configuration files/directories to be queried, modified or created
    protected String configWizardLauncher;
    protected String hostsFile;

    protected String networkConfigDir;
    protected String upgradeFileNewExt;
    protected String upgradeFileOldExt;

    protected String timeZonesDir;

    public OSSpecificFunctions(String OSName) {
        this.osName = OSName;

        doOsSpecificSetup();
    }

    public String getConfigWizardLauncher() {
        return configWizardLauncher;
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isUnix() {
        return false;    
    }

    abstract void doOsSpecificSetup();

    public String getOSName() {
        return osName;
    }

    public String getNetworkConfigurationDirectory() {
        return networkConfigDir;
    }

    public String getUpgradedNewFileExtension() {
        return upgradeFileNewExt;
    }

    public String getUpgradedOldFileExtension() {
        return upgradeFileOldExt;
    }

    public String getTimeZonesDir() {
        return timeZonesDir;
    }

    public static OSSpecificFunctions getOSSpecificFunctions() throws UnsupportedOsException {
        if (!OSDetector.isLinux())
            throw new UnsupportedOsException(OSDetector.getOSName() + " is not a supported operating system.");

        return new LinuxSpecificFunctions(OSDetector.getOSName());
    }

    public static class OsSpecificFunctionUnavailableException extends Exception {
    }

    public abstract List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs(boolean getLoopBack, boolean getIPV6) throws SocketException;

}