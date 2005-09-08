package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.NewDatabaseConfigBean;
import com.l7tech.server.config.beans.DatabaseConfigBean;
import com.l7tech.server.config.DBActions;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Properties;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:44:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewDatabaseConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(NewDatabaseConfigCommand.class.getName());
    private static final String BACKUP_FILE_NAME = "database_config_backups";
    private static final String HIBERNATE_URL_KEY = "hibernate.connection.url";
    private static final String HIBERNATE_USERNAME_KEY = "hibernate.connection.username";
    private static final String HIBERNATE_PASSWORD_KEY = "hibernate.connection.password";
    private static final String HIBERNATE_PROPERTY_COMMENTS = "This hibernate configuration file was created by the SSG configuration tool. It will be replaced if the tool is re-run";


//    private DBActions dbActions;


     private Pattern urlPattern = Pattern.compile("(^.*//).*(/).*(\\?.*$)");

    public NewDatabaseConfigCommand(ConfigurationBean bean) {
        super(bean, bean.getOSFunctions());
        init();
    }

    private void init() {
//        try {
//            dbActions = new DBActions();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard");
//        }
    }

    public boolean execute() {
        boolean success = false;

        File dbConfigFile = new File(osFunctions.getDatabaseConfig()); //hibernate config file

        if (dbConfigFile.exists()) {
            File[] files = new File[]
            {

                dbConfigFile
            };

            try {
                backupFiles(files, BACKUP_FILE_NAME);
            } catch (IOException e) {
            }
        }

        try {
            updateDbConfigFile(dbConfigFile);
            success = true;
        } catch (IOException e) {
            success = false;
        }

        return success;
    }

    private void updateDbConfigFile(File dbConfigFile) throws IOException {

        NewDatabaseConfigBean dbConfigBean = (NewDatabaseConfigBean) configBean;
        String dbUrl = dbConfigBean.getDbHostname();
        String dbName = dbConfigBean.getDbName();
        String dbUsername = dbConfigBean.getDbUsername();
        String dbPassword = dbConfigBean.getDbPassword();

        FileInputStream fis = null;
        FileOutputStream fos = null;
        Properties dbProps = new Properties();
        try {
            fis = new FileInputStream(dbConfigFile);
            dbProps.load(fis);
            fis.close();
            fis = null;

            String origUrl = (String) dbProps.get(HIBERNATE_URL_KEY);

            String newUrlString = origUrl.replaceFirst(urlPattern.pattern(), "$1" + dbUrl + "$2" + dbName + "$3");
            dbProps.setProperty(HIBERNATE_URL_KEY, newUrlString);
            dbProps.setProperty(HIBERNATE_USERNAME_KEY, dbUsername);
            dbProps.setProperty(HIBERNATE_PASSWORD_KEY, new String(dbPassword));

            fos = new FileOutputStream(dbConfigFile);
            dbProps.store(fos, HIBERNATE_PROPERTY_COMMENTS);
        } catch (FileNotFoundException fnf) {
            logger.severe("error while updating the Database configuration file");
            logger.severe(fnf.getMessage());
            throw fnf;
        } catch (IOException e) {
            logger.severe("error while updating the Database configuration file");
            logger.severe(e.getMessage());
            throw e;
        } finally {
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
    }
}