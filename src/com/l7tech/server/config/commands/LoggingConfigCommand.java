package com.l7tech.server.config.commands;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 4:48:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoggingConfigCommand extends BaseConfigurationCommand {
    static Logger logger = Logger.getLogger(LoggingConfigCommand.class.getName());


    private static final String SSG_LOG_PATTERN = "logs/ssg_%g_%u.log";
    private static final String BACKUP_FILE_NAME = "logging_config_backups";
    private static final String LOG_PATTERN_PROPERTY = "java.util.logging.FileHandler.pattern";
    private static final String PROPERTY_COMMENT = "this file was updated by the SSG configuration utility";

    public LoggingConfigCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        boolean success = true;
        String ssgLogPropsPath = osFunctions.getSsgLogPropertiesFile();
        File logProps = new File(ssgLogPropsPath);
        if (logProps.exists()) {
            File[] files = new File[]
            {
                logProps
            };

            try {
                backupFiles(files, BACKUP_FILE_NAME);
            } catch (IOException e) {
                logger.warning("unable to create backup zip file: " + osFunctions.getSsgInstallRoot() + BACKUP_FILE_NAME + ".zip");
                logger.warning(e.getMessage());
            }
        }

        success = updateSsgLogProperties(ssgLogPropsPath);
        return success;
    }

    private boolean updateSsgLogProperties(String ssgLogPropsPath) {
        boolean success = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        Properties props = null;
        try {
            File loggingPropertiesFile = new File(ssgLogPropsPath);
            props = PropertyHelper.mergeProperties(
                    loggingPropertiesFile,
                    new File(ssgLogPropsPath + "." + osFunctions.getUpgradedFileExtension()), 
                    true);

            String fullLogPattern = osFunctions.getSsgInstallRoot() + SSG_LOG_PATTERN;
            props.setProperty(LOG_PATTERN_PROPERTY, fullLogPattern);

            fos = new FileOutputStream(loggingPropertiesFile);
            props.store(fos, PROPERTY_COMMENT);
            logger.info("Updating the ssglog.properties file");

        } catch (FileNotFoundException e) {
            logger.severe("error while updating the logging configuration file");
            logger.severe(e.getMessage());
            success = false;
        } catch (IOException e) {
            logger.severe("error while updating the logging configuration file");
            logger.severe(e.getMessage());
            success = false;
        } finally{
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return success;
    }

}
