package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.password.SecurePassword.SecurePasswordType;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.ResourceBundle;

public class SecurePasswordPropertiesDialog extends JDialog {
    private static ResourceBundle resources = ResourceBundle.getBundle( SecurePasswordPropertiesDialog.class.getName() );

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

    private boolean confirmed = false;
    private char[] enteredPassword = null;

    public SecurePasswordPropertiesDialog( final Window owner, final SecurePassword securePassword) {
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

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        };
        typeComboBox.addActionListener(enableDisableListener);
        typeComboBox.setModel(new DefaultComboBoxModel(SecurePassword.SecurePasswordType.values()));
        typeComboBox.setRenderer( new TextListCellRenderer<SecurePasswordType>( new Functions.Unary<String, SecurePasswordType>() {
                    @Override
                    public String call( final SecurePasswordType securePasswordType ) {
                        return resources.getString( "securepassword.type." + securePasswordType.name() );
                    }
                } ) );
        typeComboBox.setSelectedItem(securePassword.getType() != null ? securePassword.getType() : SecurePassword.SecurePasswordType.PASSWORD);

        loadFromFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        final InputValidator inputValidator = new InputValidator( this, "Stored Password Properties" );
        inputValidator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SecurePassword.SecurePasswordType type = (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem();
                if (type == SecurePassword.SecurePasswordType.PASSWORD) {
                    char[] passToSave = newRecord ? passwordField.getPassword() : enteredPassword;
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
                    }
                }
                doConfirm();
            }
        });
        inputValidator.constrainTextField(confirmPasswordField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                SecurePassword.SecurePasswordType type = newRecord ? (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem() : securePassword.getType();
                if (type != SecurePassword.SecurePasswordType.PASSWORD
                        || new String(confirmPasswordField.getPassword()).equals(new String(passwordField.getPassword())))
                    return null;
                return "The confirmed password does not match the password.";
            }
        });
        inputValidator.constrainTextField(pemPrivateKeyField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                SecurePassword.SecurePasswordType type = newRecord ? (SecurePassword.SecurePasswordType) typeComboBox.getSelectedItem() : securePassword.getType();
                if (type == SecurePasswordType.PEM_PRIVATE_KEY) {
                    if (newRecord) {
                        String passToSave = pemPrivateKeyField.getText();
                        if (StringUtils.isEmpty(passToSave)) {
                            return "PEM private key must not be empty.";
                        } else if (!SecurePasswordPemPrivateKeyDialog.simplePemPrivateKeyValidation(passToSave)) {
                            return "The key must be in PEM private key format.";
                        }
                    } else {
                        if (enteredPassword != null && !SecurePasswordPemPrivateKeyDialog.simplePemPrivateKeyValidation(new String(enteredPassword))) {
                            return "The key must be in PEM private key format.";
                        }
                    }
                }
                return null;
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

        // resize dialog to fit content
        DialogDisplayer.pack(SecurePasswordPropertiesDialog.this);
    }

    private void modelToView() {
        nameField.setText(nn(securePassword.getName()));
        nameField.setCaretPosition(0);
        descriptionField.setText(nn(securePassword.getDescription()));
        descriptionField.setCaretPosition(0);
        final long update = securePassword.getLastUpdate();
        lastUpdateLabel.setText(update > 0L ? new Date(update).toString() : "<Never Set>");
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
