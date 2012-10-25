package com.l7tech.server.config;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public String getHostname() {
        Logger logger = Logger.getLogger(UnixSpecificFunctions.class.getName());
        String command = "/usr/bin/hostname";
        try {
            ProcResult result = ProcUtils.exec(command);
            return new String(result.getOutput());
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Error running " + command + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<unknown>";
        }
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
