package com.l7tech.external.assertions.mongodb.console;

import com.l7tech.console.panels.AssertionKeyAliasEditor;
import com.l7tech.console.panels.CertManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.mongodb.MongoDBEncryption;
import com.l7tech.external.assertions.mongodb.MongoDBReadPreference;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;


public class MongoDBConnectionDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField nameField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField uriTextField;
    private JTextField portTextField;
    private JTextField userNameTextField;
    private JPasswordField passwordTextField;
    private JTextField databaseTextField;
    private JCheckBox showPasswordCheckBox;
    private MongoDBSecurePasswordComboBox storedPasswordComboBox;
    private JButton selectPrivateKeyButton;
    private JButton manageCertificateButton;
    private JComboBox authMethodComboBox;
    private JComboBox readPreferenceComboBox;

    private static final String TITLE = "MongoDB Configuration";

    /**
     * @noinspection ThisEscapedInObjectConstruction
     */
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public MongoDBConnectionDialog(Dialog parent, MongoDBConnectionEntity mongoDBConnectionEntity) {
        super(parent, TITLE, true);

        initComponents(mongoDBConnectionEntity);
        setData(mongoDBConnectionEntity);
    }

    public MongoDBConnectionDialog(Dialog parent, MongoDBConnectionEntity mongoDBConnectionEntity, boolean edit) {
        super(parent, TITLE, true);

        initComponents(mongoDBConnectionEntity);
        setData(mongoDBConnectionEntity);
        if (edit) {
            nameField.setEditable(false);
        }
    }

    private void initComponents(final MongoDBConnectionEntity mongoDBConnectionEntity) {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
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

        validator.constrainTextFieldToBeNonEmpty("name", nameField, null);

        Utilities.setEscKeyStrokeDisposes(this);

        //Initialize authentication fields
        disablePasswordFields(mongoDBConnectionEntity);
        disableEncryptionButtons(mongoDBConnectionEntity);

        //add listeners to password fields to allow for interaction between them
        passwordTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableStoredPassword();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableStoredPassword();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableStoredPassword();
            }

            private void enableDisableStoredPassword() {
                String password = new String(passwordTextField.getPassword());

                if (!password.isEmpty()) {
                    storedPasswordComboBox.setEnabled(false);
                } else if (password.isEmpty()) {
                    storedPasswordComboBox.setEnabled(true);
                }
            }
        });

        selectPrivateKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                AssertionKeyAliasEditor keyPropertiesDialog = new AssertionKeyAliasEditor(TopComponents.getInstance().getTopParent(), mongoDBConnectionEntity, false);
                keyPropertiesDialog.pack();
                Utilities.centerOnScreen(keyPropertiesDialog);
                DialogDisplayer.display(keyPropertiesDialog);
                pack();
            }
        });

        manageCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                CertManagerWindow certManagerWindow = new CertManagerWindow(TopComponents.getInstance().getTopParent());
                certManagerWindow.pack();
                Utilities.centerOnScreen(certManagerWindow);
                DialogDisplayer.display(certManagerWindow);
                pack();
            }
        });


        storedPasswordComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (!Goid.isDefault(storedPasswordComboBox.getSelectedSecurePassword().getGoid())) {
                        passwordTextField.setEnabled(false);
                        showPasswordCheckBox.setEnabled(false);
                    } else {
                        passwordTextField.setEnabled(true);
                        showPasswordCheckBox.setEnabled(true);
                    }
                }
            }
        });

        authMethodComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (authMethodComboBox.getSelectedItem().equals(MongoDBEncryption.SSL)) {
                        manageCertificateButton.setEnabled(true);
                        selectPrivateKeyButton.setEnabled(false);
                        passwordTextField.setEnabled(true);
                    } else if (authMethodComboBox.getSelectedItem().equals(MongoDBEncryption.X509_Auth)) {
                        manageCertificateButton.setEnabled(true);
                        selectPrivateKeyButton.setEnabled(true);
                        passwordTextField.setEnabled(false);
                    } else {
                        manageCertificateButton.setEnabled(false);
                        selectPrivateKeyButton.setEnabled(false);
                        passwordTextField.setEnabled(true);
                    }
                }
            }
        });


        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                char echoChar = showPasswordCheckBox.isSelected() ?
                        (char) 0 :
                        '\u25cf';
                passwordTextField.setEchoChar(echoChar);
            }
        });

        pack();
    }

    public void disablePasswordFields(MongoDBConnectionEntity entity) {

        if (entity.getPassword() != null && !entity.getPassword().isEmpty()) {
            storedPasswordComboBox.setEnabled(false);
        } else if (!Goid.isDefault(entity.getStoredPasswordGoid()) &&
                entity.getStoredPasswordGoid() != null) {
            passwordTextField.setEnabled(false);
        } else if (entity.getAuthType() != null && entity.getAuthType().equals(MongoDBEncryption.X509_Auth.name())) {
            passwordTextField.setEnabled(false);
        }
    }

    public void disableEncryptionButtons(MongoDBConnectionEntity entity) {
        if (entity.getAuthType() != null && entity.getAuthType().equals(MongoDBEncryption.SSL.name())) {
            manageCertificateButton.setEnabled(true);
            selectPrivateKeyButton.setEnabled(false);
        } else if (entity.getAuthType() != null && entity.getAuthType().equals(MongoDBEncryption.X509_Auth.name())) {
            selectPrivateKeyButton.setEnabled(true);
            manageCertificateButton.setEnabled(true);
        } else {
            selectPrivateKeyButton.setEnabled(false);
            manageCertificateButton.setEnabled(false);
        }
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void setData(MongoDBConnectionEntity mongoDBConnectionEntity) {
        if (mongoDBConnectionEntity == null) {
            return;
        }

        populateAuthMethod(mongoDBConnectionEntity);
        populateReadPreference(mongoDBConnectionEntity);

        nameField.setText(mongoDBConnectionEntity.getName());
        uriTextField.setText(mongoDBConnectionEntity.getUri());
        if (mongoDBConnectionEntity.getPort() == null) {
            portTextField.setText("27017");
        } else {
            portTextField.setText(mongoDBConnectionEntity.getPort());
        }
        databaseTextField.setText(mongoDBConnectionEntity.getDatabaseName());
        userNameTextField.setText(mongoDBConnectionEntity.getUsername());
        passwordTextField.setText(mongoDBConnectionEntity.getPassword());

        if (mongoDBConnectionEntity.getStoredPasswordGoid() != null) {
            storedPasswordComboBox.setSelectedSecurePassword(mongoDBConnectionEntity.getStoredPasswordGoid());
        }

        if (mongoDBConnectionEntity.getAuthType() != null) {
            authMethodComboBox.setSelectedItem(MongoDBEncryption.valueOf(mongoDBConnectionEntity.getAuthType()));
        }

        if (mongoDBConnectionEntity.getReadPreference() != null) {
            readPreferenceComboBox.setSelectedItem(MongoDBReadPreference.valueOf(mongoDBConnectionEntity.getReadPreference()));
        }

        pack();
    }

    public MongoDBConnectionEntity getData(MongoDBConnectionEntity mongoDBConnectionEntity) {
        mongoDBConnectionEntity.setName(nameField.getText().trim());
        mongoDBConnectionEntity.setUri(uriTextField.getText());
        mongoDBConnectionEntity.setPort(portTextField.getText());
        mongoDBConnectionEntity.setDatabaseName(databaseTextField.getText());
        mongoDBConnectionEntity.setUsername(userNameTextField.getText());
        mongoDBConnectionEntity.setPassword(new String(passwordTextField.getPassword()));
        mongoDBConnectionEntity.setStoredPasswordGoid(storedPasswordComboBox.getSelectedSecurePassword().getGoid());
        mongoDBConnectionEntity.setAuthType(((MongoDBEncryption) authMethodComboBox.getSelectedItem()).name());
        if (!MongoDBEncryption.X509_Auth.equals(authMethodComboBox.getSelectedItem())){
            mongoDBConnectionEntity.setUsesNoKey(true);
        }
        mongoDBConnectionEntity.setReadPreference(((MongoDBReadPreference) readPreferenceComboBox.getSelectedItem()).name());

        return mongoDBConnectionEntity;
    }

    private void populateAuthMethod(MongoDBConnectionEntity mongoDBConnectionEntity) {
        MongoDBEncryption selectedAuthMethod = MongoDBEncryption.NO_ENCRYPTION;
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for (MongoDBEncryption encryptionMethod : MongoDBEncryption.values()) {
            model.addElement(encryptionMethod);
            if (encryptionMethod.equals(mongoDBConnectionEntity.getAuthType())) {
                selectedAuthMethod = encryptionMethod;
            }
        }

        authMethodComboBox.setModel(model);
        authMethodComboBox.setSelectedItem(selectedAuthMethod);
    }

    private void populateReadPreference(MongoDBConnectionEntity mongoDBConnectionEntity) {
        MongoDBReadPreference selectedReadPreference = MongoDBReadPreference.Primary;
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for (MongoDBReadPreference aReadPreference : MongoDBReadPreference.values()) {
            model.addElement(aReadPreference);
            if (aReadPreference.equals(mongoDBConnectionEntity.getReadPreference())) {
                selectedReadPreference = aReadPreference;
            }
        }

        readPreferenceComboBox.setModel(model);
        readPreferenceComboBox.setSelectedItem(selectedReadPreference);
    }
}
