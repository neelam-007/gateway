package com.l7tech.server.config.commands;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.ui.gui.ConfigurationWizard;
import com.l7tech.common.BuildInfo;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String HIBERNATE_MAXCONNECTIONS_KEY = "hibernate.c3p0.max_size";
    private static final String HIBERNATE_MAXCONNECTIONS_VALUE = "510";
    private static final String HIBERNATE_MAXCONNECTIONS_DEFVALUE = "100";

    private static final String HIBERNATE_MYSQL_URL_START = "jdbc:mysql://";
    public static final String HIBERNATE_DEFAULT_CONNECTION_URL = HIBERNATE_MYSQL_URL_START + "localhost/ssg";

    private static Map<String, String> paramDefaults;

    private static final String HIBERNATE_URL_AUTORECONNECTPOOLS_PARAM="autoReconnectForPools=true";

    //the package names change for these in 3.6/4.0
    public static final String HIBERNATE_DIALECT_PROP_NAME = "hibernate.dialect";
    private static final String HIBERNATE_DIALECT_PROP_VALUE = "org.hibernate.dialect.MySQLDialect";

    private static final String HIBERNATE_TXN_FACTORY_PROP_NAME = "hibernate.transaction.factory_class";
    private static final String HIBERNATE_TXN_FACTORY_PROP_VALUE = "org.hibernate.transaction.JDBCTransactionFactory";

    private static final String HIBERNATE_CXN_PROVIDER_PROP_NAME = "hibernate.connection.provider_class";
    private static final String HIBERNATE_CXN_PROVIDER_PROP_VALUE = "org.hibernate.connection.C3P0ConnectionProvider";


    private static final String DB_URL_PLACEHOLDER = "DB_URL";

    private Pattern urlPattern = Pattern.compile("^" + HIBERNATE_MYSQL_URL_START + "(.*)(/)(.*)\\?.*$");

    static {
        paramDefaults = new TreeMap<String, String>();
        paramDefaults.put(new String("failOverReadOnly"), new String("false"));
        paramDefaults.put(new String("autoReconnect"), new String("false"));
        paramDefaults.put(new String("socketTimeout"), new String("120000"));
        paramDefaults.put(new String("useNewIO"), new String("true"));
        paramDefaults.put(new String("characterEncoding"), new String("UTF8"));
        paramDefaults.put(new String("characterSetResults"), new String("UTF8"));
    }

    public SsgDatabaseConfigCommand(ConfigurationBean bean) {
        super(bean);
        init();
    }

    private void init() {}

    public boolean execute() {
        boolean success = false;

        File dbConfigFile = new File(getOsFunctions().getDatabaseConfig()); //hibernate config file

        if (dbConfigFile.exists()) {
            File[] files = new File[]
            {

                dbConfigFile
            };

            backupFiles(files, BACKUP_FILE_NAME);
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

        FileOutputStream fos = null;

        try {
            Properties dbProps = PropertyHelper.mergeProperties(
                    dbConfigFile,
                    new File(dbConfigFile.getAbsolutePath() + "." + getOsFunctions().getUpgradedFileExtension()),
                    true, true);

            String origUrl = (String) dbProps.get(HIBERNATE_URL_KEY);

            if (checkIsDefault(origUrl))
                origUrl = HIBERNATE_DEFAULT_CONNECTION_URL;

            String[] parts = origUrl.split("\\?");

            String paramPart = null;
            if (parts != null && parts.length > 1)
                paramPart = parts[1];

            paramPart = replaceParams(paramPart);

            origUrl = origUrl.replaceAll(HIBERNATE_URL_AUTORECONNECTPOOLS_PARAM, "");

            String newUrlString = HIBERNATE_MYSQL_URL_START + dbUrl + "/" + dbName + "?" + paramPart;

            dbProps.setProperty(HIBERNATE_URL_KEY, newUrlString);
            dbProps.setProperty(HIBERNATE_USERNAME_KEY, dbUsername);
            dbProps.setProperty(HIBERNATE_PASSWORD_KEY, new String(dbPassword));

            upgradePackageName(dbProps);
            setMaxConnections(dbProps);

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
        } catch (NumberFormatException e) {
            logger.severe("error while updating the Database configuration file");
            throw e;
        }
        finally {
            if (fos != null)
                try { fos.close(); } catch (IOException e) {}
        }
    }

    private void upgradePackageName(Properties dbProps) {
        String currentVersion = ConfigurationWizard.getCurrentVersion();
        float currentVersionFloat = 0.0f;
        try {
            currentVersionFloat = Float.parseFloat(currentVersion);
            if (currentVersionFloat >= 3.6f) {
                dbProps.setProperty(HIBERNATE_DIALECT_PROP_NAME, HIBERNATE_DIALECT_PROP_VALUE);
                dbProps.setProperty(HIBERNATE_CXN_PROVIDER_PROP_NAME, HIBERNATE_CXN_PROVIDER_PROP_VALUE);
                dbProps.setProperty(HIBERNATE_TXN_FACTORY_PROP_NAME, HIBERNATE_TXN_FACTORY_PROP_VALUE);
            }
        } catch (NumberFormatException ex) {
            //version string is something other than a normal number ... try just the major and minor instead
            String s = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();

            try {
                currentVersionFloat = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                logger.warning("Couldn't determine build version so database configuration package upgrade could not proceed.");
                throw e;
            }
        }
    }

    private boolean checkIsDefault(String origUrl) {
        boolean isDefault = false;
        Matcher matcher = urlPattern.matcher(origUrl);

        if (StringUtils.isEmpty(origUrl) || origUrl.equals(DB_URL_PLACEHOLDER)) {
            logger.info("Found an empty Database URL, replacing with the default");
            isDefault = true;
        } else if (!origUrl.startsWith(HIBERNATE_MYSQL_URL_START) || !matcher.matches()) {
            logger.info("Found an invalid database URL, replacing with the default");
            isDefault = true;
        }
        return isDefault;
    }

    private void setMaxConnections(Properties dbProps) {
        //fix the maximum connections for upgrades if is a default configuration
        String maxConnections = dbProps.getProperty(HIBERNATE_MAXCONNECTIONS_KEY);
        if (StringUtils.equals(maxConnections, HIBERNATE_MAXCONNECTIONS_DEFVALUE))
            dbProps.setProperty(HIBERNATE_MAXCONNECTIONS_KEY, HIBERNATE_MAXCONNECTIONS_VALUE);
    }

    private String replaceParams(String paramPart) {

        //form the baseline parameters that should always be present;
        Map<String, String> baseline = new LinkedHashMap<String, String>();
        for (String s : paramDefaults.keySet()) {
            baseline.put(new String(s), new String(paramDefaults.get(s)));
        }

        //now check if the existing params have anything that should be updated and update the baseline to reflect
        // the existing config
        String[] existingParams = null;
        if (StringUtils.isNotEmpty(paramPart)) {
            existingParams = paramPart.split("&");
            if (existingParams != null && existingParams.length != 0) {
                for (String ep : existingParams) {
                    String[] pair = ep.split("=");
                    if (pair != null && pair.length == 2) {
                        String key = pair[0];
                        String value = pair[1];
                        baseline.put(key, value);
                    }
                }
            }
        }

        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for (String key : baseline.keySet()) {
            if (isFirst) isFirst = false;
            else sb.append("&");

            sb.append(key).append("=").append(baseline.get(key));
        }

        return sb.toString();

    }
}