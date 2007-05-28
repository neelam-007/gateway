/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting.console;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.ftp.FtpTestException;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ftprouting.FtpCredentialsSource;
import com.l7tech.external.assertions.ftprouting.FtpFileNameSource;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.external.assertions.ftprouting.FtpSecurity;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * Dialog for editing the FtpRoutingAssertion.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpRoutingPropertiesDialog extends JDialog implements AssertionPropertiesEditor<FtpRoutingAssertion> {

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
    private JTextField _timeoutTextField;               // blank allowed
    private JButton _testButton;
    private JButton _okButton;
    private JButton _cancelButton;
    private JRadioButton wssRemoveRadioButton;
    private JRadioButton wssLeaveRadioButton;

    public static final int DEFAULT_PORT_FTP = 21;
    public static final int DEFAULT_PORT_FTPS_IMPLICIT = 990;

    private FtpRoutingAssertion _assertion;
    private boolean _wasOkButtonPressed = false;
    private EventListenerList _listenerList = new EventListenerList();

    /**
     * Creates new form ServicePanel
     * @param owner  parent for dialog
     * @param a      assertion to edit
     */
    public FtpRoutingPropertiesDialog(Frame owner, FtpRoutingAssertion a) {
        super(owner, true);
        setTitle("FTP(S) Routing Properties");
        _assertion = a;
        initComponents();
        initFormData();
    }

    /**
     * @return true unless the dialog was exited via the OK button.
     */
    public boolean isCanceled() {
        return !_wasOkButtonPressed;
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        _listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        _listenerList.remove(PolicyListener.class, listener);
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
            public void actionPerformed(ActionEvent e) {
                _verifyServerCertCheckBox.setEnabled(_ftpsExplicitRadioButton.isSelected() || _ftpsImplicitRadioButton.isSelected());
                setDefaultPortNumber();
            }
        };
        _ftpUnsecuredRadioButton.addActionListener(securityListener);
        _ftpsExplicitRadioButton.addActionListener(securityListener);
        _ftpsImplicitRadioButton.addActionListener(securityListener);

        _hostNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        _portNumberTextField.setDocument(new NumberField(5));
        _timeoutTextField.setDocument(new NumberField(6));

        final ActionListener filenameListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _filenamePatternTextField.setEnabled(_filenamePatternRadioButton.isSelected());
                enableOrDisableComponents();
            }
        };
        _filenameAutoRadioButton.addActionListener(filenameListener);
        _filenamePatternRadioButton.addActionListener(filenameListener);

        _filenamePatternTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });
        Utilities.enableGrayOnDisabled(_filenamePatternTextField);

        final ActionListener credentialsListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _userNameTextField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
                _passwordField.setEnabled(_credentialsSpecifyRadioButton.isSelected());
                enableOrDisableComponents();
            }
        };
        _credentialsPassThruRadioButton.addActionListener(credentialsListener);
        _credentialsSpecifyRadioButton.addActionListener(credentialsListener);

        _userNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });
        Utilities.enableGrayOnDisabled(_userNameTextField);
        Utilities.enableGrayOnDisabled(_passwordField);

        _testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });

        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getData(_assertion);
                fireEventAssertionChanged(_assertion);
                _wasOkButtonPressed = true;
                dispose();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                FtpRoutingPropertiesDialog.this.dispose();
            }
        });
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
        _portNumberTextField.setText(Integer.toString(_assertion.getPort()));
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

        _timeoutTextField.setText(Integer.toString(_assertion.getTimeout() / 1000));

        if (_assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER) {
            wssRemoveRadioButton.setSelected(true);
        } else if (_assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS) {
            wssLeaveRadioButton.setSelected(true);
        }
    }

    private void setDefaultPortNumber() {
        int port = DEFAULT_PORT_FTP;
        if (_ftpsImplicitRadioButton.isSelected()) {
            port = DEFAULT_PORT_FTPS_IMPLICIT;
        }
        _portNumberTextField.setText(Integer.toString(port));
    }

    /**
     * Enable/disable the OK and test buttons if all settings are OK.
     */
    private void enableOrDisableComponents() {
        final boolean canTest = _hostNameTextField.getText().length() != 0
                && _credentialsSpecifyRadioButton.isSelected()
                && _userNameTextField.getText().length() != 0;
        _testButton.setEnabled(canTest);

        final boolean canOK = _hostNameTextField.getText().length() != 0
                && !(_filenamePatternRadioButton.isSelected() && _filenamePatternTextField.getText().length() == 0)
                && !(_credentialsSpecifyRadioButton.isSelected() && _userNameTextField.getText().length() == 0);
        _okButton.setEnabled(canOK);
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return _wasOkButtonPressed;
    }

    public void setData(FtpRoutingAssertion assertion) {
        this._assertion = assertion;
        initFormData();
    }

    /** Copies view into model. */
    public FtpRoutingAssertion getData(FtpRoutingAssertion assertion) {
        if (_ftpUnsecuredRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
            _verifyServerCertCheckBox.setSelected(false);
        } else if (_ftpsExplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_EXPLICIT);
        } else if (_ftpsImplicitRadioButton.isSelected()) {
            assertion.setSecurity(FtpSecurity.FTPS_IMPLICIT);
        }

        assertion.setVerifyServerCert(_verifyServerCertCheckBox.isSelected());

        assertion.setHostName(_hostNameTextField.getText());

        if (_portNumberTextField.getText().length() == 0) {
            setDefaultPortNumber();
        }
        assertion.setPort(Integer.parseInt(_portNumberTextField.getText()));

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

        if (_timeoutTextField.getText().length() == 0) {
            _timeoutTextField.setText(Integer.toString(FtpRoutingAssertion.DEFAULT_TIMEOUT / 1000));
        }
        assertion.setTimeout(Integer.parseInt(_timeoutTextField.getText()) * 1000);

        if (wssRemoveRadioButton.isSelected()) {
            assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);
        } else if (wssLeaveRadioButton.isSelected()) {
            assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
        }

        return assertion;
    }

    private void testConnection() {
        final FtpRoutingAssertion a = getData(new FtpRoutingAssertion());
        final boolean isFtps = a.getSecurity() == FtpSecurity.FTPS_EXPLICIT || a.getSecurity() == FtpSecurity.FTPS_IMPLICIT;
        final boolean isExplicit = a.getSecurity() == FtpSecurity.FTPS_EXPLICIT;
        try {
            Registry.getDefault().getFtpManager().testConnection(
                    isFtps,
                    isExplicit,
                    a.isVerifyServerCert(),
                    a.getHostName(),
                    a.getPort(),
                    a.getUserName(),
                    a.getPassword(),
                    a.getDirectory(),
                    a.getTimeout());
            JOptionPane.showMessageDialog(
                    this,
                    "The Gateway has verified the connection to this FTP server.",
                    "FTP Connection Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to test connection to FTP server.", e);
        } catch (FtpTestException e) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("The Gateway was unable to connect to this FTP server:"));
            panel.add(new JLabel(e.getMessage()));
            if (e.getSessionLog() != null && e.getSessionLog().length() != 0) {
                panel.add(Box.createVerticalStrut(10));
                panel.add(new JLabel("Detail log of FTP session:"));
                JTextArea sessionLog = new JTextArea(e.getSessionLog());
                sessionLog.setAlignmentX(Component.LEFT_ALIGNMENT);
                sessionLog.setBorder(BorderFactory.createEtchedBorder());
                sessionLog.setEditable(false);
                sessionLog.setEnabled(true);
                sessionLog.setFont(new Font(null, Font.PLAIN, 11));
                panel.add(sessionLog);
            }
            JOptionPane.showMessageDialog(
                    this,
                    panel,
                    "FTP Connection Failure",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
