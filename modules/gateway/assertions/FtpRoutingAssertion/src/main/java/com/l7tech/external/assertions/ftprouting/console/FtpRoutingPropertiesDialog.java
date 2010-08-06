/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting.console;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.RoutingDialogUtils;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.gateway.common.transport.ftp.FtpTestException;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Dialog for editing the FtpRoutingAssertion.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpRoutingPropertiesDialog extends AssertionPropertiesEditorSupport<FtpRoutingAssertion> {

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
    private JButton _okButton;
    private JButton _cancelButton;
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JLabel portStatusLabel;
    private JComboBox messageSource;

    public static final int DEFAULT_PORT_FTP = 21;

    private FtpRoutingAssertion _assertion;
    private boolean _wasOkButtonPressed = false;
    private EventListenerList _listenerList = new EventListenerList();

    private AbstractButton[] secHdrButtons = { wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };

    private static class MsgSrcComboBoxItem {
        private boolean undefined = false;

        private final MessageTargetableSupport target;
        public MsgSrcComboBoxItem(MessageTargetableSupport target) {
            this.target = target;
        }

        public MessageTargetableSupport getTarget() {
            return target;
        }

        public void setUndefined(boolean undefined) {
            this.undefined = undefined;
        }

        public boolean isUndefined() {
            return undefined;
        }

        @Override
        public String toString() {
            return (undefined ? "Undefined: " : "") + target.getTargetName();
        }
    }

    /**
     * Creates new form ServicePanel
     * @param owner  parent for dialog
     * @param a      assertion to edit
     */
    public FtpRoutingPropertiesDialog(Window owner, FtpRoutingAssertion a) {
        super(owner, a);
        _assertion = a;
        initComponents();
        initFormData();
    }

    /**
     * Notify the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        final CompositeAssertion parent = a.getParent();
        if (parent == null)
          return;

        SwingUtilities.invokeLater(
          new Runnable() {
              @Override
              public void run() {
                  int[] indices = new int[parent.getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = _listenerList.getListeners(PolicyListener.class);
                  for (EventListener listener : listeners) {
                      ((PolicyListener)listener).assertionsChanged(event);
                  }
              }
          });
    }

    /**
     * This method is called from within the static factory to
     * initialize the form.
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(_mainPanel, BorderLayout.CENTER);
        Utilities.setEscKeyStrokeDisposes(this);

        final ActionListener securityListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
                setDefaultPortNumber();
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

        _portNumberTextField.setDocument(new NumberField(5));
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
        Utilities.enableGrayOnDisabled(_userNameTextField);
        Utilities.enableGrayOnDisabled(_passwordField);

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

        _okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((MsgSrcComboBoxItem) messageSource.getSelectedItem()).isUndefined()) {
                    JOptionPane.showMessageDialog(_okButton, "Undefined context variable for message source: " + ((MsgSrcComboBoxItem) messageSource.getSelectedItem()).getTarget().getTargetName());
                    return;
                }
                getData(_assertion);
                fireEventAssertionChanged(_assertion);
                _wasOkButtonPressed = true;
                dispose();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                FtpRoutingPropertiesDialog.this.dispose();
            }
        });

        populateReqMsgSrcComboBox();
    }

    private void populateReqMsgSrcComboBox() {
        messageSource.removeAllItems();

        messageSource.addItem(new MsgSrcComboBoxItem(new MessageTargetableSupport(TargetMessageType.REQUEST, false)));
        messageSource.addItem(new MsgSrcComboBoxItem(new MessageTargetableSupport(TargetMessageType.RESPONSE, false)));
        MessageTargetableSupport currentMessageSource = _assertion.getRequestTarget();

        final Map<String, VariableMetadata> predecessorVariables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(_assertion);
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName : predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final MsgSrcComboBoxItem item = new MsgSrcComboBoxItem(new MessageTargetableSupport(variableName));
                messageSource.addItem(item);
                if ( currentMessageSource != null && variableName.equals(currentMessageSource.getOtherTargetMessageVariable())) {
                    messageSource.setSelectedItem(item);
                }
            }
        }
        if (currentMessageSource != null && currentMessageSource.getTarget() == TargetMessageType.OTHER &&
            currentMessageSource.getOtherTargetMessageVariable() != null && ! predecessorVariableNames.contains(currentMessageSource.getOtherTargetMessageVariable())) {
            MsgSrcComboBoxItem current = new MsgSrcComboBoxItem(new MessageTargetableSupport(currentMessageSource.getOtherTargetMessageVariable()));
            current.setUndefined(true);
            messageSource.addItem(current);
            messageSource.setSelectedItem(current);
        }
    }

    private void initFormData() {
        final FtpSecurity security = _assertion.getSecurity();
        if (security == null || security == FtpSecurity.FTP_UNSECURED) {
            _ftpUnsecuredRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_EXPLICIT) {
            _ftpsExplicitRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_IMPLICIT) {
            _ftpsImplicitRadioButton.doClick(0);
        }

        _verifyServerCertCheckBox.setSelected(_assertion.isVerifyServerCert());
        _hostNameTextField.setText(_assertion.getHostName());
        _portNumberTextField.setText(_assertion.getPort());
        _directoryTextField.setText(_assertion.getDirectory());

        if (_assertion.getFileNameSource() == null || _assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            _filenameAutoRadioButton.doClick(0);
        } else if (_assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
            _filenamePatternRadioButton.doClick(0);
        }
        _filenamePatternTextField.setText(_assertion.getFileNamePattern());

        if (_assertion.getCredentialsSource() == null || _assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            _credentialsPassThruRadioButton.doClick(0);
        } else if (_assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            _credentialsSpecifyRadioButton.doClick(0);
            _userNameTextField.setText(_assertion.getUserName());
            _passwordField.setText(_assertion.getPassword());
        }

        _useClientCertCheckBox.setSelected(_assertion.isUseClientCert());
        _clientCertsComboBox.select(_assertion.getClientCertKeystoreId(), _assertion.getClientCertKeyAlias());

        _timeoutTextField.setText(Integer.toString(_assertion.getTimeout() / 1000));

        RoutingDialogUtils.configSecurityHeaderRadioButtons(_assertion, -1, null, secHdrButtons);

        enableOrDisableComponents();
    }

    private void setDefaultPortNumber() {
        int port = DEFAULT_PORT_FTP;
        if (_ftpsImplicitRadioButton.isSelected()) {
            port = FtpRoutingAssertion.DEFAULT_FTPS_IMPLICIT_PORT;
        }
        _portNumberTextField.setText(Integer.toString(port));
    }

    /**
     * Enable/disable the OK and test buttons if all settings are OK.
     */
    private void enableOrDisableComponents() {
        final boolean isFtps = _ftpsExplicitRadioButton.isSelected() || _ftpsImplicitRadioButton.isSelected();
        _verifyServerCertCheckBox.setEnabled(isFtps);
        _userNameTextField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
        _passwordField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
        _useClientCertCheckBox.setEnabled(isFtps);
        _clientCertsComboBox.setEnabled(_useClientCertCheckBox.isEnabled() && _useClientCertCheckBox.isSelected());

        final boolean canTest = _hostNameTextField.getText().length() != 0
                && _credentialsSpecifyRadioButton.isSelected()
                && _userNameTextField.getText().length() != 0
                && (!_useClientCertCheckBox.isSelected() || _clientCertsComboBox.getSelectedIndex() != -1);
        _testButton.setEnabled(canTest);

        boolean portStatusLabelVisible = setPortStatusLabelVisibility();

        final boolean canOK = _hostNameTextField.getText().length() != 0
                && !(_filenamePatternRadioButton.isSelected() && _filenamePatternTextField.getText().length() == 0)
                && !(_credentialsSpecifyRadioButton.isSelected() && _userNameTextField.getText().length() == 0)
                && (!_useClientCertCheckBox.isSelected() || _clientCertsComboBox.getSelectedIndex() != -1)
                && (! portStatusLabelVisible);
        _okButton.setEnabled(!isReadOnly() && canOK);
    }

    /**
     * Set the visibility of the port status label depending on if the port number is between 1 and 65535.
     * @return true if the port numbere is valid, false otherwise.
     */
    private boolean setPortStatusLabelVisibility() {
        boolean portStatusLabelVisible;
        String portStr = _portNumberTextField.getText();
        if ("".equals(portStr)) { // Since _portNumberTextField allows a blank.
            portStatusLabelVisible = false;
        } else {
            int port = Integer.parseInt(portStr);
            portStatusLabelVisible = (port <= 0) || (port > 65535);
        }
        portStatusLabel.setVisible(portStatusLabelVisible);
        return portStatusLabelVisible;
    }

    @Override
    public boolean isConfirmed() {
        return _wasOkButtonPressed;
    }

    @Override
    public void setData(FtpRoutingAssertion assertion) {
        this._assertion = assertion;
        initFormData();
    }

    /** Copies view into model. */
    @Override
    public FtpRoutingAssertion getData(FtpRoutingAssertion assertion) {
        if (_ftpUnsecuredRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
            _verifyServerCertCheckBox.setSelected(false);
        } else if (_ftpsExplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_EXPLICIT);
        } else if (_ftpsImplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_IMPLICIT);
        }

        assertion.setRequestTarget(((MsgSrcComboBoxItem) messageSource.getSelectedItem()).getTarget());

        assertion.setVerifyServerCert(_verifyServerCertCheckBox.isSelected());

        assertion.setHostName(_hostNameTextField.getText());

        if (_portNumberTextField.getText().length() == 0) {
            setDefaultPortNumber();
        }
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

        assertion.setUseClientCert(_useClientCertCheckBox.isSelected());
        if (_useClientCertCheckBox.isSelected()) {
            assertion.setClientCertKeystoreId(_clientCertsComboBox.getSelectedKeystoreId());
            assertion.setClientCertKeyAlias(_clientCertsComboBox.getSelectedKeyAlias());
        }

        if (_timeoutTextField.getText().length() == 0) {
            _timeoutTextField.setText(Integer.toString(FtpRoutingAssertion.DEFAULT_TIMEOUT / 1000));
        }
        assertion.setTimeout(Integer.parseInt(_timeoutTextField.getText()) * 1000);

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);

        return assertion;
    }

    @Override
    protected void configureView() {
        enableOrDisableComponents();
    }

    /**
     * Runs connection test with cancellable progress bar. Displays result with
     * session log if failure.
     */
    private void testConnection() {
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
           .append(a.getPassword()).append(" ")
           .append(a.getDirectory()).append(" ");
        return Syntax.getReferencedNames(tmp.toString()).length > 0;
    }
}
