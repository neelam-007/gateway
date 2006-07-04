package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.commands.SsgDatabaseConfigCommand;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBActionsListener;
import com.l7tech.common.gui.util.Utilities;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
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

    private JPanel mainPanel;
    private JRadioButton createNewDb;
    private JRadioButton existingDb;
    private JTextField dbHostname;
    private JTextField dbName;
    private JTextField dbUsername;
    private JPasswordField dbPassword;
    private JTextPane errorMessagePane;

    private DBActions dbActions;

    private static final int CONFIRM_OVERWRITE_YES = 0;
    private static final int CONFIRM_OVERWRITE_NO = 1;

    private static final String MYSQL_CLASS_NOT_FOUND_MSG = "Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard";
    private static final String CONFIG_FILE_NOT_FOUND_MSG = "Could not find the database configuration file. Cannot determine existing configuration.";
    private static final String CONFIG_FILE_IO_ERROR_MSG = "Error while reading the database configuration file. Cannot determine existing configuration.";
    private static final String MISSING_FIELDS_MSG = "Some required fields are missing. \n\n" +
                        "Please fill in all the required fields (indicated in red above)";

    private ActionListener existingDbActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            populateExistingDbFields();
        }
    };

    private ActionListener newDbActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            populateNewDbFields();
        }
    };

    private JLabel dbHostnameLabel;
    private JLabel dbNameLabel;
    private JLabel dbUsernameLabel;
    private JLabel dbPasswordLabel;
    private Map dbProps;


    private Map getDbProps() {
        if (dbProps == null) {
            try {
                dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                        SsgDatabaseConfigBean.PROP_DB_USERNAME,
                        SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                        SsgDatabaseConfigBean.PROP_DB_URL
                });
            } catch (FileNotFoundException e) {
                logger.warning(CONFIG_FILE_NOT_FOUND_MSG);
                logger.warning(e.getMessage());
            } catch (IOException e) {
                logger.warning(CONFIG_FILE_IO_ERROR_MSG);
                logger.warning(e.getMessage());
            }
        }
        return dbProps;
    }

    public ConfigWizardNewDBPanel(WizardStepPanel next) {
        super(next);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = new SsgDatabaseConfigBean();
        configCommand = new SsgDatabaseConfigCommand(configBean);
        try {
            dbActions = new DBActions();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(MYSQL_CLASS_NOT_FOUND_MSG);
        }

        ButtonGroup group = new ButtonGroup();
        group.add(createNewDb);
        group.add(existingDb);

        createNewDb.addActionListener(newDbActionListener);
        existingDb.addActionListener(existingDbActionListener);

        boolean isNewDb = checkIsNewDb(getDbProps());
        if (!isNewDb) {
            existingDb.setSelected(true);
        } else {
            createNewDb.setSelected(true);
        }

        stepLabel = "Setup The SSG Database";

        errorMessagePane.setBackground(mainPanel.getBackground());
        errorMessagePane.setForeground(Color.RED);
        errorMessagePane.setVisible(false);


        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private boolean checkIsNewDb(Map props) {
        if (props == null || props.isEmpty()) return true;

        String existingDBUrl = (String) props.get(SsgDatabaseConfigBean.PROP_DB_URL);

        if (StringUtils.isEmpty(existingDBUrl)) {
            return true;
        }

        //if an exising username, password and non default URL are found, then this machine already has a database
        //configured
        String existingDbUsername = (String) props.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
        String existingDbPassword = (String) props.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);

        Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
        if (matcher.matches()
                && StringUtils.isNotEmpty(existingDbUsername)
                && StringUtils.isNotEmpty(existingDbPassword)) {
            return false;
        }

        return true;
    }

    protected void updateModel() {
        SsgDatabaseConfigBean dbConfigBean = (SsgDatabaseConfigBean)configBean;

        dbConfigBean.setDbHostname(dbHostname.getText());
        dbConfigBean.setDbUsername(dbUsername.getText());
        dbConfigBean.setDbPassword(new String(dbPassword.getPassword()));
        dbConfigBean.setDbName(dbName.getText());
    }

    public boolean isValidated() {
        resetLabelColours();
        boolean isOk = true;
        //enforce that all the required fields are present.
        isOk = checkRequiredFields();

        String rootUsername = "";
        String rootPassword = "";

        String username = dbUsername.getText();
        String password = new String(dbPassword.getPassword());

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

        if (isOk) hideErrorMessage();
        return isOk;
    }

    protected void updateView() {
        if (!createNewDb.isSelected()) {
            populateExistingDbFields();
        }
    }

    private void resetLabelColours() {
        dbHostnameLabel.setForeground(Color.BLACK);
        dbNameLabel.setForeground(Color.BLACK);
        dbUsernameLabel.setForeground(Color.BLACK);
        dbPasswordLabel.setForeground(Color.BLACK);
    }

    private boolean checkRequiredFields() {
        boolean ok = true;

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

    private void populateExistingDbFields() {

        SsgDatabaseConfigBean dbBean = (SsgDatabaseConfigBean) configBean;
        if (dbBean == null) {
            return;
        }

        String existingDbUsername = dbBean.getDbUsername();
        String existingDbName = dbBean.getDbName();
        String existingDbHostname = dbBean.getDbHostname();
        String existingDbPassword = null;

        getDbProps();

        //first try and get the bean information if it exists and use that.
        if (dbBean == null ||
                StringUtils.isEmpty(existingDbUsername) ||
                StringUtils.isEmpty(existingDbName) ||
                StringUtils.isEmpty(existingDbHostname)) {

            existingDbUsername = (String) dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
            existingDbPassword = (String) dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);

            String existingDBUrl = (String) dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);

            if (StringUtils.isNotEmpty(existingDBUrl)) {
                Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(existingDBUrl);
                if (matcher.matches()) {
                    existingDbHostname = matcher.group(1);
                    existingDbName = matcher.group(2);
                }
            }
        }

        dbUsername.setText(StringUtils.isNotEmpty(existingDbUsername)?existingDbUsername:"");
        dbHostname.setText(StringUtils.isNotEmpty(existingDbHostname)?existingDbHostname:"");
        dbName.setText(StringUtils.isNotEmpty(existingDbName)?existingDbName:"");
        dbPassword.setText(StringUtils.isNotEmpty(existingDbPassword)?existingDbPassword:"");
    }

    private void populateNewDbFields() {
        if (configBean == null) {
            clearDbFields();
        } else {
            SsgDatabaseConfigBean dbBean = (SsgDatabaseConfigBean) configBean;
            dbHostname.setText(StringUtils.isEmpty(dbBean.getDbHostname())?"":dbBean.getDbHostname());
            dbUsername.setText(StringUtils.isEmpty(dbBean.getDbUsername())?"":dbBean.getDbUsername());
            dbName.setText(StringUtils.isEmpty(dbBean.getDbName())?"":dbBean.getDbName());
            dbPassword.setText(StringUtils.isEmpty(dbBean.getDbPassword())?"":dbBean.getDbPassword());
        }
    }

    private void clearDbFields() {
        dbHostname.setText("");
        dbUsername.setText("");
        dbName.setText("");
        dbPassword.setText("");
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

    public void showSuccess(String message) {}

    public void hideErrorMessage() {
        errorMessagePane.setVisible(false);
    }

    public String getPrivilegedUsername(String defaultContent) {
        if (configBean != null) {
            SsgDatabaseConfigBean dbBean = (SsgDatabaseConfigBean) configBean;
            String username = dbBean.getPrivUsername();
            if (StringUtils.isEmpty(username)) {
                username = showGetUsernameDialog("Please enter the database root user's username (needed to create a database):", defaultContent);
                dbBean.setPrivUserName(username);
            } else {
                username = dbBean.getPrivUsername();
            }
            return username;
        }
        return showGetUsernameDialog("Please enter the database root user's username (needed to create a database):", defaultContent);
    }

    private String showGetUsernameDialog(String label, String defaultContent) {
        JTextField usernameFld = new JTextField();
        if (defaultContent != null) {
            usernameFld.setText(defaultContent);
        }
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.add(new JLabel(label));
        msgPanel.add(usernameFld);
        JOptionPane.showConfirmDialog(null, msgPanel,
                "Enter Username",
                JOptionPane.OK_CANCEL_OPTION);

        return usernameFld.getText();
    }

    public char[] getPrivilegedPassword() {
        return showGetPasswordDialog("Please enter the root database password:");
    }

    public boolean getGenericUserConfirmation(String msg) {
        int result = JOptionPane.showConfirmDialog(this,msg,
            "Confirmation", JOptionPane.YES_NO_OPTION);

        return (result == JOptionPane.OK_OPTION);
    }

    public Map<String, String> getPrivelegedCredentials(String description, String usernamePrompt, String passwordPrompt, String defaultUsername) {
        CredentialsDialog dlg = new CredentialsDialog(getParentWizard(), "Enter Credentials for Priveleged user", true);

        if (StringUtils.isNotEmpty(description)) dlg.setDescription(description);
        if (StringUtils.isNotEmpty(defaultUsername)) dlg.setUsername(defaultUsername);

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (dlg.wasCancelled()) return null;


        Map<String, String> creds = new HashMap<String, String>();
        creds.put(DBActions.USERNAME_KEY, dlg.getUsername());
        String passwd = new String(dlg.getPassword());
        if (passwd == null) passwd = "";

        creds.put(DBActions.PASSWORD_KEY, passwd);
        return creds;
    }
}