package com.l7tech.external.assertions.ssh.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;

/**
 * SFTP polling listener properties dialog.
 */
public class SftpPollingListenerPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger( SftpPollingListenerPropertiesDialog.class.getName() );
    private static final String DIALOG_TITLE = "SFTP Polling Listener Properties";

    private JPanel mainPanel;
    private JTextField nameField;
    private JCheckBox enabledCheckBox;
    private JRadioButton passwordRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox validateServersHostCheckBox;
    private JButton manageHostKeyButton;
    private JTextField usernameField;
    private SecurePasswordComboBox passwordField;
    private SecurePasswordComboBox privateKeyField;
    private JButton managePasswordsPrivateKeysButton;
    private JTextField directoryField;
    private JComboBox contentTypeComboBox;
    private JSpinner pollingIntervalField;
    private JCheckBox enableResponsesCheckBox;
    private JCheckBox deleteProcessedMessagesCheckBox;
    private JCheckBox hardwiredServiceCheckBox;
    private JComboBox serviceNameComboBox;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel byteLimitHolderPanel;
    private ByteLimitPanel byteLimitPanel;

    private String hostKey;
    private SsgActiveConnector connector;
    private boolean confirmed = false;

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    public SftpPollingListenerPropertiesDialog(Dialog owner, SsgActiveConnector connector) {
        super(owner, DIALOG_TITLE, true);
        this.connector = connector;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);

        nameField.setDocument(new MaxLengthDocument(128));
        nameField.getDocument().addDocumentListener(enableDisableListener);
        hostField.setDocument(new MaxLengthDocument(255));
        hostField.getDocument().addDocumentListener(enableDisableListener);
        portField.setDocument(new MaxLengthDocument(5));
        portField.getDocument().addDocumentListener(enableDisableListener);
        validateServersHostCheckBox.addActionListener(enableDisableListener);
        usernameField.setDocument(new MaxLengthDocument(255));
        usernameField.getDocument().addDocumentListener(enableDisableListener);
        passwordField.addActionListener(enableDisableListener);
        privateKeyField.addActionListener(enableDisableListener);
        directoryField.getDocument().addDocumentListener(enableDisableListener);
        contentTypeComboBox.addActionListener(enableDisableListener);

        pollingIntervalField.setModel(new SpinnerNumberModel(60, 1, Integer.MAX_VALUE, 5));

        ActionListener authTypeActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.setEnabled(passwordRadioButton.isSelected());
                privateKeyField.setEnabled(privateKeyRadioButton.isSelected());
            }
        };
        passwordRadioButton.addActionListener(authTypeActionListener);
        privateKeyRadioButton.addActionListener(authTypeActionListener);

        manageHostKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SftpPollingListenerPropertiesDialog.this, hostKey,
                        HostKeyDialog.HostKeyValidationType.VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT);
                Utilities.centerOnParentWindow(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if(dialog.isConfirmed()) {
                            hostKey = dialog.getHostKey();
                            enableOrDisableComponents();
                        }
                    }
                });
            }
        });

        passwordField.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());
        privateKeyField.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());

        // load private key type (password type loaded by default by SecurePasswordComboBox constructor)
        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);

        managePasswordsPrivateKeysButton.addActionListener(enableDisableListener);
        managePasswordsPrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                passwordField.reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
                privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                pack();
            }
        });

        DefaultComboBoxModel contentTypeComboBoxModel = new DefaultComboBoxModel();
        ContentTypeHeader[] offeredTypes = new ContentTypeHeader[] {
                ContentTypeHeader.XML_DEFAULT,
                ContentTypeHeader.TEXT_DEFAULT,
                ContentTypeHeader.SOAP_1_2_DEFAULT,
                ContentTypeHeader.APPLICATION_JSON,
                ContentTypeHeader.OCTET_STREAM_DEFAULT,
        };
        for (ContentTypeHeader offeredType : offeredTypes) {
            contentTypeComboBoxModel.addElement(offeredType.getFullValue());
        }
        contentTypeComboBox.setModel(contentTypeComboBoxModel);

        serviceNameComboBox.setRenderer( TextListCellRenderer.<ServiceComboItem>basicComboBoxRenderer() );
        ServiceComboBox.populateAndSelect(serviceNameComboBox, false, 0L);
        hardwiredServiceCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serviceNameComboBox.setEnabled(hardwiredServiceCheckBox.isSelected());
            }
        });

        InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        byteLimitPanel = new ByteLimitPanel();
        byteLimitPanel.setAllowContextVars(false);
        byteLimitHolderPanel.setLayout(new BorderLayout());
        byteLimitHolderPanel.add(byteLimitPanel, BorderLayout.CENTER);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return byteLimitPanel.validateFields();
            }
        });

        pack();
        modelToView( connector );
        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {
        boolean enableOkButton = true;

        if(nameField.getText() == null || nameField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        if(hostField.getText() == null || hostField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        try {
            int port = Integer.parseInt(portField.getText().trim());
            if(port <= 0 || port > 65535) {
                enableOkButton = false;
            }
        } catch (Exception e) {
            enableOkButton = false;
        }

        if(!validateServersHostCheckBox.isSelected()) {
            manageHostKeyButton.setEnabled(false);
        } else {
            manageHostKeyButton.setEnabled(true);
            if(hostKey == null) {
                enableOkButton = false;
            }
        }

        String userName = usernameField.getText();
        if(StringUtils.isEmpty(userName) || "root".equals(userName)) {
            enableOkButton = false;
        }

        if(passwordRadioButton.isSelected()) {
            if(passwordField.getSelectedSecurePassword() == null) {
                enableOkButton = false;
            }
            privateKeyField.setEnabled(false);
        } else if(privateKeyRadioButton.isSelected()) {
            if(privateKeyField.getSelectedSecurePassword() == null) {
                enableOkButton = false;
            }
            passwordField.setEnabled(false);
        }

        if(directoryField.getText() == null || directoryField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        Object ctypeObj = contentTypeComboBox.getSelectedItem();
        if(ctypeObj == null || ctypeObj.toString().trim().length() == 0) {
            enableOkButton = false;
        }

        if(hardwiredServiceCheckBox.isSelected() && serviceNameComboBox.getSelectedItem() == null) {
            enableOkButton = false;
        }

        if (!StringUtils.isEmpty(byteLimitPanel.validateFields())) {
            enableOkButton = false;
        }

        okButton.setEnabled(enableOkButton);
    }

    private void modelToView( final SsgActiveConnector connector ) {
        nameField.setText( connector.getName() == null ? "" : connector.getName().trim() );
        enabledCheckBox.setSelected( connector.isEnabled() );

        hostField.setText( connector.getProperty( PROPERTIES_KEY_SFTP_HOST, "" ) );
        portField.setText( connector.getProperty( PROPERTIES_KEY_SFTP_PORT, "" ) );
        if( connector.getProperty( SsgActiveConnector.PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT) != null) {
            validateServersHostCheckBox.setSelected(true);
            hostKey = connector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT);
            if ( hostKey.isEmpty() ) hostKey = null;
        } else {
            validateServersHostCheckBox.setSelected(false);
            hostKey = null;
        }
        usernameField.setText( connector.getProperty( PROPERTIES_KEY_SFTP_USERNAME, "" ) );

        final long passwordOid = connector.getLongProperty( SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID, -1L);
        final long keyOid = connector.getLongProperty( SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID, -1L);
        if( passwordOid != -1L ) {
            passwordField.setSelectedSecurePassword( passwordOid );
            passwordRadioButton.setSelected(true);
        } else if ( keyOid != -1L ) {
            privateKeyField.setSelectedSecurePassword( keyOid );
            privateKeyRadioButton.setSelected(true);
        } else {
           passwordRadioButton.setSelected(true);
        }

        directoryField.setText( connector.getProperty( PROPERTIES_KEY_SFTP_DIRECTORY, "" ) );

        String ctype = connector.getProperty(PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE);
        if (ctype == null) {
            contentTypeComboBox.setSelectedIndex(-1);
        } else {
            contentTypeComboBox.setSelectedItem(ctype);
            if (!ctype.equalsIgnoreCase((String)contentTypeComboBox.getSelectedItem())) {
                ((DefaultComboBoxModel)contentTypeComboBox.getModel()).addElement(ctype);
                contentTypeComboBox.setSelectedItem(ctype);
            }
        }

        pollingIntervalField.setValue( connector.getIntegerProperty( PROPERTIES_KEY_POLLING_INTERVAL, 60 ));
        enableResponsesCheckBox.setSelected( connector.getBooleanProperty( PROPERTIES_KEY_ENABLE_RESPONSE_MESSAGES ) );
        deleteProcessedMessagesCheckBox.setSelected( connector.getBooleanProperty( PROPERTIES_KEY_SFTP_DELETE_ON_RECEIVE ) );

        if( connector.getHardwiredServiceOid() != null ) {
            hardwiredServiceCheckBox.setSelected(true);
            ServiceComboBox.populateAndSelect(serviceNameComboBox, true, connector.getHardwiredServiceOid());
        } else {
            hardwiredServiceCheckBox.setSelected(false);
        }
        serviceNameComboBox.setEnabled(hardwiredServiceCheckBox.isSelected());

        final TransportAdmin transportAdmin = getTransportAdmin();
        if ( transportAdmin != null ) {
            byteLimitPanel.setValue( connector.getProperty(PROPERTIES_KEY_REQUEST_SIZE_LIMIT), transportAdmin.getXmlMaxBytes() );
        }

        enableOrDisableComponents();
    }

    private void viewToModel( final SsgActiveConnector connector ) {
        connector.setName( nameField.getText() );
        connector.setType( ACTIVE_CONNECTOR_TYPE_SFTP );
        connector.setEnabled( enabledCheckBox.isSelected() );

        setProperty( connector, PROPERTIES_KEY_SFTP_HOST, hostField.getText() );
        setProperty( connector, PROPERTIES_KEY_SFTP_PORT, portField.getText() );

        if( validateServersHostCheckBox.isSelected() ) {
            setProperty( connector, PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT, hostKey );
        } else {
            connector.removeProperty( PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT );
        }

        setProperty( connector, PROPERTIES_KEY_SFTP_USERNAME, usernameField.getText() );
        connector.removeProperty( PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID );
        connector.removeProperty( PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID );
        if( passwordRadioButton.isSelected() ) {
            if(passwordField.getSelectedSecurePassword() != null) {
                connector.setProperty(
                        PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID,
                        Long.toString( passwordField.getSelectedSecurePassword().getOid() ) );
            }
        } else if( privateKeyRadioButton.isSelected() ) {
            connector.setProperty(
                    PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID,
                    Long.toString( privateKeyField.getSelectedSecurePassword().getOid() ) );
        }

        setProperty( connector, PROPERTIES_KEY_SFTP_DIRECTORY, directoryField.getText() );
        setProperty( connector, PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE, contentTypeComboBox.getSelectedItem().toString() );
        setProperty( connector, PROPERTIES_KEY_POLLING_INTERVAL, pollingIntervalField.getValue().toString() );
        setProperty( connector, PROPERTIES_KEY_ENABLE_RESPONSE_MESSAGES, Boolean.toString( enableResponsesCheckBox.isSelected() ) );
        setProperty( connector, PROPERTIES_KEY_SFTP_DELETE_ON_RECEIVE, Boolean.toString( deleteProcessedMessagesCheckBox.isSelected() ) );

        final PublishedService service = hardwiredServiceCheckBox.isSelected() ?
            ServiceComboBox.getSelectedPublishedService(serviceNameComboBox) :
            null;
        connector.setHardwiredServiceOid( service == null ? null : service.getOid() );

        connector.removeProperty( PROPERTIES_KEY_REQUEST_SIZE_LIMIT );
        String requestByteLimit = byteLimitPanel.getValue();
        if (!StringUtils.isEmpty(requestByteLimit)) {
            setProperty( connector, PROPERTIES_KEY_REQUEST_SIZE_LIMIT, requestByteLimit );
        }
    }

    private void setProperty( final SsgActiveConnector connector, final String name, final String value ) {
        final String trimmedValue = value == null ? null : value.trim();
        if ( trimmedValue != null && !trimmedValue.isEmpty() ) {
            connector.setProperty( name, trimmedValue );
        } else {
            connector.removeProperty( name );
        }
    }

    private void onSave() {
        String directory = directoryField.getText();
        if (!StringUtils.isEmpty(directory) && ("/".equals(directory) || "c:\\\\".equals(directory.toLowerCase()))) {
            DialogDisplayer.showSafeConfirmDialog(
                    this,
                    "<html><center>Root directory is currently set as the scan directory. System files <p>" +
                            "may be deleted or renamed causing unexpected system behavior.<p>" +
                            "Do you really want to continue with this?</center></html>",
                    "Confirm Use of Root Directory",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    465, 180,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.CANCEL_OPTION) {
                                return;
                            }
                            viewToModel( connector );
                            confirmed = true;
                            dispose();
                        }
                    }
            );
        } else {
            viewToModel( connector );
            confirmed = true;
            dispose();
        }
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public SsgActiveConnector getConnector() {
        return connector;
    }

    public void selectNameField() {
        nameField.requestFocus();
        nameField.selectAll();
    }

    private TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Admin context not present.");
            return null;
        }
        return registry.getTransportAdmin();
    }
}
