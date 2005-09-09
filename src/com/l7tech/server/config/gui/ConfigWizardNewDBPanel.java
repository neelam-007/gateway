package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.PasswordDialog;
import com.l7tech.server.config.DBActions;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.NewDatabaseConfigBean;
import com.l7tech.server.config.beans.DatabaseConfigBean;
import com.l7tech.server.config.commands.NewDatabaseConfigCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 2, 2005
 * Time: 4:37:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardNewDBPanel extends ConfigWizardStepPanel{
    private static final Logger logger = Logger.getLogger(ConfigWizardNewDBPanel.class.getName());

    private final static String LOCALDB_HOSTNAME="localhost";
    private final static String LOCALDB_DBNAME="ssg";
    private final static String LOCALDB_USER="gateway";
    private final static String LOCALDB_PASSWORD = "7layer";

    private final static String DEFAULT_PRIV_USER = "root";

    private JPanel mainPanel;
    private JPanel dbInfoPanel;
    private JPanel createNewPanel;
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

    private final static String PROP_DB_USERNAME = "hibernate.connection.username";
    private final static String PROP_DB_URL = "hibernate.connection.url";
    private static final String PROP_DB_PASSWORD = "hibernate.connection.password" ;

    private Pattern urlPattern = Pattern.compile("^.*//(.*)/(.*)\\?.*$");

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
        configBean = new NewDatabaseConfigBean(osFunctions);
        configCommand = new NewDatabaseConfigCommand(configBean);
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
        NewDatabaseConfigBean dbConfigBean = (NewDatabaseConfigBean)configBean;

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
        NewDatabaseConfigBean dbBean = null;
        String existingDbUsername = null;
        String existingDBUrl = null;
        String existingDbHostname = null;
        String existingDbName = null;
        String existingDbPassword = null;

        //first try and get the bean information if it exists and use that.
        boolean beanInitialized = false;
        if (configBean != null) {
            dbBean = (NewDatabaseConfigBean) configBean;
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

            FileInputStream fis = null;
            Properties props = new Properties();
            try {
                fis = new FileInputStream(osFunctions.getDatabaseConfig());
                props.load(fis);
                fis.close();
                fis = null;
                existingDbUsername =props.getProperty(PROP_DB_USERNAME);
                existingDbPassword = props.getProperty(PROP_DB_PASSWORD);
                existingDBUrl = props.getProperty(PROP_DB_URL);
                if (StringUtils.isNotEmpty(existingDBUrl)) {
                    Matcher matcher = urlPattern.matcher(existingDBUrl);
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
            } finally{
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {}
                }
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

        if (isOk == false) { // if we've passed the required fields checks
            showErrorMessage(MISSING_FIELDS_MSG);
        } else {
            if (createNewDb.isSelected()) {
                isOk = doCreateDb(privUsername.getText(),new String(privPassword.getPassword()),dbHostname.getText(), dbName.getText(), dbUsername.getText(), dbPassword.getText(), false);
            }
            else {
                isOk = doExistingDb(dbHostname.getText(), dbName.getText(), dbUsername.getText(), dbPassword.getText());
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

    private boolean doExistingDb(String hostname, String name, String username, String password) {
        String errorMsg;
        boolean isOk = false;

        int status = DBActions.DB_SUCCESS;
        logger.info("Attempting to connect to an existing database (" + hostname + "/" + name + ")" + "using username/password \"" + username + "/" + password + "\"");
        try {
            status = dbActions.checkExistingDb(hostname, name, username, password);
            if (status == DBActions.DB_SUCCESS) {
                logger.info(CONNECTION_SUCCESSFUL_MSG);
                hideErrorMessage();
                isOk = true;
            } else {
                switch (status) {

                    case DBActions.DB_AUTHORIZATION_FAILURE:
                        logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                        errorMsg = "There was an authentication error when attempting to connect to the database \"" + name + "\" using the username \"" +
                                username + "\" and password \"" + password + "\". Please check your input and try again.";
                        showErrorMessage(errorMsg);
                        logger.warning("There was an authentication error when attempting to connect to the database \"" + name + "\" using the username \"" +
                                username + "\" and password \"" + password + "\".");
                        isOk = false;
                        break;
                    case DBActions.DB_UNKNOWNDB_FAILURE:
                        logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                        errorMsg = "Could not connect to the database \"" + name + "\". The database does not exist. Please check your input and try again.";
                        showErrorMessage(errorMsg);
                        logger.warning("Could not connect to the database \"" + name + "\". The database does not exist.");
                        isOk = false;
                        break;
                    case DBActions.DB_UNKNOWN_FAILURE:
                    default:
                        logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                        errorMsg = GENERIC_DBCONNECT_ERROR_MSG;
                        showErrorMessage(errorMsg);
                        logger.warning("There was an unknown error while attempting to connect to the database.");
                        isOk = false;
                        break;
                }
            }
        } catch (DBActions.WrongDbVersionException e) {
            logger.info(CONNECTION_SUCCESSFUL_MSG + " but it appears to be an out of date version");
            logger.warning(e.getVersionMessage());

            errorMsg = "The \"" + name + "\" database exists but does not appear to be a current SSG database.\n" +
                    e.getVersionMessage() + "\n" +
                    "Would you like to create a new one in it's place?";

            int result = JOptionPane.showConfirmDialog(this,errorMsg,
                    "Incorrect Database", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                result = showReallyConfirmOverwriteMessage(name);
                if (result == CONFIRM_OVERWRITE_YES) {
                    char[]  passwd = privPassword.getPassword();
                    if (passwd == null || passwd.length == 0) {
                        passwd = showGetPasswordDialog("Please enter the root database password:");
                    }
                    logger.warning("Attempting to overwrite the existing database \"" + name + "\"");
                    isOk = doCreateDb(privUsername.getText(), new String(passwd), hostname, name, username, password, true);
                }
                else {
                    logger.warning(DBActions.UPGRADE_DB_MSG);
                }
            } else {
                logger.warning(DBActions.UPGRADE_DB_MSG);
                isOk = true;
            }
        }
        return isOk;
    }

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

    private boolean doCreateDb(String pUsername, String pPassword, String hostname, String name, String username, String password, boolean overwriteDb) {
        String errorMsg;
        boolean isOk = false;

        int status = DBActions.DB_SUCCESS;
        logger.info("Attempting to create a new database (" + hostname + "/" + name + ") using privileged user \"" + pUsername + "\"");
        String dbCreateScriptFile = osFunctions.getPathToDBCreateFile();
        boolean isWindows = osFunctions.isWindows();
        try {
            status = dbActions.createDb(pUsername, pPassword, hostname, name, username, password, dbCreateScriptFile, isWindows, overwriteDb);
            if (status == DBActions.DB_SUCCESS) {
                hideErrorMessage();
                isOk = true;
            } else {
                switch (status) {
                    case DBActions.DB_AUTHORIZATION_FAILURE:
                        errorMsg = "There was an authentication error when attempting to create the new database using the username \"" +
                                pUsername + "\". Perhaps the password is wrong. Please retry.";
                        logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
                        logger.warning(errorMsg);
                        showErrorMessage(errorMsg);
                        isOk = false;
                        break;
                    case DBActions.DB_ALREADY_EXISTS:
                        logger.warning("The database named \"" + name + "\" already exists");
                        errorMsg = "The database named \"" + name + "\" already exists. Would you like to overwrite it?";
                        int response = JOptionPane.showConfirmDialog(this,errorMsg, "Database already exists", JOptionPane.YES_NO_OPTION);
                        if (response == JOptionPane.OK_OPTION) {
                            response = showReallyConfirmOverwriteMessage(name);
                            if (response == CONFIRM_OVERWRITE_YES) {
                                logger.info("creating new database (overwriting existing one)");
                                logger.warning("The database will be overwritten");
                                isOk = doCreateDb(pUsername, pPassword, hostname, name, username, password, true);
                            }
                        } else {
                            isOk = false;
                        }
                        break;
                    case DBActions.DB_UNKNOWN_FAILURE:
                    default:
                        errorMsg = GENERIC_DBCREATE_ERROR_MSG;
                        logger.warning(errorMsg);
                        showErrorMessage(errorMsg);
                        isOk = false;
                        break;
                }
            }
        } catch (IOException e) {
            errorMsg = "Could not create the database because there was an error while reading the file \"" + dbCreateScriptFile + "\"." +
                    " The error was: " + e.getMessage();
            logger.warning(errorMsg);
            showErrorMessage(errorMsg);
            isOk = false;
        }
        return isOk;
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

    private void showErrorMessage(String errorMsg) {
        errorMessagePane.setText(errorMsg);
        errorMessagePane.setVisible(true);
    }

    private void hideErrorMessage() {
        errorMessagePane.setVisible(false);
    }
}