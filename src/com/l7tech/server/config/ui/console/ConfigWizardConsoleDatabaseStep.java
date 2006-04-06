package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.SsgDatabaseConfigCommand;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBActionsListener;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;

import java.io.*;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Dec 19, 2005
 * Time: 9:59:16 AM
 */
public class ConfigWizardConsoleDatabaseStep extends BaseConsoleStep implements DBActionsListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleDatabaseStep.class.getName());

    private SsgDatabaseConfigBean databaseBean;
    private static final String STEP_INFO = "This step lets you create or setup a connection to the SSG database";
    private static final String HEADER_DB_CONN_TYPE = "-- Select Database Connection Type --\n";
    private static final String HEADER_DB_INFO = "-- Information for new or existing database --";

    private static final String PROMPT_MAKE_NEW_DB = "1) Create a new SSG database\n";
    private static final String PROMPT_USE_EXISTING_DB = "2) Connect to an existing SSG database\n";
    private static final String PROMPT_DB_PASSWORD = "SSG Database user password: ";
    private static final String PROMPT_DB_USERNAME = "SSG Database username: ";
    private static final String PROMPT_DB_NAME = "Name of the SSG database: ";

    private static final String PROMPT_DB_HOSTNAME = "Hostname: ";

    private static final String TITLE = "SSG Database Setup";
    private DBActions dbActions;

//    private static final String MYSQL_CLASS_NOT_FOUND_MSG = "Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard";
//    private static final String GENERIC_DBCREATE_ERROR_MSG = "There was an error while attempting to create the database. Please try again";

    public ConfigWizardConsoleDatabaseStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
        init();
    }

    public String getTitle() {
        return TITLE;
    }

    private boolean doDbTest() {
        boolean success = true;
        boolean createNewDb = databaseBean.isCreateNewDb();
        String privUsername = databaseBean.getPrivUsername();
        String privPassword = databaseBean.getPrivPassword();
        String dbHostname = databaseBean.getDbHostname();
        String dbName = databaseBean.getDbName();
        String dbUsername = databaseBean.getDbUsername();
        String dbPassword = databaseBean.getDbPassword();
        
        if (createNewDb) {
            success = dbActions.doCreateDb(privUsername, privPassword, dbHostname, dbName, dbUsername, dbPassword, false, this);
        }
        else {
            success = dbActions.doExistingDb(dbName, dbHostname, dbUsername, dbPassword, privUsername, privPassword, getParent().getCurrentVersion(), this);
        }

        return success;
    }

    private boolean getConfirmationFromUser(String message) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            message + " : [n]",
        };

        String input = getData(prompts, "n");
        if (input != null) {
            return (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y"));
        }
        return false;

    }

    //DBActionsListener Implementations
    public  void showErrorMessage(String errorMsg) {
        out.println("****  " + errorMsg + "  ****");
        out.flush();
    }

    public boolean getOverwriteConfirmationFromUser(String dbName) {
        String errorMsg = "The database named \"" + dbName + "\" already exists. Would you like to overwrite it?";
        boolean confirmed = false;
        try {
            confirmed = getConfirmationFromUser(errorMsg);
            if (confirmed) {
                confirmed = getConfirmationFromUser("Are you sure you wan to overwrite the database? This cannot be undone.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return false;
        }
        return confirmed;
    }

    public void confirmCreateSuccess() {
        out.println("Database Successfully Created");
        out.flush();
    }

    public char[] getPrivilegedPassword() {
        return null;
    }

    public void hideErrorMessage() {}

    public boolean getGenericUserConfirmation(String msg) {
        String[] prompts = new String[] {
            msg + " : [n]",
        };

        String input = null;
        try {
            input = getData(prompts, "n");
            return (input != null && (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return false;
        }
        return false;
    }

    private void init() {
        try {
            dbActions = new DBActions();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(DBActions.MYSQL_CLASS_NOT_FOUND_MSG);
        }
        configBean = new SsgDatabaseConfigBean(osFunctions);
        databaseBean = (SsgDatabaseConfigBean)configBean;
        command = new SsgDatabaseConfigCommand(configBean);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        out.println(STEP_INFO);
        out.flush();

        try {
            boolean isNewDb = doDbConnectionTypePrompts(true);
            doDBInfoPrompts(isNewDb);
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String selectDefault(String fromBean, String fromProps) {
        return StringUtils.isEmpty(fromBean)?fromProps:fromBean;
    }

    private void doDBInfoPrompts(boolean isNewDb) throws IOException, WizardNavigationException {
        //if the bean didn't contain anything useful, get whatever is currently in the config file
        Map defaults = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                SsgDatabaseConfigBean.PROP_DB_URL,
                SsgDatabaseConfigBean.PROP_DB_USERNAME,
                SsgDatabaseConfigBean.PROP_DB_PASSWORD,
        });

        String defaultHostname = null;
        String defaultDbName = null;
        String defaultDbUsername = null;
        String defaultDbPassword = null;

        String existingDBUrl = (String) defaults.get(SsgDatabaseConfigBean.PROP_DB_URL);

        if (StringUtils.isNotEmpty(existingDBUrl)) {
            Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
            if (matcher.matches()) {
                defaultHostname = matcher.group(1);
                defaultDbName = matcher.group(2);
            }
        }

        defaultHostname = selectDefault(databaseBean.getDbHostname(), defaultHostname);
        defaultDbName = selectDefault(databaseBean.getDbName(), defaultDbName);
        defaultDbUsername = selectDefault(databaseBean.getDbUsername(), (String) defaults.get(SsgDatabaseConfigBean.PROP_DB_USERNAME));
        defaultDbPassword = selectDefault(databaseBean.getDbPassword(), (String) defaults.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD));

        out.println(HEADER_DB_INFO);
        out.flush();

        if (isNewDb) {
            doGetRootUsernamePrompt("root");
            doGetRootPasswordPrompt("");
        }
        doDbHostnamePrompt(defaultHostname);
        doDBNamePrompt(defaultDbName);
        doDBUsernamePrompts(defaultDbUsername);
        doDBPasswordPrompts(defaultDbPassword);

    }

    private void doGetRootPasswordPrompt(String defaultPassword) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            "Please enter the root database username (needed to create a new database): [" + defaultPassword + "] ",
        };
        databaseBean.setPrivPassword(getData(prompts, defaultPassword));
    }

    private void doGetRootUsernamePrompt(String defaultUsername) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            "Please enter the root database users' password (needed to create a new database): [" + defaultUsername + "] ",
        };
        databaseBean.setPrivUserName(getData(prompts, defaultUsername));
    }

    private void doDBPasswordPrompts(String defaultDbPassword) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            PROMPT_DB_PASSWORD + "[" + defaultDbPassword + "]",
        };
        databaseBean.setDbPassword(getData(prompts, defaultDbPassword));
    }

    private void doDBUsernamePrompts(String defaultDbUsername) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            PROMPT_DB_USERNAME + "[" + defaultDbUsername + "]",
        };
        databaseBean.setDbUsername(getData(prompts, defaultDbUsername));
    }

    private void doDBNamePrompt(String defaultDbName) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                PROMPT_DB_NAME + "[" + defaultDbName + "]",
        };
        databaseBean.setDbName(getData(prompts, defaultDbName).trim());
    }

    private void doDbHostnamePrompt(String defaultHostname) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                PROMPT_DB_HOSTNAME + "[" + defaultHostname + "]",
        };
        databaseBean.setDbHostname(getData(prompts, defaultHostname).trim());
    }

    private boolean doDbConnectionTypePrompts(boolean isCurrentDbExists) throws WizardNavigationException, IOException {
        String defaultValue = isCurrentDbExists?"2":"1";


        String[] prompts = new String[] {
                HEADER_DB_CONN_TYPE,
                PROMPT_MAKE_NEW_DB,
                PROMPT_USE_EXISTING_DB,
                "please make a selection: [" + defaultValue + "]",
        };
        String input = getData(prompts, "2");
        boolean isNewDb = input != null && input.trim().equals("1");
        databaseBean.setCreateDb(isNewDb);
        return isNewDb;
    }

    boolean validateStep() {
        boolean invalidFields =
                StringUtils.isEmpty(databaseBean.getDbHostname()) ||
                StringUtils.isEmpty(databaseBean.getDbName()) ||
                StringUtils.isEmpty(databaseBean.getDbUsername());

        return !invalidFields && doDbTest();
    }

}
