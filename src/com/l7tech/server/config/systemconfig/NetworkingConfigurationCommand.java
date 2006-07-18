package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;
import java.io.*;

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

        Map<String, NetworkingConfigurationBean.NetworkConfig> netConfigs = netBean.getNetworkingConfigurations();
        File currentWorkingDir = new File(".");
        File configDir = new File(currentWorkingDir, "configfiles");
        if (!configDir.mkdir())
            logger.info("Created new directory for networking configuration: " + configDir.getAbsolutePath());


        for (String key : netConfigs.keySet()) {
            NetworkingConfigurationBean.NetworkConfig networkConfig = netConfigs.get(key);
            if (networkConfig != null) {
                String interfaceName = networkConfig.getInterfaceName();
                logger.info("Configuring \"" + interfaceName + "\" interface");
                File netConfigFile = new File(configDir, "netconfig_" + interfaceName);

                PrintWriter pw = null;
                try {
                    if (netConfigFile.createNewFile())
                        logger.info("created file \"" + netConfigFile.getAbsolutePath() + "\"");
                    else
                        logger.info("editing file \"" + netConfigFile.getAbsolutePath() + "\"");

                    pw = new PrintWriter(new FileOutputStream(netConfigFile));
                    pw.print(networkConfig);

                } catch (IOException e) {
                    logger.severe("Could not create file:" + netConfigFile.getAbsolutePath());
                    success = false;
                } finally {
                    if (pw != null)
                        pw.close();
                }
            }
        }


        PrintWriter hostnameWriter = null;
        File hostnameFile = new File(configDir, "hostname");
        try {
            if (hostnameFile.createNewFile())
                logger.info("created file \"" + hostnameFile.getAbsolutePath() + "\"");
            else
                logger.info("editing file \"" + hostnameFile.getAbsolutePath() + "\"");

            hostnameWriter = new PrintWriter(new FileOutputStream(hostnameFile));
            hostnameWriter.println(netBean.getHostname());
        } catch (IOException e) {
            logger.severe("Error while creating file " + hostnameFile.getAbsoluteFile() + "\"" + e.getMessage() + "\"");
            success = false;
        } finally{
            if (hostnameWriter != null)
                hostnameWriter.close();
        }
        return success;
    }
}
