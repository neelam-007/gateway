package com.l7tech.server.config.commands;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.util.Date;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Aug 23, 2005
 */
public class LoggingConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(LoggingConfigCommand.class.getName());
    private static final String BACKUP_FILE_NAME = "logging_config_backups";
    private static final String PROPERTY_COMMENT = "This file was updated by the SSG configuration utility";

    public LoggingConfigCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        boolean success = false;
        PartitionInformation pi = PartitionManager.getInstance().getActivePartition();
        String ssgLogPropsPath = pi.getOSSpecificFunctions().getSsgLogPropertiesFile();

        File logProps = new File(ssgLogPropsPath);
        if ( logProps.exists() ) {
            File[] files = new File[]
            {
                logProps
            };

            backupFiles(files, BACKUP_FILE_NAME);
        }

        success =  updateSsgLogProperties(ssgLogPropsPath);

        return success;
    }

    private boolean updateSsgLogProperties(String ssgLogPropsPath) {
        boolean success = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        PropertiesConfiguration props = null;
        try {
            File loggingPropertiesFile = new File(ssgLogPropsPath);
            props = PropertyHelper.mergeProperties(
                    loggingPropertiesFile,
                    new File(ssgLogPropsPath + "." + getOsFunctions().getUpgradedNewFileExtension()),
                    true, true);

            // remove handler configuration from ssglog.properties (now created by ssglog.properties in Gateway.jar)
            //
            // we could remove all the ConsoleHandler and FileHandler configuration also but will leave as
            // it is then easier to restore a customized configuration.
            //
            props.clearProperty("handlers");

            //noinspection IOResourceOpenedButNotSafelyClosed
            fos = new FileOutputStream(loggingPropertiesFile);
            props.setHeader(PROPERTY_COMMENT + "\n" + new Date());
            props.save(fos, "iso-8859-1");
            logger.info("Updating the ssglog.properties file");

        } catch (FileNotFoundException e) {
            logger.severe("error while updating the logging configuration file");
            logger.severe(e.getMessage());
            success = false;
        } catch (ConfigurationException e) {
            logger.severe("error while updating the logging configuration file");
            logger.severe(e.getMessage());
            success = false;
        } catch (IOException e) {
            logger.severe("error while updating the logging configuration file");
            logger.severe(e.getMessage());
            success = false;
        } finally{
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
        return success;
    }

}
