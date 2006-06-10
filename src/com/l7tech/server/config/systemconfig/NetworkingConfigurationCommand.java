package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.util.List;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:24:29 PM
 */
public class NetworkingConfigurationCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(NetworkingConfigurationCommand.class.getName());

    NetworkingConfigurationBean netBean;

    protected NetworkingConfigurationCommand(ConfigurationBean bean) {
        super(bean);
        netBean = (NetworkingConfigurationBean) configBean;
    }

    public boolean execute() {
        boolean success = true;

        List<NetworkingConfigurationBean.NetworkConfig> netConfigs = netBean.getNetworkingConfigurations();
        File netconfigDir = new File(osFunctions.getSsgInstallRoot(), "networkingconfig");
        if (!netconfigDir.mkdir()) {
            logger.info("Created new directory for networking configuration: " + netconfigDir.getAbsolutePath());
        }

        File configFile = null;
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : netConfigs) {

            if (networkConfig != null) {
                String interfaceName = networkConfig.getInterfaceName();
                logger.info("Configuring \"" + interfaceName + "\" interface");
                configFile = new File(netconfigDir,     interfaceName + "_config");
                PrintWriter pw = null;
                try {
                    if (configFile.createNewFile()) logger.info("created file \"" + configFile.getAbsolutePath() + "\"");
                    else logger.info("editing file \"" + configFile.getAbsolutePath() + "\"");
                    pw = new PrintWriter(new FileOutputStream(configFile));
                    pw.print(networkConfig);
                } catch (IOException e) {
                    logger.severe("Could not create file:" + configFile.getAbsolutePath());
                    success = false;
                } finally {
                    if (pw != null) pw.close();
                }
            }
        }
        return success;
    }

    public String[] getActions() {
        return configBean.explain();
    }
}
