package com.l7tech.console.panels;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Created by yuri on 11/4/14.
 */
public class CassandraConnectionPropertiesDialog extends JDialog{
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CassandraConnectionPropertiesDialog");

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField contactPointsTextField;
    private JTextField portTextField;
    private JTextField textField1;
    private JButton manageStoredPasswordsButton;
    private SecurePasswordComboBox securePasswordComboBox;
    private JComboBox compressionComboBox;
    private JCheckBox useSSLCheckBox;
    private JTable additionalPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JCheckBox disableConfigurationCheckBox;
    private JButton testConnectionButton;
    private JButton cancelButton;
    private JButton OKButton;
    private JTextField credentialsTextField;

    private InputValidator validator;
    private final CassandraConnection connection;

    public CassandraConnectionPropertiesDialog(Dialog owner, CassandraConnection connection) {
        super(owner, resources.getString("dialog.title.manage.cassandra.connection.properties"), true);
        this.connection = connection;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        validator = new InputValidator(this, this.getTitle());
        Utilities.setEscKeyStrokeDisposes(this);

        securePasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());
        manageStoredPasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });

        validator.attachToButton(OKButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

    }

    private void onOk() {
    }

    private void doManagePasswords() {
        final SecurePassword password = securePasswordComboBox.getSelectedSecurePassword();
        final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(getOwner());

        securePasswordManagerWindow.pack();
        Utilities.centerOnParentWindow(securePasswordManagerWindow);
        DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
            @Override
            public void run() {
                securePasswordComboBox.reloadPasswordList();
                if (password != null) {
                    securePasswordComboBox.setSelectedSecurePassword(password.getGoid());
                    enableDisableComponents();
                    DialogDisplayer.pack(CassandraConnectionPropertiesDialog.this);
                } else {
                    securePasswordComboBox.setSelectedItem(null);
                }
            }
        });
    }

    private void enableDisableComponents() {
    }

    private void onCancel() {
        this.dispose();
    }
}
