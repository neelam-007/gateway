/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Dialog for editing the FtpArchiveReceiver configuration.
 *
 * @author jbufu
 * @since SecureSpan 4.6
 */
public class FtpAuditArchiverPropertiesDialog extends JDialog {

    public static final String TITLE = "FTP(S) Audit Archiver Properties";
    public static final String AUDIT_ARCHIVER_CONFIG_CLUSTER_PROPERTY_NAME = "audit.archiver.ftp.config";

    public static final int DEFAULT_PORT_FTP = 21;
    public static final int DEFAULT_PORT_FTPS_IMPLICIT = 990;

    private JPanel _mainPanel;
    private JRadioButton _ftpUnsecuredRadioButton;
    private JRadioButton _ftpsExplicitRadioButton;
    private JRadioButton _ftpsImplicitRadioButton;
    private JCheckBox _verifyServerCertCheckBox;
    private JTextField _hostNameTextField;              // blank not allowed
    private JTextField _portNumberTextField;            // blank allowed
    private JTextField _directoryTextField;             // blank allowed
    private JTextField _userNameTextField;              // blank not allowed
    private JPasswordField _passwordField;              // blank allowed
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JTextField _timeoutTextField;               // blank allowed
    private JCheckBox enabledCheckBox;
    private JButton _testButton;
    private JButton _okButton;
    private JButton _cancelButton;

    private FtpClientConfig ftpConfig;
    private ClusterProperty ftpConfigClusterProp;
    private boolean readOnly;

    private boolean confirmed = false;


    /**
     * Creates a new instance of FtpArchiveReceiverPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided FtpArchiveReceiver and if the dialog is
     * dismissed with the OK button, then the provided FtpArchiveReceiver will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     */
    public FtpAuditArchiverPropertiesDialog(Frame owner) throws HeadlessException {
        super(owner, TITLE, true);
        try {
            deserialize(readConfigFromClusterProperty());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read FTP configuration from database.", e); // shouldn't happen
        }
        initialize();
    }

    /**
     * Creates a new instance of FtpArchiveReceiverPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided FtpArchiveReceiver and if the dialog is
     * dismissed with the OK button, then the provided FtpArchiveReceiver will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     */
    public FtpAuditArchiverPropertiesDialog(Dialog owner) throws HeadlessException {
        super(owner, TITLE, true);
        try {
            deserialize(readConfigFromClusterProperty());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read FTP configuration from database.", e); // shouldn't happen
        }
        initialize();
    }

    private ClusterProperty readConfigFromClusterProperty() {
        return Registry.getDefault().getAuditAdmin().getFtpAuditArchiveConfig();
    }

    private void writeConfigToClusterProperty() {
        String errMsg = null;
        try {
            Registry.getDefault().getAuditAdmin().setFtpAuditArchiveConfig(serialize());
        } catch (UpdateException ue) {
            errMsg = ue.getMessage();
        } catch (IOException ioe) {
            errMsg = ioe.getMessage();
        }

        if (errMsg != null)
            JOptionPane.showMessageDialog(
                    FtpAuditArchiverPropertiesDialog.this,
                    "Error saving FTP configuration to database: " + errMsg, "Error",
                    JOptionPane.ERROR_MESSAGE);
    }

    private void deserialize(ClusterProperty config) throws IOException, ClassNotFoundException {

        if (config == null || config.getName() == null) {
            throw new IllegalArgumentException("Cluster property key for the FTP archiver configuration must not be null.");

        } else {
            this.ftpConfigClusterProp = config;

            if (config.getValue() == null || config.getValue().length() == 0) {
                this.ftpConfig = FtpClientConfigImpl.newFtpConfig("");
                return;
            }

            // actual deserialization
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(config.getValue())));
            ftpConfig = (FtpClientConfig) in.readObject();
        }

//            JOptionPane.showMessageDialog(
//                    FtpAuditArchiverPropertiesDialog.this,
//                    "Error reading FTP configuration to database: " + errMsg, "Error",
//                    JOptionPane.ERROR_MESSAGE);
    }

    private ClusterProperty serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(ftpConfig);
        out.flush();
        ftpConfigClusterProp.setValue(HexUtils.encodeBase64(baos.toByteArray()));
        return ftpConfigClusterProp;
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided FTP configuration.
     * object.
     */
    private void initialize() {
        readOnly = !canEditAuditArchiverConfig();
        setContentPane(_mainPanel);
        initializeFields();

        // Update all of the fields using the values from the provided FTP configuration.
        modelToView();

        Utilities.equalizeButtonSizes(new AbstractButton[] { _okButton, _cancelButton });

        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Initializes the base settings fields.
     */
    private void initializeFields() {

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setContentPane(_mainPanel);
        pack();
        setModal(true);
        getRootPane().setDefaultButton(_okButton);

        Utilities.setEscKeyStrokeDisposes(this);

        final ActionListener validationListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
                setDefaultPortNumber();
            }
        };

        _ftpUnsecuredRadioButton.addActionListener(validationListener);
        _ftpsExplicitRadioButton.addActionListener(validationListener);
        _ftpsImplicitRadioButton.addActionListener(validationListener);

        _hostNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        _portNumberTextField.setDocument(new NumberField(5));
        _timeoutTextField.setDocument(new NumberField(6));

        _userNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });
        Utilities.enableGrayOnDisabled(_userNameTextField);
        Utilities.enableGrayOnDisabled(_passwordField);
        PasswordGuiUtils.configureOptionalSecurePasswordField(_passwordField, showPasswordCheckBox, plaintextPasswordWarningLabel);

        _testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });

        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                FtpAuditArchiverPropertiesDialog.this.dispose();
            }
        });
    }

    private void onOk() {
        viewToModel();
        writeConfigToClusterProperty();
        confirmed = true;
        dispose();
    }

    /**
     * Updates the dialog fields to match the values from the backing FtpArchiveReceiver.
     */
    private void modelToView() {
        final FtpSecurity security = ftpConfig.getSecurity();
        if (security == null || security == FtpSecurity.FTP_UNSECURED) {
            _ftpUnsecuredRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_EXPLICIT) {
            _ftpsExplicitRadioButton.doClick(0);
        } else if (security == FtpSecurity.FTPS_IMPLICIT) {
            _ftpsImplicitRadioButton.doClick(0);
        }

        _hostNameTextField.setText(ftpConfig.getHost());
        _portNumberTextField.setText(Integer.toString(ftpConfig.getPort()));
        _timeoutTextField.setText(Integer.toString(ftpConfig.getTimeout() / 1000));
        _userNameTextField.setText(ftpConfig.getUser());
        _passwordField.setText(ftpConfig.getPass());
        enabledCheckBox.setSelected(ftpConfig.isEnabled());

        _verifyServerCertCheckBox.setSelected(ftpConfig.isVerifyServerCert());
        _directoryTextField.setText(ftpConfig.getDirectory());

        enableOrDisableComponents();
    }

    /**
     * Updates the backing FtpArchiveReceiver with the values from this dialog.
     */
    private void viewToModel() {
        if (_ftpUnsecuredRadioButton.isSelected()) {
            ftpConfig.setSecurity(FtpSecurity.FTP_UNSECURED);
            _verifyServerCertCheckBox.setSelected(false);
        } else if (_ftpsExplicitRadioButton.isSelected()) {
            ftpConfig.setSecurity(FtpSecurity.FTPS_EXPLICIT);
        } else if (_ftpsImplicitRadioButton.isSelected()) {
            ftpConfig.setSecurity(FtpSecurity.FTPS_IMPLICIT);
        }

        ftpConfig.setHost(_hostNameTextField.getText());

        if (_portNumberTextField.getText().length() != 0)
            ftpConfig.setPort(Integer.parseInt(_portNumberTextField.getText()));

        if (_timeoutTextField.getText().length() != 0)
            ftpConfig.setTimeout(Integer.parseInt(_timeoutTextField.getText()) * 1000);

        ftpConfig.setEnabled(enabledCheckBox.isSelected());

        ftpConfig.setUser(_userNameTextField.getText());
        ftpConfig.setPass(new String(_passwordField.getPassword()));
        ftpConfig.setDirectory(_directoryTextField.getText());

        ftpConfig.setVerifyServerCert(_verifyServerCertCheckBox.isSelected());

        ftpConfig.setCredentialsSource(FtpCredentialsSource.SPECIFIED);
        ftpConfig.setUseClientCert(false);
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
        final boolean isFtps = _ftpsExplicitRadioButton.isSelected() || _ftpsImplicitRadioButton.isSelected();
        _verifyServerCertCheckBox.setEnabled(isFtps);

        final boolean canTest = _hostNameTextField.getText().length() != 0
                && _userNameTextField.getText().length() != 0;
        _testButton.setEnabled(canTest);
        _okButton.setEnabled(canTest && !readOnly);

        final boolean canOK = _hostNameTextField.getText().length() != 0
                && !(_userNameTextField.getText().length() == 0);
        _okButton.setEnabled(canOK && !readOnly);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isCanceled() {
        return !confirmed;
    }

    /**
     * Runs connection test with cancellable progress bar. Displays result with
     * session log if failure.
     */
    private void testConnection() {
        try {
            final JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            final CancelableOperationDialog cancelDialog =
                    new CancelableOperationDialog(null, "FTP(S) Connection Test", "Testing connection to FTP(S) server ...", progressBar);
            cancelDialog.pack();
            cancelDialog.setModal(true);
            Utilities.centerOnScreen(cancelDialog);

            Callable<Boolean> callable = new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    Registry.getDefault().getFtpManager().testConnection(
                        ! _ftpUnsecuredRadioButton.isSelected(),
                        _ftpsExplicitRadioButton.isSelected(),
                        _verifyServerCertCheckBox.isSelected(),
                        _hostNameTextField.getText(),
                        Integer.parseInt(_portNumberTextField.getText()),
                        _userNameTextField.getText(),
                        new String(_passwordField.getPassword()),
                        false,
                        0,
                        null,
                        _directoryTextField.getText(),
                        Integer.parseInt(_timeoutTextField.getText()) * 1000);
                    return Boolean.TRUE;
                }
            };

            final Boolean result = Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);
            if (result == Boolean.TRUE) {
                JOptionPane.showMessageDialog(
                        FtpAuditArchiverPropertiesDialog.this,
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
                            FtpAuditArchiverPropertiesDialog.this,
                            panel,
                            "FTP(S) Connection Failure",
                            JOptionPane.ERROR_MESSAGE);

                } else {
                    throw ExceptionUtils.wrap(cause);
                }
            }
        }
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.setVisible(true);
        FtpAuditArchiverPropertiesDialog d = new FtpAuditArchiverPropertiesDialog(f);
        d.setVisible(true);
        d.dispose();
        f.dispose();
    }

    public static boolean canEditAuditArchiverConfig() {
        final Authorizer authorizer = Registry.getDefault().getSecurityProvider();
        return authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.CLUSTER_PROPERTY, new ClusterProperty(AUDIT_ARCHIVER_CONFIG_CLUSTER_PROPERTY_NAME, ""))) ||
               authorizer.hasPermission(new AttemptedUpdate(EntityType.CLUSTER_PROPERTY, new ClusterProperty(AUDIT_ARCHIVER_CONFIG_CLUSTER_PROPERTY_NAME, "")));

    }
}
