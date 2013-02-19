package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.*;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import static com.l7tech.objectmodel.imp.PersistentEntityUtil.oid;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;

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
    private JCheckBox sftpPartialUploadsCheckBox;
    private JCheckBox sftpPartialDownloadsCheckBox;
    private JCheckBox retrieveFileSizeFromVarCheckBox;
    private TargetVariablePanel fileSizeVariablePanel;
    private JCheckBox sftpPUTCheckBox;
    private JCheckBox sftpGETCheckBox;
    private JCheckBox sftpLISTCheckBox;
    private JCheckBox sftpSTATCheckBox;
    private JCheckBox sftpDELETECheckBox;
    private JCheckBox sftpMOVECheckBox;
    private JCheckBox sftpMKDIRCheckBox;
    private JCheckBox sftpRMDIRCheckBox;
    private JCheckBox scpPUTCheckBox;
    private JCheckBox scpGETCheckBox;
    private JCheckBox deleteFileOnTruncateCheckBox;

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
        scpPUTCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_PUT,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableScpPut", true)));
        scpGETCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_GET,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableScpGet", false)));
        retrieveFileSizeFromVarCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_RETRIEVE_FILE_SIZE_FROM_VARIABLE,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableScpSizeVariable", false)));
        sftpCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftp", true)));
        sftpPUTCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpPut", true)));
        sftpGETCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpGet", false)));
        sftpLISTCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpList", false)));
        sftpSTATCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpStat", false)));
        sftpDELETECheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_DELETE,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpDelete", false)));
        sftpMOVECheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MOVE,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpMove", false)));
        sftpMKDIRCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MKDIR,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpMkdir", false)));
        sftpRMDIRCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_RMDIR,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.defaultEnableSftpRmdir", false)));
        sftpPartialUploadsCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.enableSftpPartialUpload", false)));
        sftpPartialDownloadsCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.enableSftpPartialDownload", false)));
        deleteFileOnTruncateCheckBox.setSelected(getBooleanProp(props, SshCredentialAssertion.LISTEN_PROP_DELETE_FILE_ON_TRUNCATE_REQUEST,
                ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.deleteFileOnTruncateRequest", false)));
        fileSizeVariablePanel.setVariable(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_SIZE_CONTEXT_VARIABLE_NAME,
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.enableSizeContextVariableName", "")));
        idleTimeoutMinsField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES,
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.idleTimeoutMinutes", "10")));
        maxConcurrentSessionsPerUserField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER,
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.defaultMaxConcurrentSessionsPerUser", "10")));
        maxConcurrentSessionsField.setText(getStringProp(props, SshCredentialAssertion.LISTEN_PROP_MAX_SESSIONS,
                // Not a mistake, we default to the per user value
                ConfigFactory.getProperty("com.l7tech.external.assertions.ssh.defaultMaxConcurrentSessionsPerUser", "10")));
        privateKeyField.setSelectedSecurePassword(getLongProp(props, SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, SecurePassword.DEFAULT_OID));
        enableDisableComponents();
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP, String.valueOf(scpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP, String.valueOf(sftpCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_PUT, String.valueOf(scpPUTCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_GET, String.valueOf(scpGETCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_RETRIEVE_FILE_SIZE_FROM_VARIABLE, String.valueOf(retrieveFileSizeFromVarCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(sftpPUTCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET, String.valueOf(sftpGETCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(sftpLISTCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(sftpSTATCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_DELETE, String.valueOf(sftpDELETECheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MOVE, String.valueOf(sftpMOVECheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MKDIR, String.valueOf(sftpMKDIRCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_RMDIR, String.valueOf(sftpRMDIRCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS, String.valueOf(sftpPartialUploadsCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS, String.valueOf(sftpPartialDownloadsCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_DELETE_FILE_ON_TRUNCATE_REQUEST, String.valueOf(deleteFileOnTruncateCheckBox.isSelected()));
        data.put(SshCredentialAssertion.LISTEN_PROP_SIZE_CONTEXT_VARIABLE_NAME, fileSizeVariablePanel.getVariable());
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

        //Validate that file size context variable is valid
        if(retrieveFileSizeFromVarCheckBox.isSelected() && !fileSizeVariablePanel.isEntryValid()) {
            return "Error with file size context variable: " + fileSizeVariablePanel.getErrorMessage();
        }

        //If both SFTP PUT and GET are check either LIST or STAT also needs to be enabled.
        if(sftpPUTCheckBox.isSelected() && sftpGETCheckBox.isSelected() && !sftpLISTCheckBox.isSelected() && !sftpSTATCheckBox.isSelected()){
            return "Both SFTP PUT and GET commands are enabled. Either SFTP LIST or SFTP STAT must also be enabled.";
        }

        //If SFTP is enabled at least one of the commands should be selected.
        if(sftpCheckBox.isSelected() && !(
                sftpGETCheckBox.isSelected() ||
                sftpPUTCheckBox.isSelected() ||
                sftpLISTCheckBox.isSelected() ||
                sftpSTATCheckBox.isSelected() ||
                sftpDELETECheckBox.isSelected() ||
                sftpMOVECheckBox.isSelected() ||
                sftpMKDIRCheckBox.isSelected() ||
                sftpRMDIRCheckBox.isSelected())) {
            return "SFTP is enabled but none of the SFTP commands are enabled. Enable at least one of the SFTP commands.";
        }

        //If SCP is enabled at least one of the commands should be selected.
        if(scpCheckBox.isSelected() && !(scpGETCheckBox.isSelected() || scpPUTCheckBox.isSelected())) {
            return "SCP is enabled but none of the SCP commands are enabled. Enable at least one of the SCP commands.";
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
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_PUT,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_GET,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_RETRIEVE_FILE_SIZE_FROM_VARIABLE,
            SshCredentialAssertion.LISTEN_PROP_SIZE_CONTEXT_VARIABLE_NAME,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_DELETE,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MOVE,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MKDIR,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_RMDIR,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS,
            SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS,
            SshCredentialAssertion.LISTEN_PROP_DELETE_FILE_ON_TRUNCATE_REQUEST,
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

        privateKeyField.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());

        // load private key type (password type loaded by default by SecurePasswordComboBox constructor)
        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);

        managePasswordsPrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final SsgConnectorPropertiesDialog ownerJDialog = getSsgConnectorPropertiesDialog();
                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(ownerJDialog);
                dialog.pack();
                Utilities.centerOnParentWindow(dialog);
                final Option<Long> selectedPrivateKeyOid = optional( privateKeyField.getSelectedSecurePassword() ).map( oid() );
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                        if (selectedPrivateKeyOid.isSome()) privateKeyField.setSelectedSecurePassword(selectedPrivateKeyOid.some());
                        if (ownerJDialog != null) {
                            DialogDisplayer.pack(ownerJDialog);
                        }
                    }
                });
            }
        });

        sftpCheckBox.addActionListener(enableDisableComponentsActionListener);
        scpCheckBox.addActionListener(enableDisableComponentsActionListener);
        sftpPUTCheckBox.addActionListener(enableDisableComponentsActionListener);
        sftpGETCheckBox.addActionListener(enableDisableComponentsActionListener);
        sftpDELETECheckBox.addActionListener(enableDisableComponentsActionListener);
        scpGETCheckBox.addActionListener(enableDisableComponentsActionListener);
        retrieveFileSizeFromVarCheckBox.addActionListener(enableDisableComponentsActionListener);

        enableDisableComponents();
    }

    private ActionListener enableDisableComponentsActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableDisableComponents();
        }
    };

    private void enableDisableComponents(){
        sftpPUTCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpGETCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpLISTCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpSTATCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpDELETECheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpMOVECheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpMKDIRCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpRMDIRCheckBox.setEnabled(sftpCheckBox.isSelected());
        sftpPartialUploadsCheckBox.setEnabled(sftpCheckBox.isSelected() && sftpPUTCheckBox.isSelected());
        sftpPartialDownloadsCheckBox.setEnabled(sftpCheckBox.isSelected() && sftpGETCheckBox.isSelected());
        deleteFileOnTruncateCheckBox.setEnabled(sftpCheckBox.isSelected() && sftpDELETECheckBox.isSelected());

        scpPUTCheckBox.setEnabled(scpCheckBox.isSelected());
        scpGETCheckBox.setEnabled(scpCheckBox.isSelected());
        retrieveFileSizeFromVarCheckBox.setEnabled(scpCheckBox.isSelected() && scpGETCheckBox.isSelected());
        fileSizeVariablePanel.setEnabled(scpCheckBox.isSelected() && scpGETCheckBox.isSelected() && retrieveFileSizeFromVarCheckBox.isSelected());
    }

    private String nullIfEmpty( final String value ) {
        String result = value;
        if ( value != null && value.trim().isEmpty() ) {
            result = null;
        }
        return result;
    }
}
