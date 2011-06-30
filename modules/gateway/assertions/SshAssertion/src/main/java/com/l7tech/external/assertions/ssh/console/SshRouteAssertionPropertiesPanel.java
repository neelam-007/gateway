package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.RoutingDialogUtils;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gui.NumberField;
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

/**
 * Created by IntelliJ IDEA.
 * User: cmclaughlin
 * Date: 20-April-2011
 * Time: 16:34:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class SshRouteAssertionPropertiesPanel extends AssertionPropertiesOkCancelSupport<SshRouteAssertion> {
    private JPanel mainPanel;
    private JRadioButton usernamePasswordRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JTextField hostField;
    private JTextField usernameField;
    private JScrollPane privateKeyScrollPane;
    private JTextArea privateKeyField;
    private JCheckBox privateKeyRequiresPasswordCheckbox;
    private JButton loadPrivateKeyFromFileButton;
    private SecurePasswordComboBox passwordField;
    private JButton managePasswordsButton;
    private JTextField directoryTextField;
    private JComboBox messageSource;
    private JPanel usernamePanel;
    private JPanel privateKeyPanel;
    private JRadioButton specifyPatternRadioButton;
    private JRadioButton autoGenerateFileNameRadioButton;
    private JTextField filenamePatternTextField;
    private JTextField secondsTextField;
    private JTextField timeoutTextField;
    private JTextField portNumberTextField;
    private JRadioButton wssIgnoreButton;
    private JRadioButton wssCleanupButton;
    private JRadioButton wssRemoveButton;
    private JTabbedPane tabbedPane1;
    private JCheckBox validateServerSHostCheckBox;
    private JButton manageHostKeyButton;

    private SshRouteAssertion assertion;
    private InputValidator validators;
    private AbstractButton[] secHdrButtons = { wssIgnoreButton, wssCleanupButton, wssRemoveButton, null };

    private String hostKey;

    private static final ResourceBundle resources = ResourceBundle.getBundle( SshRouteAssertionPropertiesPanel.class.getName() );
    public static final int DEFAULT_PORT_FTP = 22;

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

        this.assertion = assertion;
    }

    @Override
    protected void initComponents() {

        // initialise
        final ActionListener securityListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableFields();
                final int port = getDefaultPortNumber();
                portNumberTextField.setText(Integer.toString(port));
            }
        };
        usernamePasswordRadioButton.addActionListener(enableDisableListener);
        privateKeyRadioButton.addActionListener(enableDisableListener);
        privateKeyRequiresPasswordCheckbox.addActionListener(enableDisableListener);
        validateServerSHostCheckBox.addActionListener(enableDisableListener);

        loadPrivateKeyFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        manageHostKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SshRouteAssertionPropertiesPanel.this, hostKey);
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

        timeoutTextField.setDocument(new NumberField(6));
        final ActionListener filenameListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filenamePatternTextField.setEnabled(specifyPatternRadioButton.isSelected());
                enableDisableFields();
            }
        };
        autoGenerateFileNameRadioButton.addActionListener(filenameListener);
        specifyPatternRadioButton.addActionListener(filenameListener);

        filenamePatternTextField.getDocument().addDocumentListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(filenamePatternTextField);
        
        messageSource.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", null), null, false ) );
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        //validators
        validators = new InputValidator(this, getResourceString("errorTitle"));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"),hostField,null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("specifyPatternLabel"),filenamePatternTextField,null));
        validators.addRule(validators.constrainTextFieldToBeNonEmpty(getResourceString("usernameLabel"),usernameField,null));
        validators.addRule(validators.constrainTextFieldToNumberRange(getResourceString("sshTimeoutLabel"), timeoutTextField, 2, 10));

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

                if(usernamePasswordRadioButton.isSelected() || privateKeyRequiresPasswordCheckbox.isSelected()) {
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
    }

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
        privateKeyScrollPane.setEnabled(privateKeyRadioButton.isSelected());
        privateKeyField.setEnabled(privateKeyRadioButton.isSelected());
        privateKeyRequiresPasswordCheckbox.setEnabled(privateKeyRadioButton.isSelected());
        loadPrivateKeyFromFileButton.setEnabled(privateKeyRadioButton.isSelected());
        passwordField.setEnabled(usernamePasswordRadioButton.isSelected() || privateKeyRequiresPasswordCheckbox.isSelected());
        managePasswordsButton.setEnabled(usernamePasswordRadioButton.isSelected() || privateKeyRequiresPasswordCheckbox.isSelected());
        if(!validateServerSHostCheckBox.isSelected()) {
            manageHostKeyButton.setEnabled(false);
        } else {
            manageHostKeyButton.setEnabled(true);
        }
    }

    @Override
    public void setData(SshRouteAssertion assertion) {

        //populate SSH settings
        messageSource.setModel( buildMessageSourceComboBoxModel(assertion) );
        messageSource.setSelectedItem( new MessageTargetableSupport(assertion.getRequestTarget()) );
        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);

        if(assertion.getHost() != null) {
            hostField.setText(assertion.getHost().trim());
        }
        if(assertion.getPort() != null) {
            portNumberTextField.setText(assertion.getPort().trim());
        }
        if(assertion.getDirectory() != null) {
                directoryTextField.setText(assertion.getDirectory().trim());
        }
        
        if (assertion.getFileNameSource() == null || assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
           autoGenerateFileNameRadioButton.doClick(0);
        } else if (assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
           specifyPatternRadioButton.doClick(0);
        }
        filenamePatternTextField.setText(assertion.getFileNamePattern() == null ? "" : assertion.getFileNamePattern());

        timeoutTextField.setText(Integer.toString(assertion.getTimeout() / 1000));

        // populate authorization settings
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
        
        if(assertion.getPasswordOid() != null) {
            passwordField.setSelectedSecurePassword(assertion.getPasswordOid());
            privateKeyRequiresPasswordCheckbox.setSelected(true);
        } else {
            privateKeyRequiresPasswordCheckbox.setSelected(false);
        }
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

        if (autoGenerateFileNameRadioButton.isSelected()) {
            assertion.setFileNameSource(FtpFileNameSource.AUTO);
            filenamePatternTextField.setText("");
        } else if (specifyPatternRadioButton.isSelected()) {
            assertion.setFileNameSource(FtpFileNameSource.PATTERN);
        }
        assertion.setFileNamePattern(filenamePatternTextField.getText());
         if (timeoutTextField.getText().trim().isEmpty()) {
            timeoutTextField.setText(Integer.toString(SshRouteAssertion.DEFAULT_TIMEOUT / 1000));
        }
        assertion.setTimeout(Integer.parseInt(timeoutTextField.getText()) * 1000);

        if(usernamePasswordRadioButton.isSelected()) {
            assertion.setUsePrivateKey(false);
        } else {
            assertion.setUsePrivateKey(true);
            assertion.setPrivateKey(privateKeyField.getText());
        }

        // populate authorization settings
        assertion.setUsername(usernameField.getText().trim());
       if(usernamePasswordRadioButton.isSelected() || privateKeyRequiresPasswordCheckbox.isSelected()) {
            assertion.setPasswordOid(passwordField.getSelectedSecurePassword().getOid());
        } else {
            assertion.setPasswordOid(null);
        }

        if(validateServerSHostCheckBox.isSelected() && hostKey != null) {
            assertion.setUsePublicKey(true);
            assertion.setSshPublicKey(hostKey);
        } else {
            assertion.setUsePublicKey(false);
            assertion.setSshPublicKey(null);
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
        int port = DEFAULT_PORT_FTP;
        return port;
    }
}
