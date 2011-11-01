package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.panels.ServiceComboBox;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListenerConstants;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SFTP polling listener properties dialog.
 */
public class SftpPollingListenerPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField nameField;
    private JCheckBox enabledCheckBox;
    private JRadioButton passwordRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox validateServerSHostCheckBox;
    private JButton manageHostKeyButton;
    private JTextField usernameField;
    private SecurePasswordComboBox passwordField;
    private SecurePasswordComboBox privateKeyField;
    private JButton managePasswordsPrivateKeysButton;
    private JTextField directoryField;
    private JTextField contentTypeField;
    private JSpinner pollingIntervalField;
    private JCheckBox enableResponsesCheckBox;
    private JCheckBox deleteProcessedMessagesCheckBox;
    private JCheckBox hardwiredServiceCheckBox;
    private JComboBox serviceNameComboBox;
    private JButton cancelButton;
    private JButton okButton;

    private String hostKey;
    private boolean isNew;
    private SftpPollingListenerDialogSettings configuration;
    private boolean confirmed = false;

    private Logger logger = Logger.getLogger(SftpPollingListenerPropertiesDialog.class.getName());

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    public SftpPollingListenerPropertiesDialog(Frame owner, SftpPollingListenerDialogSettings configuration, boolean isNew) {
        super(owner, isNew ? "Create a New SFTP Polling Listener" : "Edit an SFTP Polling Listener", true);
        this.isNew = isNew;
        this.configuration = configuration;
        initialize();
    }

    public SftpPollingListenerPropertiesDialog(Dialog owner, SftpPollingListenerDialogSettings configuration, boolean isNew) {
        super(owner, isNew ? "Create a New SFTP Polling Listener" : "Edit an SFTP Polling Listener", true);
        this.isNew = isNew;
        this.configuration = configuration;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);

        nameField.setDocument(new MaxLengthDocument(255));
        nameField.getDocument().addDocumentListener(enableDisableListener);
        hostField.setDocument(new MaxLengthDocument(255));
        hostField.getDocument().addDocumentListener(enableDisableListener);
        portField.setDocument(new MaxLengthDocument(5));
        portField.getDocument().addDocumentListener(enableDisableListener);
        validateServerSHostCheckBox.addActionListener((enableDisableListener));
        usernameField.setDocument(new MaxLengthDocument(255));
        usernameField.getDocument().addDocumentListener(enableDisableListener);
        passwordField.addActionListener(enableDisableListener);
        privateKeyField.addActionListener(enableDisableListener);
        directoryField.getDocument().addDocumentListener( enableDisableListener );
        contentTypeField.getDocument().addDocumentListener( enableDisableListener );

        pollingIntervalField.setModel(new SpinnerNumberModel(60, 1, Integer.MAX_VALUE, 5));

        ActionListener authTypeActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.setEnabled(passwordRadioButton.isSelected());
                privateKeyField.setEnabled(privateKeyRadioButton.isSelected());
            }
        };
        passwordRadioButton.addActionListener(authTypeActionListener);
        privateKeyRadioButton.addActionListener(authTypeActionListener);

        manageHostKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SftpPollingListenerPropertiesDialog.this, hostKey,
                        HostKeyDialog.HostKeyValidationType.VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if(dialog.isConfirmed()) {
                            hostKey = dialog.getHostKey();
                            enableOrDisableComponents();
                        }
                    }
                });
            }
        });

        passwordField.reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
        privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        managePasswordsPrivateKeysButton.addActionListener(enableDisableListener);
        managePasswordsPrivateKeysButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                passwordField.reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
                privateKeyField.reloadPasswordList(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                pack();
            }
        });

        ServiceComboBox.populateAndSelect(serviceNameComboBox, false, 0);
        hardwiredServiceCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serviceNameComboBox.setEnabled(hardwiredServiceCheckBox.isSelected());
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        pack();
        setData();
        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {
        boolean enableOkButton = true;

        if(nameField.getText() == null || nameField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        if(hostField.getText() == null || hostField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        try {
            int port = Integer.parseInt(portField.getText().trim());
            if(port <= 0 || port > 65535) {
                enableOkButton = false;
            }
        } catch (Exception e) {
            enableOkButton = false;
        }

        if(!validateServerSHostCheckBox.isSelected()) {
            manageHostKeyButton.setEnabled(false);
        } else {
            manageHostKeyButton.setEnabled(true);
            if(hostKey == null) {
                enableOkButton = false;
            }
        }

        if(usernameField.getText() == null || usernameField.getText().length() == 0) {
            enableOkButton = false;
        }

        if(passwordRadioButton.isSelected()) {
            if(passwordField.getSelectedSecurePassword() == null) {
                enableOkButton = false;
            }
            privateKeyField.setEnabled(false);
        } else if(privateKeyRadioButton.isSelected()) {
            if(privateKeyField.getSelectedSecurePassword() == null) {
                enableOkButton = false;
            }
            passwordField.setEnabled(false);
        }

        if(directoryField.getText() == null || directoryField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        if(contentTypeField.getText() == null || contentTypeField.getText().trim().length() == 0) {
            enableOkButton = false;
        }

        if(hardwiredServiceCheckBox.isSelected() && serviceNameComboBox.getSelectedItem() == null) {
            enableOkButton = false;
        }

        okButton.setEnabled(enableOkButton);
    }

    private void setData() {
        nameField.setText(configuration.getName() == null ? "" : configuration.getName().trim());

        enabledCheckBox.setSelected(isNew ? true : configuration.isActive());

        hostField.setText(configuration.getHostname() == null ? "" : configuration.getHostname().trim());
        portField.setText(Integer.toString(configuration.getPort()));
        if(configuration.getHostKey() != null) {
            validateServerSHostCheckBox.setSelected(true);
            hostKey = configuration.getHostKey();
        } else {
            validateServerSHostCheckBox.setSelected(false);
            hostKey = null;
        }
        usernameField.setText(configuration.getUsername() == null ? "" : configuration.getUsername().trim());

        if(configuration.getPasswordOid() != null) {
            passwordField.setSelectedSecurePassword(configuration.getPasswordOid());
            passwordRadioButton.setSelected(true);
        } else if (configuration.getPrivateKeyOid() != null) {
            privateKeyField.setSelectedSecurePassword(configuration.getPrivateKeyOid());
            privateKeyRadioButton.setSelected(true);
        } else {
           passwordRadioButton.setSelected(true);
        }

        directoryField.setText(configuration.getDirectory() == null ? "" : configuration.getDirectory());
        contentTypeField.setText(configuration.getContentType() == null ? "" : configuration.getContentType());
        pollingIntervalField.setValue(configuration.getPollingInterval());

        enableResponsesCheckBox.setSelected(configuration.isEnableResponses());
        deleteProcessedMessagesCheckBox.setSelected(configuration.isDeleteOnReceive());

        if(configuration.isHardwiredService()) {
            hardwiredServiceCheckBox.setSelected(true);
            if(configuration.getHardwiredServiceId() != null) {
                ServiceComboBox.populateAndSelect(serviceNameComboBox, true, configuration.getHardwiredServiceId());
            }
        } else {
            hardwiredServiceCheckBox.setSelected(false);
        }
        serviceNameComboBox.setEnabled(hardwiredServiceCheckBox.isSelected());

        enableOrDisableComponents();
    }

    private SftpPollingListenerDialogSettings getConfigurationFromDialog() {
        SftpPollingListenerDialogSettings c = new SftpPollingListenerDialogSettings();
        c.setResId(configuration.getResId());
        c.setVersion(configuration.getVersion());

        c.setName(nameField.getText() == null || nameField.getText().trim().length() == 0 ? null : nameField.getText().trim());

        c.setActive(enabledCheckBox.isSelected());

        c.setHostname(hostField.getText() == null || hostField.getText().trim().length() == 0 ? null : hostField.getText().trim());
        c.setPort(Integer.parseInt(portField.getText().trim()));

        if(validateServerSHostCheckBox.isSelected() && hostKey != null) {
            c.setHostKey(hostKey);
        } else {
            c.setHostKey(null);
        }

        c.setUsername(usernameField.getText() == null || usernameField.getText().trim().length() == 0 ? null : usernameField.getText().trim());

        if(passwordRadioButton.isSelected()) {
            if(passwordField.getSelectedSecurePassword() != null) {
                c.setPasswordOid(passwordField.getSelectedSecurePassword().getOid());
            }
        } else if(privateKeyRadioButton.isSelected()) {
            c.setPrivateKeyOid(privateKeyField.getSelectedSecurePassword().getOid());
        }

        c.setDirectory(directoryField.getText() == null || directoryField.getText().trim().length() == 0 ? null : directoryField.getText().trim());
        c.setContentType(contentTypeField.getText() == null || contentTypeField.getText().trim().length() == 0 ? null : contentTypeField.getText().trim());
        c.setPollingInterval(((Number)pollingIntervalField.getValue()).intValue());

        c.setEnableResponses(enableResponsesCheckBox.isSelected());
        c.setDeleteOnReceive(deleteProcessedMessagesCheckBox.isSelected());

        if(hardwiredServiceCheckBox.isSelected()) {
            c.setHardwiredService(true);
            PublishedService service = ServiceComboBox.getSelectedPublishedService(serviceNameComboBox);
            c.setHardwiredServiceId(service == null ? null : service.getOid());
        } else {
            c.setHardwiredService(false);
            c.setHardwiredServiceId(null);
        }

        return c;
    }

    private List<SftpPollingListenerDialogSettings> loadConfigurations(){
        List<SftpPollingListenerDialogSettings> configurations = null;

        ClusterProperty property = getClusterProperty();

        if(property!=null){
            //load the MqResources from the property string!
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SftpPollingListenerPropertiesDialog.class.getClassLoader());

                SftpPollingListenerXmlUtilities xmlUtil = new SftpPollingListenerXmlUtilities();
                configurations = xmlUtil.unmarshallFromXMLString(property.getValue());
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        }

        return configurations;
    }

    private void onSave() {
        confirmed = false;
        
        boolean clusterPropExists = false;
        //existing connections in the cluster properties
        List<SftpPollingListenerDialogSettings> configurations = loadConfigurations();
        ClusterProperty property = getClusterProperty();
        if(configurations != null)
                clusterPropExists = true;

        try {
            //update the configuration in our dialog
            SftpPollingListenerDialogSettings formConfiguration = getConfigurationFromDialog();

            //set the res id;
            if(configurations == null || configurations.size() == 0) {
                formConfiguration.setResId(0);
            } else if(isNew) {
                SftpPollingListenerDialogSettings c = configurations.get(configurations.size() - 1);
                long lastInt = c.getResId();
                formConfiguration.setResId(lastInt+1);
            }

            //increment the ID if it is a resource that is being edited.
            if(!isNew) {
                int ver = formConfiguration.getVersion();
                formConfiguration.setVersion(++ver);
            }

            SftpPollingListenerDialogSettings oneInCluster = null;
            //Update the MqResourceType in the connection list and save the list to xml property.
            if(configurations != null) {
                if(formConfiguration != null) {
                    //check for matching resId.  if match, update the existing MqResourceType object and write
                    //back to the cluster property.
                    //If no match, create a new one and add to the list, then write to the clsuter property.
                    oneInCluster = getMatchingOneFromThisList(configurations, formConfiguration);
                    if(oneInCluster != null) {
                        //update the cluster one and set back in the cluster properties
                        formConfiguration.copyPropertiesToResource(oneInCluster);
                        int match = getIndexForMatchingOneFromThisList(configurations, oneInCluster);
                        if(match >= 0)
                            configurations.set(match, oneInCluster);
                    } else {
                        //create a new one and add to the cluster properties list
                        configurations.add(formConfiguration);
                    }
                }
            } else {
                configurations = new ArrayList<SftpPollingListenerDialogSettings>();
                if(formConfiguration != null)
                    configurations.add(formConfiguration);
            }

            //get the XML for the connection list
            String xml = marshallToXMLClassLoaderSafe(configurations);
            //check the cluster property
            if(property == null) {
                property = new ClusterProperty(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY, xml);
            } else {
                property.setValue(xml);
            }
            ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();
            admin.saveProperty(property);

            configuration = formConfiguration;
            confirmed = true;
            dispose();
        } catch (Exception e) {
            PermissionDeniedException pde = ExceptionUtils.getCauseIfCausedBy(e, PermissionDeniedException.class);
            if (pde != null) {
                EntityType type = pde.getType();
                String typeName = type == null ? "entity" : type.getName();
                JOptionPane.showMessageDialog(SftpPollingListenerPropertiesDialog.this,
                        MessageFormat.format("Permission to {0} the {1} denied", pde.getOperation().getName(), typeName),
                        "Permission Denied", JOptionPane.OK_OPTION);
            } else if (ExceptionUtils.causedBy(e, IOException.class)) {
                String errorMsg = ExceptionUtils.getMessage(e, "Invalid connection settings.");
                JOptionPane.showMessageDialog(this, errorMsg, "Connection Settings", JOptionPane.ERROR_MESSAGE);
            } else if (ExceptionUtils.causedBy(e, VersionException.class)) {
                String errorMsg = ExceptionUtils.getMessage(e, "Failed to save connection settings.");
                JOptionPane.showMessageDialog(this, errorMsg, "Connection Settings", JOptionPane.ERROR_MESSAGE);
                onCancel();
            } else {
                throw new RuntimeException("Unable to save changes", e);
            }
        }
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private SftpPollingListenerDialogSettings getMatchingOneFromThisList(List<SftpPollingListenerDialogSettings> list, SftpPollingListenerDialogSettings resource) {
        for(SftpPollingListenerDialogSettings i : list) {
            if(i.getResId()==resource.getResId()){
                return i;
            }
        }
        return null;
    }

    private int getIndexForMatchingOneFromThisList(List<SftpPollingListenerDialogSettings> list, SftpPollingListenerDialogSettings resource) {
        for(int i=0; i<list.size(); i++) {
            SftpPollingListenerDialogSettings type = list.get(i);
            if(type.getResId()==resource.getResId()) {
                return i;
            }
        }
        return -1;
    }

    private String marshallToXMLClassLoaderSafe(List<SftpPollingListenerDialogSettings> configurations) {
        String xml = null;
        final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(SftpPollingListenerPropertiesDialog.class.getClassLoader());

            SftpPollingListenerXmlUtilities xmlUtil = new SftpPollingListenerXmlUtilities();
            xml = xmlUtil.marshallToXMLString(configurations);
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }

        return xml;
    }

    private ClusterProperty getClusterProperty(){
        ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();
        ClusterProperty property = null;
        try{
            property = admin.findPropertyByName(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
        }catch(FindException fe){
            logger.warning("Could not find any cluster properties for property >" + SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY + "<");
        }
        return property;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public SftpPollingListenerDialogSettings getConfiguration() {
        return configuration;
    }

    public void selectNameField() {
        nameField.requestFocus();
        nameField.selectAll();
    }
}
