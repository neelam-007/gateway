package com.l7tech.server.config.commands;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 18, 2005
 * Time: 11:45:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class RmiConfigCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(RmiConfigCommand.class.getName());
    private static final String PROP_RMI_RANDOMIZE = "java.rmi.server.randomIDs";
    public static final String PROP_RMI_USECODEBASEONLY = "java.rmi.server.useCodebaseOnly";


    public RmiConfigCommand(ConfigurationBean configBean) {
        super(configBean);
    }

    public boolean execute() {
        boolean success = true;
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        try {
            updateSystemPropertiesFile(systemPropertiesFile);
        } catch (IOException e) {
            success = false;
        }

        return success;
    }

    private void updateSystemPropertiesFile(File systemPropertiesFile) throws IOException {

        OutputStream fos = null;
        try {
            if (!systemPropertiesFile.exists()) {
                systemPropertiesFile.createNewFile();
            }


            Properties props = PropertyHelper.mergeProperties(
                    systemPropertiesFile,
                    new File(systemPropertiesFile.getAbsolutePath() + "." + getOsFunctions().getUpgradedFileExtension()),
                    true, true);
            
            props.setProperty(PROP_RMI_RANDOMIZE, "true");
            props.setProperty(PROP_RMI_USECODEBASEONLY, "true");

            fos = new FileOutputStream(systemPropertiesFile);
            props.store(fos, "Updated by the SSG Configuration Tool");
            logger.info("Setting " + PROP_RMI_RANDOMIZE + "=true" + " in system.properties file");
            logger.info("Setting " + PROP_RMI_USECODEBASEONLY + "=true" + " in system.properties file");
        } catch (FileNotFoundException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("There was an error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
