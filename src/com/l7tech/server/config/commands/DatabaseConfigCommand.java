package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.DatabaseConfigBean;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 11:53:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseConfigCommand extends BaseConfigurationCommand {

    static Logger logger = Logger.getLogger(DatabaseConfigCommand.class.getName());

    private static final String BACKUP_FILE_NAME = "database_config_backups";
    private static final String HIBERNATE_URL_KEY = "hibernate.connection.url";
    private static final String HIBERNATE_USERNAME_KEY = "hibernate.connection.username";
    private static final String HIBERNATE_PASSWORD_KEY = "hibernate.connection.password";
    private static final String HIBERNATE_PROPERTY_COMMENTS = "This hibernate configuration file was created by the SSG configuration tool. It will be replaced if the tool is re-run";

    private Pattern urlPattern = Pattern.compile("(^.*//).*(/).*(\\?.*$)");


    public DatabaseConfigCommand(ConfigurationBean bean) {
        super(bean, bean.getOSFunctions());
    }

    public boolean execute() {
        boolean success = true;
        //printPlans();
        if ( ((DatabaseConfigBean)configBean).isDbConfigOn() ){

            File dbConfigFile = new File(osFunctions.getDatabaseConfig()); //hibernate config file

            File[] files = new File[]
            {

                dbConfigFile
            };

            try {
                backupFiles(files, BACKUP_FILE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                updateDbConfigFile(dbConfigFile);
            } catch (IOException e) {
                success = false;
            }
        }
        return success;
    }

    private void updateDbConfigFile(File dbConfigFile) throws IOException {

        DatabaseConfigBean dbConfigBean = (DatabaseConfigBean) configBean;
        String dbUrl = dbConfigBean.getDbHostname();
        String dbName = dbConfigBean.getDbName();
        String dbUsername = dbConfigBean.getDbUsername();
        char[] dbPassword = dbConfigBean.getDbPassword();

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
    }

    private void printPlans() {
        DatabaseConfigBean dbConfigBean = (DatabaseConfigBean) configBean;

        boolean isRemote = dbConfigBean.isRemote();
    }
}
