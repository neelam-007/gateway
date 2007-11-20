/*
 * Copyright (C) 2003-2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.MaxLengthDocument;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.TextListCellRenderer;
import com.l7tech.common.security.rbac.AttemptedCreate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.PermissionDeniedException;
import com.l7tech.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;

import javax.naming.Context;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Dialog for configuring a JMS Queue (ie, a [JmsConnection, JmsEndpoint] pair).
 *
 * @author mike
 * @author rmak
 */
public class JmsQueuePropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JRadioButton outboundRadioButton;
    private JRadioButton inboundRadioButton;
    private JComboBox providerComboBox;
    private JTextField jndiUrlTextField;
    private JTextField icfTextField;
    private JCheckBox useJndiCredentialsCheckBox;
    private JTextField jndiUsernameTextField;
    private JPasswordField jndiPasswordField;
    private JPanel jndiExtraPropertiesOuterPanel;   // For provider-specific settings.
    private JmsExtraPropertiesPanel jndiExtraPropertiesPanel;
    private JTextField qcfTextField;
    private JTextField queueNameTextField;
    private JCheckBox useQueueCredentialsCheckBox;
    private JTextField queueUsernameTextField;
    private JPasswordField queuePasswordField;
    private JPanel queueExtraPropertiesOuterPanel;   // For provider-specific settings.
    private JmsExtraPropertiesPanel queueExtraPropertiesPanel;
    private JPanel inboundOptionsPanel;
    private JCheckBox useJmsMsgPropAsSoapActionCheckBox;
    private JLabel jmsMsgPropWithSoapActionLabel;
    private JTextField jmsMsgPropWithSoapActionTextField;
    private JComboBox acknowledgementModeComboBox;
    private JCheckBox useQueueForFailedCheckBox;
    private JLabel failureQueueLabel;
    private JTextField failureQueueNameTextField;
    private JButton testButton;
    private JButton saveButton;
    private JButton cancelButton;

    private JmsConnection connection = null;
    private JmsEndpoint endpoint = null;
    private boolean isCanceled;
    private boolean outboundOnly = false;
    private FormAuthorizationPreparer securityFormAuthorizationPreparer;

    private static class ProviderComboBoxItem {
        private JmsProvider provider;

        private ProviderComboBoxItem(JmsProvider provider) {
            this.provider = provider;
        }

        public JmsProvider getProvider() {
            return provider;
        }

        public String toString() {
            return provider.getName();
        }
    }

    private JmsQueuePropertiesDialog(Frame parent) {
        super(parent, true);
    }

    private JmsQueuePropertiesDialog(Dialog parent) {
        super(parent, true);
    }

    /**
     * Create a new JmsQueuePropertiesDialog, configured to adjust the JMS Queue defined by the union of the
     * specified connection and endpoint.  The connection and endpoint may be null, in which case a new
     * Queue will be created by the dialog.  After show() returns, check isCanceled() to see whether the user
     * OK'ed the changes.  If so, call getConnection() and getEndpoint() to read them.  If the dialog completes
     * successfully, the (possibly-new) connection and endpoint will already have been saved to the database.
     *
     * @param parent       the parent window for the new dialog.
     * @param connection   the JMS connection to edit, or null to create a new one for this Queue.
     * @param endpoint     the JMS endpoint to edit, or null to create a new one for this Queue.
     * @param outboundOnly if true, the direction will be locked and defaulted to Outbound only.
     * @return the new instance
     */
    public static JmsQueuePropertiesDialog createInstance(Window parent, JmsConnection connection, JmsEndpoint endpoint, boolean outboundOnly) {
        JmsQueuePropertiesDialog that;
        if (parent instanceof Frame)
            that = new JmsQueuePropertiesDialog((Frame)parent);
        else if (parent instanceof Dialog)
            that = new JmsQueuePropertiesDialog((Dialog)parent);
        else
            throw new IllegalArgumentException("parent must be derived from either Frame or Dialog");
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        that.securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, new AttemptedCreate(com.l7tech.common.security.rbac.EntityType.JMS_ENDPOINT));

        that.connection = connection;
        that.endpoint = endpoint;
        that.setOutboundOnly(outboundOnly);

        that.init();

        return that;
    }

    private void setOutboundOnly(boolean outboundOnly) {
        this.outboundOnly = outboundOnly;
    }

    private boolean isOutboundOnly() {
        return outboundOnly;
    }

    /**
     * Check how the dialog was closed.  Return value is only guaranteed to be valid after show() has returned.
     *
     * @return false iff. the dialog completed successfully via the "Save" button; otherwise true.
     */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Obtain the connection that was edited or created.  Return value is only guaranteed to be valid if isCanceled()
     * is false.
     *
     * @return the possibly-new JMS connection, which may have been replaced by a new instance read back from the database
     */
    public JmsConnection getConnection() {
        return connection;
    }

    /**
     * Obtain the endpoint that was edited or created.  Return value is only guaranteed to be valid if isCanceled()
     * is false.
     *
     * @return the possibly-new JMS endpoint, which may have been replaced by a new instance read back from the database
     */
    public JmsEndpoint getEndpoint() {
        return endpoint;
    }

    private void init() {
        setTitle(connection == null ? "Add JMS Queue" : "JMS Queue Properties");
        setContentPane(contentPane);
        setModal(true);

        inboundRadioButton.setEnabled(!isOutboundOnly());
        outboundRadioButton.setEnabled(!isOutboundOnly());

        inboundRadioButton.addItemListener(formPreener);

        initProviderComboBox();

        useJndiCredentialsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableJndiCredentials();
            }
        });
        Utilities.enableGrayOnDisabled(jndiUsernameTextField);
        Utilities.enableGrayOnDisabled(jndiPasswordField);

        useQueueCredentialsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableQueueCredentials();
            }
        });
        Utilities.enableGrayOnDisabled(queueUsernameTextField);
        Utilities.enableGrayOnDisabled(queuePasswordField);

        jndiUrlTextField.getDocument().addDocumentListener(formPreener);
        icfTextField.getDocument().addDocumentListener(formPreener);
        qcfTextField.getDocument().addDocumentListener(formPreener);
        queueNameTextField.getDocument().addDocumentListener(formPreener);
        failureQueueNameTextField.setDocument(new MaxLengthDocument(128));
        failureQueueNameTextField.getDocument().addDocumentListener(formPreener);

        jndiExtraPropertiesOuterPanel.addContainerListener(new ContainerListener() {
            public void componentAdded(ContainerEvent e) {
                if (jndiExtraPropertiesPanel != null) {
                    jndiExtraPropertiesPanel.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            enableOrDisableComponents();
                        }
                    });
                }
                enableOrDisableComponents();
            }
            public void componentRemoved(ContainerEvent e) {
                enableOrDisableComponents();
            }
        });

        queueExtraPropertiesOuterPanel.addContainerListener(new ContainerListener() {
            public void componentAdded(ContainerEvent e) {
                if (queueExtraPropertiesPanel != null) {
                    queueExtraPropertiesPanel.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            enableOrDisableComponents();
                        }
                    });
                }
                enableOrDisableComponents();
            }
            public void componentRemoved(ContainerEvent e) {
                enableOrDisableComponents();
            }
        });

        acknowledgementModeComboBox.setModel(new DefaultComboBoxModel(JmsAcknowledgementType.values()));
        acknowledgementModeComboBox.setRenderer(new TextListCellRenderer(new Functions.Unary<String,Object>() {
            public String call(Object o) {
                JmsAcknowledgementType type = (JmsAcknowledgementType) o;
                String text = "";

                switch( type ) {
                    case AUTOMATIC:
                        text = "On Take";
                        break;
                    case ON_COMPLETION:
                        text = "On Completion";
                        break;
                    default:
                        text = "Unknown";
                        break;
                }

                return text;
            }
        }));
        acknowledgementModeComboBox.addActionListener(formPreener);
        useQueueForFailedCheckBox.addActionListener(formPreener);
        Utilities.enableGrayOnDisabled(failureQueueNameTextField);
        useJmsMsgPropAsSoapActionCheckBox.addItemListener(formPreener);
        jmsMsgPropWithSoapActionTextField.getDocument().addDocumentListener(formPreener);
        Utilities.enableGrayOnDisabled(jmsMsgPropWithSoapActionTextField);

        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        pack();
        initializeView();
        enableOrDisableComponents();
        applyFormSecurity();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void initProviderComboBox() {
        JmsProvider[] providers = null;
        try {
            providers = Registry.getDefault().getJmsManager().getProviderList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain list of installed JMS provider types from Gateway", e);
        }

        ProviderComboBoxItem[] items = new ProviderComboBoxItem[providers.length + 1];

        // If we already have a connection, and it was using non-default provider settings,
        // preserve it's old settings in a "Custom" provider
        boolean usingCustom = false;
        if (connection != null) {
            usingCustom = true;
            for (int i = 0; i < providers.length; ++i) {
                JmsProvider provider = providers[i];
                if (providerMatchesConnection(provider, connection)) {
                    usingCustom = false;
                    break;
                }
            }
        }

        JmsProvider customProvider = new JmsProvider();
        customProvider.setName("(Custom)");
        if (usingCustom) {
            customProvider.setDefaultDestinationFactoryUrl(connection.getDestinationFactoryUrl());
            customProvider.setDefaultQueueFactoryUrl(connection.getQueueFactoryUrl());
            customProvider.setDefaultTopicFactoryUrl(connection.getTopicFactoryUrl());
            customProvider.setInitialContextFactoryClassname(connection.getInitialContextFactoryClassname());
        } else {
            customProvider.setDefaultDestinationFactoryUrl(null);
            customProvider.setDefaultQueueFactoryUrl(null);
            customProvider.setDefaultTopicFactoryUrl(null);
            customProvider.setInitialContextFactoryClassname(null);
        }
        items[0] = new ProviderComboBoxItem(customProvider);

        for (int i = 0; i < providers.length; i++)
            items[i + 1] = new ProviderComboBoxItem(providers[i]);

        providerComboBox.setModel(new DefaultComboBoxModel(items));
        providerComboBox.setSelectedIndex(0);
        providerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onProviderChanged();
            }
        });
    }

    private void onProviderChanged() {
        final ProviderComboBoxItem providerItem = (ProviderComboBoxItem)providerComboBox.getSelectedItem();
        if (providerItem == null)
            return;
        JmsProvider provider = providerItem.getProvider();

        // Queue connection factory name, defaulting to destination factory name
        String qcfName = provider.getDefaultQueueFactoryUrl();
        if (qcfName == null || qcfName.length() < 1)
            qcfName = provider.getDefaultDestinationFactoryUrl();
        if (qcfName != null)
            qcfTextField.setText(qcfName);

        String icfName = provider.getInitialContextFactoryClassname();
        if (icfName != null)
            icfTextField.setText(icfName);

        setExtraPropertiesPanels(provider, connection == null ? null : connection.properties() );
    }

    /**
     * Inserts subpanels for extra settings according to the provider type selected.
     *
     * @param provider              the provider type selected
     * @param extraProperties       data structure used by the subpanels to transmit settings
     */
    private void setExtraPropertiesPanels(JmsProvider provider, Properties extraProperties) {
        final String icfClassname = provider.getInitialContextFactoryClassname();
        if ("com.tibco.tibjms.naming.TibjmsInitialContextFactory".equals(icfClassname)) {
            jndiExtraPropertiesPanel = new TibcoEmsJndiExtraPropertiesPanel(extraProperties);
            jndiExtraPropertiesOuterPanel.add(jndiExtraPropertiesPanel);
            queueExtraPropertiesPanel = new TibcoEmsQueueExtraPropertiesPanel(extraProperties);
            queueExtraPropertiesOuterPanel.add(queueExtraPropertiesPanel);
        } else if ("com.ibm.mq.jms.context.WMQInitialContextFactory".equals(icfClassname) ||
                "com.sun.jndi.ldap.LdapCtxFactory".equals(icfClassname)) {
            // TODO this casts too broad a net; we need to have an actual "provider type" enum.
            jndiExtraPropertiesPanel = null;
            jndiExtraPropertiesOuterPanel.removeAll();
            queueExtraPropertiesPanel = new MQSeriesQueueExtraPropertiesPanel(extraProperties);
            queueExtraPropertiesOuterPanel.add(queueExtraPropertiesPanel);
        } else {
            jndiExtraPropertiesPanel = null;
            jndiExtraPropertiesOuterPanel.removeAll();
            queueExtraPropertiesPanel = null;
            queueExtraPropertiesOuterPanel.removeAll();
        }
        pack();
    }

    /**
     * @param provider      must not be <code>null</code>
     * @param connection    must not be <code>null</code>
     * @return <code>true</code> if the initial context factory class name in
     *         <code>provider</code> and <code>connection</code> matches exactly
     */
    private boolean providerMatchesConnection(JmsProvider provider, JmsConnection connection) {
        return provider.getInitialContextFactoryClassname() != null &&
          provider.getInitialContextFactoryClassname().equals(connection.getInitialContextFactoryClassname());
    }

    private RunOnChangeListener formPreener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableOrDisableComponents();
        }
    });

    /**
     * Extract information from the view and create a new JmsConnection object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     * <p/>
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsConnection with the current settings, or null if one could not be created.  The new connection
     *         will not yet have been saved to the database.
     */
    private JmsConnection makeJmsConnectionFromView() {
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "At minimum, the name, queue name, naming URL and factory URL are required.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsConnection conn;
        if (connection != null) {
            conn = new JmsConnection();
            conn.copyFrom(connection);
        } else {
            final ProviderComboBoxItem providerItem = ((ProviderComboBoxItem)providerComboBox.getSelectedItem());
            if (providerItem == null) {
                conn = new JmsConnection();
            } else {
                JmsProvider provider = providerItem.getProvider();
                conn = provider.createConnection(queueNameTextField.getText(),
                  jndiUrlTextField.getText());
            }
            if (conn.getName()==null || conn.getName().trim().length()==0)
                conn.setName("Custom");
        }

        Properties properties = new Properties();
        if (useJndiCredentialsCheckBox.isSelected()) {
            properties.setProperty(Context.SECURITY_PRINCIPAL, jndiUsernameTextField.getText());
            properties.setProperty(Context.SECURITY_CREDENTIALS, new String(jndiPasswordField.getPassword()));
        }

        if (useQueueCredentialsCheckBox.isSelected()) {
            conn.setUsername(queueUsernameTextField.getText());
            conn.setPassword(new String(queuePasswordField.getPassword()));
        } else {
            conn.setUsername(null);
            conn.setPassword(null);
        }

        conn.setJndiUrl(jndiUrlTextField.getText());
        conn.setInitialContextFactoryClassname(icfTextField.getText());
        conn.setQueueFactoryUrl(qcfTextField.getText());
        if (jndiExtraPropertiesPanel != null) {
            properties.putAll(jndiExtraPropertiesPanel.getProperties());
        }
        if (queueExtraPropertiesPanel != null) {
            properties.putAll(queueExtraPropertiesPanel.getProperties());
        }
        if (useJmsMsgPropAsSoapActionCheckBox.isSelected()) {
            properties.put(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION, jmsMsgPropWithSoapActionTextField.getText());
        }
        conn.properties(properties);

        return conn;
    }

    /**
     * Extract information from the view and create a new JmsEndpoint object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     * <p/>
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsEndpoint with the current settings, or null if one could not be created.  The new connection
     *         will not yet have been saved to the database.
     */
    private JmsEndpoint makeJmsEndpointFromView() {
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "The queue name must be provided.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsEndpoint ep = new JmsEndpoint();
        if (endpoint != null)
            ep.copyFrom(endpoint);
        String name = queueNameTextField.getText();
        ep.setName(name);
        ep.setDestinationName(name);
        JmsAcknowledgementType type = (JmsAcknowledgementType) acknowledgementModeComboBox.getSelectedItem();
        if (!inboundRadioButton.isSelected()) type = null; // only applicable for inbound
        ep.setAcknowledgementType(type);
        if ( type == null || type == JmsAcknowledgementType.AUTOMATIC ) {
            ep.setFailureDestinationName(null);
        } else if ( useQueueForFailedCheckBox.isSelected() ) {
            ep.setFailureDestinationName( failureQueueNameTextField.getText() );
        } else {
            ep.setFailureDestinationName( null );
        }
        ep.setMessageSource(inboundRadioButton.isSelected());

        if (useQueueCredentialsCheckBox.isSelected()) {
            ep.setUsername(queueUsernameTextField.getText());
            ep.setPassword(new String(queuePasswordField.getPassword()));
        } else {
            ep.setUsername(null);
            ep.setPassword(null);
        }

        // Preserve old OID, if we have one
/*
        if (endpoint != null) {
            ep.setOid(endpoint.getOid());
            ep.setVersion( endpoint.getVersion() );
        }
*/

        return ep;
    }

    private void selectProviderForConnection(JmsConnection connection) {
        int numProviders = providerComboBox.getModel().getSize();
        for (int i = 0; i < numProviders; ++i) {
            ProviderComboBoxItem item = (ProviderComboBoxItem)providerComboBox.getModel().getElementAt(i);
            JmsProvider provider = item.getProvider();
            if (providerMatchesConnection(provider, connection)) {
                providerComboBox.setSelectedItem(item);
                return;
            }
        }
        providerComboBox.setSelectedIndex(0);
    }

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        if (connection != null) {
            // configure gui from connection
            selectProviderForConnection(connection);
            qcfTextField.setText(connection.getQueueFactoryUrl());
            jndiUrlTextField.setText(connection.getJndiUrl());
            icfTextField.setText(connection.getInitialContextFactoryClassname());

            String jndiUsername = connection.properties().getProperty(Context.SECURITY_PRINCIPAL);
            String jndiPassword = connection.properties().getProperty(Context.SECURITY_CREDENTIALS);
            useJndiCredentialsCheckBox.setSelected(jndiUsername != null || jndiPassword != null);
            jndiUsernameTextField.setText(jndiUsername);
            jndiPasswordField.setText(jndiPassword);
            enableOrDisableJndiCredentials();

            useQueueCredentialsCheckBox.setSelected(connection.getUsername() != null || connection.getUsername() != null);
            queueUsernameTextField.setText(connection.getUsername());
            queuePasswordField.setText(connection.getPassword());
            enableOrDisableQueueCredentials();
        } else {
            // No connection is set
            providerComboBox.setSelectedIndex(0);
            qcfTextField.setText("");
            icfTextField.setText("");
            jndiUrlTextField.setText("");

            useJndiCredentialsCheckBox.setSelected(false);
            jndiUsernameTextField.setText("");
            jndiPasswordField.setText("");
            enableOrDisableJndiCredentials();

            useQueueCredentialsCheckBox.setSelected(false);
            queueUsernameTextField.setText(null);
            queuePasswordField.setText(null);
            enableOrDisableQueueCredentials();
        }

        useQueueForFailedCheckBox.setSelected(false);
        failureQueueNameTextField.setText("");
        if (endpoint != null) {
            JmsAcknowledgementType type = endpoint.getAcknowledgementType();
            if (type != null) {
                acknowledgementModeComboBox.setSelectedItem(type);
            }
            if ( endpoint.getAcknowledgementType() == JmsAcknowledgementType.ON_COMPLETION ) {
                String name = endpoint.getFailureDestinationName();
                if (name != null && name.length() > 0) {
                    useQueueForFailedCheckBox.setSelected(true);
                    failureQueueNameTextField.setText(name);
                }
            }
        }

        useJmsMsgPropAsSoapActionCheckBox.setSelected(false);
        jmsMsgPropWithSoapActionTextField.setText("");
        if (endpoint != null) {
            // Configure gui from endpoint
            queueNameTextField.setText(endpoint.getDestinationName());
            inboundRadioButton.setSelected(endpoint.isMessageSource());
            if (connection != null) {
                final String propName = (String)connection.properties().get(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION);
                if (propName != null) {
                    useJmsMsgPropAsSoapActionCheckBox.setSelected(true);
                    jmsMsgPropWithSoapActionTextField.setText(propName);
                }
            }
        } else {
            // No endpoint is set
            queueNameTextField.setText("");
            inboundRadioButton.setSelected(false);
        }
    }

    /**
     * Returns true iff. the form has enough information to construct a JmsConnection.
     */
    private boolean validateForm() {
        if (queueNameTextField.getText().length() < 1)
            return false;
        if (jndiUrlTextField.getText().length() < 1)
            return false;
        if (qcfTextField.getText().length() < 1)
            return false;
        if (icfTextField.getText().length() < 1)
            return false;
        if (jndiExtraPropertiesPanel != null && !jndiExtraPropertiesPanel.validatePanel())
            return false;
        if (queueExtraPropertiesPanel != null && !queueExtraPropertiesPanel.validatePanel())
            return false;
        if (inboundRadioButton.isSelected() && (
                (useJmsMsgPropAsSoapActionCheckBox.isSelected() && jmsMsgPropWithSoapActionTextField.getText().length() == 0) ||
                (JmsAcknowledgementType.ON_COMPLETION == acknowledgementModeComboBox.getSelectedItem() &&
                        useQueueForFailedCheckBox.isSelected() &&
                        failureQueueNameTextField.getText().length()==0)
        )) return false;
        return true;
    }

    private void enableOrDisableJndiCredentials() {
        jndiUsernameTextField.setEnabled(useJndiCredentialsCheckBox.isSelected());
        jndiPasswordField.setEnabled(useJndiCredentialsCheckBox.isSelected());
    }

    private void enableOrDisableQueueCredentials() {
        queueUsernameTextField.setEnabled(useQueueCredentialsCheckBox.isSelected());
        queuePasswordField.setEnabled(useQueueCredentialsCheckBox.isSelected());
    }

    /**
     * Adjust components based on the state of the form.
     */
    private void enableOrDisableComponents() {
        if (inboundRadioButton.isSelected()) {
            Utilities.setEnabled(inboundOptionsPanel, true);
            useJmsMsgPropAsSoapActionCheckBox.setEnabled(true);
            jmsMsgPropWithSoapActionLabel.setEnabled(useJmsMsgPropAsSoapActionCheckBox.isSelected());
            jmsMsgPropWithSoapActionTextField.setEnabled(useJmsMsgPropAsSoapActionCheckBox.isSelected());
            enableOrDisableAcknowledgementControls();
        } else {
            Utilities.setEnabled(inboundOptionsPanel, false);
        }
        final boolean valid = validateForm();
        saveButton.setEnabled(valid);
        testButton.setEnabled(valid);
    }

    private void enableOrDisableAcknowledgementControls() {
        boolean enabled = false;
        boolean checkBoxEnabled = false;

        if ( JmsAcknowledgementType.ON_COMPLETION == acknowledgementModeComboBox.getSelectedItem() ) {
            checkBoxEnabled = true;
            if ( useQueueForFailedCheckBox.isSelected() ) {
                enabled = true;        
            }
        }

        useQueueForFailedCheckBox.setEnabled(checkBoxEnabled);
        failureQueueLabel.setEnabled(enabled);
        failureQueueNameTextField.setEnabled(enabled);
    }

    private void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        securityFormAuthorizationPreparer.prepare(new Component[]{
                outboundRadioButton,
                inboundRadioButton,
                providerComboBox,
                jndiUrlTextField,
                icfTextField,
                useJndiCredentialsCheckBox,
                jndiUsernameTextField,
                jndiPasswordField,
                qcfTextField,
                queueNameTextField,
                useQueueCredentialsCheckBox,
                queueUsernameTextField,
                queuePasswordField,
                useJmsMsgPropAsSoapActionCheckBox,
                jmsMsgPropWithSoapActionTextField,
                acknowledgementModeComboBox,
                useQueueForFailedCheckBox,
                failureQueueNameTextField,
        });
        securityFormAuthorizationPreparer.prepare(jndiExtraPropertiesOuterPanel);
        securityFormAuthorizationPreparer.prepare(queueExtraPropertiesOuterPanel);
    }

    private void onTest() {
        try {
            final JmsConnection newConnection = makeJmsConnectionFromView();
            if (newConnection == null)
                return;

            final JmsEndpoint newEndpoint = makeJmsEndpointFromView();
            if (newEndpoint == null)
                return;

            Registry.getDefault().getJmsManager().testEndpoint(newConnection, newEndpoint);
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "The Gateway has verified the existence of this JMS Queue.",
              "JMS Connection Successful",
              JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "The Gateway was unable to find this JMS Queue:\n" +
              e1.getMessage(),
              "JMS Connection Settings",
              JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        JmsConnection newConnection = makeJmsConnectionFromView();
        if (newConnection == null)
            return;

        JmsEndpoint newEndpoint = makeJmsEndpointFromView();
        if (newEndpoint == null)
            return;

        try {
            long oid = Registry.getDefault().getJmsManager().saveConnection(newConnection);
            newConnection.setOid(oid);
            newEndpoint.setConnectionOid(newConnection.getOid());
            oid = Registry.getDefault().getJmsManager().saveEndpoint(newEndpoint);
            newEndpoint.setOid(oid);

            connection = Registry.getDefault().getJmsManager().findConnectionByPrimaryKey(newConnection.getOid());
            endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(newEndpoint.getOid());
            isCanceled = false;
            dispose();
        } catch (Exception e) {
            PermissionDeniedException pde = (PermissionDeniedException) ExceptionUtils.getCauseIfCausedBy(e, PermissionDeniedException.class);
            if (pde != null) {
                EntityType type = pde.getType();
                String tname = type == null ? "entity" : type.getName();
                JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
                        MessageFormat.format("Permission to {0} the {1} denied", pde.getOperation().getName(), tname),
                        "Permission Denied", JOptionPane.OK_OPTION);
            } else {
                throw new RuntimeException("Unable to save changes to this JMS Queue", e);
            }
        }
    }

    private void onCancel() {
        isCanceled = true;
        dispose();
    }
}
