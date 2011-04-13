/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.RoutingDialogUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.gateway.common.transport.ftp.FtpTestException;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

/**
 * Dialog for editing the FtpRoutingAssertion.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpRoutingPropertiesDialog extends AssertionPropertiesOkCancelSupport<FtpRoutingAssertion> {

    private JPanel _mainPanel;
    private JRadioButton _ftpUnsecuredRadioButton;
    private JRadioButton _ftpsExplicitRadioButton;
    private JRadioButton _ftpsImplicitRadioButton;
    private JCheckBox _verifyServerCertCheckBox;
    private JTextField _hostNameTextField;              // blank not allowed
    private JTextField _portNumberTextField;            // blank allowed
    private JTextField _directoryTextField;             // blank allowed
    private JRadioButton _filenameAutoRadioButton;
    private JRadioButton _filenamePatternRadioButton;
    private JTextField _filenamePatternTextField;       // blank not allowed
    private JRadioButton _credentialsPassThruRadioButton;
    private JRadioButton _credentialsSpecifyRadioButton;
    private JTextField _userNameTextField;              // blank not allowed
    private JPasswordField _passwordField;              // blank allowed
    private JCheckBox _useClientCertCheckBox;
    private PrivateKeysComboBox _clientCertsComboBox;
    private JTextField _timeoutTextField;               // blank allowed
    private JButton _testButton;
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JComboBox messageSource;
    private JCheckBox contextVariableInPassword;
    private char echoChar;
    private AbstractButton[] secHdrButtons = { wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };
    private InputValidator validators;

    public static final int DEFAULT_PORT_FTP = 21;
    private static final ResourceBundle resources = ResourceBundle.getBundle( FtpRoutingPropertiesDialog.class.getName() );
    
    /**
     * Creates new form ServicePanel
     * @param owner  parent for dialog
     * @param a      assertion to edit
     */
    public FtpRoutingPropertiesDialog(Window owner, FtpRoutingAssertion a) {
        super(FtpRoutingAssertion.class,  owner, a, true);
        initComponents();
        setData(a);
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

        final ActionListener securityListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
                final int port = getDefaultPortNumber();
                _portNumberTextField.setText(Integer.toString(port));
            }
        };
        _ftpUnsecuredRadioButton.addActionListener(securityListener);
        _ftpsExplicitRadioButton.addActionListener(securityListener);
        _ftpsImplicitRadioButton.addActionListener(securityListener);

        _hostNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        _portNumberTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        }));

        _timeoutTextField.setDocument(new NumberField(6));

        final ActionListener filenameListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _filenamePatternTextField.setEnabled(_filenamePatternRadioButton.isSelected());
                enableOrDisableComponents();
            }
        };
        _filenameAutoRadioButton.addActionListener(filenameListener);
        _filenamePatternRadioButton.addActionListener(filenameListener);

        _filenamePatternTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });
        Utilities.enableGrayOnDisabled(_filenamePatternTextField);

        final ActionListener credentialsListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };
        _credentialsPassThruRadioButton.addActionListener(credentialsListener);
        _credentialsSpecifyRadioButton.addActionListener(credentialsListener);

        _userNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        echoChar = _passwordField.getEchoChar();
        contextVariableInPassword.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (contextVariableInPassword.isSelected()) {
                    _passwordField.setText("");
                }
                _passwordField.enableInputMethods(contextVariableInPassword.isSelected());
                _passwordField.setEchoChar(contextVariableInPassword.isSelected() ? (char)0 : echoChar);
            }
        });

        Utilities.enableGrayOnDisabled(_userNameTextField);
        Utilities.enableGrayOnDisabled(_passwordField);
        Utilities.enableGrayOnDisabled(contextVariableInPassword);

        _useClientCertCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });
        _clientCertsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        });

        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        _testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });

        messageSource.addItem(new MessageTargetableSupport(TargetMessageType.REQUEST));
        messageSource.addItem(new MessageTargetableSupport(TargetMessageType.RESPONSE));

        //validators
        validators = new InputValidator(this, getResourceString("errorTitle"));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"),_hostNameTextField,null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("specifyPatternLabel"),_filenamePatternTextField,null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"),_userNameTextField,null));

        validators.addRule(new InputValidator.ComponentValidationRule(_clientCertsComboBox) {
            @Override
            public String getValidationError() {
                if(_useClientCertCheckBox.isSelected() && _clientCertsComboBox.getSelectedIndex() == -1){
                    return getResourceString("clientCertError");
                }
                return null;
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(_portNumberTextField) {
            @Override
            public String getValidationError() {
                boolean portIsValid = isPortValid();
                if(!portIsValid){
                    final int port = getDefaultPortNumber();
                    return MessageFormat.format(getResourceString("portError"),port);

                }
                return null;
            }
        });

    }

    private String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

    private void populateReqMsgSrcComboBox(FtpRoutingAssertion assertion) {
        MessageTargetableSupport msgSource = assertion.getRequestTarget();
        TargetMessageType msgSourceType = msgSource != null ? msgSource.getTarget() : null;

        if (msgSourceType == TargetMessageType.REQUEST)
            messageSource.setSelectedIndex(0);
        else if (msgSourceType == TargetMessageType.RESPONSE)
            messageSource.setSelectedIndex(1);
        else {
            String msgSourceVariable = msgSourceType == TargetMessageType.OTHER ? msgSource.getOtherTargetMessageVariable() : null;
            if (msgSourceVariable != null) {
                boolean msgSourceFound = false;
                for (int i=2; i < messageSource.getItemCount(); i++) {
                    MessageTargetableSupport messageSourceItem = (MessageTargetableSupport) messageSource.getItemAt(i);
                    if (msgSourceVariable.equals(messageSourceItem.getOtherTargetMessageVariable())) {
                        msgSourceFound = true;
                        messageSource.setSelectedIndex(i);
                        break;
                    }
                }
                if (!msgSourceFound) {
                    MessageTargetableSupport notFoundMsgSource = new MessageTargetableSupport(msgSourceVariable);
                    messageSource.addItem(notFoundMsgSource);
                    messageSource.setSelectedItem(notFoundMsgSource);
                }
            }
        }
    }

    private void modelToView(FtpRoutingAssertion assertion) {
        final FtpSecurity security = assertion.getSecurity();
        if (security == null || security == FtpSecurity.FTP_UNSECURED) {
            _ftpUnsecuredRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_EXPLICIT) {
            _ftpsExplicitRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_IMPLICIT) {
            _ftpsImplicitRadioButton.doClick(0);
        }

        _verifyServerCertCheckBox.setSelected(assertion.isVerifyServerCert());
        _hostNameTextField.setText(assertion.getHostName());
        _portNumberTextField.setText(assertion.getPort());
        _directoryTextField.setText(assertion.getDirectory());

        if (assertion.getFileNameSource() == null || assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            _filenameAutoRadioButton.doClick(0);
        } else if (assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
            _filenamePatternRadioButton.doClick(0);
        }
        _filenamePatternTextField.setText(assertion.getFileNamePattern());

        if (assertion.getCredentialsSource() == null || assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            _credentialsPassThruRadioButton.doClick(0);
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            _credentialsSpecifyRadioButton.doClick(0);
            _userNameTextField.setText(assertion.getUserName());
            _passwordField.setText(assertion.getPassword());
            _passwordField.enableInputMethods(assertion.isPasswordUsesContextVariables());
            contextVariableInPassword.setSelected(assertion.isPasswordUsesContextVariables());
        }

        _useClientCertCheckBox.setSelected(assertion.isUseClientCert());
        _clientCertsComboBox.select(assertion.getClientCertKeystoreId(), assertion.getClientCertKeyAlias());

        _timeoutTextField.setText(Integer.toString(assertion.getTimeout() / 1000));

        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);

        populateReqMsgSrcComboBox(assertion);
        messageSource.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", null), null, false ) );

        enableOrDisableComponents();
    }

    private int getDefaultPortNumber() {
        int port = DEFAULT_PORT_FTP;
        if (_ftpsImplicitRadioButton.isSelected()) {
            port = FtpRoutingAssertion.DEFAULT_FTPS_IMPLICIT_PORT;
        }
        return port;
    }

    /**
     * Enable/disable the OK and test buttons if all settings are OK.
     */
    private void enableOrDisableComponents() {
        final boolean isFtps = _ftpsExplicitRadioButton.isSelected() || _ftpsImplicitRadioButton.isSelected();
        _verifyServerCertCheckBox.setEnabled(isFtps);
        _userNameTextField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
        _passwordField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
        contextVariableInPassword.setEnabled(_credentialsSpecifyRadioButton.isSelected());
        _useClientCertCheckBox.setEnabled(isFtps);
        _clientCertsComboBox.setEnabled(_useClientCertCheckBox.isEnabled() && _useClientCertCheckBox.isSelected());


    }

    /**
     * @return Return true iff the port number is between 1 and 65535 or references a context variable.
     */
    private boolean isPortValid() {
        boolean isValid;
        String portStr = _portNumberTextField.getText();
        try {
            int port = Integer.parseInt(portStr);
            isValid = port > 0 && port < 65535;
        } catch (NumberFormatException e) {
            // must be using context variable
            isValid = Syntax.getReferencedNames(portStr).length > 0;
        }
        return isValid;
    }


    @Override
    public void setData(FtpRoutingAssertion assertion) {
        messageSource.setModel( buildMessageSourceComboBoxModel(assertion) );
        messageSource.setSelectedItem( new MessageTargetableSupport(assertion.getRequestTarget()) );
        modelToView(assertion);
    }

    /** Copies view into model. */
    @Override
    public FtpRoutingAssertion getData(FtpRoutingAssertion assertion) {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }
        
        if (_ftpUnsecuredRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
            _verifyServerCertCheckBox.setSelected(false);
        } else if (_ftpsExplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_EXPLICIT);
        } else if (_ftpsImplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_IMPLICIT);
        }

        assertion.setRequestTarget((MessageTargetableSupport) messageSource.getSelectedItem());

        assertion.setVerifyServerCert(_verifyServerCertCheckBox.isSelected());

        assertion.setHostName(_hostNameTextField.getText());

        assertion.setPort(_portNumberTextField.getText());

        assertion.setDirectory(_directoryTextField.getText());

        if (_filenameAutoRadioButton.isSelected()) {
            assertion.setFileNameSource(FtpFileNameSource.AUTO);
            _filenamePatternTextField.setText("");
        } else if (_filenamePatternRadioButton.isSelected()) {
            assertion.setFileNameSource(FtpFileNameSource.PATTERN);
        }

        assertion.setFileNamePattern(_filenamePatternTextField.getText());

        if (_credentialsPassThruRadioButton.isSelected()) {
            assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
            _userNameTextField.setText("");
            _passwordField.setText("");
        } else if (_credentialsSpecifyRadioButton.isSelected()) {
            assertion.setCredentialsSource(FtpCredentialsSource.SPECIFIED);
        }
        assertion.setUserName(_userNameTextField.getText());
        assertion.setPassword(new String(_passwordField.getPassword()));
        assertion.setPasswordUsesContextVariables(contextVariableInPassword.isSelected());

        assertion.setUseClientCert(_useClientCertCheckBox.isSelected());
        if (_useClientCertCheckBox.isSelected()) {
            assertion.setClientCertKeystoreId(_clientCertsComboBox.getSelectedKeystoreId());
            assertion.setClientCertKeyAlias(_clientCertsComboBox.getSelectedKeyAlias());
        }

        if (_timeoutTextField.getText().trim().isEmpty()) {
            _timeoutTextField.setText(Integer.toString(FtpRoutingAssertion.DEFAULT_TIMEOUT / 1000));
        }
        assertion.setTimeout(Integer.parseInt(_timeoutTextField.getText()) * 1000);

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        return assertion;
    }

    /**
     * Runs connection test with cancellable progress bar. Displays result with
     * session log if failure.
     */
    private void testConnection() {
        // validate for test
        final String error = validators.validate();
        if(error != null){
            JOptionPane.showMessageDialog(
                        FtpRoutingPropertiesDialog.this,
                        error,
                        "FTP(S) Connection Failure",
                        JOptionPane.ERROR_MESSAGE);
                return;
        }

        try {
            final FtpRoutingAssertion a = getData(new FtpRoutingAssertion());

            if (ftpConfigurationUsesVariables(a)) {
                JOptionPane.showMessageDialog(
                        FtpRoutingPropertiesDialog.this,
                        "FTP configuration uses context variables, connection cannot be tested at policy design time.",
                        "FTP(S) Connection Failure",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            final JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            final CancelableOperationDialog cancelDialog =
                    new CancelableOperationDialog(null, "FTP(S) Connection Test", "Testing connection to FTP(S) server ...", progressBar);
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
                        "The Gateway has verified the connection to this FTP(S) server.",
                        "FTP(S) Connection Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (InterruptedException e) {
            // Swing thread interrupted.
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof FtpTestException) {
                    final FtpTestException fte = (FtpTestException)cause;
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.add(new JLabel("The Gateway was unable to connect to this FTP(S) server:"));
                    panel.add(new JLabel(fte.getMessage()));
                    if (fte.getSessionLog() != null && fte.getSessionLog().length() != 0) {
                        panel.add(Box.createVerticalStrut(10));
                        panel.add(new JLabel("Detail log of FTP(S) session:"));
                        JTextArea sessionLog = new JTextArea(fte.getSessionLog());
                        sessionLog.setAlignmentX(Component.LEFT_ALIGNMENT);
                        sessionLog.setBorder(BorderFactory.createEtchedBorder());
                        sessionLog.setEditable(false);
                        sessionLog.setEnabled(true);
                        sessionLog.setFont(new Font(null, Font.PLAIN, 11));
                        panel.add(sessionLog);
                    }
                    JOptionPane.showMessageDialog(
                            FtpRoutingPropertiesDialog.this,
                            panel,
                            "FTP(S) Connection Failure",
                            JOptionPane.ERROR_MESSAGE);

                } else {
                    throw ExceptionUtils.wrap(cause);
                }
            }
        }
    }

    private boolean ftpConfigurationUsesVariables(FtpRoutingAssertion a) {
        StringBuilder tmp = new StringBuilder();
        tmp.append(a.getHostName()).append(" ")
           .append(a.getPort()).append(" ")
           .append(a.getUserName()).append(" ")
           .append(a.getDirectory()).append(" ");
        if (a.isPasswordUsesContextVariables())
            tmp.append(a.getPassword());
        return Syntax.getReferencedNames(tmp.toString()).length > 0;
    }
}
