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
import com.l7tech.message.CommandKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityUtil;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Option;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static com.l7tech.util.Option.optional;

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
    private JRadioButton passThroughCredentialsInRadioButton;
    private JRadioButton specifyUserCredentialsRadioButton;
    private JTextField readTimeoutTextField;
    private JComboBox contentTypeComboBox;
    private JPanel specifyUserCredentialsPanel;
    private JLabel userNameLabel;
    private SecurePasswordComboBox privateKeyField;
    private JPanel responseLimitHolderPanel;
    private JCheckBox preserveFileMetadataCheckBox;
    private JRadioButton SFTPRadioButton;
    private JCheckBox failIfFileExistsCheckBox;
    private JTextField fileOffsetTextField;
    private JTextField fileLengthTextField;
    private JTextField newFileNameTextField;
    private JComboBox commandTypeComboBox;
    private TargetVariablePanel commandVariableNameTargetVariablePanel;
    private TargetVariablePanel saveFileSizeContextVariable;
    private JCheckBox setFileSizeToCheckBox;
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
        SCPRadioButton.addActionListener(enableDisableListener);
        SFTPRadioButton.addActionListener(enableDisableListener);
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

        passwordField.setRenderer(TextListCellRenderer.basicComboBoxRenderer());
        privateKeyField.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        // load private key type (password type loaded by default by SecurePasswordComboBox constructor)
        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);

        managePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                // save selection
                final Option<Goid> selectedPasswordGoid = optional(passwordField.getSelectedSecurePassword()).map(PersistentEntityUtil.goid());
                final Option<Goid> selectedPrivateKeyGoid = optional( privateKeyField.getSelectedSecurePassword() ).map( PersistentEntityUtil.goid() );
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        passwordField.reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
                        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                        // load selection
                        if (selectedPasswordGoid.isSome()) passwordField.setSelectedSecurePassword(selectedPasswordGoid.some());
                        if (selectedPrivateKeyGoid.isSome()) privateKeyField.setSelectedSecurePassword(selectedPrivateKeyGoid.some());
                        pack();
                    }
                });
            }
        });

        commandTypeComboBox.addActionListener(enableDisableListener);
        setFileSizeToCheckBox.addActionListener(enableDisableListener);
        commandVariableNameTargetVariablePanel.setValueWillBeWritten(false);

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
        responseLimitPanel.setAllowContextVars(true);
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
                if(specifyUserCredentialsRadioButton.isSelected() && passwordRadioButton.isSelected() && (passwordField == null || passwordField.getItemCount() == 0)) {
                    return getResourceString("passwordEmptyError");
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(privateKeyField) {
            @Override
            public String getValidationError() {
                if(specifyUserCredentialsRadioButton.isSelected() && privateKeyRadioButton.isSelected() && (privateKeyField == null || privateKeyField.getItemCount() == 0)) {
                    return getResourceString("privateKeyEmptyError");
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(validateServerSHostCheckBox) {
            @Override
            public String getValidationError() {

                if(validateServerSHostCheckBox.isSelected() && (hostKey == null || hostKey.equalsIgnoreCase(""))) {
                    return getResourceString("hostKeyEmptyError");
                }
                return null;
            }
        });

        // validate the commandtype variable name is correct
        validators.addRule(new InputValidator.ComponentValidationRule(commandTypeComboBox) {
            @Override
            public String getValidationError() {

                if(isCommandTypeFromVariable() && (!commandVariableNameTargetVariablePanel.isEntryValid())) {
                    return getResourceString("commandVariableNameError");
                }
                return null;
            }
        });

        //validate that an offset is given
        validators.addRule(new InputValidator.ComponentValidationRule(fileOffsetTextField) {
            @Override
            public String getValidationError() {

                if((CommandKnob.CommandType.PUT.equals(getSelectedCommandType()) || CommandKnob.CommandType.GET.equals(getSelectedCommandType()) || isCommandTypeFromVariable()) && (fileOffsetTextField.getText() == null || fileOffsetTextField.getText().isEmpty())) {
                    return getResourceString("fileOffsetError");
                }
                return null;
            }
        });

        //validate that a file length is given
        validators.addRule(new InputValidator.ComponentValidationRule(fileLengthTextField) {
            @Override
            public String getValidationError() {

                if((CommandKnob.CommandType.GET.equals(getSelectedCommandType()) || isCommandTypeFromVariable()) && (fileLengthTextField.getText() == null || fileLengthTextField.getText().isEmpty())) {
                    return getResourceString("fileLengthError");
                }
                return null;
            }
        });

        //validate that a new file name is given.
        validators.addRule(new InputValidator.ComponentValidationRule(newFileNameTextField) {
            @Override
            public String getValidationError() {

                if(SFTPRadioButton.isSelected() && (CommandKnob.CommandType.MOVE.equals(getSelectedCommandType()) || isCommandTypeFromVariable()) && (newFileNameTextField.getText() == null || newFileNameTextField.getText().isEmpty())) {
                    return getResourceString("newFileNameError");
                }
                return null;
            }
        });

        // validate the file size variable is valid
        validators.addRule(new InputValidator.ComponentValidationRule(setFileSizeToCheckBox) {
            @Override
            public String getValidationError() {

                if(setFileSizeToCheckBox.isSelected() && !saveFileSizeContextVariable.isEntryValid()) {
                    return getResourceString("saveFileSizeContextVariableNameError");
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
        populateCommandComboBox();
        commandVariableNameTargetVariablePanel.setEnabled(isCommandTypeFromVariable());
        CommandKnob.CommandType selectedCommandType = getSelectedCommandType();

        manageHostKeyButton.setEnabled(validateServerSHostCheckBox.isSelected());
        contentTypeComboBox.setEnabled(CommandKnob.CommandType.GET.equals(selectedCommandType) ||  isCommandTypeFromVariable());
        messageSource.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) ||  isCommandTypeFromVariable());
        messageTarget.setEnabled( CommandKnob.CommandType.GET.equals(selectedCommandType) || isCommandTypeFromVariable() || (SFTPRadioButton.isSelected() && (CommandKnob.CommandType.LIST.equals(selectedCommandType) || CommandKnob.CommandType.STAT.equals(selectedCommandType))) );
        messageTargetVariablePanel.setEnabled(
                messageTarget.isEnabled() &&
                messageTarget.getSelectedItem()!=null &&
                ((MessageTargetable)messageTarget.getSelectedItem()).getTarget()== TargetMessageType.OTHER );
        if(SFTPRadioButton.isSelected()){
            preserveFileMetadataCheckBox.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());
            fileOffsetTextField.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || CommandKnob.CommandType.GET.equals(selectedCommandType) || isCommandTypeFromVariable());
            fileLengthTextField.setEnabled(CommandKnob.CommandType.GET.equals(selectedCommandType) || isCommandTypeFromVariable());
            newFileNameTextField.setEnabled(CommandKnob.CommandType.MOVE.equals(selectedCommandType) || isCommandTypeFromVariable());
            failIfFileExistsCheckBox.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());
        } else {
            preserveFileMetadataCheckBox.setEnabled(false);
            fileOffsetTextField.setEnabled(false);
            fileLengthTextField.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());
            newFileNameTextField.setEnabled(false);
            failIfFileExistsCheckBox.setEnabled(false);
        }

        //Advanced Tab
        setFileSizeToCheckBox.setEnabled(CommandKnob.CommandType.GET.equals(selectedCommandType) || (SFTPRadioButton.isSelected() && CommandKnob.CommandType.STAT.equals(selectedCommandType)) || isCommandTypeFromVariable());
        saveFileSizeContextVariable.setEnabled(setFileSizeToCheckBox.isEnabled() && setFileSizeToCheckBox.isSelected());
        responseLimitPanel.setEnabled(CommandKnob.CommandType.GET.equals(selectedCommandType) || isCommandTypeFromVariable());
        wssIgnoreButton.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());
        wssCleanupButton.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());
        wssRemoveButton.setEnabled(CommandKnob.CommandType.PUT.equals(selectedCommandType) || isCommandTypeFromVariable());

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

    private void populateCommandComboBox() {
        //need to temporarily remove the action listener while updating the command combo box
        commandTypeComboBox.removeActionListener(enableDisableListener);
        int selectedIndex = commandTypeComboBox.getSelectedIndex();
        commandTypeComboBox.removeAllItems();
        commandTypeComboBox.addItem(getResourceString("commandTypeFromVariable"));
        commandTypeComboBox.addItem(getResourceString("commandTypePut"));
        commandTypeComboBox.addItem(getResourceString("commandTypeGet"));
        if(SFTPRadioButton.isSelected()){
            commandTypeComboBox.addItem(getResourceString("commandTypeList"));
            commandTypeComboBox.addItem(getResourceString("commandTypeStat"));
            commandTypeComboBox.addItem(getResourceString("commandTypeDelete"));
            commandTypeComboBox.addItem(getResourceString("commandTypeMove"));
            commandTypeComboBox.addItem(getResourceString("commandTypeMKDIR"));
            commandTypeComboBox.addItem(getResourceString("commandTypeRMDIR"));
        }

        if(selectedIndex < commandTypeComboBox.getItemCount()){
            commandTypeComboBox.setSelectedIndex(selectedIndex);
        } else {
            //Select Put by default
            commandTypeComboBox.setSelectedIndex(1);
        }
        commandTypeComboBox.addActionListener(enableDisableListener);
    }

    /*
     * Populate fields in SSH Route dialog.
     */
    @Override
    public void setData(SshRouteAssertion assertion) {
        preserveFileMetadataCheckBox.setSelected(assertion.isPreserveFileMetadata());
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
        saveFileSizeContextVariable.setAssertion(assertion, getPreviousAssertion());
        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);

        SCPRadioButton.setSelected(assertion.isScpProtocol());
        if(assertion.isRetrieveCommandTypeFromVariable()){
            commandTypeComboBox.setSelectedIndex(0);
        } else {
            switch (assertion.getCommandType()) {
                case PUT: commandTypeComboBox.setSelectedIndex(1); break;
                case GET: commandTypeComboBox.setSelectedIndex(2); break;
                case LIST: commandTypeComboBox.setSelectedIndex(3); break;
                case STAT: commandTypeComboBox.setSelectedIndex(4); break;
                case DELETE: commandTypeComboBox.setSelectedIndex(5); break;
                case MOVE: commandTypeComboBox.setSelectedIndex(6); break;
                case MKDIR: commandTypeComboBox.setSelectedIndex(7); break;
                case RMDIR: commandTypeComboBox.setSelectedIndex(8); break;
            }
        }

        commandVariableNameTargetVariablePanel.setVariable(assertion.getCommandTypeVariableName());
        failIfFileExistsCheckBox.setSelected(assertion.isFailIfFileExists());
        fileOffsetTextField.setText(assertion.getFileOffset());
        fileLengthTextField.setText(assertion.getFileLength());
        newFileNameTextField.setText(assertion.getNewFileName());

        setFileSizeToCheckBox.setSelected(assertion.isSetFileSizeToContextVariable());
        saveFileSizeContextVariable.setVariable(assertion.getSaveFileSizeContextVariable());

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

        connectTimeoutTextField.setText(Integer.toString(assertion.getConnectTimeout()));
        readTimeoutTextField.setText(Integer.toString(assertion.getReadTimeout()));

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
        if(assertion.getPasswordGoid() != null) {
            passwordField.setSelectedSecurePassword(assertion.getPasswordGoid());
        }

        if(assertion.isUsePrivateKey() && assertion.getPrivateKeyGoid() != null) {
            privateKeyRadioButton.setSelected(true);
            privateKeyField.setSelectedSecurePassword(assertion.getPrivateKeyGoid());
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
        assertion.setPreserveFileMetadata(SFTPRadioButton.isSelected() && preserveFileMetadataCheckBox.isSelected());
        assertion.setHost(hostField.getText().trim());
        assertion.setPort(portNumberTextField.getText().trim());
        assertion.setDirectory(directoryTextField.getText());
        assertion.setFileName(fileNameTextField.getText());
        if (connectTimeoutTextField.getText().trim().isEmpty()) {
            connectTimeoutTextField.setText(Integer.toString(SshRouteAssertion.DEFAULT_CONNECT_TIMEOUT));
        }
        assertion.setConnectTimeout(Integer.parseInt(connectTimeoutTextField.getText()));

        assertion.setScpProtocol(SCPRadioButton.isSelected());
        if(!isCommandTypeFromVariable()){
            assertion.setCommandType(getSelectedCommandType());
        } else {
            assertion.setCommandType(SshRouteAssertion.DEFAULT_COMMAND_TYPE);
        }
        assertion.setRetrieveCommandTypeFromVariable(isCommandTypeFromVariable());

        assertion.setCommandTypeVariableName(commandVariableNameTargetVariablePanel.getVariable());
        assertion.setFailIfFileExists(failIfFileExistsCheckBox.isSelected());
        assertion.setFileOffset(fileOffsetTextField.getText());
        assertion.setFileLength(fileLengthTextField.getText());
        assertion.setNewFileName(newFileNameTextField.getText());

        assertion.setSetFileSizeToContextVariable(setFileSizeToCheckBox.isSelected());
        assertion.setSaveFileSizeContextVariable(saveFileSizeContextVariable.getVariable());

        if (readTimeoutTextField.getText().trim().isEmpty()) {
            readTimeoutTextField.setText(Integer.toString(SshRouteAssertion.DEFAULT_READ_TIMEOUT));
        }
        assertion.setReadTimeout(Integer.parseInt(readTimeoutTextField.getText()));

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
        if ( CommandKnob.CommandType.GET.equals(assertion.getCommandType()) ||
                CommandKnob.CommandType.LIST.equals(assertion.getCommandType()) ||
                CommandKnob.CommandType.STAT.equals(assertion.getCommandType()) ) {
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
                assertion.setPasswordGoid(passwordField.getSelectedSecurePassword().getGoid());
                assertion.setPrivateKeyGoid(null);
            } else if (privateKeyRadioButton.isSelected()) {
                assertion.setUsePrivateKey(true);
                assertion.setPrivateKeyGoid(privateKeyField.getSelectedSecurePassword().getGoid());
                assertion.setPasswordGoid(null);
            }
        }

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        return assertion;
    }
    
    /**
     * @return Return true if the port number is between 1 and 65535 or references a context variable.
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

    private CommandKnob.CommandType getSelectedCommandType(){
        switch (commandTypeComboBox.getSelectedIndex()){
            case -1: return SshRouteAssertion.DEFAULT_COMMAND_TYPE;
            case 0: return null;
            case 1: return CommandKnob.CommandType.PUT;
            case 2: return CommandKnob.CommandType.GET;
            case 3: return CommandKnob.CommandType.LIST;
            case 4: return CommandKnob.CommandType.STAT;
            case 5: return CommandKnob.CommandType.DELETE;
            case 6: return CommandKnob.CommandType.MOVE;
            case 7: return CommandKnob.CommandType.MKDIR;
            case 8: return CommandKnob.CommandType.RMDIR;
            default: throw new IllegalStateException("This should never occur!!!");
        }
    }

    private boolean isCommandTypeFromVariable() {
        return commandTypeComboBox.getSelectedIndex()==0;
    }
}
