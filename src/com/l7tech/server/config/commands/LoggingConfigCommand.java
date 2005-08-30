package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.OSSpecificFunctions;

import java.util.Properties;
import java.util.logging.Logger;
import java.io.*;

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

    public LoggingConfigCommand(ConfigurationBean bean, OSSpecificFunctions osFunctions) {
        super(bean, osFunctions);
    }

    public boolean execute() {
        boolean success = true;
        String ssgLogPropsPath = osFunctions.getSsgLogPropertiesFile();
        File logProps = new File(ssgLogPropsPath);
        File[] files = new File[]
        {
            logProps
        };

        try {
            backupFiles(files, BACKUP_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        Properties props = new Properties();
        try {
            fis = new FileInputStream(ssgLogPropsPath);
            props.load(fis);
            fis.close();
            fis = null;

            String fullLogPattern = osFunctions.getSsgInstallRoot() + SSG_LOG_PATTERN;
            props.setProperty(LOG_PATTERN_PROPERTY, fullLogPattern);


            fos = new FileOutputStream(ssgLogPropsPath);
            props.store(fos, PROPERTY_COMMENT);

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
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

}
