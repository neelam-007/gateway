package com.l7tech.server.config;

import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * User: megery
 * Date: Apr 26, 2007
 * Time: 3:59:23 PM
 */
public abstract class UnixSpecificFunctions extends OSSpecificFunctions {

    public UnixSpecificFunctions(String OSName) {
        super(OSName);
    }

    void doOsSpecificSetup() {
        doSpecialSetup();
    }

    //put anything that is specific to the flabour of UNIX you are working with (i.e. solaris, Linux etc) in here.
    abstract void doSpecialSetup();

    public boolean isUnix() {
        return true;
    }

    public List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs(boolean getLoopBack, boolean getIPV6) throws SocketException {
        List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();

        List<NetworkInterface> allInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : allInterfaces) {
            boolean addIt = ( (!networkInterface.isLoopback()) || (networkInterface.isLoopback() && getLoopBack));
            if (addIt) {
                addNetworkConfigToList(networkInterface, networkConfigs, getIPV6);
            }
        }

        return networkConfigs;
    }

    private void addNetworkConfigToList(NetworkInterface networkInterface, List<NetworkingConfigurationBean.NetworkConfig> networkConfigs, boolean getIPV6) throws SocketException {
        NetworkingConfigurationBean.NetworkConfig aConfig = createNetworkConfig(networkInterface, false);
        if (aConfig != null) networkConfigs.add(aConfig);

        Enumeration subInterfaces = networkInterface.getSubInterfaces();
        while (subInterfaces.hasMoreElements()) {
            NetworkInterface subInterface = (NetworkInterface) subInterfaces.nextElement();
            if (!subInterface.isLoopback()) {
                NetworkingConfigurationBean.NetworkConfig subConfig = createNetworkConfig(subInterface, false);
                if (subConfig != null) networkConfigs.add(subConfig);
            }
        }
    }

    abstract NetworkingConfigurationBean.NetworkConfig createNetworkConfig(NetworkInterface networkInterface, boolean includeIPV6);
}
