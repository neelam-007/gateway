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

    public UnixSpecificFunctions( final String OSName ) {
        super(OSName);
    }

    @Override
    void doOsSpecificSetup() {
        doSpecialSetup();
    }

    //put anything that is specific to the flavour of UNIX you are working with (i.e. Solaris, Linux etc) in here.
    abstract void doSpecialSetup();

    @Override
    public boolean isUnix() {
        return true;
    }

    @Override
    public List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs( final boolean getLoopBack,
                                                                              final boolean getIPV6) throws SocketException {
        final List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();

        final Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
        if ( allInterfaces != null ) {
            for ( final NetworkInterface networkInterface : Collections.list(allInterfaces) ) {
                final boolean addIt = ( (!networkInterface.isLoopback()) || (networkInterface.isLoopback() && getLoopBack));
                if (addIt) {
                    addNetworkConfigToList(networkInterface, networkConfigs);
                }
            }
        }

        return networkConfigs;
    }

    private void addNetworkConfigToList( final NetworkInterface networkInterface,
                                         final List<NetworkingConfigurationBean.NetworkConfig> networkConfigs ) throws SocketException {
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
