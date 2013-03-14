package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
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
    private JTextField otkDbNameTextField;
    private JTextField otkDbUserNameTextField;
    private JTextField jdbcConnNewTextField;
    private JPanel createDatabasePanel;
    private JTabbedPane tabbedPane;
    private JList<String> mysqlAccessList;
    private JButton removeHostButton;
    private JButton editHostButton;
    private JButton addHostButton;
    private JCheckBox failIfUserAlreadyCheckBox;
    private JButton manageStoredPasswordsButton;
    private JCheckBox createUserCheckBox;
    private SecurePasswordComboBox securePasswordComboBox;
    private static final Logger logger = Logger.getLogger(OAuthInstallerSecureZoneDatabaseDialog.class.getName());
    private List<String> connectionNames;
    private DefaultListModel<String> mysqlHostList = new DefaultListModel<>();
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

        createUserCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableFields();
            }
        });
        mysqlAccessList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableFields();
            }
        });

        manageStoredPasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                doManagePasswords();
            }
        });

        addHostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAddEditHostDialog(false);
            }
        });

        removeHostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = mysqlAccessList.getSelectedIndex();
                if (selected >= 0) {
                    mysqlHostList.removeElementAt(mysqlAccessList.getSelectedIndex());
                }
            }
        });

        editHostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAddEditHostDialog(true);
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

    private void openAddEditHostDialog(final boolean edit) {
        final int selectedIndex = mysqlAccessList.getSelectedIndex();
        String hostName = null;
        if(edit){
            hostName = mysqlHostList.getElementAt(selectedIndex);
        }
        final OAuthInstallerSecureZoneDatabaseHostDialog hostDialog = new OAuthInstallerSecureZoneDatabaseHostDialog(hostName, this);
        hostDialog.pack();
        Utilities.centerOnParentWindow(hostDialog);
        DialogDisplayer.display(hostDialog, new Runnable(){

            @Override
            public void run() {
                if (hostDialog.getHostname() != null) {
                    if(edit){
                        mysqlHostList.setElementAt(hostDialog.getHostname(), selectedIndex);
                    } else {
                        mysqlHostList.addElement(hostDialog.getHostname());
                    }
                }
            }
        });
    }

    private void doManagePasswords() {
        SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(SwingUtilities.getWindowAncestor(this));
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        // save selection
        final SecurePassword password = securePasswordComboBox.getSelectedSecurePassword();
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                securePasswordComboBox.reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
                // load selection
                if(password != null) {
                    securePasswordComboBox.setSelectedSecurePassword(password.getOid());
                }
                pack();
            }
        });
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

        createUserCheckBox.setSelected(true);

        mysqlHostList.addElement("localhost");
        mysqlAccessList.setModel(mysqlHostList);

        final boolean enableDbCreate = SyspropUtil.getBoolean(
                "com.l7tech.external.assertions.oauthinstaller.console.OAuthInstallerSecureZoneDatabaseDialog.enableCreateDb",
                true);

        if (!enableDbCreate) {
            tabbedPane.remove(createDatabasePanel);
        }

        enableFields();
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
        // Maximum db name length is 64 characters http://dev.mysql.com/doc/refman/5.0/en/identifiers.html
        if (!validateStringWithDialog(otkDbName, "newOtkDbSchemaName") || !validateStringMaxLengthWithDialog(otkDbName, "newOtkDbSchemaName", 64)) {
            return;
        }

        final String otkDbUsername = otkDbUserNameTextField.getText().trim();
        if (!validateStringWithDialog(otkDbUsername, "otkDatabaseUser")) {
            return;
        }

        SecurePassword securePassword = securePasswordComboBox.getSelectedSecurePassword();
        if(securePassword == null){
            DialogDisplayer.showMessageDialog(this, resourceBundle.getString("otkDbUserPasswordInfo"), "Invalid Password", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        if(mysqlHostList.isEmpty()){
            DialogDisplayer.showMessageDialog(this, resourceBundle.getString("dbUserGrantMissing"), "No MySQL Host Grants", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final String newJdbcConnName = jdbcConnNewTextField.getText().trim();
        if (!validateStringWithDialog(newJdbcConnName, "newJdbcConnectionName") || !validateStringMaxLengthWithDialog(newJdbcConnName, "newJdbcConnectionName", 128)) {
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
                    admin.createOtkDatabase(mysqlHost, mysqlPort, adminUser, adminPassword, otkDbName, otkDbUsername, securePassword.getOid(), newJdbcConnName, getHostGrants(), createUserCheckBox.isSelected(), failIfUserAlreadyCheckBox.isSelected()));
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

    private List<String> getHostGrants() {
        List<String> grants = new ArrayList<>(mysqlHostList.getSize());
        Enumeration<String> elements = mysqlHostList.elements();
        while(elements.hasMoreElements()){
            grants.add(elements.nextElement());
        }
        return grants;
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

    private boolean validateStringMaxLengthWithDialog(final String toValidate, final String resourceKey, final int maxLength) {
        if (toValidate.length() > maxLength) {
            // never include the value in the message as it may be a password

            String resourceValue = resourceBundle.getString(resourceKey);
            if (resourceValue.endsWith(":")) {
                resourceValue = resourceValue.substring(0, resourceValue.length() - 1);
            }
            DialogDisplayer.showMessageDialog(this, "Value for '" + resourceValue + "' is too long, max length is " + maxLength + " characters", "Invalid value", JOptionPane.WARNING_MESSAGE, null);
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

    private void enableFields(){
        failIfUserAlreadyCheckBox.setEnabled(createUserCheckBox.isSelected());
        removeHostButton.setEnabled(mysqlAccessList.getSelectedIndex() >= 0);
        editHostButton.setEnabled(mysqlAccessList.getSelectedIndex() >= 0);
    }
}
