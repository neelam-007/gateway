package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.DBInfoGetter;
import com.l7tech.server.config.DefaultLicenseChecker;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.commands.SsgDatabaseConfigCommand;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBActionsListener;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * User: megery
 * Date: Dec 19, 2005
 * Time: 9:59:16 AM
 */
public class ConfigWizardConsoleDatabaseStep extends BaseConsoleStep implements DBActionsListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleDatabaseStep.class.getName());

    private SsgDatabaseConfigBean databaseBean;
    private static final String STEP_INFO = "This step lets you create or set up a connection to the SSG database";
    private static final String HEADER_DB_CONN_TYPE = "-- Select Database Connection Type --" + getEolChar();
    private static final String HEADER_NEW_DB_INFO = "-- Information for new database --";
    private static final String HEADER_EXISTING_DB_INFO = "-- Information for existing database --";
    private static final String REPLICATED_HOSTNAME_INSTRUCTIONS = "Specify the database hostname. If you are using a replicated database, enter the hostnames of the replicated pair in failover order, separated by commas.";

    private static final String PROMPT_MAKE_NEW_DB = "1) Create a new SSG database" + getEolChar();
    private static final String PROMPT_USE_EXISTING_DB = "2) Connect to an existing SSG database" + getEolChar();

    private static final String TITLE = "Set Up the SSG Database";
    private DBActions dbActions;
    private boolean createNewDb;
    private static final String REALLY_CONFIRM_OVERWRITE = "Are you sure you want to overwrite the database? This cannot be undone.";
    private static final String WIZARD_CANNOT_PROCEED = "The Wizard cannot proceed without a valid database, please make a new selection";

    public ConfigWizardConsoleDatabaseStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + getEolChar());

        try {
            doDbConnectionTypePrompts(true);
            doDBInfoPrompts();
            storeInput();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    public String getTitle() {
        return TITLE;
    }


    private String selectDefault(String fromBean, String fromProps) {
        if (StringUtils.isEmpty(fromBean))
            return StringUtils.isEmpty(fromProps)?"":fromProps;

        return fromBean;
    }

    private void init() {
        OSSpecificFunctions osf = getParentWizard().getOsFunctions();
        try {
            dbActions = new DBActions(osf);
            dbActions.setLicenseChecker(new DefaultLicenseChecker());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(DBActions.MYSQL_CLASS_NOT_FOUND_MSG);
        }
        configBean = new SsgDatabaseConfigBean();
        databaseBean = (SsgDatabaseConfigBean)configBean;
        configCommand = new SsgDatabaseConfigCommand(configBean);
    }

    private boolean doDbTest() {
        boolean success;
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
            success = dbActions.doExistingDb(dbName, dbHostname, dbUsername, dbPassword, privUsername, privPassword, getParentWizard().getCurrentVersion(), this);
        }

        return success;
    }

    private void doDBInfoPrompts() throws IOException, WizardNavigationException {
        //if the bean didn't contain anything useful, get whatever is currently in the config file
        Map<String, String> defaults = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                DBInformation.PROP_DB_URL,
                DBInformation.PROP_DB_USERNAME,
                DBInformation.PROP_DB_PASSWORD,
        });

        String defaultHostname = null;
        String defaultDbName = null;
        String defaultDbUsername;

        String existingDBUrl = (String) defaults.get(DBInformation.PROP_DB_URL);

        if (StringUtils.isNotEmpty(existingDBUrl)) {
            Matcher matcher = DBInformation.dbUrlPattern.matcher(existingDBUrl);
            if (matcher.matches()) {
                defaultHostname = matcher.group(1);
                defaultDbName = matcher.group(2);
            }
        }

        defaultHostname = selectDefault(databaseBean.getDbHostname(), defaultHostname);
        defaultDbName = selectDefault(databaseBean.getDbName(), defaultDbName);

        defaultDbUsername = defaults.get(DBInformation.PROP_DB_USERNAME);
        defaultDbUsername = selectDefault(databaseBean.getDbUsername(), defaultDbUsername );

        if (createNewDb) printText(HEADER_NEW_DB_INFO + getEolChar());

        else printText(HEADER_EXISTING_DB_INFO + getEolChar());

        printText(REPLICATED_HOSTNAME_INSTRUCTIONS + getEolChar() + getEolChar());

        DBInformation dbInfo = new DBInfoGetter(parent.getWizardUtils(), isShowNavigation()).getDbInfo(defaultHostname, defaultDbName, defaultDbUsername, "", createNewDb);
//        databaseBean.setDbInformation(dbInfo);
//        databaseBean.setDbHostname(dbInfo.getHostname());
//        databaseBean.setDbName(dbInfo.getDbName());
//        databaseBean.setDbUsername(dbInfo.getUsername());
//        databaseBean.setDbPassword(dbInfo.getPassword());
        getParentWizard().setDbInfo(dbInfo);
    }

    private String doGetRootPasswordPrompt(String defaultPassword) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            "Please enter the password for the root database user (needed to create a new database): [" + defaultPassword + "] ",
        };
        String passwd = getSecretData(prompts, defaultPassword, null,null);
        databaseBean.setPrivPassword(passwd);
        return passwd;
    }

    private String doGetRootUsernamePrompt(String defaultUsername) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
             "Please enter the username for the root database user (needed to create a new database): [" + defaultUsername + "] ",
        };
        String username = getData(prompts, defaultUsername, (String[]) null,null);
        databaseBean.setPrivUserName(username);
        return username;
    }

//    private void doDBPasswordPrompts(String defaultDbPassword) throws IOException, WizardNavigationException {
//        String[] prompts = new String[] {
//            PROMPT_DB_PASSWORD + "[" + defaultDbPassword + "] ",
//        };
//        databaseBean.setDbPassword(getSecretData(prompts, defaultDbPassword, null, null));
//    }
//
//    private void doDBUsernamePrompts(String defaultDbUsername) throws IOException, WizardNavigationException {
//        String[] prompts = new String[] {
//            PROMPT_DB_USERNAME + "[" + defaultDbUsername + "] ",
//        };
//        databaseBean.setDbUsername(getData(prompts, defaultDbUsername, (String[]) null,null));
//    }
//
//    private void doDBNamePrompt(String defaultDbName) throws IOException, WizardNavigationException {
//        String[] prompts = new String[] {
//                PROMPT_DB_NAME + "[" + defaultDbName + "] ",
//        };
//        databaseBean.setDbName(getData(prompts, defaultDbName, (String[]) null,null).trim());
//    }
//
//    private void doDbHostnamePrompt(String defaultHostname) throws IOException, WizardNavigationException {
//        String[] prompts = new String[] {
//                PROMPT_DB_HOSTNAME + "[" + defaultHostname + "] ",
//        };
//        databaseBean.setDbHostname(getData(prompts, defaultHostname, (String[]) null,null).trim());
//    }

    private void doDbConnectionTypePrompts(boolean isCurrentDbExists) throws WizardNavigationException, IOException {
        String defaultValue = isCurrentDbExists?"2":"1";

        String[] prompts = new String[] {
                HEADER_DB_CONN_TYPE,
                PROMPT_MAKE_NEW_DB,
                PROMPT_USE_EXISTING_DB,
                "Please make a selection: [" + defaultValue + "] ",
        };

        String input;
        input = getData(
                prompts,
                "2",
                new String[] {"1","2"},
                null
        );

        createNewDb = StringUtils.equals(input, "1");
    }

    public boolean validateStep() {
        boolean invalidFields =
                StringUtils.isEmpty(databaseBean.getDbHostname()) ||
                StringUtils.isEmpty(databaseBean.getDbName()) ||
                StringUtils.isEmpty(databaseBean.getDbUsername());

        boolean testPassed = false;
        if (!invalidFields) {
            testPassed = doDbTest();
            if (!testPassed) {
                databaseBean.setPrivUserName("");
                databaseBean.setPrivPassword("");
            }
        }
        return !invalidFields && testPassed;
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
            confirmed = getConfirmationFromUser(errorMsg, "n");
            if (confirmed) {
                confirmed = getConfirmationFromUser(REALLY_CONFIRM_OVERWRITE, "n");
            } else {
                showErrorMessage(WIZARD_CANNOT_PROCEED);
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        } catch (WizardNavigationException e) {
            return false;
        }
        return confirmed;
    }

    public void showSuccess(String message) {
        if (StringUtils.isNotEmpty(message)) printText(message);
    }

    public String getPrivilegedUsername(String defaultUsername) {
        String username = null;
        try {
            username = doGetRootUsernamePrompt(defaultUsername == null?"root":defaultUsername);
        } catch (IOException e) {
            logger.severe(e.getMessage());
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
            logger.severe(e.getMessage());
        } catch (WizardNavigationException e) {
            return null;
        }

        if (passwd == null) passwd = "";
        return passwd.toCharArray();
    }

    public void hideErrorMessage() {/*noop for console wizard*/}


    public boolean getGenericUserConfirmation(String msg) {
        boolean input = false;
        try {
            input = getConfirmationFromUser(msg, "n");
        } catch (IOException e) {
            logger.severe(e.getMessage());
        } catch (WizardNavigationException e) {
            input = false;
        }
        return input;
    }

    public Map<String, String> getPrivelegedCredentials(String description, String usernamePrompt, String passwordPrompt, String defaultUsername) {
        if (StringUtils.isEmpty(defaultUsername)) defaultUsername = "root";
        if (StringUtils.isEmpty(usernamePrompt)) usernamePrompt = "Please enter the username of the root database user: [" + defaultUsername + "] ";
        if (StringUtils.isEmpty(passwordPrompt)) passwordPrompt = "Please enter the password of the root database user: ";

        try {
            String username = getData(
                    new String[] {usernamePrompt},
                    defaultUsername,
                    (String[]) null,
                    null);

            String password = getSecretData(
                    new String[] {passwordPrompt},
                    "",
                    null,
                    null);
            Map<String, String> creds = new HashMap<String, String>();
            creds.put(DBActions.USERNAME_KEY, username);
            databaseBean.setPrivUserName(username);

            creds.put(DBActions.PASSWORD_KEY, password);
            databaseBean.setPrivPassword(password);

            return creds;

        } catch (IOException e) {
            logger.severe(e.getMessage());
        } catch (WizardNavigationException e) {
            return null;
        }
        return null;
    }
}
