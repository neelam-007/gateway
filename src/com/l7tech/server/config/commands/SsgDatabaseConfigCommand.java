package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.ui.gui.ConfigurationWizard;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Properties;
import java.io.*;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:44:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class SsgDatabaseConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(SsgDatabaseConfigCommand.class.getName());
    private static final String BACKUP_FILE_NAME = "database_config_backups";
    private static final String HIBERNATE_URL_KEY = "hibernate.connection.url";
    private static final String HIBERNATE_USERNAME_KEY = "hibernate.connection.username";
    private static final String HIBERNATE_PASSWORD_KEY = "hibernate.connection.password";
    private static final String HIBERNATE_PROPERTY_COMMENTS = "This hibernate configuration file was created by the SSG configuration tool. It will be replaced if the tool is re-run";
    public static final String HIBERNATE_DEFAULT_CONNECTION_URL="jdbc:mysql://localhost/ssg?failOverReadOnly=false&autoReconnect=false&socketTimeout=120000&useNewIO=true&characterEncoding=UTF8&characterSetResults=UTF8";
    public static final String HIBERNATE_URL_AUTORECONNECTPOOLS_PATTERN="autoReconnectForPools=true(&*)";

    //the package names change for these in 3.6/4.0
    public static final String HIBERNATE_DIALECT_PROP_NAME = "hibernate.dialect";
    private static final String HIBERNATE_DIALECT_PROP_VALUE = "org.hibernate.dialect.MySQLDialect";

    private static final String HIBERNATE_TXN_FACTORY_PROP_NAME = "hibernate.transaction.factory_class";
    private static final String HIBERNATE_TXN_FACTORY_PROP_VALUE = "org.hibernate.transaction.JDBCTransactionFactory";

    private static final String HIBERNATE_CXN_PROVIDER_PROP_NAME = "hibernate.connection.provider_class";
    private static final String HIBERNATE_CXN_PROVIDER_PROP_VALUE = "org.hibernate.connection.C3P0ConnectionProvider";


    private static final String DB_URL_PLACEHOLDER = "DB_URL";

    private Pattern urlPattern = Pattern.compile("(^.*//).*(/).*(\\?.*$)");

    public SsgDatabaseConfigCommand(ConfigurationBean bean) {
        super(bean);
        init();
    }

    private void init() {}

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

        SsgDatabaseConfigBean dbConfigBean = (SsgDatabaseConfigBean) configBean;
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
            if (StringUtils.isEmpty(origUrl) || origUrl.equals(DB_URL_PLACEHOLDER)) {
                origUrl = HIBERNATE_DEFAULT_CONNECTION_URL;
            }
            origUrl = origUrl.replaceAll(HIBERNATE_URL_AUTORECONNECTPOOLS_PATTERN, "");

            String newUrlString = origUrl.replaceFirst(urlPattern.pattern(), "$1" + dbUrl + "$2" + dbName + "$3");
            dbProps.setProperty(HIBERNATE_URL_KEY, newUrlString);
            dbProps.setProperty(HIBERNATE_USERNAME_KEY, dbUsername);
            dbProps.setProperty(HIBERNATE_PASSWORD_KEY, new String(dbPassword));

            String currentVersion = ConfigurationWizard.getCurrentVersion();
            if (Float.parseFloat(currentVersion) >= 3.6f) {
                dbProps.setProperty(HIBERNATE_DIALECT_PROP_NAME, HIBERNATE_DIALECT_PROP_VALUE);
                dbProps.setProperty(HIBERNATE_CXN_PROVIDER_PROP_NAME, HIBERNATE_CXN_PROVIDER_PROP_VALUE);
                dbProps.setProperty(HIBERNATE_TXN_FACTORY_PROP_NAME, HIBERNATE_TXN_FACTORY_PROP_VALUE);
            }

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