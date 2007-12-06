package com.l7tech.server.config.db;

import java.io.Serializable;

public class DBInformation implements Serializable {
    private String hostname = "";
    private String dbName = "";
    private String username = "";
    private String password = "";
    private String privUsername = "";
    private String privPassword = "";
    private boolean isNew;

    public DBInformation(String hostname, String dbName, String username, String password, String privUsername, String privPassword) {
        this.hostname = hostname;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
        this.privUsername = privUsername;
        this.privPassword = privPassword;
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
}
