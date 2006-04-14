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
    private static final String HEADER_NEW_DB_INFO = "-- Information for new database --";
    private static final String HEADER_EXISTING_DB_INFO = "-- Information for existing database --";

    private static final String PROMPT_MAKE_NEW_DB = "1) Create a new SSG database\n";
    private static final String PROMPT_USE_EXISTING_DB = "2) Connect to an existing SSG database\n";
    private static final String PROMPT_DB_PASSWORD = "SSG Database user password: ";
    private static final String PROMPT_DB_USERNAME = "SSG Database username: ";
    private static final String PROMPT_DB_NAME = "Name of the SSG database: ";

    private static final String PROMPT_DB_HOSTNAME = "Hostname: ";

    private static final String TITLE = "SSG Database Setup";
    private DBActions dbActions;

    public ConfigWizardConsoleDatabaseStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
        init();
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + "\n");

        try {
            boolean isNewDb = doDbConnectionTypePrompts(true);
            doDBInfoPrompts(isNewDb);
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return TITLE;
    }

    private boolean getConfirmationFromUser(String message) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                message + " : [n]",
        };

        String input = getData(prompts, "n", true);
        if (input != null) {
            return (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y"));
        }
        return false;

    }


    private String selectDefault(String fromBean, String fromProps) {
        return StringUtils.isEmpty(fromBean)?fromProps:fromBean;
    }

    private void init() {
        try {
            dbActions = new DBActions();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(DBActions.MYSQL_CLASS_NOT_FOUND_MSG);
        }
        configBean = new SsgDatabaseConfigBean(osFunctions);
        databaseBean = (SsgDatabaseConfigBean)configBean;
        configCommand = new SsgDatabaseConfigCommand(configBean);
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
        String defaultRootUsername = null;
        String defaultRootPasswd = null;

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

        defaultRootUsername = selectDefault(databaseBean.getPrivUsername(), "root");
        defaultRootPasswd = selectDefault(databaseBean.getPrivPassword(), "");

        if (isNewDb) {
            printText(HEADER_NEW_DB_INFO + "\n");
            doGetRootUsernamePrompt(defaultRootUsername);
            doGetRootPasswordPrompt(defaultRootPasswd);
        }
        else {
            printText(HEADER_EXISTING_DB_INFO + "\n");
        }

        doDbHostnamePrompt(defaultHostname);
        doDBNamePrompt(defaultDbName);
        doDBUsernamePrompts(defaultDbUsername);
        doDBPasswordPrompts(defaultDbPassword);
    }

    private String doGetRootPasswordPrompt(String defaultPassword) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            "Please enter the root database users' password (needed to create a new database): [" + defaultPassword + "] ",
        };
        String passwd = getData(prompts, defaultPassword, true);
        databaseBean.setPrivPassword(passwd);
        return passwd;
    }

    private String doGetRootUsernamePrompt(String defaultUsername) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
             "Please enter the root database username (needed to create a new database): [" + defaultUsername + "] ",
        };
        String username = getData(prompts, defaultUsername, true);
        databaseBean.setPrivUserName(username);
        return username;
    }

    private void doDBPasswordPrompts(String defaultDbPassword) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            PROMPT_DB_PASSWORD + "[" + defaultDbPassword + "]",
        };
        databaseBean.setDbPassword(getData(prompts, defaultDbPassword, true));
    }

    private void doDBUsernamePrompts(String defaultDbUsername) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            PROMPT_DB_USERNAME + "[" + defaultDbUsername + "]",
        };
        databaseBean.setDbUsername(getData(prompts, defaultDbUsername, true));
    }

    private void doDBNamePrompt(String defaultDbName) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                PROMPT_DB_NAME + "[" + defaultDbName + "]",
        };
        databaseBean.setDbName(getData(prompts, defaultDbName, true).trim());
    }

    private void doDbHostnamePrompt(String defaultHostname) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                PROMPT_DB_HOSTNAME + "[" + defaultHostname + "]",
        };
        databaseBean.setDbHostname(getData(prompts, defaultHostname, true).trim());
    }

    private boolean doDbConnectionTypePrompts(boolean isCurrentDbExists) throws WizardNavigationException, IOException {
        String defaultValue = isCurrentDbExists?"2":"1";


        String[] prompts = new String[] {
                HEADER_DB_CONN_TYPE,
                PROMPT_MAKE_NEW_DB,
                PROMPT_USE_EXISTING_DB,
                "please make a selection: [" + defaultValue + "]",
        };
        String input = getData(prompts, "2", true);
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

//DBActionsListener Implementations

    public  void showErrorMessage(String errorMsg) {
        printText(new String[] {
            "****  " + errorMsg + "  ****\n",
            "\n",
        });
    }

    public boolean getOverwriteConfirmationFromUser(String dbName) {
        String errorMsg = "The database named \"" + dbName + "\" already exists. Would you like to overwrite it?";
        boolean confirmed = false;
        try {
            confirmed = getConfirmationFromUser(errorMsg);
            if (confirmed) {
                confirmed = getConfirmationFromUser("Are you sure you wan to overwrite the database? This cannot be undone.");
            } else {
                showErrorMessage("The Wizard cannot proceed without a valid database, please make a new selection");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return false;
        }
        return confirmed;
    }

    public void confirmCreateSuccess() {
        printText("Database Successfully Created\n");
    }

    public String getPrivilegedUsername() {
        String username = null;
        try {
            username = doGetRootUsernamePrompt("root");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return null;
        }
        return username;
    }

    public char[] getPrivilegedPassword() {
        String passwd = null;
        try {
            passwd = doGetRootPasswordPrompt("");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return null;
        }
        return passwd.toCharArray();
    }

    public void hideErrorMessage() {/*noop for console wizard*/}


    public boolean getGenericUserConfirmation(String msg) {
        String[] prompts = new String[] {
            msg + " : [n]",
        };

        String input = null;
        try {
            input = getData(prompts, "n", true);
            return (input != null && (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WizardNavigationException e) {
            return false;
        }
        return false;
    }
}
