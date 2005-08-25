package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.DatabaseConfigCommand;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 11:41:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseConfigBean extends BaseConfigurationBean {

    private boolean isRemote;
    private String dbHostname;
    private String dbUsername;
    private String dbName;
    private char[] dbPassword;

    private final static String NAME = "Database Configuration";
    private final static String DESCRIPTION = "Configures the database properties for an SSG";

    private final static String SETUP_REMOTE_DB_INFO = "Setup a connection to a remote database";
    private final static String SETUP_LOCAL_DB_INFO = "Setup a connection to a local database";
    private boolean doDbConfig;
    private static final String SKIPPING_CONFIGURATION_INFO = "Can't connect to database - configuration skipped";


    public DatabaseConfigBean(OSSpecificFunctions osFunctions) {
        super(NAME, DESCRIPTION, osFunctions);
        ELEMENT_KEY = this.getClass().getName();
        init();
    }

    private void init() {
        doDbConfig = true;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
    }

    public String getDbHostname() {
        return dbHostname;
    }

    public void setDbHostname(String dbHostname) {
        this.dbHostname = dbHostname;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public char[] getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(char[] dbPassword) {
        this.dbPassword = dbPassword;
    }

    void reset() {
        isRemote = false;
        dbHostname = "";
        dbUsername ="";
        dbName = "";
        dbPassword = new char[0];

    }

    public String[] explain() {
        ArrayList explanations = new ArrayList();
        explanations.add(getName() + " - " + getDescription());
        if (isDbConfigOn()) {
            if (isRemote()) {
                explanations.add(insertTab + SETUP_REMOTE_DB_INFO);
                explanations.add(insertTab + "    HOSTNAME = " + getDbHostname());
                explanations.add(insertTab + "    USERNAME = " + getDbUsername());
                explanations.add(insertTab + "    DATABASE = " + getDbUsername());
            } else {
                explanations.add(insertTab + SETUP_LOCAL_DB_INFO);
            }
        }
        else {
            explanations.add(insertTab + SKIPPING_CONFIGURATION_INFO);
            explanations.add(insertTab + "tried to connect to:");
            explanations.add(insertTab + "    HOSTNAME = " + getDbHostname());
            explanations.add(insertTab + "    USERNAME = " + getDbUsername());
            explanations.add(insertTab + "    DATABASE = " + getDbUsername());
        }

        return (String[]) explanations.toArray(new String[explanations.size()]);
    }

    public void setDBConfigOn(boolean dbConfigOn) {
        this.doDbConfig = dbConfigOn;
    }

    public boolean isDbConfigOn() {
        return doDbConfig;
    }
}
