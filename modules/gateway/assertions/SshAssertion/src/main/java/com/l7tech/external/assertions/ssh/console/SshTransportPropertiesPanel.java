package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ConfigFactory;
import org.apache.commons.lang.StringUtils;

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
    private JTextField maxConcurrentSessionsPerUserField;
    private JTextField hostPrivateKeyTypeField;
    private JButton setHostPrivateKeyButton;
    private JTextField idleTimeoutMinsField;

    private String hostPrivateKey;
    private String encryptedHostPrivateKey;
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

        encryptedHostPrivateKey = props.get(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY);
        try {
            hostPrivateKey = String.valueOf(Registry.getDefault().getTrustedCertManager().decryptPassword(encryptedHostPrivateKey));

            if (!StringUtils.isEmpty(hostPrivateKey)) {
                String determinedAlgorithm = SshKeyUtil.getPemPrivateKeyAlgorithm(hostPrivateKey);
                if (determinedAlgorithm != null) {
                    hostPrivateKeyTypeField.setText(SshKeyUtil.PEM + " " + determinedAlgorithm);
                } else {
                    hostPrivateKeyTypeField.setText("Unknown type");
                }
            }
        } catch (Exception e) {
            // validation error will occur if host private key is empty
        }
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP, String.valueOf(scpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP, String.valueOf(sftpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES, idleTimeoutMinsField.getText());
        data.put(SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER, maxConcurrentSessionsPerUserField.getText());
        data.put(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, encryptedHostPrivateKey);
        return data;
    }

    @Override
    public String getValidationError() {
        if (StringUtils.isEmpty(hostPrivateKey)) {
            return "The Host private key field must not be empty.";
        }

        try {
            encryptedHostPrivateKey = Registry.getDefault().getTrustedCertManager().encryptPassword(hostPrivateKey.toCharArray());
        } catch (FindException fe) {
            return "Cannot encrypt the given private key.  Please try a different private key.";
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
            SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY,
        };
    }

    protected void initComponents() {
        validator = new InputValidator(this, "SSH Transport Properties Validation");
        validator.addRule(validator.constrainTextFieldToNumberRange(
                "Max. concurrent session(s) per user", idleTimeoutMinsField, 0, Integer.MAX_VALUE));
        validator.addRule(validator.constrainTextFieldToNumberRange(
                "Idle timeout (in minutes)", maxConcurrentSessionsPerUserField, 0, Integer.MAX_VALUE));

        setHostPrivateKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SwingUtilities.getWindowAncestor(
                        SshTransportPropertiesPanel.this), null, HostKeyDialog.HostKeyValidationType.VALIDATE_PEM_PRIVATE_KEY_FORMAT);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isConfirmed()) {
                            hostPrivateKey = dialog.getHostKey();
                            String determinedAlgorithm = SshKeyUtil.getPemPrivateKeyAlgorithm(hostPrivateKey);
                            if (determinedAlgorithm != null) {
                                hostPrivateKeyTypeField.setText(SshKeyUtil.PEM + " " + determinedAlgorithm);
                            } else {
                                hostPrivateKeyTypeField.setText("Unknown type");
                            }
                        }
                    }
                });
            }
        });
    }
}
