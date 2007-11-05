package com.l7tech.server.config.beans;

import com.l7tech.server.config.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:42:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SsgDatabaseConfigBean extends BaseConfigurationBean {
    private final static String NAME = "Database Configuration";
    private final static String DESCRIPTION = "Configures the database properties for an SSG";

    private String privUsername;
    private String privPassword;
    private String dbHostname;
    private String dbUsername;
    private String dbPassword;
    private String dbName;

    public final static String PROP_DB_USERNAME = "hibernate.connection.username";
    public final static String PROP_DB_URL = "hibernate.connection.url";
    public final static String PROP_DB_PASSWORD = "hibernate.connection.password" ;
    public final static Pattern dbUrlPattern = Pattern.compile("^.*//(.*)/(.*)\\?.*$");

    public SsgDatabaseConfigBean() {
        super(NAME, DESCRIPTION);
        ELEMENT_KEY = this.getClass().getName();
        init();
    }

    public SsgDatabaseConfigBean(String configFile) throws IOException {
        this();
        if (StringUtils.isNotEmpty(configFile)) {
            initFromConfigFile(configFile);
        }
    }

    private void initFromConfigFile(String configFile) throws IOException {
        Map<String, String> defaults = PropertyHelper.getProperties(configFile, new String[] {
                PROP_DB_URL,
                PROP_DB_USERNAME,
                PROP_DB_PASSWORD
        });

        dbUsername = defaults.get(PROP_DB_USERNAME);
        dbPassword = defaults.get(PROP_DB_PASSWORD);
        String existingDBUrl = (String) defaults.get(SsgDatabaseConfigBean.PROP_DB_URL);

        if (StringUtils.isNotEmpty(existingDBUrl)) {
            Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
            if (matcher.matches()) {
                dbHostname = matcher.group(1);
                dbName = matcher.group(2);
            }
        } else {
            throw new IOException("no database url was found while reading the configfile [" + configFile + "].");
        }
    }

    private void init() {
    }

    public void reset() {}

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        explanations.add(insertTab + "Setup connection to a database:");
        explanations.add(insertTab + "    HOSTNAME = " + getDbHostname());
        explanations.add(insertTab + "    USERNAME = " + getDbUsername());
        explanations.add(insertTab + "    DATABASE = " + getDbName());
    }

    public void setPrivUserName(String username) {
        privUsername = username;
    }

    public void setPrivPassword(String password) {
        privPassword = password;
    }

    public void setDbHostname(String hostname) {
        dbHostname = hostname;
    }

    public void setDbUsername(String username) {
        dbUsername = username;
    }

    public void setDbPassword(String password) {
        dbPassword = password;
    }

    public void setDbName(String name) {
        dbName = name;
    }

    public String getPrivUsername() {
        return privUsername;
    }

    public String getPrivPassword() {
        return privPassword;
    }

    public String getDbHostname() {
        return dbHostname;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbName() {
        return dbName;
    }
}