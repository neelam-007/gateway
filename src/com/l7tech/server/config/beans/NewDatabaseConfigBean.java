package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:42:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewDatabaseConfigBean extends BaseConfigurationBean {
    private final static String NAME = "Database Configuration";
    private final static String DESCRIPTION = "Configures the database properties for an SSG";

    private boolean isCreateNewDb;
    private String privUsername;
    private String privPassword;
    private String dbHostname;
    private String dbUsername;
    private String dbPassword;
    private String dbName;

    public NewDatabaseConfigBean(OSSpecificFunctions osFunctions) {
        super(NAME, DESCRIPTION, osFunctions);
        ELEMENT_KEY = this.getClass().getName();
        init();
    }

    private void init() {
        isCreateNewDb = true;
    }

    void reset() {
    }

    public String[] explain() {
        ArrayList explanations = new ArrayList();
        explanations.add(getName() + " - " + getDescription());
        explanations.add(insertTab + "Setup connection to a database:");
        explanations.add(insertTab + "    HOSTNAME = " + getDbHostname());
        explanations.add(insertTab + "    USERNAME = " + getDbUsername());
        explanations.add(insertTab + "    DATABASE = " + getDbName());
        return (String[]) explanations.toArray(new String[explanations.size()]);
    }

    public void setCreateDb(boolean selected) {
        isCreateNewDb = selected;
    }

    public boolean isCreateNewDb() {
        return isCreateNewDb;
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