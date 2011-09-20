package com.l7tech.external.assertions.ssh.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.RoutingDialogUtils;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class SshRouteAssertionPropertiesPanel extends AssertionPropertiesOkCancelSupport<SshRouteAssertion> {
    public static final int DEFAULT_PORT_SSH = 22;
    private static final ResourceBundle resources = ResourceBundle.getBundle( SshRouteAssertionPropertiesPanel.class.getName());

    private JPanel mainPanel;
    private JRadioButton usernamePasswordRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JTextField hostField;
    private JTextField usernameField;
    private JScrollPane privateKeyScrollPane;
    private JTextArea privateKeyField;
    private JButton loadPrivateKeyFromFileButton;
    private SecurePasswordComboBox passwordField;
    private JButton managePasswordsButton;
    private JTextField directoryTextField;
    private JComboBox messageSource;
    private JTextField fileNameTextField;
    private JTextField connectTimeoutTextField;
    private JTextField portNumberTextField;
    private JRadioButton wssIgnoreButton;
    private JRadioButton wssCleanupButton;
    private JRadioButton wssRemoveButton;
    private JCheckBox validateServerSHostCheckBox;
    private JButton manageHostKeyButton;
    private JRadioButton SCPRadioButton;
    private JRadioButton SFTPRadioButton;
    private JRadioButton uploadToRadioButton;
    private JRadioButton downloadFromRadioButton;
    private JRadioButton passThroughCredentialsInRadioButton;
    private JRadioButton specifyUserCredentialsRadioButton;
    private JTextField readTimeoutTextField;
    private JComboBox contentTypeComboBox;
    private JPanel specifyUserCredentialsPanel;
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
        usernamePasswordRadioButton.addActionListener(enableDisableListener);
        privateKeyRadioButton.addActionListener(enableDisableListener);
        validateServerSHostCheckBox.addActionListener(enableDisableListener);
        passThroughCredentialsInRadioButton.addActionListener(enableDisableListener);
        specifyUserCredentialsRadioButton.addActionListener(enableDisableListener);
        fileNameTextField.getDocument().addDocumentListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(fileNameTextField);

        loadPrivateKeyFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

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

        managePasswordsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                passwordField.reloadPasswordList();
            }
        });

        final ActionListener downloadUploadRadioListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableFields();
            }
        };
        downloadFromRadioButton.addActionListener(downloadUploadRadioListener);
        uploadToRadioButton.addActionListener(downloadUploadRadioListener);
        
        messageSource.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", null), null, false ) );
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

        //validators
        validators = new InputValidator(this, getResourceString("errorTitle"));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"), hostField, null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("fileNameLabel"), fileNameTextField, null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"), usernameField, null));
        validators.addRule(validators.constrainTextFieldToNumberRange(getResourceString("sshTimeoutLabel"), connectTimeoutTextField, 2, 10));

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

                if(usernamePasswordRadioButton.isSelected()) {
                    if (passwordField == null || passwordField.getItemCount() == 0)
                    {
                        return getResourceString("passwordEmptyError");
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

        super.initComponents();
    }   // initComponents()

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doRead(fc, false);
            }
        });
    }


    private void doRead(JFileChooser dlg, boolean publicKey) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            privateKeyField.setText(new String(IOUtils.slurpFile(new File(filename))));
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    private void enableDisableFields() {
        // connection tab
        if(!validateServerSHostCheckBox.isSelected()) {
                manageHostKeyButton.setEnabled(false);
            } else {
                manageHostKeyButton.setEnabled(true);
            }
            if (downloadFromRadioButton.isSelected()) {
                contentTypeComboBox.setEnabled(true);
                readTimeoutTextField.setEnabled(true);
                messageSource.setEnabled(false);
            } else {
                contentTypeComboBox.setEnabled(false);
                readTimeoutTextField.setEnabled(false);
                messageSource.setEnabled(true);
            }

        // authentication tab
        specifyUserCredentialsPanel.setEnabled(specifyUserCredentialsRadioButton.isSelected());
        if (specifyUserCredentialsRadioButton.isSelected()) {
            usernameField.setEnabled(true);
            usernamePasswordRadioButton.setEnabled(true);
            privateKeyRadioButton.setEnabled(true);
            privateKeyScrollPane.setEnabled(privateKeyRadioButton.isSelected());
            privateKeyField.setEnabled(privateKeyRadioButton.isSelected());
            loadPrivateKeyFromFileButton.setEnabled(privateKeyRadioButton.isSelected());
            passwordField.setEnabled(usernamePasswordRadioButton.isSelected());
            managePasswordsButton.setEnabled(usernamePasswordRadioButton.isSelected());
        } else {
            usernameField.setEnabled(false);
            usernamePasswordRadioButton.setEnabled(false);
            privateKeyRadioButton.setEnabled(false);
            privateKeyScrollPane.setEnabled(false);
            privateKeyField.setEnabled(false);
            loadPrivateKeyFromFileButton.setEnabled(false);
            passwordField.setEnabled(false);
            managePasswordsButton.setEnabled(false);
        }
    }

    /*
     * Populate fields in SSH Route dialog.
     */
    @Override
    public void setData(SshRouteAssertion assertion) {
        messageSource.setModel(buildMessageSourceComboBoxModel(assertion));
        messageSource.setSelectedItem(new MessageTargetableSupport(assertion.getRequestTarget()));
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

        if(assertion.isUsePrivateKey()) {
            privateKeyRadioButton.setSelected(true);
            privateKeyField.setText(assertion.getPrivateKey() == null ? "" : assertion.getPrivateKey());
        } else {
            usernamePasswordRadioButton.setSelected(true);
        }
        usernameField.setText(assertion.getUsername() == null ? "" : assertion.getUsername());

        if(assertion.isUsePublicKey() && assertion.getSshPublicKey() != null) {
            validateServerSHostCheckBox.setSelected(true);
            hostKey = assertion.getSshPublicKey();
        } else {
            validateServerSHostCheckBox.setSelected(false);
            hostKey = null;
        }

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

        assertion.setRequestTarget((MessageTargetableSupport) messageSource.getSelectedItem());
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

        if(usernamePasswordRadioButton.isSelected()) {
            assertion.setUsePrivateKey(false);
        } else {
            assertion.setUsePrivateKey(true);
            assertion.setPrivateKey(privateKeyField.getText());
        }

        if(validateServerSHostCheckBox.isSelected() && hostKey != null) {
            assertion.setUsePublicKey(true);
            assertion.setSshPublicKey(hostKey);
        } else {
            assertion.setUsePublicKey(false);
            assertion.setSshPublicKey(null);
        }

        // populate authorization settings

        assertion.setCredentialsSourceSpecified(specifyUserCredentialsRadioButton.isSelected());
        if (specifyUserCredentialsRadioButton.isSelected()) {
            assertion.setUsername(usernameField.getText().trim());
            if(usernamePasswordRadioButton.isSelected()) {
                assertion.setPasswordOid(passwordField.getSelectedSecurePassword().getOid());
            } else {
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
            isValid = port > 0 && port < 65535;
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
        int port = DEFAULT_PORT_SSH;
        return port;
    }
}
