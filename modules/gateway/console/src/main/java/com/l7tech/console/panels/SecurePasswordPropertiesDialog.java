package com.l7tech.console.panels;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class SecurePasswordPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField nameField;
    private JLabel lastUpdateLabel;
    private JButton changePasswordButton;
    private JTextField descriptionField;
    private JCheckBox allowVariableCheckBox;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel confirmPasswordLabel;
    private JLabel passwordLabel;
    private JLabel lastUpdateLabelLabel;

    private final SecurePassword securePassword;
    private final boolean newRecord;
    private final InputValidator inputValidator;

    private boolean confirmed = false;
    private char[] enteredPassword = null;

    public SecurePasswordPropertiesDialog(Window owner, SecurePassword securePassword) {
        super(owner, "Stored Password Properties");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        Utilities.setEscKeyStrokeDisposes(this);

        this.securePassword = securePassword;

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        inputValidator = new InputValidator(this, "Stored Password Properties");
        inputValidator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewToModel();
                confirmed = true;
                dispose();
            }
        });

        newRecord = securePassword.getOid() == SecurePassword.DEFAULT_OID;

        if (newRecord) {
            lastUpdateLabel.setVisible(false);
            lastUpdateLabelLabel.setVisible(false);
            changePasswordButton.setVisible(false);
        } else {
            passwordField.setVisible(false);
            passwordField.setEnabled(false);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setEnabled(false);
            passwordLabel.setVisible(false);
            confirmPasswordLabel.setVisible(false);
        }


        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return VariableMetadata.validateName(nameField.getText());
            }
        });
        inputValidator.constrainTextFieldToBeNonEmpty("password", passwordField, null);
        inputValidator.constrainTextFieldToBeNonEmpty("confirmed password", confirmPasswordField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (new String(confirmPasswordField.getPassword()).equals(new String(passwordField.getPassword())))
                    return null;
                return "The confirmed password does not match the password.";
            }
        });

        changePasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char[] got = PasswordDoubleEntryDialog.getPassword(SecurePasswordPropertiesDialog.this, "Enter Password");
                if (got == null)
                    return;
                enteredPassword = got;
                lastUpdateLabel.setText(new Date().toString());
            }
        });

        modelToView();
    }

    private void modelToView() {
        nameField.setText(securePassword.getName());
        descriptionField.setText(securePassword.getDescription());
        final long update = securePassword.getLastUpdate();
        lastUpdateLabel.setText(update > 0 ? new Date(update).toString() : "<Never Set>");
        allowVariableCheckBox.setSelected(securePassword.isUsageFromVariable());
    }

    private void viewToModel() {
        if (newRecord)
            enteredPassword = passwordField.getPassword();
        securePassword.setName(nameField.getText());
        securePassword.setDescription(descriptionField.getText());
        securePassword.setUsageFromVariable(allowVariableCheckBox.isSelected());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Get the plaintext password.  Only meaningful if {@link #isConfirmed()}.
     * @return For a new SecurePassword entity: the password entered (and confirmed) by the user.  Never null.
     *         For an existing SecurePassword:  the new password, if the user requested a password change, otherwise null.
     */
    public char[] getEnteredPassword() {
        return enteredPassword;
    }
}
