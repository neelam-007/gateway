package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ConfigFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SshTransportPropertiesPanel extends CustomTransportPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox scpCheckBox;
    private JCheckBox sftpCheckBox;
    private JTextField maxConcurrentSessionsField;
    private JTextField maxConcurrentSessionsPerUserField;
    private JTextField idleTimeoutMinsField;
    private SecurePasswordComboBox privateKeyField;
    private JButton managePasswordsPrivateKeysButton;

    private InputValidator validator;

    public SshTransportPropertiesPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initComponents();
    }

    private boolean getBooleanProp(Map<String, String> map, String key, boolean dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        return Boolean.parseBoolean(val);
    }

    private long getLongProp(Map<String, String> map, String key, long dflt) {
        long result = dflt;
        String val = map.get(key);
        if (val != null) {
            try {
                result = Long.parseLong(val);
            } catch (NumberFormatException e) {
                // do nothing, use default value
            }
        }
        return result;
    }

    private String getStringProp(Map<String, String> map, String key, String dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        return val;
    }

    @Override
    public void setData(Map<String, String> props) {
        scpCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableScp", true)));
        sftpCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftp", true)));
        idleTimeoutMinsField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES,
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.idleTimeoutMinutes", "10")));
        maxConcurrentSessionsPerUserField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER,
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.defaultMaxConcurrentSessionsPerUser", "10")));
        maxConcurrentSessionsField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_MAX_SESSIONS,
                // Not a mistake, we default to the per user value
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.defaultMaxConcurrentSessionsPerUser", "10")));
        privateKeyField.setSelectedSecurePassword(getLongProp(props, SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, SecurePassword.DEFAULT_OID));
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP, String.valueOf(scpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP, String.valueOf(sftpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES, nullIfEmpty( idleTimeoutMinsField.getText() ));
        data.put(SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER, nullIfEmpty( maxConcurrentSessionsPerUserField.getText() ));
        data.put(SshCredentialAssertion.LISTEN_PROP_MAX_SESSIONS, nullIfEmpty( maxConcurrentSessionsField.getText() ));
        data.put(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, String.valueOf(privateKeyField.getSelectedSecurePassword().getOid()));
        return data;
    }

    @Override
    public String getValidationError() {
        if (privateKeyField.getSelectedSecurePassword() == null) {
            return "The Host private key field must not be empty.";
        }

        String validationError = validator.validate();
        if(validationError != null){
            return validationError;
        }

        return null;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return new String[] {
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP,
            SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES,
            SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER,
            SshCredentialAssertion.LISTEN_PROP_MAX_SESSIONS,
            SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY,
        };
    }

    protected void initComponents() {
        validator = new InputValidator(this, "SSH Transport Properties Validation");
        validator.addRule(validator.constrainTextFieldToNumberRange(
                "Max. concurrent session(s)", maxConcurrentSessionsField, 0L, (long) Integer.MAX_VALUE ));
        validator.addRule(validator.constrainTextFieldToNumberRange(
                "Max. concurrent session(s) per user", idleTimeoutMinsField, 0L, (long) Integer.MAX_VALUE ));
        validator.addRule(validator.constrainTextFieldToNumberRange(
                "Idle timeout (in minutes)", maxConcurrentSessionsPerUserField, 0L, (long) Integer.MAX_VALUE ));

        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);

        managePasswordsPrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final Window parentWindow = SwingUtilities.getWindowAncestor(SshTransportPropertiesPanel.this);
                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(parentWindow);
                dialog.pack();
                Utilities.centerOnParentWindow(dialog);
                DialogDisplayer.display(dialog);
                privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                if (parentWindow != null) {
                    parentWindow.pack();
                }
            }
        });
    }

    private String nullIfEmpty( final String value ) {
        String result = value;
        if ( value != null && value.trim().isEmpty() ) {
            result = null;
        }
        return result;
    }
}
