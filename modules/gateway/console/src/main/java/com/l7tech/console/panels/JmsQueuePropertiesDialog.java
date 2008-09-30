/*
 * Copyright (C) 2003-2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.transport.jms.*;
import static com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;

import javax.naming.Context;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for configuring a JMS Queue (ie, a [JmsConnection, JmsEndpoint] pair).
 *
 * @author mike
 * @author rmak
 */
public class JmsQueuePropertiesDialog extends JDialog {
    private static final EntityHeader[] EMPTY_ENTITY_HEADER = new EntityHeader[0];

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
    private JTabbedPane tabbedPane;
    private JRadioButton inboundReplyAutomaticRadioButton;
    private JRadioButton inboundReplyNoneRadioButton;
    private JRadioButton inboundReplySpecifiedQueueRadioButton;
    private JTextField inboundReplySpecifiedQueueField;
    private JRadioButton outboundReplyAutomaticRadioButton;
    private JRadioButton outboundReplyNoneRadioButton;
    private JRadioButton outboundReplySpecifiedQueueRadioButton;
    private JTextField outboundReplySpecifiedQueueField;
    private JCheckBox associateQueueWithPublishedCheckBox;
    private JLabel serviceNameLabel;
    private JComboBox serviceNameCombo;
    private JRadioButton outboundFormatAutoRadioButton;
    private JRadioButton outboundFormatTextRadioButton;
    private JRadioButton outboundFormatBytesRadioButton;
    private JCheckBox disableListeningTheQueueCheckBox;
    private JRadioButton inboundMessageIdRadioButton;
    private JRadioButton inboundCorrelationIdRadioButton;
    private JRadioButton outboundCorrelationIdRadioButton;
    private JRadioButton outboundMessageIdRadioButton;
    private JPanel inboundCorrelationPanel;
    private JPanel outboundCorrelationPanel;
    private JPanel outboundOptionsPanel;
    private JPanel inboundOptionsPanel;

    private JmsConnection connection = null;
    private JmsEndpoint endpoint = null;
    private boolean isCanceled;
    private boolean outboundOnly = false;
    private FormAuthorizationPreparer securityFormAuthorizationPreparer;
    private Logger logger = Logger.getLogger(JmsQueuePropertiesDialog.class.getName());

    public ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    private static class ProviderComboBoxItem {
        private JmsProvider provider;

        private ProviderComboBoxItem(JmsProvider provider) {
            this.provider = provider;
        }

        public JmsProvider getProvider() {
            return provider;
        }

        @Override
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
        that.securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, new AttemptedCreate( com.l7tech.gateway.common.security.rbac.EntityType.JMS_ENDPOINT));

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
        Utilities.enableGrayOnDisabled(inboundReplySpecifiedQueueField);
        Utilities.enableGrayOnDisabled(outboundReplySpecifiedQueueField);

        // Limit the input in the below text fields (The max length of these texts
        // depends on our MySql tables, jms_endpoint and jms_connection.)
        // Case 1: in the JNDI Tab
        icfTextField.setDocument(new MaxLengthDocument(255));
        jndiUrlTextField.setDocument(new MaxLengthDocument(255));
        jndiUsernameTextField.setDocument(new MaxLengthDocument(32));
        jndiPasswordField.setDocument(new MaxLengthDocument(32));
        // Case 2: in the Queue Tab
        qcfTextField.setDocument(new MaxLengthDocument(255));
        queueNameTextField.setDocument(new MaxLengthDocument(128));
        queueUsernameTextField.setDocument(new MaxLengthDocument(32));
        queuePasswordField.setDocument(new MaxLengthDocument(32));
        // Case 3: in the Inbound or Outbound Options Tab
        inboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));
        failureQueueNameTextField.setDocument(new MaxLengthDocument(128));
        jmsMsgPropWithSoapActionTextField.setDocument(new MaxLengthDocument(255));
        outboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));

        // Add a doc listener for those text fields that need to dectect if input is validated or not.
        // Case 1: in the JNDI Tab
        icfTextField.getDocument().addDocumentListener(formPreener);
        jndiUrlTextField.getDocument().addDocumentListener(formPreener);
        // Case 2: in the Queue Tab
        qcfTextField.getDocument().addDocumentListener(formPreener);
        queueNameTextField.getDocument().addDocumentListener(formPreener);
        // Case 3: in the Inbound or Outbound Options Tab
        inboundReplySpecifiedQueueField.getDocument().addDocumentListener(formPreener);
        failureQueueNameTextField.getDocument().addDocumentListener(formPreener);
        jmsMsgPropWithSoapActionTextField.getDocument().addDocumentListener(formPreener);
        outboundReplySpecifiedQueueField.getDocument().addDocumentListener(formPreener);

        final ComponentEnabler inboundEnabler = new ComponentEnabler(new Functions.Nullary<Boolean>() {
            public Boolean call() {
                return inboundReplySpecifiedQueueRadioButton.isSelected() || inboundReplyAutomaticRadioButton.isSelected();
            }
        }, inboundCorrelationPanel, inboundMessageIdRadioButton, inboundCorrelationIdRadioButton);

        inboundReplySpecifiedQueueRadioButton.addActionListener(inboundEnabler);
        inboundReplyAutomaticRadioButton.addActionListener(inboundEnabler);
        inboundReplyNoneRadioButton.addActionListener(inboundEnabler);

        final ComponentEnabler outboundEnabler = new ComponentEnabler(new Functions.Nullary<Boolean>() {
            public Boolean call() {
                return outboundReplySpecifiedQueueRadioButton.isSelected();
            }
        }, outboundReplySpecifiedQueueField, outboundCorrelationPanel, outboundMessageIdRadioButton, outboundCorrelationIdRadioButton);

        outboundReplySpecifiedQueueRadioButton.addActionListener(outboundEnabler);
        outboundReplyAutomaticRadioButton.addActionListener(outboundEnabler);
        outboundReplyNoneRadioButton.addActionListener(outboundEnabler);

        jndiExtraPropertiesOuterPanel.addContainerListener(new ContainerListener() {
            public void componentAdded(ContainerEvent e) {
                if (jndiExtraPropertiesPanel != null) {
                    jndiExtraPropertiesPanel.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent ce) {
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
                        public void stateChanged(ChangeEvent ce) {
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

        acknowledgementModeComboBox.setModel(new DefaultComboBoxModel(values()));
        acknowledgementModeComboBox.setRenderer(new TextListCellRenderer(new Functions.Unary<String,Object>() {
            public String call(Object o) {
                JmsAcknowledgementType type = (JmsAcknowledgementType) o;
                String text;

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
        associateQueueWithPublishedCheckBox.addActionListener(formPreener);

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
        JmsProvider[] providers;
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
            for (JmsProvider provider : providers) {
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
        String qcfName = (connection != null && providerMatchesConnection(provider, connection))?
                connection.getQueueFactoryUrl() : provider.getDefaultQueueFactoryUrl();
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
            jndiExtraPropertiesOuterPanel.removeAll();
            jndiExtraPropertiesOuterPanel.add(jndiExtraPropertiesPanel);
            queueExtraPropertiesPanel = new TibcoEmsQueueExtraPropertiesPanel(extraProperties);
            queueExtraPropertiesOuterPanel.removeAll();
            queueExtraPropertiesOuterPanel.add(queueExtraPropertiesPanel);
        } else if ("com.ibm.mq.jms.context.WMQInitialContextFactory".equals(icfClassname) ||
                "com.sun.jndi.ldap.LdapCtxFactory".equals(icfClassname)) {
            // TODO this casts too broad a net; we need to have an actual "provider type" enum.
            jndiExtraPropertiesPanel = null;
            jndiExtraPropertiesOuterPanel.removeAll();
            queueExtraPropertiesPanel = new MQSeriesQueueExtraPropertiesPanel(extraProperties);
            queueExtraPropertiesOuterPanel.removeAll();
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
    private static boolean providerMatchesConnection(JmsProvider provider, JmsConnection connection) {
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

        if (associateQueueWithPublishedCheckBox.isSelected()) {
            ComboItem item = (ComboItem)serviceNameCombo.getSelectedItem();
            properties.setProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString());
            properties.setProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID, (new Long(item.serviceID)).toString());
        } else {
            properties.setProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE, (Boolean.FALSE).toString());
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

        JmsOutboundMessageType omt = JmsOutboundMessageType.AUTOMATIC;
        if (outboundFormatBytesRadioButton.isSelected())
            omt = JmsOutboundMessageType.ALWAYS_BINARY;
        else if (outboundFormatTextRadioButton.isSelected())
            omt = JmsOutboundMessageType.ALWAYS_TEXT;
        ep.setOutboundMessageType(omt);

        if (inboundRadioButton.isSelected()) {
            configureEndpointReplyBehaviour(
                    ep, "Inbound",
                    inboundReplyAutomaticRadioButton,
                    inboundReplyNoneRadioButton,
                    inboundReplySpecifiedQueueRadioButton,
                    inboundReplySpecifiedQueueField,
                    inboundMessageIdRadioButton);
        } else {
            configureEndpointReplyBehaviour(
                    ep, "Outbound",
                    outboundReplyAutomaticRadioButton,
                    outboundReplyNoneRadioButton,
                    outboundReplySpecifiedQueueRadioButton,
                    outboundReplySpecifiedQueueField, 
                    outboundMessageIdRadioButton);
        }

        if (!inboundRadioButton.isSelected()) type = null; // only applicable for inbound
        ep.setAcknowledgementType(type);
        if ( type == null || type == AUTOMATIC ) {
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

        // Save if the queue is disabled or not
        if (inboundRadioButton.isSelected()) {
            ep.setDisabled(disableListeningTheQueueCheckBox.isSelected());
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

    private static void configureEndpointReplyBehaviour(JmsEndpoint ep, String what, final JRadioButton autoButton, final JRadioButton noneButton, final JRadioButton specifiedButton, final JTextField specifiedField, JRadioButton messageIdRadioButton) {
        if (autoButton.isSelected()) {
            ep.setReplyType(JmsReplyType.AUTOMATIC);
            if (ep.isMessageSource()) ep.setUseMessageIdForCorrelation(messageIdRadioButton.isSelected());
        } else if (noneButton.isSelected()) {
            ep.setReplyType(JmsReplyType.NO_REPLY);
        } else if (specifiedButton.isSelected()) {
            ep.setReplyType(JmsReplyType.REPLY_TO_OTHER);
            final String t = specifiedField.getText();
            ep.setUseMessageIdForCorrelation(messageIdRadioButton.isSelected());
            if (t == null || t.length() == 0) throw new IllegalStateException(what + " Specified Queue name must be set");
            ep.setReplyToQueueName(t);
        } else {
            throw new IllegalStateException(what + " was selected, but no reply type was selected");
        }
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

    private static class ComboItem {
        ComboItem(String name, long id) {
            serviceName = name;
            serviceID = id;
        }

        @Override
        public String toString() {
            return serviceName;
        }

        String serviceName;
        long serviceID;

        @Override
        @SuppressWarnings({ "RedundantIfStatement" })
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ComboItem comboItem = (ComboItem) o;

            if (serviceID != comboItem.serviceID) return false;
            if (serviceName != null ? !serviceName.equals(comboItem.serviceName) : comboItem.serviceName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (serviceName != null ? serviceName.hashCode() : 0);
            result = 31 * result + (int) (serviceID ^ (serviceID >>> 32));
            return result;
        }
    }

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        boolean isHardWired = false;
        long hardWiredId = 0;
        if (connection != null) {
            Properties props = connection.properties();

            // configure gui from connection
            selectProviderForConnection(connection);
            qcfTextField.setText(connection.getQueueFactoryUrl());
            jndiUrlTextField.setText(connection.getJndiUrl());
            icfTextField.setText(connection.getInitialContextFactoryClassname());

            String jndiUsername = props.getProperty(Context.SECURITY_PRINCIPAL);
            String jndiPassword = props.getProperty(Context.SECURITY_CREDENTIALS);
            useJndiCredentialsCheckBox.setSelected(jndiUsername != null || jndiPassword != null);
            jndiUsernameTextField.setText(jndiUsername);
            jndiPasswordField.setText(jndiPassword);
            enableOrDisableJndiCredentials();

            useQueueCredentialsCheckBox.setSelected(connection.getUsername() != null || connection.getPassword() != null);
            queueUsernameTextField.setText(connection.getUsername());
            queuePasswordField.setText(connection.getPassword());
            enableOrDisableQueueCredentials();

            String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
            if (tmp != null) {
                if (Boolean.parseBoolean(tmp)) {
                    tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                    isHardWired = true;
                    hardWiredId = Long.parseLong(tmp);
                }
            }
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

        // populate the service combo
        EntityHeader[] allServices = EMPTY_ENTITY_HEADER;
        try {
            ServiceAdmin sa = getServiceAdmin();
            allServices = sa.findAllPublishedServices();
        } catch (Exception e) {
            logger.log(Level.WARNING, "problem listing services", e);
        }
        if (allServices == null || allServices.length == 0) {
            // Case 1: the queue associated with a published service and the user may be with a role of Manage JMS Queue.
            if (isHardWired) {
                String message = "Service " + hardWiredId + " is selected, but cannot be displayed.";
                serviceNameCombo.addItem(new ComboItem(message, hardWiredId));
                associateQueueWithPublishedCheckBox.setSelected(true);
            }
            // Case 2: There are no any published services at all.
            else {
                // We just want to show the message "No published services available." in the combo box.
                // So "-1" is just a dummy ServiceOID and it won't be used since the checkbox is set to disabled.
                serviceNameCombo.addItem(new ComboItem("No published services available.", -1));
                associateQueueWithPublishedCheckBox.setEnabled(false);
            }
        } else {
            java.util.List<EntityHeader> onlySoapServicesList = new ArrayList<EntityHeader>();
            for (EntityHeader entity : allServices) {
                if (((ServiceHeader)entity).isSoap()) onlySoapServicesList.add(entity);
            }
            EntityHeader[] soapServices = onlySoapServicesList.toArray(new EntityHeader[onlySoapServicesList.size()]);
            ComboItem[] comboitems = new ComboItem[soapServices.length];
            Object selectMe = null;
            for (int i = 0; i < soapServices.length; i++) {
                comboitems[i] = new ComboItem(((ServiceHeader)soapServices[i]).getDisplayName(), soapServices[i].getOid());
                if (isHardWired && soapServices[i].getOid() == hardWiredId) {
                    selectMe = comboitems[i];
                }
            }
            serviceNameCombo.setModel(new DefaultComboBoxModel(comboitems));
            if (selectMe != null) {
                serviceNameCombo.setSelectedItem(selectMe);
                associateQueueWithPublishedCheckBox.setSelected(true);
            } else {
                associateQueueWithPublishedCheckBox.setSelected(false);
            }
        }
        useQueueForFailedCheckBox.setSelected(false);
        failureQueueNameTextField.setText("");
        if (endpoint != null) {
            JmsAcknowledgementType type = endpoint.getAcknowledgementType();
            if (type != null) {
                acknowledgementModeComboBox.setSelectedItem(type);
            }
            if ( endpoint.getAcknowledgementType() == ON_COMPLETION ) {
                String name = endpoint.getFailureDestinationName();
                if (name != null && name.length() > 0) {
                    useQueueForFailedCheckBox.setSelected(true);
                    failureQueueNameTextField.setText(name);
                }
            }

            switch (endpoint.getOutboundMessageType()) {
            case AUTOMATIC:
                outboundFormatAutoRadioButton.setSelected(true);
                break;
            case ALWAYS_BINARY:
                outboundFormatBytesRadioButton.setSelected(true);
                break;
            case ALWAYS_TEXT:
                outboundFormatTextRadioButton.setSelected(true);
            }

            final boolean use = endpoint.isUseMessageIdForCorrelation();
            if (endpoint.isMessageSource()) {
                inboundReplyAutomaticRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.AUTOMATIC);
                inboundReplyNoneRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.NO_REPLY);
                inboundReplySpecifiedQueueRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.REPLY_TO_OTHER);
                inboundReplySpecifiedQueueField.setText(endpoint.getReplyToQueueName());
                if (use)
                    inboundMessageIdRadioButton.setSelected(true);
                else
                    inboundCorrelationIdRadioButton.setSelected(true);
            } else {
                outboundReplyAutomaticRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.AUTOMATIC);
                outboundReplyNoneRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.NO_REPLY);
                outboundReplySpecifiedQueueRadioButton.setSelected(endpoint.getReplyType() == JmsReplyType.REPLY_TO_OTHER);
                outboundReplySpecifiedQueueField.setText(endpoint.getReplyToQueueName());
                if (use)
                    outboundMessageIdRadioButton.setSelected(true);
                else
                    outboundCorrelationIdRadioButton.setSelected(true);
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

        if (inboundRadioButton.isSelected() && endpoint.isDisabled()) {
            disableListeningTheQueueCheckBox.setSelected(true);
        }
    }

    /**
     * Returns true iff. the form has enough information to construct a JmsConnection.
     * @return true iff. form has enough info to construct a JmsConnection
     */
    private boolean validateForm() {
        if (queueNameTextField.getText().trim().length() < 1)
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
        if (outboundRadioButton.isSelected() && !isOutboundPaneValid())
            return false;
        //noinspection RedundantIfStatement
        if (inboundRadioButton.isSelected() && !isInboundPaneValid())
            return false;
        return true;
    }

    private boolean isOutboundPaneValid() {
        //noinspection RedundantIfStatement
        if (outboundReplySpecifiedQueueField.isEnabled() &&
            outboundReplySpecifiedQueueField.getText().trim().length() == 0)
            return false;
        return true;
    }
    
    private boolean isInboundPaneValid() {
        if (acknowledgementModeComboBox.getSelectedItem() == ON_COMPLETION &&
            useQueueForFailedCheckBox.isSelected() &&
            failureQueueNameTextField.getText().trim().length() == 0)
            return false;
        if (useJmsMsgPropAsSoapActionCheckBox.isSelected() &&
            jmsMsgPropWithSoapActionTextField.getText().length() == 0)
            return false;
        //noinspection RedundantIfStatement
        if (inboundReplySpecifiedQueueField.isEnabled() &&
            inboundReplySpecifiedQueueField.getText().trim().length() == 0)
            return false;
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
            useJmsMsgPropAsSoapActionCheckBox.setEnabled(true);
            jmsMsgPropWithSoapActionLabel.setEnabled(useJmsMsgPropAsSoapActionCheckBox.isSelected());
            jmsMsgPropWithSoapActionTextField.setEnabled(useJmsMsgPropAsSoapActionCheckBox.isSelected());
            serviceNameLabel.setEnabled(associateQueueWithPublishedCheckBox.isSelected());
            serviceNameCombo.setEnabled(associateQueueWithPublishedCheckBox.isSelected());
            tabbedPane.setEnabledAt(3, true);
            tabbedPane.setEnabledAt(4, false);
            enableOrDisableAcknowledgementControls();
            final boolean specified = inboundReplySpecifiedQueueRadioButton.isSelected();
            final boolean auto = inboundReplyAutomaticRadioButton.isSelected();
            inboundReplySpecifiedQueueField.setEnabled(specified);
            inboundMessageIdRadioButton.setEnabled(specified || auto);
            inboundCorrelationIdRadioButton.setEnabled(specified || auto);
        } else {
            tabbedPane.setEnabledAt(3, false);
            tabbedPane.setEnabledAt(4, true);
            outboundReplySpecifiedQueueField.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundMessageIdRadioButton.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundCorrelationIdRadioButton.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
        }

        final boolean valid = validateForm();
        saveButton.setEnabled(valid);
        testButton.setEnabled(valid);
    }

    private void enableOrDisableAcknowledgementControls() {
        boolean enabled = false;
        boolean checkBoxEnabled = false;

        if ( ON_COMPLETION == acknowledgementModeComboBox.getSelectedItem() ) {
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
        securityFormAuthorizationPreparer.prepare(inboundOptionsPanel);
        securityFormAuthorizationPreparer.prepare(outboundOptionsPanel);
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
        } catch (Exception ex) {
            String errorMsg = (ExceptionUtils.causedBy(ex, JmsNotSupportTopicException.class))?
                    ex.getMessage() : "The Gateway was unable to find this JMS Queue:\n" + ex.getMessage();
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              errorMsg,
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

        // For the case where the queue name is changed, then the connection should be updated.
        newConnection.setName(newEndpoint.getName());

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
            PermissionDeniedException pde = ExceptionUtils.getCauseIfCausedBy(e, PermissionDeniedException.class);
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

    private class ComponentEnabler implements ActionListener {
        private final Functions.Nullary<Boolean> f;
        private final JComponent[] components;

        public ComponentEnabler(Functions.Nullary<Boolean> f, JComponent... components) {
            this.f = f;
            this.components = components;
        }

        public void actionPerformed(ActionEvent e) {
            for (JComponent component : components) {
                component.setEnabled(f.call());
            }
            final boolean valid = validateForm();
            saveButton.setEnabled(valid);
            testButton.setEnabled(valid);
            enableOrDisableComponents();
        }
    }
}
