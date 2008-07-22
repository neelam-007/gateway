package com.l7tech.server.config;

import com.l7tech.server.config.ui.console.ConsoleWizardUtils;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.IOException;

/**
 * User: megery
 * Date: Dec 4, 2007
 * Time: 11:11:03 AM
 */
public class DBInfoGetter {
    private static final String PROMPT_DB_PASSWORD = "SSG Database user password: ";
    private static final String PROMPT_DB_PASSWORD_CONFIRM = "SSG Database user password again: ";
    private static final String PROMPT_DB_USERNAME = "SSG Database username: ";
    private static final String PROMPT_DB_NAME = "Name of the SSG database: ";
    private static final String PROMPT_DB_HOSTNAME = "Hostname: ";

    private ConsoleWizardUtils utils;
    boolean isNavAware;

    public DBInfoGetter(ConsoleWizardUtils utils, boolean isNavAware) {
        this.utils = utils;
        this.isNavAware = isNavAware;
    }

    public DBInformation getDbInfo(String defaultHostname, String defaultDbName, String defaultDbUsername, String defaultPassword, boolean createNewDb) throws IOException, WizardNavigationException {
        String dbHostname = doPrompts(defaultHostname, PROMPT_DB_HOSTNAME, false);
        String dbName = doPrompts(defaultDbName, PROMPT_DB_NAME, false);
        String dbUsername = doPrompts(defaultDbUsername, PROMPT_DB_USERNAME, false);
        //don't pass in a default password so a user can enter a blank one if so desired
        String dbPassword = doPrompts("", PROMPT_DB_PASSWORD, true);

        if (createNewDb) {
            String dbPasswordConfirm = doPrompts("", PROMPT_DB_PASSWORD_CONFIRM, true);
            while(!dbPassword.equals(dbPasswordConfirm)) {
                utils.printText("The passwords do not match. Please try again.\n");
                dbPassword = doPrompts("", PROMPT_DB_PASSWORD, true);
                dbPasswordConfirm = doPrompts("", PROMPT_DB_PASSWORD_CONFIRM, true);
            }
        }

        DBInformation dbInfo = new DBInformation(dbHostname, dbName, dbUsername, dbPassword, null, null);
        dbInfo.setNew(createNewDb);
        return dbInfo;
    }

    private String doPrompts(String defaultValue, String prompt, boolean secret) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            prompt + "[" + defaultValue + "] ",
        };
        if (secret)
            return utils.getSecretData(prompts, defaultValue, isNavAware, null, null);
        else
            return utils.getData(prompts, defaultValue, isNavAware, (String[]) null,null);
    }
}
