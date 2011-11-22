package com.l7tech.external.assertions.ssh.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class SshRouteAssertionPropertiesPanel extends AssertionPropertiesOkCancelSupport<SshRouteAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle( SshRouteAssertionPropertiesPanel.class.getName());

    private JPanel mainPanel;
    private JRadioButton passwordRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JTextField hostField;
    private JTextField usernameField;
    private SecurePasswordComboBox passwordField;
    private JButton managePasswordsButton;
    private JTextField directoryTextField;
    private JComboBox messageSource;
    private JComboBox messageTarget;
    private JPanel messageTargetVariableNamePanel;
    private TargetVariablePanel messageTargetVariablePanel;
    private JTextField fileNameTextField;
    private JTextField connectTimeoutTextField;
    private JTextField portNumberTextField;
    private JRadioButton wssIgnoreButton;
    private JRadioButton wssCleanupButton;
    private JRadioButton wssRemoveButton;
    private JCheckBox validateServerSHostCheckBox;
    private JButton manageHostKeyButton;
    private JRadioButton SCPRadioButton;
    private JRadioButton uploadToRadioButton;
    private JRadioButton downloadFromRadioButton;
    private JRadioButton passThroughCredentialsInRadioButton;
    private JRadioButton specifyUserCredentialsRadioButton;
    private JTextField readTimeoutTextField;
    private JComboBox contentTypeComboBox;
    private JPanel specifyUserCredentialsPanel;
    private JLabel userNameLabel;
    private SecurePasswordComboBox privateKeyField;
    private JPanel responseLimitHolderPanel;
    private ByteLimitPanel responseLimitPanel;
    private InputValidator validators;
    private AbstractButton[] secHdrButtons = { wssIgnoreButton, wssCleanupButton, wssRemoveButton, null };
    private String hostKey;

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableDisableFields();
        }
    };

    public SshRouteAssertionPropertiesPanel(Frame parent, SshRouteAssertion assertion) {
        super(SshRouteAssertion.class, parent, assertion.getPropertiesDialogTitle(), true);

        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        passwordRadioButton.addActionListener(enableDisableListener);
        privateKeyRadioButton.addActionListener(enableDisableListener);
        validateServerSHostCheckBox.addActionListener(enableDisableListener);
        passThroughCredentialsInRadioButton.addActionListener(enableDisableListener);
        specifyUserCredentialsRadioButton.addActionListener(enableDisableListener);
        fileNameTextField.getDocument().addDocumentListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(fileNameTextField);

        manageHostKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SshRouteAssertionPropertiesPanel.this, hostKey,
                        HostKeyDialog.HostKeyValidationType.VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if(dialog.isConfirmed()) {
                            hostKey = dialog.getHostKey();
                            enableDisableFields();
                        }
                    }
                });
            }
        });

        // load private keys to this combo box
        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);

        managePasswordsButton.addActionListener(new ActionListener() {
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

        downloadFromRadioButton.addActionListener(enableDisableListener);
        uploadToRadioButton.addActionListener(enableDisableListener);
        
        messageSource.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", null), null, false ) );
        messageTarget.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", "Message Variable"), null, true ) );
        messageTarget.addActionListener( enableDisableListener );
        messageTargetVariablePanel = new TargetVariablePanel();
        messageTargetVariableNamePanel.setLayout(new BorderLayout());
        messageTargetVariableNamePanel.add( messageTargetVariablePanel, BorderLayout.CENTER );
        messageTargetVariablePanel.addChangeListener( enableDisableListener );
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

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

        responseLimitPanel = new ByteLimitPanel();
        responseLimitPanel.setAllowContextVars(false);
        responseLimitHolderPanel.setLayout(new BorderLayout());
        responseLimitHolderPanel.add(responseLimitPanel, BorderLayout.CENTER);

        //validators
        validators = new InputValidator(this, getResourceString("errorTitle"));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"), hostField, null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("fileNameLabel"), fileNameTextField, null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"), usernameField, null));
        validators.addRule(validators.constrainTextFieldToNumberRange(getResourceString("sshTimeoutLabel"), connectTimeoutTextField, 1L, (long) Short.MAX_VALUE ));
        validators.addRule(validators.constrainTextFieldToNumberRange(getResourceString("sshTimeoutLabel"), readTimeoutTextField, 1L, (long) Short.MAX_VALUE ));

        validators.addRule(new InputValidator.ComponentValidationRule(portNumberTextField) {
            @Override
            public String getValidationError() {
                boolean portIsValid = isPortValid();
                if(!portIsValid){
                    final int port = getDefaultPortNumber();
                    return MessageFormat.format(getResourceString("portError"), port);

                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(passwordField) {
            @Override
            public String getValidationError() {
                if(specifyUserCredentialsRadioButton.isSelected() && passwordRadioButton.isSelected()) {
                    if (passwordField == null || passwordField.getItemCount() == 0)
                    {
                        return getResourceString("passwordEmptyError");
                    }
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(privateKeyField) {
            @Override
            public String getValidationError() {
                if(specifyUserCredentialsRadioButton.isSelected() && privateKeyRadioButton.isSelected()) {
                    if (privateKeyField == null || privateKeyField.getItemCount() == 0)
                    {
                        return getResourceString("privateKeyEmptyError");
                    }
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(validateServerSHostCheckBox) {
            @Override
            public String getValidationError() {

                if(validateServerSHostCheckBox.isSelected()) {
                    if (hostKey == null || hostKey.equalsIgnoreCase(""))
                    {
                        return getResourceString("hostKeyEmptyError");
                    }
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return messageTargetVariablePanel.getErrorMessage();
            }
        });

        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return responseLimitPanel.validateFields();
            }
        });

        super.initComponents();
    }   // initComponents()

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    private void enableDisableFields() {
        // connection tab
        manageHostKeyButton.setEnabled(validateServerSHostCheckBox.isSelected());
        boolean isDownloadFrom = downloadFromRadioButton.isSelected();
        contentTypeComboBox.setEnabled(isDownloadFrom);
        messageSource.setEnabled(!isDownloadFrom);
        messageTarget.setEnabled( isDownloadFrom );
        messageTargetVariablePanel.setEnabled(
                messageTarget.isEnabled() &&
                messageTarget.getSelectedItem()!=null &&
                ((MessageTargetable)messageTarget.getSelectedItem()).getTarget()== TargetMessageType.OTHER );
        responseLimitPanel.setEnabled(isDownloadFrom);
        wssIgnoreButton.setEnabled(!isDownloadFrom);
        wssCleanupButton.setEnabled(!isDownloadFrom);
        wssRemoveButton.setEnabled(!isDownloadFrom);

        // authentication tab
        boolean isSpecifyUserCredentials = specifyUserCredentialsRadioButton.isSelected();
        specifyUserCredentialsPanel.setEnabled(isSpecifyUserCredentials);
        userNameLabel.setEnabled(isSpecifyUserCredentials);
        usernameField.setEnabled(isSpecifyUserCredentials);
        passwordRadioButton.setEnabled(isSpecifyUserCredentials);
        privateKeyRadioButton.setEnabled(isSpecifyUserCredentials);
        privateKeyField.setEnabled(isSpecifyUserCredentials && privateKeyRadioButton.isSelected());
        passwordField.setEnabled(isSpecifyUserCredentials && passwordRadioButton.isSelected());
        managePasswordsButton.setEnabled(isSpecifyUserCredentials);
    }

    /*
     * Populate fields in SSH Route dialog.
     */
    @Override
    public void setData(SshRouteAssertion assertion) {
        messageSource.setModel(buildMessageSourceComboBoxModel(assertion));
        messageSource.setSelectedItem(new MessageTargetableSupport(assertion.getRequestTarget()));
        messageTarget.setModel( buildMessageTargetComboBoxModel(false) );
        final MessageTargetableSupport responseTarget = new MessageTargetableSupport( assertion.getResponseTarget() );
        messageTarget.setSelectedItem( new MessageTargetableSupport( responseTarget.getTarget() ) );
        if ( responseTarget.getTarget() == TargetMessageType.OTHER ){
            messageTargetVariablePanel.setVariable( responseTarget.getOtherTargetMessageVariable() );
        } else {
            messageTargetVariablePanel.setVariable( "" );
        }
        messageTargetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);

        SCPRadioButton.setSelected(assertion.isScpProtocol());
        downloadFromRadioButton.setSelected(assertion.isDownloadCopyMethod());

        if(assertion.getHost() != null) {
            hostField.setText(assertion.getHost().trim());
        }
        if(assertion.getPort() != null) {
            portNumberTextField.setText(assertion.getPort().trim());
        }
        if(assertion.getDirectory() != null) {
            directoryTextField.setText(assertion.getDirectory().trim());
        }
        if (assertion.getFileName() != null) {
           fileNameTextField.setText(assertion.getFileName());
        }

        connectTimeoutTextField.setText(Integer.toString(assertion.getConnectTimeout() / 1000));
        readTimeoutTextField.setText(Integer.toString(assertion.getReadTimeout() / 1000));

        String contentType = assertion.getDownloadContentType();
        if (contentType == null) {
            contentTypeComboBox.setSelectedIndex(0);
        } else {
            contentTypeComboBox.setSelectedItem(contentType);
            if (!contentType.equalsIgnoreCase((String)contentTypeComboBox.getSelectedItem())) {
                ((DefaultComboBoxModel)contentTypeComboBox.getModel()).addElement(contentType);
                contentTypeComboBox.setSelectedItem(contentType);
            }
        }

        // populate authorization settings
        specifyUserCredentialsRadioButton.setSelected(assertion.isCredentialsSourceSpecified());
        passThroughCredentialsInRadioButton.setSelected(!assertion.isCredentialsSourceSpecified());
        if(assertion.getPasswordOid() != null) {
            passwordField.setSelectedSecurePassword(assertion.getPasswordOid());
        }

        if(assertion.isUsePrivateKey() && assertion.getPrivateKeyOid() != null) {
            privateKeyRadioButton.setSelected(true);
            privateKeyField.setSelectedSecurePassword(assertion.getPrivateKeyOid());
        } else {
            passwordRadioButton.setSelected(true);
        }

        usernameField.setText(assertion.getUsername() == null ? "" : assertion.getUsername());

        if(assertion.isUsePublicKey() && assertion.getSshPublicKey() != null) {
            validateServerSHostCheckBox.setSelected(true);
            hostKey = assertion.getSshPublicKey();
        } else {
            validateServerSHostCheckBox.setSelected(false);
            hostKey = null;
        }

        responseLimitPanel.setValue(assertion.getResponseByteLimit(), Registry.getDefault().getPolicyAdmin().getXmlMaxBytes());

        enableDisableFields();

        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);
    }

    /** Copies view into model. */
    @Override
    public SshRouteAssertion getData(SshRouteAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }

        // populate SSH settings

        assertion.setHost(hostField.getText().trim());
        assertion.setPort(portNumberTextField.getText().trim());
        assertion.setDirectory(directoryTextField.getText());
        assertion.setFileName(fileNameTextField.getText());
        if (connectTimeoutTextField.getText().trim().isEmpty()) {
            connectTimeoutTextField.setText(Integer.toString(SshRouteAssertion.DEFAULT_CONNECT_TIMEOUT / 1000));
        }
        assertion.setConnectTimeout(Integer.parseInt(connectTimeoutTextField.getText()) * 1000);

        assertion.setScpProtocol(SCPRadioButton.isSelected());
        assertion.setDownloadCopyMethod(downloadFromRadioButton.isSelected());

        if (readTimeoutTextField.getText().trim().isEmpty()) {
            readTimeoutTextField.setText(Integer.toString(SshRouteAssertion.DEFAULT_READ_TIMEOUT / 1000));
        }
        assertion.setReadTimeout(Integer.parseInt(readTimeoutTextField.getText()) * 1000);

        if (contentTypeComboBox.getSelectedItem() != null) {
            assertion.setDownloadContentType(contentTypeComboBox.getSelectedItem().toString());
        }

        if(validateServerSHostCheckBox.isSelected() && hostKey != null) {
            assertion.setUsePublicKey(true);
            assertion.setSshPublicKey(hostKey);
        } else {
            assertion.setUsePublicKey(false);
            assertion.setSshPublicKey(null);
        }

        assertion.setRequestTarget( new MessageTargetableSupport(TargetMessageType.REQUEST, false) );
        assertion.setResponseTarget( new MessageTargetableSupport(TargetMessageType.RESPONSE, true) );
        if ( assertion.isDownloadCopyMethod() ) {
            final MessageTargetableSupport responseTarget =
                    new MessageTargetableSupport((MessageTargetable) messageTarget.getSelectedItem());
            if ( responseTarget.getTarget()==TargetMessageType.OTHER ) {
                responseTarget.setOtherTargetMessageVariable( messageTargetVariablePanel.getVariable());
                responseTarget.setSourceUsedByGateway( false );
                responseTarget.setTargetModifiedByGateway( true );
            }
            assertion.setResponseTarget( responseTarget );
        } else {
            assertion.setRequestTarget((MessageTargetableSupport) messageSource.getSelectedItem());
        }

        assertion.setResponseByteLimit(responseLimitPanel.getValue());

        // populate authorization settings

        assertion.setCredentialsSourceSpecified(specifyUserCredentialsRadioButton.isSelected());
        if (specifyUserCredentialsRadioButton.isSelected()) {
            assertion.setUsername(usernameField.getText().trim());
            if(passwordRadioButton.isSelected()) {
                assertion.setUsePrivateKey(false);
                assertion.setPasswordOid(passwordField.getSelectedSecurePassword().getOid());
                assertion.setPrivateKeyOid(null);
            } else if (privateKeyRadioButton.isSelected()) {
                assertion.setUsePrivateKey(true);
                assertion.setPrivateKeyOid(privateKeyField.getSelectedSecurePassword().getOid());
                assertion.setPasswordOid(null);
            }
        }

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        return assertion;
    }
    
    /**
     * @return Return true iff the port number is between 1 and 65535 or references a context variable.
     */
    private boolean isPortValid() {
        boolean isValid;
        String portStr = portNumberTextField.getText();
        try {
            int port = Integer.parseInt(portStr);
            isValid = port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            // must be using context variable
            isValid = Syntax.getReferencedNames(portStr).length > 0;
        }
        return isValid;
    }

    private String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

     private int getDefaultPortNumber() {
        return SshRouteAssertion.DEFAULT_SSH_PORT;
    }
}
