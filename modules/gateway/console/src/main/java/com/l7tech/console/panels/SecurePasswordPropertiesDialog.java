package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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
    private JPanel pemPrivateKeyFieldPanel;
    private JTextArea pemPrivateKeyField;
    private JButton loadFromFileButton;
    private JLabel pemPrivateKeyLabel;
    private JComboBox typeComboBox;
    private JLabel typeLabel;

    private final SecurePassword securePassword;
    private final boolean newRecord;
    private final InputValidator inputValidator;

    private boolean confirmed = false;
    private char[] enteredPassword = null;

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    public SecurePasswordPropertiesDialog(Window owner, SecurePassword securePassword) {
        super(owner, "Stored Password Properties");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        Utilities.setEscKeyStrokeDisposes(this);

        this.securePassword = securePassword;

        newRecord = securePassword.getOid() == SecurePassword.DEFAULT_OID;

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        typeComboBox.addActionListener(enableDisableListener);
        typeComboBox.setModel(new DefaultComboBoxModel(SecurePassword.SecurePasswordType.values()));
        typeComboBox.setSelectedItem(securePassword.getType() != null ? securePassword.getType() : SecurePassword.SecurePasswordType.PASSWORD);

        loadFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        inputValidator = new InputValidator(this, "Stored Password Properties");
        inputValidator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char[] passToSave = null;
                switch ((SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem()) {
                    case PASSWORD:
                        passToSave = newRecord ? passwordField.getPassword() : enteredPassword;
                        break;
                    case PEM_PRIVATE_KEY:
                        passToSave = (newRecord && pemPrivateKeyField.getText() != null) ? pemPrivateKeyField.getText().toCharArray() : enteredPassword;
                        break;
                    default:
                        break;
                }

                if (passToSave != null && passToSave.length < 1) {
                    DialogDisplayer.showConfirmDialog(buttonOK,
                            "The password will be empty.  Save it anyway?",
                            "Save Empty Password?",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (JOptionPane.YES_OPTION == option) {
                                doConfirm();
                            }
                        }
                    });
                } else {
                    doConfirm();
                }
            }
        });
        inputValidator.constrainTextField(confirmPasswordField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                SecurePassword.SecurePasswordType type = (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem();
                if (type != SecurePassword.SecurePasswordType.PASSWORD
                        || new String(confirmPasswordField.getPassword()).equals(new String(passwordField.getPassword())))
                    return null;
                return "The confirmed password does not match the password.";
            }
        });
        inputValidator.constrainTextField(pemPrivateKeyField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                SecurePassword.SecurePasswordType type = (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem();
                if (type != SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY
                        || StringUtils.isEmpty(pemPrivateKeyField.getText())
                        || SecurePasswordPemPrivateKeyDialog.simplePemPrivateKeyValidation(pemPrivateKeyField.getText()))
                    return null;
                return "The key must be in PEM private key format.";
            }
        });

        final int maxPasswordLength = EntityUtil.getMaxFieldLength(SecurePassword.class, "encodedPassword", 128);

        changePasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char[] got = null;
                switch ((SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem()) {
                    case PASSWORD:
                        got = PasswordDoubleEntryDialog.getPassword(SecurePasswordPropertiesDialog.this, "Enter Password", maxPasswordLength);
                        break;
                    case PEM_PRIVATE_KEY:
                        got = SecurePasswordPemPrivateKeyDialog.getPemPrivateKey(SecurePasswordPropertiesDialog.this, "Enter PEM Private Key", maxPasswordLength);
                        break;
                    default:
                        break;
                }

                if (got == null)
                    return;
                enteredPassword = got;
                lastUpdateLabel.setText(new Date().toString());
            }
        });

        Utilities.setMaxLength(nameField.getDocument(), EntityUtil.getMaxFieldLength(SecurePassword.class, "name", 128));
        Utilities.setMaxLength(descriptionField.getDocument(), EntityUtil.getMaxFieldLength(SecurePassword.class, "description", 128));
        Utilities.setMaxLength(passwordField.getDocument(), maxPasswordLength);
        Utilities.setMaxLength(confirmPasswordField.getDocument(), maxPasswordLength);
        Utilities.setMaxLength(pemPrivateKeyField.getDocument(), maxPasswordLength);

        enableOrDisableComponents();
        modelToView();
    }

    private void doConfirm() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    private void enableOrDisableComponents() {
        if (newRecord) {
            lastUpdateLabel.setVisible(false);
            lastUpdateLabelLabel.setVisible(false);
            changePasswordButton.setVisible(false);
            final SecurePassword.SecurePasswordType type = (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem();
            switch (type) {
                case PASSWORD:
                    passwordField.setVisible(true);
                    passwordField.setEnabled(true);
                    confirmPasswordField.setVisible(true);
                    confirmPasswordField.setEnabled(true);
                    passwordLabel.setVisible(true);
                    confirmPasswordLabel.setVisible(true);
                    pemPrivateKeyFieldPanel.setVisible(false);
                    pemPrivateKeyFieldPanel.setEnabled(false);
                    pemPrivateKeyLabel.setVisible(false);
                    break;
                case PEM_PRIVATE_KEY:
                    passwordField.setVisible(false);
                    passwordField.setEnabled(false);
                    confirmPasswordField.setVisible(false);
                    confirmPasswordField.setEnabled(false);
                    passwordLabel.setVisible(false);
                    confirmPasswordLabel.setVisible(false);
                    pemPrivateKeyFieldPanel.setVisible(true);
                    pemPrivateKeyFieldPanel.setEnabled(true);
                    pemPrivateKeyLabel.setVisible(true);
                    break;
                default:
                    break;
            }
        } else {
            typeComboBox.setVisible(false);
            typeComboBox.setEnabled(false);
            typeLabel.setVisible(false);

            pemPrivateKeyFieldPanel.setVisible(false);
            pemPrivateKeyFieldPanel.setEnabled(false);
            pemPrivateKeyLabel.setVisible(false);

            passwordField.setVisible(false);
            passwordField.setEnabled(false);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setEnabled(false);
            passwordLabel.setVisible(false);
            confirmPasswordLabel.setVisible(false);
        }
        pack();
    }

    private void modelToView() {
        nameField.setText(nn(securePassword.getName()));
        nameField.setCaretPosition(0);
        descriptionField.setText(nn(securePassword.getDescription()));
        descriptionField.setCaretPosition(0);
        final long update = securePassword.getLastUpdate();
        lastUpdateLabel.setText(update > 0 ? new Date(update).toString() : "<Never Set>");
        allowVariableCheckBox.setSelected(securePassword.isUsageFromVariable());
    }

    private void viewToModel() {
        final SecurePassword.SecurePasswordType type = (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem();
        if (newRecord) {
            switch (type) {
                case PASSWORD:
                    enteredPassword = passwordField.getPassword();
                    break;
                case PEM_PRIVATE_KEY:
                    enteredPassword = pemPrivateKeyField.getText() != null ? pemPrivateKeyField.getText().toCharArray() : null;
                    break;
            }
        }
        securePassword.setName(nameField.getText());
        securePassword.setDescription(descriptionField.getText());
        securePassword.setType(type);
        securePassword.setUsageFromVariable(allowVariableCheckBox.isSelected());
    }

    private String nn(String s) {
        return s == null ? "" : s;
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

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doRead(fc);
            }
        });
    }

    private void doRead(JFileChooser dlg) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            pemPrivateKeyField.setText(new String(IOUtils.slurpFile(new File(filename))));
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
