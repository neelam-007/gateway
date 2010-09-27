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

    // - PUBLIC

    public UnixSpecificFunctions( final String OSName ) {
        super(OSName);
    }

    @Override
    public List<NetworkingConfigurationBean.InterfaceConfig> getNetworkConfigs( final boolean getLoopBack) throws SocketException {
        final List<NetworkingConfigurationBean.InterfaceConfig> interfaceConfigs = new ArrayList<NetworkingConfigurationBean.InterfaceConfig>();

        final Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
        if ( allInterfaces != null ) {
            for ( final NetworkInterface networkInterface : Collections.list(allInterfaces) ) {
                if ( getLoopBack || ! networkInterface.isLoopback() ) {
                    addNetworkConfigToList(networkInterface, interfaceConfigs);
                }
            }
        }

        return interfaceConfigs;
    }

    // - PRIVATE

    private void addNetworkConfigToList( final NetworkInterface networkInterface,
                                         final List<NetworkingConfigurationBean.InterfaceConfig> interfaceConfigs) throws SocketException {
        NetworkingConfigurationBean.InterfaceConfig aConfig = createNetworkConfig(networkInterface, false);
        if (aConfig != null) interfaceConfigs.add(aConfig);

        Enumeration subInterfaces = networkInterface.getSubInterfaces();
        while (subInterfaces.hasMoreElements()) {
            NetworkInterface subInterface = (NetworkInterface) subInterfaces.nextElement();
            if (!subInterface.isLoopback()) {
                NetworkingConfigurationBean.InterfaceConfig subConfig = createNetworkConfig(subInterface, false);
                if (subConfig != null) interfaceConfigs.add(subConfig);
            }
        }
    }
}
