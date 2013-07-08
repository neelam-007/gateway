package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.password.SecurePassword.SecurePasswordType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class SecurePasswordPropertiesDialog extends JDialog {
    private static ResourceBundle resources = ResourceBundle.getBundle( SecurePasswordPropertiesDialog.class.getName() );

    @SuppressWarnings({ "RedundantTypeArguments" })
    private static final Collection<Integer> RSA_KEY_SIZES = CollectionUtils.<Integer>list( 512, 768, 1024, 1280, 2048 );

    private static final Pattern CONTEXT_VARIABLE_SYNTAX = Pattern.compile("[A-Za-z][A-Za-z0-9_\\-]*");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private SquigglyTextField nameField;
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
    private JButton viewPublicKeyButton;
    private JCheckBox generateCheckBox;
    private JComboBox generateKeyBitsComboBox;
    private SecurityZoneWidget zoneControl;

    private final SecurePassword securePassword;
    private final boolean newRecord;

    private boolean confirmed = false;
    private char[] enteredPassword;
    private String pemPublicKey;
    private int generateKeybits;

    public SecurePasswordPropertiesDialog( final Window owner, final SecurePassword securePassword, final boolean readOnly) {
        super(owner, "Stored Password Properties");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        Utilities.setEscKeyStrokeDisposes(this);

        final boolean[] allowVariableCheckBoxStateStash = new boolean[1];


        this.securePassword = securePassword;

        newRecord = securePassword.getOid() == SecurePassword.DEFAULT_OID;

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        
        nameField.getDocument().addDocumentListener(new RunOnChangeListener() {


            @Override
            public void run() {
                
                if ( (! ( CONTEXT_VARIABLE_SYNTAX.matcher(nameField.getText()).matches()
                           || nameField.getText().equals(""))  )  )  {

                    if ( allowVariableCheckBox.isEnabled() ) {
                        allowVariableCheckBoxStateStash[0] = allowVariableCheckBox.isSelected();
                        allowVariableCheckBox.setSelected(false);
                        allowVariableCheckBox.setEnabled(false);
                        nameField.setToolTipText("You may set this name but you will not be able to use the " +
                                                 "corresponding Context Variable in Policy to obtain its value.");
                        nameField.setAll();
                     
                    }

                } else {

                    if (! allowVariableCheckBox.isEnabled() ) {
                        allowVariableCheckBox.setEnabled(true);
                        allowVariableCheckBox.setSelected(allowVariableCheckBoxStateStash[0]);
                        nameField.setToolTipText("");
                        nameField.setNone();
                    }

                }

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

        generateCheckBox.addActionListener( enableDisableListener );
        generateKeyBitsComboBox.setModel( Utilities.comboBoxModel( RSA_KEY_SIZES ) );
        generateKeyBitsComboBox.setSelectedIndex( generateKeyBitsComboBox.getModel().getSize()-1 );

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
                                } else {
                                    dispose();
                                }
                            }
                        });
                        return;
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
                        if ( !generateCheckBox.isSelected() ) {
                            String passToSave = pemPrivateKeyField.getText();
                            if (StringUtils.isEmpty(passToSave)) {
                                return "PEM private key must not be empty.";
                            } else if (!SecurePasswordPemPrivateKeyDialog.simplePemPrivateKeyValidation(passToSave)) {
                                return "The key must be in PEM private key format.";
                            }
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
                changePassword( maxPasswordLength );
            }
        });

        viewPublicKeyButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                viewPublicKey();
            }
        } );

        Utilities.setMaxLength(nameField.getDocument(), EntityUtil.getMaxFieldLength(SecurePassword.class, "name", 128));
        Utilities.setMaxLength(descriptionField.getDocument(), EntityUtil.getMaxFieldLength(SecurePassword.class, "description", 128));
        Utilities.setMaxLength(passwordField.getDocument(), maxPasswordLength);
        Utilities.setMaxLength(confirmPasswordField.getDocument(), maxPasswordLength);
        Utilities.setMaxLength(pemPrivateKeyField.getDocument(), maxPasswordLength);

        zoneControl.configure(securePassword.getOid() == SecurePassword.DEFAULT_OID ? OperationType.CREATE : readOnly ? OperationType.READ : OperationType.UPDATE, securePassword);

        enableOrDisableComponents();
        modelToView();
    }

    private void changePassword( final int maxPasswordLength ) {
        char[] got = null;
        switch ((SecurePasswordType) typeComboBox.getSelectedItem()) {
            case PASSWORD:
                got = PasswordDoubleEntryDialog.getPassword( this, "Enter Password", maxPasswordLength );
                break;
            case PEM_PRIVATE_KEY:
                got = SecurePasswordPemPrivateKeyDialog.getPemPrivateKey( this, "Enter PEM Private Key", maxPasswordLength );
                break;
            default:
                break;
        }

        if (got == null)
            return;
        pemPublicKey = null;
        enteredPassword = got;
        lastUpdateLabel.setText(new Date().toString());
        enableOrDisableComponents();
    }

    private void viewPublicKey() {
        String publicKey = pemPublicKey;

        if ( publicKey == null ) {
            try {
                publicKey = pemPublicKey = Registry.getDefault().getTrustedCertManager().getSecurePasswordPublicKey( securePassword.getOid() );
            } catch ( IllegalStateException e ) {
                // not connected
            } catch ( ObjectNotFoundException e ) {
                DialogDisplayer.showMessageDialog( this, "Public Key Error", "The stored password could not be found.", null );
            } catch ( FindException e ) {
                DialogDisplayer.showMessageDialog( this, "Public Key Error", "Error accessing public key:\n" + ExceptionUtils.getMessage( e ), e );
            }
        }

        if ( publicKey != null ) {
            final Object message = Utilities.getTextDisplayComponent( publicKey, 600, 240, -1, -1 );
            DialogDisplayer.showMessageDialog( this, message, "PEM Public Key", JOptionPane.PLAIN_MESSAGE, null );
        }
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
            viewPublicKeyButton.setVisible(false);
            final SecurePasswordType type = (SecurePasswordType) typeComboBox.getSelectedItem();
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
                    passwordField.setVisible( false );
                    passwordField.setEnabled( false );
                    confirmPasswordField.setVisible( false );
                    confirmPasswordField.setEnabled( false );
                    passwordLabel.setVisible( false );
                    confirmPasswordLabel.setVisible( false );
                    pemPrivateKeyFieldPanel.setVisible( true );
                    pemPrivateKeyFieldPanel.setEnabled( true );
                    pemPrivateKeyLabel.setVisible( true );
                    generateKeyBitsComboBox.setEnabled( generateCheckBox.isSelected() );
                    pemPrivateKeyField.setEnabled( !generateCheckBox.isSelected() );
                    loadFromFileButton.setEnabled( !generateCheckBox.isSelected() );
                    break;
                default:
                    break;
            }
        } else {
            typeComboBox.setEnabled(false);
            typeLabel.setEnabled(false);

            pemPrivateKeyFieldPanel.setVisible(false);
            pemPrivateKeyFieldPanel.setEnabled( false );
            pemPrivateKeyLabel.setVisible( false );
            viewPublicKeyButton.setVisible( securePassword.getType() == SecurePasswordType.PEM_PRIVATE_KEY );
            viewPublicKeyButton.setEnabled( viewPublicKeyButton.isVisible() && enteredPassword==null );

            passwordField.setVisible( false );
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
        if (newRecord && !generateCheckBox.isSelected()) {
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
        securePassword.setSecurityZone(zoneControl.getSelectedZone());
        generateKeybits = generateCheckBox.isSelected() ? (Integer)generateKeyBitsComboBox.getSelectedItem() : 0;
    }

    private String nn(String s) {
        return s == null ? "" : s;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Get the plaintext password.  Only meaningful if {@link #isConfirmed()}.
     * @return For a new SecurePassword entity: the password entered (and confirmed) by the user.  Or null if a PEM key should be generated.
     *         For an existing SecurePassword:  the new password, if the user requested a password change, otherwise null.
     */
    public char[] getEnteredPassword() {
        return enteredPassword;
    }

    /**
     * The number of bits to use in the generated RSA key.
     *
     * @return The key bits
     */
    public int getGenerateKeybits() {
        return generateKeybits;
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
