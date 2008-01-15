package com.l7tech.server.config.db;

import com.l7tech.server.config.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBInformation implements Serializable {
    private String hostname = "";
    private String dbName = "";
    private String username = "";
    private String password = "";
    private String privUsername = "";
    private String privPassword = "";
    private boolean isNew;
    public final static String PROP_DB_USERNAME = "hibernate.connection.username";
    public final static String PROP_DB_URL = "hibernate.connection.url";
    public final static String PROP_DB_PASSWORD = "hibernate.connection.password" ;
    public final static Pattern dbUrlPattern = Pattern.compile("^.*//(.*)/(.*)\\?.*$");

    public DBInformation(String hostname, String dbName, String username, String password, String privUsername, String privPassword) {
        this.hostname = hostname;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
        this.privUsername = privUsername;
        this.privPassword = privPassword;
    }

    public DBInformation(String dbConfigFile) throws IOException {
        initFromConfigFile(dbConfigFile);
    }

    public DBInformation() {
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivUsername() {
        return privUsername;
    }

    public void setPrivUsername(String privUsername) {
        this.privUsername = privUsername;
    }

    public String getPrivPassword() {
        return privPassword;
    }

    public void setPrivPassword(String privPassword) {
        this.privPassword = privPassword;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    private void initFromConfigFile(String configFile) throws IOException {
        Map<String, String> defaults = PropertyHelper.getProperties(configFile, new String[] {
                PROP_DB_URL,
                PROP_DB_USERNAME,
                PROP_DB_PASSWORD
        });

        String dbUsername = defaults.get(PROP_DB_USERNAME);
        String dbPassword = defaults.get(PROP_DB_PASSWORD);
        String existingDBUrl = (String) defaults.get(PROP_DB_URL);

        String dbHostname = "";
        String dbName = "";

        if (StringUtils.isNotEmpty(existingDBUrl)) {
            Matcher matcher = dbUrlPattern.matcher(existingDBUrl);
            if (matcher.matches()) {
                dbHostname = matcher.group(1);
                dbName = matcher.group(2);
            }
            setHostname(dbHostname);
            setDbName(dbName);
            setUsername(dbUsername);
            setPassword(dbPassword);
        } else {
            throw new IOException("no database url was found while reading the configfile [" + configFile + "].");
        }
    }
}
