package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBActionsListener;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.commands.SsgDatabaseConfigCommand;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:37:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardNewDBPanel extends ConfigWizardStepPanel implements DBActionsListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardNewDBPanel.class.getName());

    private final static String LOCALDB_HOSTNAME="localhost";
    private final static String LOCALDB_DBNAME="ssg";
    private final static String LOCALDB_USER="gateway";
    private final static String LOCALDB_PASSWORD = "7layer";

    private final static String DEFAULT_PRIV_USER = "root";

    private JPanel mainPanel;
    private JRadioButton createNewDb;
    private JRadioButton existingDb;
    private JTextField privUsername;
    private JPasswordField privPassword;
    private JTextField dbHostname;
    private JTextField dbName;
    private JTextField dbUsername;
    private JTextField dbPassword;
    private JTextPane errorMessagePane;
    private JLabel privUserLabel;
    private JLabel privPasswordLabel;

    private DBActions dbActions;

    private static final int CONFIRM_OVERWRITE_YES = 0;
    private static final int CONFIRM_OVERWRITE_NO = 1;

    private static final String CONNECTION_UNSUCCESSFUL_MSG = "Connection to the database was unsuccessful - see warning/errors for details";
    private static final String CONNECTION_SUCCESSFUL_MSG = "Connection to the database was a success";
    private static final String GENERIC_DBCREATE_ERROR_MSG = "There was an error while attempting to create the database. Please try again";
    private static final String GENERIC_DBCONNECT_ERROR_MSG = "There was an error while attempting to connect to the database. Please try again";
    private static final String MYSQL_CLASS_NOT_FOUND_MSG = "Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard";
    private static final String CONFIG_FILE_NOT_FOUND_MSG = "Could not find the database configuration file. Cannot determine existing configuration.";
    private static final String CONFIG_FILE_IO_ERROR_MSG = "Error while reading the database configuration file. Cannot determine existing configuration.";
    private static final String MISSING_FIELDS_MSG = "Some required fields are missing. \n\n" +
                        "Please fill in all the required fields (indicated in red above)";


    private ActionListener dbActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            enableFields();
        }
    };
    private JLabel dbHostnameLabel;
    private JLabel dbNameLabel;
    private JLabel dbUsernameLabel;
    private JLabel dbPasswordLabel;


    private void enableFields() {
        boolean isEnabled = false;
        if (createNewDb.isSelected()) {
            isEnabled = true;
        }
        privUsername.setEnabled(isEnabled);
        privPassword.setEnabled(isEnabled);
        privUserLabel.setEnabled(isEnabled);
        privPasswordLabel.setEnabled(isEnabled);
    }

    public ConfigWizardNewDBPanel(OSSpecificFunctions functions) {
        super(null, functions);
        init();
    }

    public ConfigWizardNewDBPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = new SsgDatabaseConfigBean(osFunctions);
        configCommand = new SsgDatabaseConfigCommand(configBean);
        try {
            dbActions = new DBActions();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(MYSQL_CLASS_NOT_FOUND_MSG);
        }

        ButtonGroup group = new ButtonGroup();
        group.add(createNewDb);
        group.add(existingDb);

        createNewDb.setSelected(true);
        enableFields();

        createNewDb.addActionListener(dbActionListener);
        existingDb.addActionListener(dbActionListener);

        stepLabel = "Setup The SSG Database";

        privUsername.setText(DEFAULT_PRIV_USER);
        dbHostname.setText(LOCALDB_HOSTNAME);
        dbName.setText(LOCALDB_DBNAME);
        dbUsername.setText(LOCALDB_USER);
        dbPassword.setText(LOCALDB_PASSWORD);

        errorMessagePane.setBackground(mainPanel.getBackground());
        errorMessagePane.setForeground(Color.RED);
        errorMessagePane.setVisible(false);


        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateModel(HashMap settings) {
        SsgDatabaseConfigBean dbConfigBean = (SsgDatabaseConfigBean)configBean;

        boolean isNew = createNewDb.isSelected();
        dbConfigBean.setCreateDb(isNew);

        dbConfigBean.setPrivUserName(privUsername.getText());
        dbConfigBean.setPrivPassword(new String(privPassword.getPassword()));

        dbConfigBean.setDbHostname(dbHostname.getText());
        dbConfigBean.setDbUsername(dbUsername.getText());
        dbConfigBean.setDbPassword(dbPassword.getText());
        dbConfigBean.setDbName(dbName.getText());
    }

    protected void updateView(HashMap settings) {
        SsgDatabaseConfigBean dbBean = null;
        String existingDbUsername = null;
        String existingDBUrl = null;
        String existingDbHostname = null;
        String existingDbName = null;
        String existingDbPassword = null;

        //first try and get the bean information if it exists and use that.
        boolean beanInitialized = false;
        if (configBean != null) {
            dbBean = (SsgDatabaseConfigBean) configBean;
            existingDbUsername = dbBean.getDbUsername();
            existingDbName = dbBean.getDbName();
            existingDbHostname = dbBean.getDbHostname();
            existingDbPassword = dbBean.getDbPassword();

            beanInitialized = (
                    StringUtils.isNotEmpty(existingDbUsername) &&
                    StringUtils.isNotEmpty(existingDbName) &&
                    StringUtils.isNotEmpty(existingDbHostname));
                    //don't include check for the password here since it MIGHT be empty on purpose
        } else {
            beanInitialized = false;
        }


        if (!beanInitialized) {
            try {
                Map props = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                    SsgDatabaseConfigBean.PROP_DB_USERNAME,
                    SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                    SsgDatabaseConfigBean.PROP_DB_URL
                });
                existingDbUsername =(String) props.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
                existingDbPassword = (String) props.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
                existingDBUrl = (String) props.get(SsgDatabaseConfigBean.PROP_DB_URL);
                if (StringUtils.isNotEmpty(existingDBUrl)) {
                    Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
                    if (matcher.matches()) {
                        existingDbHostname = matcher.group(1);
                        existingDbName = matcher.group(2);
                    }
                }
            } catch (FileNotFoundException e) {
                logger.warning(CONFIG_FILE_NOT_FOUND_MSG);
                logger.warning(e.getMessage());
            } catch (IOException e) {
                logger.warning(CONFIG_FILE_IO_ERROR_MSG);
                logger.warning(e.getMessage());
            }
        }

        if (StringUtils.isNotEmpty(existingDbUsername)) {
            dbUsername.setText(existingDbUsername);
        }
        if (StringUtils.isNotEmpty(existingDbHostname)) {
            dbHostname.setText(existingDbHostname);
        }

        if (StringUtils.isNotEmpty(existingDbName)) {
            dbName.setText(existingDbName);
        }

        if (StringUtils.isNotEmpty(existingDbPassword)) {
            dbPassword.setText(existingDbPassword);
        }

    }

    public boolean onNextButton() {
        resetLabelColours();
        boolean isOk = true;
        //enforce that all the required fields are present.
        isOk = checkRequiredFields();

        String rootUsername = privUsername.getText();
        String rootPassword = String.valueOf(privPassword.getPassword());

        String username = dbUsername.getText();
        String password = dbPassword.getText();

        String currentVersion = ConfigurationWizard.getCurrentVersion();
        String theDbName = dbName.getText();
        String hostname = dbHostname.getText();

        if (!isOk) { // if we've passed the required fields checks
            showErrorMessage(MISSING_FIELDS_MSG);
        } else {
            if (createNewDb.isSelected()) {
                isOk = dbActions.doCreateDb(rootUsername, rootPassword, hostname, theDbName, username, password, false, this);
            }
            else {
                isOk = dbActions.doExistingDb(theDbName, hostname, username, password, rootUsername, rootPassword, currentVersion, this);
            }
        }

        return isOk;
    }

    private void resetLabelColours() {
        privUserLabel.setForeground(Color.BLACK);
        privPasswordLabel.setForeground(Color.BLACK);
        dbHostnameLabel.setForeground(Color.BLACK);
        dbNameLabel.setForeground(Color.BLACK);
        dbUsernameLabel.setForeground(Color.BLACK);
        dbPasswordLabel.setForeground(Color.BLACK);
    }

    private boolean checkRequiredFields() {
        boolean ok = true;
        if (createNewDb.isSelected()) {
            if (StringUtils.isEmpty(privUsername.getText())) {
                doInformMissingFields(privUserLabel);
                ok = false;
            }
            else {
                ok = true;
            }
        }

        if (StringUtils.isEmpty(dbHostname.getText())) {
            doInformMissingFields(dbHostnameLabel);
            ok = false;
        }
        if (StringUtils.isEmpty(dbName.getText())) {
            doInformMissingFields(dbNameLabel);
            ok = false;
        }
        if (StringUtils.isEmpty(dbUsername.getText())) {
            doInformMissingFields(dbUsernameLabel);
            ok = false;
        }
        return ok;
    }

    private void doInformMissingFields(JLabel theLabel) {
        theLabel.setForeground(Color.RED);
    }

//    private boolean doExistingDb(String hostname, String name, String username, String password) {
//        String errorMsg;
//        boolean isOk = false;
//
//        DBActions.DBActionsResult status;
//        logger.info("Attempting to connect to an existing database (" + hostname + "/" + name + ")" + "using username/password \"" + username + "/" + password + "\"");
//        status = dbActions.checkExistingDb(hostname, name, username, password);
//        if (status.getStatus() == DBActions.DB_SUCCESS) {
//            logger.info(CONNECTION_SUCCESSFUL_MSG);
//            logger.info("Now Checking database version");
//            String dbVersion = dbActions.checkDbVersion(hostname, name, username, password);
//            String currentVersion = getParentWizard().getCurrentVersion();
//            if (dbVersion == null) {
//                errorMsg = "The " + dbName + " database does not appear to be a valid SSG database.";
//                logger.warning(errorMsg);
//                showErrorMessage(errorMsg);
//                isOk = false;
//            } else {
//                if (dbVersion.equals(currentVersion)) {
//                    logger.info("Database version is correct (" + dbVersion + ")");
//                    hideErrorMessage();
//                    isOk = true;
//                }
//                else {
//                    errorMsg = "The current database version (" + dbVersion+ ") is incorrect (needs to be " + currentVersion + ") and needs to be upgraded" ;
//                    logger.warning(errorMsg);
//                    try {
//                        isOk = doDbUpgrade(hostname, name, currentVersion, dbVersion);
//                    } catch (IOException e) {
//                        errorMsg = "There was an error while attempting to upgrade the database";
//                        logger.severe(errorMsg);
//                        logger.severe(e.getMessage());
//                        showErrorMessage(errorMsg);
//                        isOk = true;
//                    }
//                }
//            }
//        } else {
//            switch (status.getStatus()) {
//                case DBActions.DB_UNKNOWNHOST_FAILURE:
//                    errorMsg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
//                    logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
//                    logger.warning(errorMsg);
//                    showErrorMessage(errorMsg);
//                    isOk = false;
//                    break;
//                case DBActions.DB_AUTHORIZATION_FAILURE:
//                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
//                    errorMsg = "There was an authentication error when attempting to connect to the database \"" + name + "\" using the username \"" +
//                            username + "\" and password \"" + password + "\". Please check your input and try again.";
//                    showErrorMessage(errorMsg);
//                    logger.warning("There was an authentication error when attempting to connect to the database \"" + name + "\" using the username \"" +
//                            username + "\" and password \"" + password + "\".");
//                    isOk = false;
//                    break;
//                case DBActions.DB_UNKNOWNDB_FAILURE:
//                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
//                    errorMsg = "Could not connect to the database \"" + name + "\". The database does not exist or the user \"" + username + "\" does not have permission to access it." +
//                            "Please check your input and try again.";
//                    showErrorMessage(errorMsg);
//                    logger.warning("Could not connect to the database \"" + name + "\". The database does not exist.");
//                    isOk = false;
//                    break;
//                default:
//                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
//                    errorMsg = GENERIC_DBCONNECT_ERROR_MSG;
//                    showErrorMessage(errorMsg);
//                    logger.warning("There was an unknown error while attempting to connect to the database.");
//                    isOk = false;
//                    break;
//            }
//        }
//        return isOk;
//    }

//    private boolean doDbUpgrade(String hostname, String dbName, String currentVersion, String dbVersion) throws IOException {
//        boolean isOk = false;
//        String msg = "The \"" + dbName + "\" database appears to be a " + dbVersion + " database and needs to be upgraded to " + currentVersion + "\n" +
//                "Would you like to attempt an upgrade?";
//
//        int result = JOptionPane.showConfirmDialog(this,msg,
//                "Incorrect Database Version", JOptionPane.YES_NO_OPTION);
//
//        if (result == JOptionPane.OK_OPTION) {
//            char[]  passwd = privPassword.getPassword();
//            if (passwd == null || passwd.length == 0) {
//                passwd = showGetPasswordDialog("Please enter the root database password:");
//            }
//            logger.info("Attempting to upgrade the existing database \"" + dbName+ "\"");
////            String errorMessage = null;
//            DBActions.DBActionsResult upgradeResult = dbActions.upgradeDbSchema(hostname, privUsername.getText(), new String(passwd), dbName, dbVersion, currentVersion, osFunctions);
//            switch (upgradeResult.getStatus()) {
//                case DBActions.DB_SUCCESS:
//                    logger.info("Database successfully upgraded");
//                    isOk = true;
//                    break;
//                case DBActions.DB_AUTHORIZATION_FAILURE:
//                    msg = "login to the database with the supplied credentials failed, please try again";
//                    logger.warning(msg);
//                    showErrorMessage(msg);
//                    isOk = false;
//                    break;
//                case DBActions.DB_UNKNOWNHOST_FAILURE:
//                    msg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
//                    logger.warning(msg);
//                    showErrorMessage(msg);
//                    isOk = false;
//                    break;
//                default:
//                    msg = "Database upgrade process failed";
//                    showErrorMessage(msg);
//                    logger.warning(msg);
//                    isOk = false;
//                    break;
//            }
//        } else {
//            showErrorMessage("The database must be a correct version before proceeding.");
//            isOk = false;
//        }
//        return isOk;
//    }

    private char[] showGetPasswordDialog(String s) {

        JPasswordField pwdFld = new JPasswordField();
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.add(new JLabel(s));
        msgPanel.add(pwdFld);
        JOptionPane.showConfirmDialog(null, msgPanel,
                "Enter Password",
                JOptionPane.OK_CANCEL_OPTION);

        return pwdFld.getPassword();
    }

    private int showReallyConfirmOverwriteMessage(String dbName) {
        Object[] options = new Object[2];
        options[CONFIRM_OVERWRITE_YES] = "Yes";
        options[CONFIRM_OVERWRITE_NO] = "No";

        return JOptionPane.showOptionDialog(this,"Are you certain you want to overwrite the \"" + dbName + "\" database? All existing data will be lost.",
            "Confirm Database Overwrite",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]);
    }

    //DBActionsListener implementations
    public void showErrorMessage(String errorMsg) {
        errorMessagePane.setText(errorMsg);
        errorMessagePane.setVisible(true);
    }

    public boolean getOverwriteConfirmationFromUser(String dbName) {
        String errorMsg = "The database named \"" + dbName + "\" already exists. Would you like to overwrite it?";
        boolean isOkToOverwrite = false;
        int response = JOptionPane.showConfirmDialog(this,errorMsg, "Database already exists", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.OK_OPTION) {
            response = showReallyConfirmOverwriteMessage(dbName);
            if (response == CONFIRM_OVERWRITE_YES) {
                logger.info("creating new database (overwriting existing one)");
                logger.warning("The database will be overwritten");
                isOkToOverwrite = true;
            }
        }
        return isOkToOverwrite;
    }

    public void confirmCreateSuccess() {}

    public void hideErrorMessage() {
        errorMessagePane.setVisible(false);
    }

    public String getPrivilegedUsername() {
        String tempun = privUsername.getText();
        return (null == tempun || tempun == "")?"root":tempun;
    }

    public char[] getPrivilegedPassword() {
        return showGetPasswordDialog("Please enter the root database password:");
    }

    public boolean getGenericUserConfirmation(String msg) {
        int result = JOptionPane.showConfirmDialog(this,msg,
            "Confirmation", JOptionPane.YES_NO_OPTION);

        return (result == JOptionPane.OK_OPTION);
    }
}