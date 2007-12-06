package com.l7tech.server.config.beans;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.db.DBInformation;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public final static String PROP_DB_USERNAME = "hibernate.connection.username";
    public final static String PROP_DB_URL = "hibernate.connection.url";
    public final static String PROP_DB_PASSWORD = "hibernate.connection.password" ;
    public final static Pattern dbUrlPattern = Pattern.compile("^.*//(.*)/(.*)\\?.*$");
    private DBInformation dbInformation;

    public SsgDatabaseConfigBean() {
        super(NAME, DESCRIPTION);
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

        String dbUsername = defaults.get(PROP_DB_USERNAME);
        String dbPassword = defaults.get(PROP_DB_PASSWORD);
        String existingDBUrl = (String) defaults.get(SsgDatabaseConfigBean.PROP_DB_URL);

        String dbHostname = "";
        String dbName = "";

        if (StringUtils.isNotEmpty(existingDBUrl)) {
            Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
            if (matcher.matches()) {
                dbHostname = matcher.group(1);
                dbName = matcher.group(2);
            }
            dbInformation.setHostname(dbHostname);
            dbInformation.setDbName(dbName);
            dbInformation.setUsername(dbUsername);
            dbInformation.setPassword(dbPassword);
        } else {
            throw new IOException("no database url was found while reading the configfile [" + configFile + "].");
        }
    }

    private void init() {
        dbInformation = new DBInformation();
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
        dbInformation.setPrivUsername(username);
    }

    public void setPrivPassword(String password) {
        dbInformation.setPrivPassword(password);
    }

    public void setDbHostname(String hostname) {
        dbInformation.setHostname(hostname);
    }

    public void setDbUsername(String username) {
        dbInformation.setUsername(username);
    }

    public void setDbPassword(String password) {
        dbInformation.setPassword(password);
    }

    public void setDbName(String name) {
        dbInformation.setDbName(name);
    }

    public String getPrivUsername() {
        return dbInformation.getPrivUsername();
    }

    public String getPrivPassword() {
        return dbInformation.getPrivPassword();
    }

    public String getDbHostname() {
        return dbInformation.getHostname();
    }

    public String getDbUsername() {
        return dbInformation.getUsername();
    }

    public String getDbPassword() {
        return dbInformation.getPassword();
    }

    public String getDbName() {
        return dbInformation.getDbName();
    }

    public DBInformation getDbInformation() {
        return dbInformation;
    }

    public void setDbInformation(DBInformation dbInformation) {
        this.dbInformation = dbInformation;
    }
}