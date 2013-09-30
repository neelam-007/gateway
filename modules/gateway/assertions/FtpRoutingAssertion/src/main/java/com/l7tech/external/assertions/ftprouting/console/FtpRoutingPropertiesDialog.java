/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.gateway.common.transport.ftp.FtpTestException;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.BetterComboBox;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Dialog for editing the FtpRoutingAssertion.
 *
 * @since SecureSpan 4.0
 * @author rmak
 * @author jwilliams
 */
public class FtpRoutingPropertiesDialog extends AssertionPropertiesOkCancelSupport<FtpRoutingAssertion> {

    private static final ResourceBundle resources = ResourceBundle.getBundle(FtpRoutingPropertiesDialog.class.getName());

    private static final char SINGLE_SPACE_SEPARATOR = ' ';

    private static final String COMPONENT_LABEL_SUFFIX = ":";
    private static final int OPTION_COMPONENT_NULL_SELECTION_INDEX = -1;

    private static final String PROTOCOL_COMBO_ITEM_FTP_UNSECURED_LABEL = getResourceString("ftpUnsecuredDescription");
    private static final String PROTOCOL_COMBO_ITEM_FTPS_EXPLICIT_LABEL = getResourceString("explicitSslDescription");
    private static final String PROTOCOL_COMBO_ITEM_FTPS_IMPLICIT_LABEL = getResourceString("implicitSslDescription");

    private static final int COMMAND_COMBO_ITEM_FROM_VARIABLE_INDEX = 0;
    private static final String COMMAND_COMBO_ITEM_FROM_VARIABLE_LABEL = getResourceString("commandTypeFromVariable");

    private static final String ENABLED_PROPERTY_NAME = "enabled";

    private static final ContentTypeHeader[] OFFERED_CONTENT_TYPE_HEADERS = new ContentTypeHeader[] {
            ContentTypeHeader.XML_DEFAULT,
            ContentTypeHeader.TEXT_DEFAULT,
            ContentTypeHeader.SOAP_1_2_DEFAULT,
            ContentTypeHeader.APPLICATION_JSON,
            ContentTypeHeader.OCTET_STREAM_DEFAULT,
    };

    private static final int PORT_RANGE_START = 1;
    private static final int PORT_RANGE_END = 65535;

    private JPanel _mainPanel;
    private JCheckBox verifyServerCertCheckBox;
    private JTextField hostNameTextField;
    private JTextField portNumberTextField;
    private JTextField directoryTextField;
    private JRadioButton passThroughCredsRadioButton;
    private JRadioButton specifyUserCredsRadioButton;
    private JTextField userNameTextField;
    private SecurePasswordComboBox storedPasswordComboBox;
    private JCheckBox supplyClientCertCheckBox;
    private JTextField timeoutTextField;
    private JButton testConnectionButton;
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JComboBox<MessageTargetableSupport> messageSourceComboBox;
    private JCheckBox contextVariableInPasswordCheckBox;
    private JComboBox<MessageTargetableSupport> messageTargetComboBox;
    private JPanel targetVariablePanelHolder;
    private JPanel responseLimitHolderPanel;
    private JComboBox<String> contentTypeComboBox;
    private BetterComboBox<String> commandComboBox;
    private JTextField argumentsTextField;
    private JButton manageStoredPasswordsButton;
    private JPanel commandVariablePanelHolder;
    private JComboBox<String> protocolComboBox;
    private PrivateKeysComboBox clientCertComboBox;
    private JPasswordField plaintextPasswordField;
    private JRadioButton storedPasswordRadioButton;
    private JRadioButton plaintextPasswordRadioButton;
    private JLabel plainTextPasswordWarningLabel;
    private JCheckBox autoFilenameCheckBox;
    private JLabel userNameLabel;
    private AbstractButton[] secHdrButtons = { wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };
    private ByteLimitPanel responseLimitPanel;
    private TargetVariablePanel commandVariablePanel;
    private TargetVariablePanel targetVariablePanel;

    private char echoChar;
    private InputValidator inputValidator, connectionTestValidator;

    /**
     * Creates new form ServicePanel
     * @param owner  parent for dialog
     * @param a      assertion to edit
     */
    public FtpRoutingPropertiesDialog(Window owner, FtpRoutingAssertion a) {
        super(FtpRoutingAssertion.class,  owner, a, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return _mainPanel;
    }

    /**
     * This method is called from within the static factory to
     * initialize the form.
     */
    @Override
    protected void initComponents() {
        super.initComponents();

        // --- Connection tab ---

        // Protocol
        protocolComboBox.addItem(PROTOCOL_COMBO_ITEM_FTP_UNSECURED_LABEL);
        protocolComboBox.addItem(PROTOCOL_COMBO_ITEM_FTPS_EXPLICIT_LABEL);
        protocolComboBox.addItem(PROTOCOL_COMBO_ITEM_FTPS_IMPLICIT_LABEL);

        protocolComboBox.setSelectedIndex(OPTION_COMPONENT_NULL_SELECTION_INDEX);

        protocolComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final boolean isFtps = (!PROTOCOL_COMBO_ITEM_FTP_UNSECURED_LABEL.equals(protocolComboBox.getSelectedItem()));
                portNumberTextField.setText(Integer.toString(getDefaultPortNumber()));
                supplyClientCertCheckBox.setEnabled(isFtps);
                verifyServerCertCheckBox.setEnabled(isFtps);
            }
        });

        // Connect timeout
        timeoutTextField.setDocument(new NumberField(6));

        // Verify server certificate check box
        verifyServerCertCheckBox.addPropertyChangeListener(ENABLED_PROPERTY_NAME, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if(!verifyServerCertCheckBox.isEnabled()) {
                    verifyServerCertCheckBox.setSelected(false);
                }
            }
        });

        // Test Connection button
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });

        // Command variable
        commandVariablePanel = new TargetVariablePanel();
        commandVariablePanelHolder.setLayout(new BorderLayout());
        commandVariablePanelHolder.add(commandVariablePanel, BorderLayout.CENTER);

        // Command combo box
        DefaultComboBoxModel<String> commandComboBoxModel = new DefaultComboBoxModel<>();

        commandComboBoxModel.insertElementAt(COMMAND_COMBO_ITEM_FROM_VARIABLE_LABEL, COMMAND_COMBO_ITEM_FROM_VARIABLE_INDEX);

        for (FtpMethod ftpMethod : getFtpMethods()) {
            commandComboBoxModel.addElement(ftpMethod.getWspName());
        }

        commandComboBox.setModel(commandComboBoxModel);

        commandComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableCommandSettingsComponents();
            }
        });

        // Message source combo box
        messageSourceComboBox.setRenderer(
                new TextListCellRenderer<>(getMessageNameFunction("Default", null), null, false));

        // Message target variable
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanelHolder.setLayout(new BorderLayout());
        targetVariablePanelHolder.add(targetVariablePanel, BorderLayout.CENTER);

        // Message target combo box
        messageTargetComboBox.setRenderer(
                new TextListCellRenderer<>(getMessageNameFunction("Default", "Message Variable"), null, true));

        messageTargetComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableTargetVariablePanel();
            }
        });

        // --- Authentication tab ---

        // Pass through credentials option
        passThroughCredsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableCredentialsPanelComponents();
            }
        });

        // Specify user credentials option
        specifyUserCredsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableCredentialsPanelComponents();
            }
        });

        // User name
        Utilities.enableGrayOnDisabled(userNameLabel);
        Utilities.enableGrayOnDisabled(userNameTextField);

        // Stored password option
        storedPasswordRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableCredentialsPanelComponents();
            }
        });

        // Plaintext password option
        plaintextPasswordRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableCredentialsPanelComponents();
            }
        });

        // Stored password combo box
        storedPasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        // Manage stored passwords button
        manageStoredPasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                doManagePasswords();
            }
        });

        // Plaintext password
        echoChar = plaintextPasswordField.getEchoChar();
        Utilities.enableGrayOnDisabled(plaintextPasswordField);

        // Context variable in password check box
        Utilities.enableGrayOnDisabled(contextVariableInPasswordCheckBox);

        contextVariableInPasswordCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plaintextPasswordField.enableInputMethods(contextVariableInPasswordCheckBox.isSelected());
                plaintextPasswordField.setEchoChar(contextVariableInPasswordCheckBox.isSelected() ? (char) 0 : echoChar);
            }
        });

        // Plaintext warning label
        plainTextPasswordWarningLabel.setVisible(false);

        // Supply client certificate check box
        CheckAndComboPairListener supplyClientCertCheckBoxListener =
                new CheckAndComboPairListener(supplyClientCertCheckBox, clientCertComboBox);

        supplyClientCertCheckBox.addItemListener(supplyClientCertCheckBoxListener);
        supplyClientCertCheckBox.addPropertyChangeListener(ENABLED_PROPERTY_NAME, supplyClientCertCheckBoxListener);

        // --- Advanced tab ---

        // Content type combo box
        DefaultComboBoxModel<String> contentTypeComboBoxModel = new DefaultComboBoxModel<>();

        for (ContentTypeHeader offeredType : OFFERED_CONTENT_TYPE_HEADERS) {
            contentTypeComboBoxModel.addElement(offeredType.getFullValue());
        }

        contentTypeComboBox.setModel(contentTypeComboBoxModel);

        // Response limit settings
        responseLimitPanel = new ByteLimitPanel();
        responseLimitPanel.setAllowContextVars(true);
        responseLimitHolderPanel.setLayout(new BorderLayout());
        responseLimitHolderPanel.add(responseLimitPanel, BorderLayout.CENTER);

        // WSS security settings
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        // TODO: add more validators for various ftp commands e.g. require arguments for MKD or LIST?
        // --- VALIDATION ---
        inputValidator = new InputValidator(this, getResourceString("errorTitle"));
        connectionTestValidator = new InputValidator(this, getResourceString("errorTitle"));

        // host must be set
        inputValidator.addRule(
                inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"),
                hostNameTextField, null));
        connectionTestValidator.addRule(
                connectionTestValidator.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"),
                hostNameTextField, null));

        // port number must be within allowable range or reference a valid context variable
        InputValidator.ValidationRule portNumberRule = new InputValidator.ComponentValidationRule(portNumberTextField) {
            @Override
            public String getValidationError() {
                if (!isPortValid()) {
                    return MessageFormat.format(getResourceString("portError"), getDefaultPortNumber());
                }

                return null;
            }
        };

        // connection timeout must be set
        inputValidator.addRule(
                inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("timeoutLabel"),
                        timeoutTextField, null));
        connectionTestValidator.addRule(
                connectionTestValidator.constrainTextFieldToBeNonEmpty(getResourceString("timeoutLabel"),
                        timeoutTextField, null));

        inputValidator.addRule(portNumberRule);
        connectionTestValidator.addRule(portNumberRule);

        // the ftp command combo box must have a selection
        inputValidator.addRule(new InputValidator.ComponentValidationRule(commandComboBox) {
            @Override
            public String getValidationError() {
                if (commandComboBox.getSelectedItem() == null
                        || commandComboBox.getSelectedIndex() == OPTION_COMPONENT_NULL_SELECTION_INDEX) {
                    return getResourceString("commandNullError");
                }

                return null;
            }
        });

        // validate the command variable
        inputValidator.addRule(new InputValidator.ComponentValidationRule(commandComboBox) {
            @Override
            public String getValidationError() {
                if (isCommandTypeFromVariable() && (commandVariablePanel.getVariable().trim().isEmpty())) {
                    return getResourceString("commandVariableNullError");
                }

                return null;
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return commandVariablePanel.getErrorMessage();
            }
        });

        /**
         * - either arguments or auto-generated file name must be specified, depending on ftp command
         * - auto-filename checkbox invalid for non-upload commands
         */
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (isCommandUploadType((String) commandComboBox.getSelectedItem())) {
                    if (!autoFilenameCheckBox.isSelected() && argumentsTextField.getText().trim().isEmpty()) {
                        return getResourceString("filenameNullForUploadCommandError");
                    }
                } else if (autoFilenameCheckBox.isSelected()) {
                    return getResourceString("nonUploadAutoFilenameError");
                }

                if (isCommandRetrieve((String) commandComboBox.getSelectedItem())) {
                    if(argumentsTextField.getText().trim().isEmpty()) {
                        return getResourceString("retrNoArgumentError");
                    }
                }

                return null;
            }
        });

        // validate target message variable
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return targetVariablePanel.getErrorMessage();
            }
        });

        // specify user credentials must be selected for connection tests
        connectionTestValidator.addRule(new InputValidator.ComponentValidationRule(storedPasswordComboBox) {
            @Override
            public String getValidationError() {
                if(!specifyUserCredsRadioButton.isSelected()) {
                    return getResourceString("connectionTestFailureCredentialsFromRequestMessage");
                }

                return null;
            }
        });

        // username must be set if the specify credentials option has been selected
        inputValidator.addRule(
                inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"),
                        userNameTextField, null));
        connectionTestValidator.addRule(
                connectionTestValidator.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"),
                        userNameTextField, null));

        // a password option must be selected if specify credentials selected
        InputValidator.ComponentValidationRule passwordSourceRule =
                new InputValidator.ComponentValidationRule(specifyUserCredsRadioButton) {
            @Override
            public String getValidationError() {
                if(specifyUserCredsRadioButton.isSelected()
                        && !storedPasswordRadioButton.isSelected() && !plaintextPasswordRadioButton.isSelected()) {
                    return getResourceString("passwordSourceUnspecifiedError");
                }

                return null;
            }
        };

        inputValidator.addRule(passwordSourceRule);
        connectionTestValidator.addRule(passwordSourceRule);

        // a stored password must be specified if the field is enabled
        inputValidator.addRule(new InputValidator.ComponentValidationRule(storedPasswordComboBox) {
            @Override
            public String getValidationError() {
                if(specifyUserCredsRadioButton.isSelected() && (storedPasswordComboBox == null
                        || storedPasswordComboBox.getItemCount() == 0)) {
                    return getResourceString("passwordEmptyError");
                }

                return null;
            }
        });

        // only plaintext passwords can be used in the connection test
        connectionTestValidator.addRule(new InputValidator.ComponentValidationRule(plaintextPasswordRadioButton) {
            @Override
            public String getValidationError() {
                if(!plaintextPasswordRadioButton.isSelected()) {
                    return getResourceString("connectionTestFailurePasswordNotPlaintextMessage");
                }

                return null;
            }
        });

        // if plaintext password is selected, the password must be specified
        inputValidator.addRule(
                inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("plaintextPasswordRadioButton"),
                        plaintextPasswordField, null));
        connectionTestValidator.addRule(
                connectionTestValidator.constrainTextFieldToBeNonEmpty(getResourceString("plaintextPasswordRadioButton"),
                        plaintextPasswordField, null));

        // client certificate must be specified if the field is enabled
        InputValidator.ValidationRule clientCertRule = new InputValidator.ComponentValidationRule(clientCertComboBox) {
            @Override
            public String getValidationError() {
                if (supplyClientCertCheckBox.isSelected()
                        && clientCertComboBox.getSelectedIndex() == OPTION_COMPONENT_NULL_SELECTION_INDEX) {
                    return getResourceString("clientCertError");
                }

                return null;
            }
        };

        inputValidator.addRule(clientCertRule);
        connectionTestValidator.addRule(clientCertRule);

        // STOU command cannot use auto-generated file name
        inputValidator.addRule(new InputValidator.ComponentValidationRule(autoFilenameCheckBox) {
            @Override
            public String getValidationError() {
                if (autoFilenameCheckBox.isSelected()
                        && (commandComboBox.getSelectedItem()).equals(FtpMethod.FTP_STOU.getWspName())) {
                    return MessageFormat.format(getResourceString("stouAutoFilenameError"), getDefaultPortNumber());
                }

                return null;
            }
        });

        // content type must be specified if the combo box is enabled
        inputValidator.addRule(new InputValidator.ComponentValidationRule(contentTypeComboBox) {
            @Override
            public String getValidationError() {
                if(contentTypeComboBox.getSelectedIndex() == OPTION_COMPONENT_NULL_SELECTION_INDEX) {
                    return getResourceString("contentTypeNullError");
                }

                return null;
            }
        });

        // validate response limit panel settings
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return responseLimitPanel.validateFields();
            }
        });
    }

    private void doManagePasswords() {
        SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(SwingUtilities.getWindowAncestor(this));
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);

        // maintain selection
        final SecurePassword password = storedPasswordComboBox.getSelectedSecurePassword();

        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                storedPasswordComboBox.reloadPasswordList();

                // load selection
                if(password != null) {
                    storedPasswordComboBox.setSelectedSecurePassword(password.getGoid());
                }
            }
        });
    }

    private void enableOrDisableCommandSettingsComponents() {
        commandVariablePanel.setEnabled(isCommandTypeFromVariable());

        if (commandComboBox.getSelectedIndex() == OPTION_COMPONENT_NULL_SELECTION_INDEX) {
            messageSourceComboBox.setEnabled(false);
            messageTargetComboBox.setEnabled(false);
            contentTypeComboBox.setEnabled(false);
            responseLimitPanel.setEnabled(false);
            autoFilenameCheckBox.setEnabled(false);
        } else {
            if (commandComboBox.getSelectedIndex() == COMMAND_COMBO_ITEM_FROM_VARIABLE_INDEX) {
                messageSourceComboBox.setEnabled(true);
                contentTypeComboBox.setEnabled(true);
                responseLimitPanel.setEnabled(true);
                autoFilenameCheckBox.setEnabled(true);
            } else {
                boolean isUploadCommand = isCommandUploadType((String) commandComboBox.getSelectedItem());

                messageSourceComboBox.setEnabled(isUploadCommand);
                autoFilenameCheckBox.setEnabled(isUploadCommand);
                contentTypeComboBox.setEnabled(!isUploadCommand);
                responseLimitPanel.setEnabled(!isUploadCommand);
            }

            messageTargetComboBox.setEnabled(true);
        }

        if (!autoFilenameCheckBox.isEnabled()) {
            autoFilenameCheckBox.setSelected(false);
        }

        enableOrDisableTargetVariablePanel();
    }

    private void enableOrDisableTargetVariablePanel() {
        Object selectedTarget = messageTargetComboBox.getSelectedItem();

        targetVariablePanel.setEnabled(messageTargetComboBox.isEnabled() && selectedTarget != null
                && ((MessageTargetable) selectedTarget).getTarget() == TargetMessageType.OTHER);
    }

    private void enableOrDisableCredentialsPanelComponents() {
        boolean isSpecifyUserCreds = specifyUserCredsRadioButton.isSelected();

        userNameLabel.setEnabled(isSpecifyUserCreds);
        userNameTextField.setEnabled(isSpecifyUserCreds);

        // enable/disable credentials type options
        storedPasswordRadioButton.setEnabled(isSpecifyUserCreds);
        plaintextPasswordRadioButton.setEnabled(isSpecifyUserCreds);

        // enable/disable stored password control
        storedPasswordComboBox.setEnabled(isSpecifyUserCreds && storedPasswordRadioButton.isSelected());
        manageStoredPasswordsButton.setEnabled(isSpecifyUserCreds && storedPasswordRadioButton.isSelected());

        // enable/disable password field and context variable check box
        plaintextPasswordField.setEnabled(isSpecifyUserCreds && plaintextPasswordRadioButton.isSelected());
        contextVariableInPasswordCheckBox.setEnabled(isSpecifyUserCreds && plaintextPasswordRadioButton.isSelected());

        // show/hide plaintext warning label
        plainTextPasswordWarningLabel.setVisible(isSpecifyUserCreds && plaintextPasswordRadioButton.isSelected());
    }

    private void populateRequestSourceComboBox(FtpRoutingAssertion assertion) {
        MessageTargetableSupport msgSource = assertion.get_requestTarget();
        TargetMessageType msgSourceType = msgSource != null ? msgSource.getTarget() : null;

        if (msgSourceType == TargetMessageType.REQUEST)
            messageSourceComboBox.setSelectedIndex(0);
        else if (msgSourceType == TargetMessageType.RESPONSE)
            messageSourceComboBox.setSelectedIndex(1);
        else {
            String msgSourceVariable = msgSourceType == TargetMessageType.OTHER ? msgSource.getOtherTargetMessageVariable() : null;
            if (msgSourceVariable != null) {
                boolean msgSourceFound = false;
                for (int i = 2; i < messageSourceComboBox.getItemCount(); i++) {
                    MessageTargetableSupport messageSourceItem = messageSourceComboBox.getItemAt(i);
                    if (msgSourceVariable.equals(messageSourceItem.getOtherTargetMessageVariable())) {
                        msgSourceFound = true;
                        messageSourceComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                if (!msgSourceFound) {
                    MessageTargetableSupport notFoundMsgSource = new MessageTargetableSupport(msgSourceVariable);
                    messageSourceComboBox.addItem(notFoundMsgSource);
                    messageSourceComboBox.setSelectedItem(notFoundMsgSource);
                }
            }
        }
    }

    @Override
    public void setData(FtpRoutingAssertion assertion) {
        // protocol
        final FtpSecurity security = assertion.getSecurity();

        if (security == null || security == FtpSecurity.FTP_UNSECURED) {
            protocolComboBox.setSelectedItem(PROTOCOL_COMBO_ITEM_FTP_UNSECURED_LABEL);
        } else if (security == FtpSecurity.FTPS_EXPLICIT) {
            protocolComboBox.setSelectedItem(PROTOCOL_COMBO_ITEM_FTPS_EXPLICIT_LABEL);
        } else if (security == FtpSecurity.FTPS_IMPLICIT) {
            protocolComboBox.setSelectedItem(PROTOCOL_COMBO_ITEM_FTPS_IMPLICIT_LABEL);
        }

        // server settings
        verifyServerCertCheckBox.setSelected(assertion.isVerifyServerCert());
        hostNameTextField.setText(assertion.getHostName());
        portNumberTextField.setText(assertion.getPort());
        timeoutTextField.setText(Integer.toString(assertion.getTimeout() / 1000));

        // ftp command
        if (assertion.getFtpMethod() == null) {
            if (assertion.getOtherCommand()) {
                commandComboBox.setSelectedIndex(COMMAND_COMBO_ITEM_FROM_VARIABLE_INDEX);
                commandVariablePanel.setVariable(assertion.getFtpMethodOtherCommand());
            } else {
                commandComboBox.setSelectedIndex(OPTION_COMPONENT_NULL_SELECTION_INDEX);
                enableOrDisableCommandSettingsComponents();
            }
        } else {
            final String ftpMethod = assertion.getFtpMethod().getWspName();
            commandComboBox.setSelectedItem(ftpMethod);
        }

        // message source
        messageSourceComboBox.setModel(buildMessageSourceComboBoxModel(assertion));

        populateRequestSourceComboBox(assertion);

        /*
        messageTargetComboBox.setModel(buildMessageTargetComboBoxModel(false));
        final MessageTargetableSupport responseTarget = new MessageTargetableSupport(assertion.getResponseTarget());
        messageTargetComboBox.setSelectedItem(new MessageTargetableSupport(responseTarget.getTarget()));
         */

        // message target
        messageTargetComboBox.setModel(buildMessageTargetComboBoxModel(false));
        final MessageTargetableSupport responseTarget = new MessageTargetableSupport(assertion.getResponseTarget());
        messageTargetComboBox.setSelectedItem(responseTarget);

        // message target variable
        if (responseTarget.getTarget() == TargetMessageType.OTHER) {
            targetVariablePanel.setVariable(responseTarget.getOtherTargetMessageVariable());
        } else {
            targetVariablePanel.setVariable(StringUtils.EMPTY);
        }

        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());

        // directory
        directoryTextField.setText(assertion.getDirectory());

        // auto file name
        autoFilenameCheckBox.setSelected(assertion.getFileNameSource() == FtpFileNameSource.AUTO);

        // arguments
        argumentsTextField.setText(assertion.getArguments());

        // credentials
        if (assertion.getCredentialsSource() == null || assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            passThroughCredsRadioButton.doClick(0);
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            specifyUserCredsRadioButton.doClick(0);
            userNameTextField.setText(assertion.getUserName());

            if(null != assertion.getPasswordGoid()) {
                storedPasswordRadioButton.setSelected(true);
                storedPasswordComboBox.setSelectedSecurePassword(assertion.getPasswordGoid());
            } else {
                plaintextPasswordRadioButton.setSelected(true);
                plaintextPasswordField.setText(assertion.getPassword());
                plaintextPasswordField.enableInputMethods(assertion.isPasswordUsesContextVariables());
                contextVariableInPasswordCheckBox.setSelected(assertion.isPasswordUsesContextVariables());
            }
        }

        if(assertion.getPasswordGoid() != null) {
            storedPasswordComboBox.setSelectedSecurePassword(assertion.getPasswordGoid());
        }

        supplyClientCertCheckBox.setSelected(assertion.isUseClientCert());
        clientCertComboBox.select(assertion.getClientCertKeystoreId(), assertion.getClientCertKeyAlias());

        // content type settings
        String contentType = assertion.getDownloadedContentType();

        if (contentType == null) {
            contentTypeComboBox.setSelectedIndex(0);
        } else {
            contentTypeComboBox.setSelectedItem(contentType);
            if (!contentType.equalsIgnoreCase((String) contentTypeComboBox.getSelectedItem())) {
                ((DefaultComboBoxModel<String>) contentTypeComboBox.getModel()).addElement(contentType);
                contentTypeComboBox.setSelectedItem(contentType);
            }
        }

        // response limit settings
        responseLimitPanel.setValue(assertion.getResponseByteLimit(), Registry.getDefault().getPolicyAdmin().getXmlMaxBytes());

        // WSS settings
        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, OPTION_COMPONENT_NULL_SELECTION_INDEX, null, secHdrButtons);

        // enable/disable components
        enableOrDisableCredentialsPanelComponents();
    }

    /** Copies view into model. */
    @Override
    public FtpRoutingAssertion getData(FtpRoutingAssertion assertion) {
        // perform validations
        final String error = inputValidator.validate();

        if(error != null) {
            throw new ValidationException(error);
        }

        fillAssertionConnectionData(assertion);
        fillAssertionCommandData(assertion);
        fillAssertionCredentialsData(assertion);
        fillAssertionAdvancedData(assertion);

        return assertion;
    }

    private void fillAssertionConnectionData(final FtpRoutingAssertion assertion) {
        // security
        if (PROTOCOL_COMBO_ITEM_FTP_UNSECURED_LABEL.equals(protocolComboBox.getSelectedItem())) {
            assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
            verifyServerCertCheckBox.setSelected(false);
        } else if (PROTOCOL_COMBO_ITEM_FTPS_EXPLICIT_LABEL.equals(protocolComboBox.getSelectedItem())) {
            assertion.setSecurity(FtpSecurity.FTPS_EXPLICIT);
        } else if (PROTOCOL_COMBO_ITEM_FTPS_IMPLICIT_LABEL.equals(protocolComboBox.getSelectedItem())) {
            assertion.setSecurity(FtpSecurity.FTPS_IMPLICIT);
        }

        // server settings
        assertion.setVerifyServerCert(verifyServerCertCheckBox.isSelected());
        assertion.setHostName(hostNameTextField.getText());
        assertion.setPort(portNumberTextField.getText());

        // timeout
        if (timeoutTextField.getText().trim().isEmpty()) {
            timeoutTextField.setText(Integer.toString(FtpRoutingAssertion.DEFAULT_TIMEOUT / 1000));
        } else {
            assertion.setTimeout(Integer.parseInt(timeoutTextField.getText()) * 1000);
        }
    }

    private void fillAssertionCommandData(final FtpRoutingAssertion assertion) {
        // ftp command
        if (commandVariablePanel.isEnabled()) {
            assertion.setFtpMethod(null);
            assertion.setOtherCommand(true);
            assertion.setFtpMethodOtherCommand(commandVariablePanel.getVariable());
        } else {
            assertion.setFtpMethod((FtpMethod) FtpMethod.getEnumTranslator().stringToObject(commandComboBox.getSelectedItem().toString()));
        }

        // message source
        assertion.set_requestTarget((MessageTargetableSupport) messageSourceComboBox.getSelectedItem());

        // message target
        final MessageTargetableSupport responseTarget =
                new MessageTargetableSupport((MessageTargetable) messageTargetComboBox.getSelectedItem());

        if (responseTarget.getTarget() == TargetMessageType.OTHER) {
            responseTarget.setOtherTargetMessageVariable(targetVariablePanel.getVariable());
            responseTarget.setSourceUsedByGateway(false);
            responseTarget.setTargetModifiedByGateway(true);
        }

        assertion.setResponseTarget(responseTarget);

        // directory
        assertion.setDirectory(directoryTextField.getText());

        // auto file name
        if (autoFilenameCheckBox.isSelected()) {
            assertion.setFileNameSource(FtpFileNameSource.AUTO);
        } else {
            assertion.setFileNameSource(FtpFileNameSource.ARGUMENT);
        }

        // arguments
        assertion.setArguments(argumentsTextField.getText().trim());
    }

    private void fillAssertionAdvancedData(final FtpRoutingAssertion assertion) {
        // content type
        assertion.setDownloadedContentType(contentTypeComboBox.getSelectedItem().toString());

        // response limit settings
        assertion.setResponseByteLimit(responseLimitPanel.getValue());

        // WSS settings
        RoutingDialogUtils.configSecurityHeaderHandling(assertion,
                OPTION_COMPONENT_NULL_SELECTION_INDEX, secHdrButtons);
    }

    private void fillAssertionCredentialsData(final FtpRoutingAssertion assertion) {
        // credentials
        if (passThroughCredsRadioButton.isSelected()) {
            assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
            userNameTextField.setText(StringUtils.EMPTY);
            plaintextPasswordField.setText(StringUtils.EMPTY);
            assertion.setPasswordGoid(null);
        } else if (specifyUserCredsRadioButton.isSelected()) {
            assertion.setCredentialsSource(FtpCredentialsSource.SPECIFIED);

            if(storedPasswordRadioButton.isSelected()) {
                assertion.setPasswordGoid(storedPasswordComboBox.getSelectedSecurePassword().getGoid());
                assertion.setPassword(null);
            } else {
                assertion.setPasswordGoid(null);
                assertion.setPassword(new String(plaintextPasswordField.getPassword()));
            }
        }

        assertion.setUserName(userNameTextField.getText());
        assertion.setPasswordUsesContextVariables(contextVariableInPasswordCheckBox.isSelected());
        assertion.setUseClientCert(supplyClientCertCheckBox.isSelected());

        if (supplyClientCertCheckBox.isSelected()) {
            assertion.setClientCertKeystoreId(clientCertComboBox.getSelectedKeystoreId());
            assertion.setClientCertKeyAlias(clientCertComboBox.getSelectedKeyAlias());
        } else {
            assertion.setClientCertKeystoreId(null);
            assertion.setClientCertKeyAlias(null);
        }
    }

    /**
     * Runs connection test with cancellable progress bar. Displays result with session log if failure.
     */
    private void testConnection() {
        // validate for test
        final String error = connectionTestValidator.validate();

        if(error != null){
            JOptionPane.showMessageDialog(
                    FtpRoutingPropertiesDialog.this,
                    error,
                    getResourceString("connectionTestFailureTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            final FtpRoutingAssertion a = new FtpRoutingAssertion();

            fillAssertionConnectionData(a);
            fillAssertionCommandData(a);
            fillAssertionCredentialsData(a);

            if (ftpConfigurationUsesVariables(a)) {
                JOptionPane.showMessageDialog(
                        FtpRoutingPropertiesDialog.this,
                        getResourceString("connectionTestFailureContextVariablesUsedMessage"),
                        getResourceString("connectionTestFailureTitle"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            final JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            final CancelableOperationDialog cancelDialog =
                    new CancelableOperationDialog(null, getResourceString("connectionTestAttemptTitle"),
                            getResourceString("connectionTestAttemptMessage"), progressBar);

            cancelDialog.pack();
            cancelDialog.setModal(true);
            Utilities.centerOnScreen(cancelDialog);

            Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    final boolean isFtps = a.getSecurity() == FtpSecurity.FTPS_EXPLICIT || a.getSecurity() == FtpSecurity.FTPS_IMPLICIT;
                    final boolean isExplicit = a.getSecurity() == FtpSecurity.FTPS_EXPLICIT;

                    Registry.getDefault().getFtpManager().testConnection(
                            isFtps,
                            isExplicit,
                            a.isVerifyServerCert(),
                            a.getHostName(),
                            Integer.parseInt(a.getPort()),
                            a.getUserName(),
                            a.getPassword(),
                            a.isUseClientCert(),
                            a.getClientCertKeystoreId(),
                            a.getClientCertKeyAlias(),
                            a.getDirectory(),
                            a.getTimeout());

                    return Boolean.TRUE;
                }
            };

            final Boolean result = Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);

            if (result == Boolean.TRUE) {
                JOptionPane.showMessageDialog(
                        FtpRoutingPropertiesDialog.this,
                        getResourceString("connectionTestSuccessMessage"),
                        getResourceString("connectionTestSuccessTitle"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (InterruptedException e) {
            // Swing thread interrupted.
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();

            if (cause != null) {
                if (cause instanceof FtpTestException) {
                    final FtpTestException fte = (FtpTestException) cause;

                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.add(new JLabel(getResourceString("connectionTestFailureMessage")));
                    panel.add(new JLabel(fte.getMessage()));

                    if (fte.getSessionLog() != null && fte.getSessionLog().length() != 0) {
                        JTextArea sessionLog = new JTextArea(fte.getSessionLog());

                        sessionLog.setAlignmentX(Component.LEFT_ALIGNMENT);
                        sessionLog.setBorder(BorderFactory.createEtchedBorder());
                        sessionLog.setEditable(false);
                        sessionLog.setEnabled(true);
                        sessionLog.setFont(new Font(null, Font.PLAIN, 11));

                        panel.add(Box.createVerticalStrut(10));
                        panel.add(new JLabel(getResourceString("connectionTestFailureDetailLogMessage")));
                        panel.add(sessionLog);
                    }

                    JOptionPane.showMessageDialog (
                            FtpRoutingPropertiesDialog.this,
                            panel,
                            getResourceString("connectionTestFailureTitle"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    throw ExceptionUtils.wrap(cause);
                }
            }
        }
    }

    private boolean ftpConfigurationUsesVariables(FtpRoutingAssertion a) {
        StringBuilder tmp = new StringBuilder();

        tmp.append(a.getHostName()).append(SINGLE_SPACE_SEPARATOR)
           .append(a.getPort()).append(SINGLE_SPACE_SEPARATOR)
           .append(a.getUserName()).append(SINGLE_SPACE_SEPARATOR)
           .append(a.getDirectory()).append(SINGLE_SPACE_SEPARATOR);

        if (a.isPasswordUsesContextVariables()) {
            tmp.append(a.getPassword());
        }

        return Syntax.getReferencedNames(tmp.toString()).length > 0;
    }

    @Override
    protected ComboBoxModel<MessageTargetableSupport> buildMessageTargetComboBoxModel(final boolean includeDefault) {
        final DefaultComboBoxModel<MessageTargetableSupport> comboBoxModel = new DefaultComboBoxModel<>();
        if (includeDefault) comboBoxModel.addElement(null);
        comboBoxModel.addElement(new MessageTargetableSupport(TargetMessageType.RESPONSE));
        comboBoxModel.addElement(new MessageTargetableSupport(TargetMessageType.OTHER));
        return comboBoxModel;
    }

    private int getDefaultPortNumber() {
        int port = FtpRoutingAssertion.DEFAULT_FTP_PORT;

        if (PROTOCOL_COMBO_ITEM_FTPS_IMPLICIT_LABEL.equals(protocolComboBox.getSelectedItem())) {
            port = FtpRoutingAssertion.DEFAULT_FTPS_IMPLICIT_PORT;
        }

        return port;
    }

    /**
     * @return Return true if the port number is between 1 and 65535 or references a context variable.
     */
    private boolean isPortValid() {
        boolean isValid;
        String portStr = portNumberTextField.getText();

        try {
            int port = Integer.parseInt(portStr);
            isValid = port >= PORT_RANGE_START && port <= PORT_RANGE_END;
        } catch (NumberFormatException e) {
            // must be using context variable
            isValid = Syntax.getReferencedNames(portStr).length > 0;
        }

        return isValid;
    }

    private boolean isCommandTypeFromVariable() {
        return commandComboBox.getSelectedIndex() == COMMAND_COMBO_ITEM_FROM_VARIABLE_INDEX;
    }

    private boolean isCommandUploadType(String command) {
        FtpMethod method = (FtpMethod) FtpMethod.getEnumTranslator().stringToObject(command);

        return method != null &&
                (method.equals(FtpMethod.FTP_PUT)
                        || method.equals(FtpMethod.FTP_APPE)
                        || method.equals(FtpMethod.FTP_STOU));
    }

    private boolean isCommandRetrieve(String command) {
        FtpMethod method = (FtpMethod) FtpMethod.getEnumTranslator().stringToObject(command);

        return method != null && method.equals(FtpMethod.FTP_GET);
    }

    private FtpMethod[] getFtpMethods() {
        return new FtpMethod[] {
                FtpMethod.FTP_GET,
                FtpMethod.FTP_PUT,
                FtpMethod.FTP_DELE,
                FtpMethod.FTP_LIST,
                FtpMethod.FTP_ABOR,
                FtpMethod.FTP_ACCT,
                FtpMethod.FTP_ADAT,
                FtpMethod.FTP_ALLO,
                FtpMethod.FTP_APPE,
                FtpMethod.FTP_AUTH,
                FtpMethod.FTP_CCC,
                FtpMethod.FTP_CDUP,
                FtpMethod.FTP_CONF,
                FtpMethod.FTP_CWD,
                FtpMethod.FTP_ENC,
                FtpMethod.FTP_EPRT,
                FtpMethod.FTP_EPSV,
                FtpMethod.FTP_FEAT,
                FtpMethod.FTP_HELP,
                FtpMethod.FTP_LANG,
                FtpMethod.FTP_MDTM,
                FtpMethod.FTP_MIC,
                FtpMethod.FTP_MKD,
                FtpMethod.FTP_MLSD,
                FtpMethod.FTP_MLST,
                FtpMethod.FTP_MODE,
                FtpMethod.FTP_NLST,
                FtpMethod.FTP_NOOP,
                FtpMethod.FTP_OPTS,
                FtpMethod.FTP_PASS,
                FtpMethod.FTP_PASV,
                FtpMethod.FTP_PBSZ,
                FtpMethod.FTP_PORT,
                FtpMethod.FTP_PROT,
                FtpMethod.FTP_PWD,
                FtpMethod.FTP_QUIT,
                FtpMethod.FTP_REIN,
                FtpMethod.FTP_RMD,
                FtpMethod.FTP_RNFR,
                FtpMethod.FTP_RNTO,
                FtpMethod.FTP_SITE,
                FtpMethod.FTP_SIZE,
                FtpMethod.FTP_STAT,
                FtpMethod.FTP_STOU,
                FtpMethod.FTP_STRU,
                FtpMethod.FTP_SYST,
                FtpMethod.FTP_TYPE,
                FtpMethod.FTP_USER  };
    }

    /**
     * Returns the value of the specified resource string. If the string has a label suffix, e.g. a colon,
     * it is removed.
     * @param key the key of the resource
     * @return the resource string
     */
    private static String getResourceString(String key) {
        final String value = resources.getString(key);

        if(value.endsWith(COMPONENT_LABEL_SUFFIX)) {
            return value.substring(0, value.lastIndexOf(COMPONENT_LABEL_SUFFIX));
        }

        return value;
    }

    private class CheckAndComboPairListener implements PropertyChangeListener, ItemListener {
        final JCheckBox checkBox;
        final JComboBox comboBox;

        public CheckAndComboPairListener(JCheckBox checkBox, JComboBox comboBox) {
            this.checkBox = checkBox;
            this.comboBox = comboBox;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if(!checkBox.isEnabled()) {
                // remove ItemListener temporarily to avoid duplicate handling of Combo Box disabling
                checkBox.removeItemListener(this);
                checkBox.setSelected(false);
                checkBox.addItemListener(this);

                disableComboBox();
            }
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            comboBox.setEnabled(checkBox.isEnabled() && checkBox.isSelected());

            if(!comboBox.isEnabled()) {
                deselectComboBoxItem();
            }
        }

        private void disableComboBox() {
            comboBox.setEnabled(false);
            deselectComboBoxItem();
        }

        private void deselectComboBoxItem() {
            comboBox.setSelectedIndex(OPTION_COMPONENT_NULL_SELECTION_INDEX);
        }
    }
}
