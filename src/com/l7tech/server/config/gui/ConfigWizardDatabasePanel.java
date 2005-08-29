package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.DBChecker;
import com.l7tech.server.config.commands.DatabaseConfigCommand;
import com.l7tech.server.config.beans.DatabaseConfigBean;
import com.l7tech.server.config.beans.ConfigurationBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 9, 2005
 * Time: 3:25:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardDatabasePanel extends ConfigWizardStepPanel {
    private final static String LOCALDB_HOSTNAME="localhost";
    private final static String LOCALDB_DBNAME="ssg";
    private final static String LOCALDB_USER="gateway";
    private final static char[] LOCALDB_PASSWORD = {'7', 'l', 'a', 'y', 'e', 'r'};

    private final static String MYSQL_CONNECTION_PREFIX = "jdbc:mysql://";
    private final static String PROP_DB_USERNAME = "hibernate.connection.username";
    private final static String PROP_DB_URL = "hibernate.connection.url";

    private Pattern urlPattern = Pattern.compile("^.*//(.*)/(.*)\\?.*$");


    DBChecker dbChecker = new DBChecker(1);
    private JPanel mainPanel;
    private JPasswordField password;
    private JTextField username;
    private JTextField database;
    private JTextField hostname;
    private JRadioButton remoteDatabase;
    private JRadioButton localDatabase;
    private boolean isDBFailure = false;



    private ActionListener controlListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableControls();
        }
    };
    private JLabel hostnameLabel;
    private JLabel databaseLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JTextPane errorMsg;


    /**
     * Creates new form WizardPanel
     */
    public ConfigWizardDatabasePanel(OSSpecificFunctions functions) {
        super(null, functions);
        init();
    }

    public ConfigWizardDatabasePanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
            setShowDescriptionPanel(false);
            configBean = new DatabaseConfigBean(osFunctions);
            configCommand = new DatabaseConfigCommand(configBean);

            remoteDatabase.addActionListener(controlListener);
            localDatabase.addActionListener(controlListener);

            ButtonGroup localOrRemote = new ButtonGroup();
            localOrRemote.add(remoteDatabase);
            localOrRemote.add(localDatabase);

            database.setText(LOCALDB_DBNAME);       //set the default name for the database

            stepLabel = "Setup SSG Configuration Storage";
            errorMsg.setBackground(mainPanel.getBackground());
            errorMsg.setVisible(false);
            enableControls();
            setLayout(new BorderLayout());
            add(mainPanel, BorderLayout.CENTER);
        }

    protected void updateModel(HashMap settings) {
        DatabaseConfigBean dbConfigBean = (DatabaseConfigBean) configBean;
        boolean isRemote = remoteDatabase.isSelected();
        dbConfigBean.setRemote(isRemote);

        if (isRemote) {
            dbConfigBean.setDbHostname(hostname.getText());
            dbConfigBean.setDbUsername(username.getText());
            dbConfigBean.setDbName(database.getText());
            dbConfigBean.setDbPassword(password.getPassword());
        } else {
            dbConfigBean.setDbHostname(LOCALDB_HOSTNAME);
            dbConfigBean.setDbUsername(LOCALDB_USER);
            dbConfigBean.setDbName(LOCALDB_DBNAME);
            dbConfigBean.setDbPassword(LOCALDB_PASSWORD);
        }
    }

    protected void updateView(HashMap settings) {
        DatabaseConfigBean dbBean;
        String existingDbUsername = null;
        String existingDBUrl = null;
        String existingDBHostname = null;
        String existingDBName = null;
        dbChecker.resetRetryCount();

        //first try and get the bean information if it exists and use that.
        boolean beanInitialized = false;
        if (configBean != null) {
            dbBean = (DatabaseConfigBean) configBean;
            existingDbUsername = dbBean.getDbUsername();
            existingDBName = dbBean.getDbName();
            existingDBHostname = dbBean.getDbHostname();
            beanInitialized = (
                    StringUtils.isNotEmpty(existingDbUsername) &&
                    StringUtils.isNotEmpty(existingDBName) &&
                    StringUtils.isNotEmpty(existingDBHostname));
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
                existingDBUrl = props.getProperty(PROP_DB_URL);
                if (StringUtils.isNotEmpty(existingDBUrl)) {
                    Matcher matcher = urlPattern.matcher(existingDBUrl);
                    if (matcher.matches()) {
                        existingDBHostname = matcher.group(1);
                        existingDBName = matcher.group(2);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(existingDbUsername)) {
            username.setText(existingDbUsername);
        }
        if (StringUtils.isNotEmpty(existingDBHostname)) {
            hostname.setText(existingDBHostname);
        }

        if (StringUtils.isNotEmpty(existingDBName)) {
            database.setText(existingDBName);
        }

        if (existingDBHostname.equalsIgnoreCase(LOCALDB_HOSTNAME)) {
            localDatabase.setSelected(true);
        }
    }

    public boolean onNextButton() {
        int dbStatus = testDb();
        if (dbStatus != DBChecker.DB_SUCCESS) {
            if (dbStatus == DBChecker.DB_MAX_RETRIES_EXCEEDED || dbStatus == DBChecker.DB_CHECK_INTERNAL_ERROR) {
                ((DatabaseConfigBean)configBean).setDBConfigOn(false);
                JOptionPane.showMessageDialog(this, "Database connection failed, skipping database configuration.\n" +
                        "Please see logs for details\n" +
                        "You can run this tool again later to configure the database", "Skipping Database Configuration", JOptionPane.ERROR_MESSAGE);
            } else {
                showDbFailure(dbStatus);
            }
        }
        return (dbStatus == DBChecker.DB_SUCCESS ||
                dbStatus == DBChecker.DB_MAX_RETRIES_EXCEEDED ||
                dbStatus == DBChecker.DB_CHECK_INTERNAL_ERROR);
    }

    private int testDb() {
        String connectionString = getConnectionString();
        String name = getUsername();
        String pwd = new String(getPassword());

        int failureCode = dbChecker.checkDb(connectionString, name, pwd);
        return failureCode;
    }

    private void showDbFailure(int reason) {
        String msg = "could not connect to specified database with the supplied credentials, please try again";
        errorMsg.setText(msg);
        errorMsg.setForeground(Color.RED);
        errorMsg.setVisible(true);
    }

    private void enableControls() {
        boolean enabled= false;
        if (localDatabase.isSelected()) {
            enabled = false;
        }
        else if (remoteDatabase.isSelected()) {
            enabled = true;
        }

        hostnameLabel.setEnabled(enabled);
        databaseLabel.setEnabled(enabled);
        usernameLabel.setEnabled(enabled);
        passwordLabel.setEnabled(enabled);

        hostname.setEnabled(enabled);
        database.setEnabled(enabled);
        username.setEnabled(enabled);
        password.setEnabled(enabled);
    }

    private char[] getPassword() {
        char[] pwd = ((DatabaseConfigBean)configBean).getDbPassword();
        if (pwd == null || pwd.length == 0) {
            pwd = remoteDatabase.isSelected()?password.getPassword():LOCALDB_PASSWORD;
        }
        return pwd;
    }

    private String getUsername() {
        String name = ((DatabaseConfigBean)configBean).getDbUsername();
        if (StringUtils.isEmpty(name)) {
            name = remoteDatabase.isSelected()?username.getText():LOCALDB_USER;
        }
        return name;
    }

    private String getConnectionString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(MYSQL_CONNECTION_PREFIX).append(getDBHostname()).append("/").append(getDBName());
        return buffer.toString();
    }

    private String getDBName() {
        String theDBname = ((DatabaseConfigBean)configBean).getDbHostname();
        if (StringUtils.isEmpty(theDBname)) {
            theDBname = remoteDatabase.isSelected()?database.getText():LOCALDB_DBNAME;
        }
        return theDBname;
    }

    private String getDBHostname() {
        String theHostname = ((DatabaseConfigBean)configBean).getDbHostname();
        if (StringUtils.isEmpty(theHostname)) {
            theHostname = remoteDatabase.isSelected()?hostname.getText():LOCALDB_HOSTNAME;
        }
        return theHostname;
    }
}
