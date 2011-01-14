package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;

import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:24:29 PM
 */
public class NetworkingConfigurationCommand extends BaseConfigurationCommand<NetworkingConfigurationBean> {

    // - PUBLIC

    @Override
    public boolean execute() {
        return writeInterfaceFiles() & writeNetworkFile() & writeResolvConfFile();
    }

    // - PROTECTED

    protected NetworkingConfigurationCommand(NetworkingConfigurationBean bean) {
        super(bean);
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(NetworkingConfigurationCommand.class.getName());

    private boolean writeInterfaceFiles() {
        boolean success = true;
        List<NetworkingConfigurationBean.InterfaceConfig> netConfigs = configBean.getAllNetworkInterfaces();
        for (NetworkingConfigurationBean.InterfaceConfig interfaceConfig : netConfigs) {
            if (interfaceConfig != null && interfaceConfig.isDirtyFlag()) {
                String interfaceName = interfaceConfig.getInterfaceName();
                logger.info("Configuring \"" + interfaceName + "\" interface");
                success &= writeConfigFile("ifcfg-" + interfaceName, interfaceConfig.getInterfaceConfig());
            }
        }
        return success;
    }

    private boolean writeNetworkFile() {
        return writeConfigFile("network", configBean.getNetworkConfig());
    }

    private boolean writeResolvConfFile() {
        return writeConfigFile("resolv.conf", configBean.getResolvConf());
    }
}
