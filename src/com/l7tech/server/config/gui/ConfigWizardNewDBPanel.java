package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.DBActions;
import com.l7tech.server.config.commands.DatabaseConfigCommand;
import com.l7tech.server.config.commands.NewDatabaseConfigCommand;
import com.l7tech.server.config.beans.DatabaseConfigBean;
import com.l7tech.server.config.beans.NewDatabaseConfigBean;

import javax.swing.*;
import java.util.HashMap;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private JTextField privPassword;
    private JTextField dbHostname;
    private JTextField dbName;
    private JTextField dbUsername;
    private JTextField dbPassword;
    private JTextPane errorMessagePane;

    private DBActions dbActions;
    private ActionListener dbActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            enableFields();
        }
    };
    private JLabel privUserLabel;
    private JLabel privPasswordLabel;

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
            throw new RuntimeException("Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard");
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
        dbConfigBean.setPrivPassword(privPassword.getText());

        dbConfigBean.setDbHostname(dbHostname.getText());
        dbConfigBean.setDbUsername(dbUsername.getText());
        dbConfigBean.setDbPassword(dbPassword.getText());
        dbConfigBean.setDbName(dbName.getText());
    }

    protected void updateView(HashMap settings) {
        NewDatabaseConfigBean dbConfigBean = (NewDatabaseConfigBean)configBean;
        boolean isNew = dbConfigBean.isCreateNewDb();
        createNewDb.setSelected(isNew);
    }

    public boolean onNextButton() {
        boolean isOk = false;
        if (createNewDb.isSelected()) {
            isOk = doCreateDb(privUsername.getText(),privPassword.getText(),dbHostname.getText(), dbName.getText(), dbUsername.getText(), dbPassword.getText(), false);
        }
        else {
            isOk = doExistingDb(dbHostname.getText(), dbName.getText(), dbUsername.getText(), dbPassword.getText());
        }

        return isOk;
    }

    private boolean doExistingDb(String hostname, String name, String username, String password) {
        String errorMsg;
        boolean isOk = false;

        int status = DBActions.DB_SUCCESS;
        logger.info("Attempting to connect to an existing database (" + username + ":" + password + "@" + hostname + "/" + name + ")");
        status = dbActions.checkExistingDb(hostname, name, username, password);
        if (status == DBActions.DB_SUCCESS) {
            logger.info("Connection to the database was a success");
            hideErrorMessage();
            isOk = true;
        } else {
            switch (status) {
                case DBActions.DB_AUTHORIZATION_FAILURE:
                    errorMsg = "There was an authentication error when attempting to connect to the database \"" + name + "\" using the username \"" +
                            username + "\" and password \"" + password + "\". Please check your input and try again.";
                    showErrorMessage(errorMsg);
                    isOk = false;
                    break;
                case DBActions.DB_UNKNOWNDB_FAILURE:
                    errorMsg = "Could not connect to the database \"" + name + "\". The database does not exist. Please check your input and try again.";
                    showErrorMessage(errorMsg);
                    isOk = false;
                    break;
                case DBActions.DB_UNKNOWN_FAILURE:
                default:
                    errorMsg = "There was an error while attempting to connect to the database. Please try again";
                    showErrorMessage(errorMsg);
                    isOk = false;
                    break;
            }
            logger.info("Connection to the database was unsuccessful - see warning/errors for details");
            logger.warning(errorMsg);
        }
        return isOk;
    }

    private boolean doCreateDb(String pUsername, String pPassword, String hostname, String name, String username, String password, boolean overwriteDb) {
        String errorMsg;
        boolean isOk = false;

        int status = DBActions.DB_SUCCESS;
        logger.info("Attempting to create a new database (" + hostname + "/" + name + ") using priveleged user/password \"" + pUsername + "/" + pPassword + "\"");
        String dbCreateScriptFile = osFunctions.getPathToDBCreateFile();
        boolean isWindows = osFunctions.isWindows();
        status = dbActions.createDb(pUsername, pPassword, hostname, name, username, password, dbCreateScriptFile, isWindows, overwriteDb);
        if (status == DBActions.DB_SUCCESS) {
            hideErrorMessage();
            isOk = true;
        } else {
            logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
            switch (status) {
                case DBActions.DB_AUTHORIZATION_FAILURE:
                    errorMsg = "There was an authentication error when attempting to create the new database using the username \"" +
                            pUsername + "\" and password \"" + pPassword + "\"- please retry";
                    logger.warning(errorMsg);
                    showErrorMessage(errorMsg);
                    isOk = false;
                    break;
                case DBActions.DB_ALREADY_EXISTS:
                    logger.warning("The database named \"" + name + "\" already exists");
                    errorMsg = "The database named \"" + name + "\" already exists. Would you like to overwrite it?";
                    int response = JOptionPane.showConfirmDialog(this,errorMsg, "Database already exists", JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.OK_OPTION) {
                        Object[] options = {"Yes", "No"};
                        response = JOptionPane.showOptionDialog(this,"Are you certain you want to overwrite the \"" + name + "\"database? All existing data will be lost.",
                                "Confirm Database Overwrite",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                options[1]);
                        if (response == JOptionPane.OK_OPTION) {
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
                    errorMsg = "There was an error while attempting to create the database. Please try again";
                    logger.warning(errorMsg);
                    showErrorMessage(errorMsg);
                    isOk = false;
                    break;
            }
        }
        return isOk;
    }

    private void showErrorMessage(String errorMsg) {
        errorMessagePane.setText(errorMsg);
        errorMessagePane.setVisible(true);
    }

    private void hideErrorMessage() {
        errorMessagePane.setVisible(false);
    }
}