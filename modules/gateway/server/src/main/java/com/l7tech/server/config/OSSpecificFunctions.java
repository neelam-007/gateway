package com.l7tech.server.config;

import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.server.config.exceptions.UnsupportedOsException;
import com.l7tech.util.OSDetector;

import java.net.NetworkInterface;
import java.util.List;
import java.net.SocketException;

/**
 * A class that encapsulates the configuration locations for an SSG on a supported platform.
 *
 * Written By: megery
 * Date: Aug 12, 2005
 * Time: 3:06:33 PM
 */
public abstract class OSSpecificFunctions {

    // - PUBLIC

    public OSSpecificFunctions(String OSName) {
        this.osName = OSName;
        doOsSpecificSetup();
    }

    public String getOSName() {
        return osName;
    }

    public String getNetworkConfigurationDirectory() {
        return networkConfigDir;
    }

    public String getTimeZonesDir() {
        return timeZonesDir;
    }

    public String getKeymapsDir() {
        return keymapsDir;
    }

    public static OSSpecificFunctions getOSSpecificFunctions() throws UnsupportedOsException {
        if (!OSDetector.isLinux())
            throw new UnsupportedOsException(OSDetector.getOSName() + " is not a supported operating system.");

        return new LinuxSpecificFunctions(OSDetector.getOSName());
    }

    public abstract List<NetworkingConfigurationBean.InterfaceConfig> getNetworkConfigs(boolean getLoopBack) throws SocketException;
    public abstract String getHostname();

    // - PROTECTED

    protected String osName;

    // configuration files/directories to be queried, modified or created
    protected String networkConfigDir;
    protected String timeZonesDir;
    protected String keymapsDir;

    // - PACKAGE

    abstract void doOsSpecificSetup();
    abstract NetworkingConfigurationBean.InterfaceConfig createNetworkConfig(NetworkInterface networkInterface, boolean includeIPV6);
}