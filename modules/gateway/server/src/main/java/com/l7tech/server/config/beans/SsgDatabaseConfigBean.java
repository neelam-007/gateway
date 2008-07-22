package com.l7tech.server.config.beans;

import com.l7tech.server.config.SharedWizardInfo;

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

    public SsgDatabaseConfigBean() {
        super(NAME, DESCRIPTION);
        init();
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
        SharedWizardInfo.getInstance().getDbinfo().setPrivUsername(username);
    }

    public void setPrivPassword(String password) {
        SharedWizardInfo.getInstance().getDbinfo().setPrivPassword(password);
    }

    public void setDbHostname(String hostname) {
        SharedWizardInfo.getInstance().getDbinfo().setHostname(hostname);
    }

    public void setDbUsername(String username) {
        SharedWizardInfo.getInstance().getDbinfo().setUsername(username);
    }

    public void setDbPassword(String password) {
        SharedWizardInfo.getInstance().getDbinfo().setPassword(password);
    }

    public void setDbName(String name) {
        SharedWizardInfo.getInstance().getDbinfo().setDbName(name);
    }

    public String getPrivUsername() {
        return SharedWizardInfo.getInstance().getDbinfo().getPrivUsername();
    }

    public String getPrivPassword() {
        return SharedWizardInfo.getInstance().getDbinfo().getPrivPassword();
    }

    public String getDbHostname() {
        return SharedWizardInfo.getInstance().getDbinfo().getHostname();
    }

    public String getDbUsername() {
        return SharedWizardInfo.getInstance().getDbinfo().getUsername();
    }

    public String getDbPassword() {
        return SharedWizardInfo.getInstance().getDbinfo().getPassword();
    }

    public String getDbName() {
        return SharedWizardInfo.getInstance().getDbinfo().getDbName();
    }
}