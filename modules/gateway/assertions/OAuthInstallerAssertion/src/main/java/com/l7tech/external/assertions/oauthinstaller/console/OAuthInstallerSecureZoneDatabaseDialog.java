package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

public class OAuthInstallerSecureZoneDatabaseDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea schemaTextArea;
    private JTextField mySqlDbHostTextField;
    private JTextField mySqlDbPortTextField;
    private JTextField adminUsernameTextField;
    private JPasswordField adminPasswordField;
    private JPanel mysqlPanel;
    private JTextField otkDbNameTextField;
    private JTextField otkDbUserNameTextField;
    private JPasswordField otkUserPasswordField1;
    private JPasswordField otkUserPasswordField2;
    private JTextField jdbcConnNewTextField;
    private JPanel createDatabasePanel;
    private JTabbedPane tabbedPane;
    private static final Logger logger = Logger.getLogger(OAuthInstallerSecureZoneDatabaseDialog.class.getName());
    private List<String> connectionNames;
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle( OAuthInstallerSecureZoneDatabaseDialog.class.getName() );

    public OAuthInstallerSecureZoneDatabaseDialog(Dialog owner) {
        super(owner, "Manage OTK Database", true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonCancel);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initialize();
    }

    private void initialize(){

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);
        final String otkDbSchema = admin.getOAuthDatabaseSchema();
        schemaTextArea.setText(otkDbSchema);
        schemaTextArea.setCaretPosition(0);

        try {
            connectionNames = Registry.getDefault().getJdbcConnectionAdmin().getAllJdbcConnectionNames();
        } catch (FindException e) {
            final String msg = "Problem checking gateway for JDBC Connection name uniqueness: " + e.getMessage();
            DialogDisplayer.showMessageDialog(this, msg, "Could not look up JDBC Connections", JOptionPane.WARNING_MESSAGE, null);
            dispose();
        }

        // set defaults
        mySqlDbHostTextField.setText("localhost");
        mySqlDbPortTextField.setText("3306");
        otkDbNameTextField.setText("otk_db");
        otkDbUserNameTextField.setText("otk_user");
        jdbcConnNewTextField.setText("OAuth");

        final boolean enableDbCreate = SyspropUtil.getBoolean(
                "com.l7tech.external.assertions.oauthinstaller.console.OAuthInstallerSecureZoneDatabaseDialog.enableCreateDb",
                true);

        if (!enableDbCreate) {
            tabbedPane.remove(createDatabasePanel);
        }
    }

    private void onOK() {
        // validate all fields
        final String mysqlHost = mySqlDbHostTextField.getText().trim();
        if(!validateStringWithDialog(mysqlHost, "mysqlDbHost")){
            return;
        }

        final String mysqlPort = mySqlDbPortTextField.getText().trim();
        if(!validatePortNumberWithDialog(mysqlPort, "mysqlDbPort")){
            return;
        }

        final String adminUser = adminUsernameTextField.getText().trim();
        if(!validateStringWithDialog(adminUser, "adminUsername")){
            return;
        }

        final String adminPassword = new String(adminPasswordField.getPassword());
        if(!validateStringWithDialog(adminPassword, "adminPassword")){
            return;
        }

        final String otkDbName = otkDbNameTextField.getText().trim();
        if(!validateStringWithDialog(otkDbName, "newOtkDbSchemaName")){
            return;
        }

        final String otkDbUsername = otkDbUserNameTextField.getText().trim();
        if (!validateStringWithDialog(otkDbUsername, "otkDatabaseUser")) {
            return;
        }

        final String otkDbPassword1 = new String(otkUserPasswordField1.getPassword());
        final String otkDbPassword2 = new String(otkUserPasswordField2.getPassword());
        if(!validateStringWithDialog(otkDbPassword1, "otkDbUserPassword")){
            return;
        }

        if (!otkDbPassword1.equals(otkDbPassword2)) {
            DialogDisplayer.showMessageDialog(this, "OTK Database User passwords do not match", "Invalid Password", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final String newJdbcConnName = jdbcConnNewTextField.getText().trim();
        if(!validateStringWithDialog(newJdbcConnName, "newJdbcConnectionName")){
            return;
        }

        if (connectionNames.contains(newJdbcConnName)) {
            DialogDisplayer.showMessageDialog(this, "A JDBC Connection named '" + newJdbcConnName + "' already exists on the Gateway", "JDBC Connection name is invalid", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);

        try {
            final Either<String,String> either = doAsyncAdmin(admin,
                    OAuthInstallerSecureZoneDatabaseDialog.this,
                    "Creating OTK Database",
                    "The OTK Database is being created.",
                    admin.createOtkDatabase(mysqlHost, mysqlPort, adminUser, adminPassword, otkDbName, otkDbUsername, otkDbPassword1, newJdbcConnName));
            if (either.isRight()) {
                if (!either.right().isEmpty()) {
                    DialogDisplayer.showMessageDialog(this, either.right(), "Problem creating OTK Database", JOptionPane.WARNING_MESSAGE, null);
                    return;
                } else {
                    DialogDisplayer.showMessageDialog(this, "OTK Database successfully created", "Completed",
                            JOptionPane.INFORMATION_MESSAGE, null);
                }
            } else {
                DialogDisplayer.showMessageDialog(this, either.left(), "Problem creating OTK Database", JOptionPane.WARNING_MESSAGE, null);
                return;
            }
        } catch (Exception e) {
            handleException(e);
        }

        dispose();
    }

    private boolean validateStringWithDialog(final String toValidate, final String resourceKey) {
        if (toValidate.trim().isEmpty()) {
            // never include the value in the message as it may be a password

            String resourceValue = resourceBundle.getString(resourceKey);
            if (resourceValue.endsWith(":")) {
                resourceValue = resourceValue.substring(0, resourceValue.length() - 1);
            }
            DialogDisplayer.showMessageDialog(this, "Value for '" + resourceValue + "' is required", "Invalid value", JOptionPane.WARNING_MESSAGE, null);
            return false;
        }

        return true;
    }

    private boolean validatePortNumberWithDialog(final String portNumber, final String resourceKey) {
        if (!validateStringWithDialog(portNumber, resourceKey)) {
            return false;
        }

        final Integer portNum;
        try {
            portNum = Integer.valueOf(portNumber);
        } catch (NumberFormatException e) {
            DialogDisplayer.showMessageDialog(this, "Invalid port number: " + portNumber, "Invalid value", JOptionPane.WARNING_MESSAGE, null);
            return false;
        }

        if (portNum < 0 || portNum > 65535) {
            DialogDisplayer.showMessageDialog(this, "Invalid port number: " + portNumber+". Must be between 0 and 65535.", "Invalid value", JOptionPane.WARNING_MESSAGE, null);
            return false;
        }

        return true;
    }

    private void onCancel() {
        dispose();
    }

    private void handleException(Exception e) {
        if (e instanceof InterruptedException) {
            // do nothing, user cancelled
            logger.info("User cancelled installation of the OTK Database.");
        } else if (e instanceof InvocationTargetException) {
            DialogDisplayer.showMessageDialog(this, "Could not invoke OTK Database create process on Gateway",
                    "Create Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else if (e instanceof RuntimeException) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during creation of OTK Database: \n" + ExceptionUtils.getMessage(e),
                    "Create Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during creation of OTK Database: \n" + ExceptionUtils.getMessage(e),
                    "Create Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        }
    }

}
