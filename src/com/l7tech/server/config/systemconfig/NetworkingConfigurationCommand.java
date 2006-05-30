package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.util.List;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:24:29 PM
 */
public class NetworkingConfigurationCommand extends BaseConfigurationCommand {
    NetworkingConfigurationBean netBean;

    protected NetworkingConfigurationCommand(ConfigurationBean bean) {
        super(bean);
        netBean = (NetworkingConfigurationBean) configBean;
    }

    public boolean execute() {
        boolean success = true;
        System.out.println("Here's what would have been done had this been implemented");
        System.out.println("The values are: ");
        List<NetworkingConfigurationBean.NetworkConfig> netConfigs = netBean.getNetworkingConfigurations();
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : netConfigs) {
            System.out.println("Interface: " + networkConfig.getInterfaceName());
            System.out.println("Boot Protocol: " + networkConfig.getBootProto());
            System.out.println("IPAddress: " + networkConfig.getIpAddress());
            System.out.println("NetMask: " + networkConfig.getNetMask());
            System.out.println("Gateway: " + networkConfig.getGateway());
        }
        return success;
    }

    public String[] getActions() {
        return configBean.explain();
    }
}
