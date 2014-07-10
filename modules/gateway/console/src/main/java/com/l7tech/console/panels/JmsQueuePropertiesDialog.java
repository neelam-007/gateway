package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.transport.jms.*;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.Pair;

import javax.naming.Context;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType.*;

/**
 * Dialog for configuring a JMS Destination (ie, a [JmsConnection, JmsEndpoint] pair).
 *
 * @author mike
 * @author rmak
 */
public class JmsQueuePropertiesDialog extends JDialog {
    private static final String TYPE_QUEUE = "Queue";
    private static final String TYPE_TOPIC = "Topic";

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
    private JCheckBox useJmsMsgPropAsSoapActionRadioButton;
    private JCheckBox associateQueueWithPublishedService;
    private JPanel inboundOptionsPanel;
    private JPanel outboundOptionsPanel;

    private JRadioButton specifyContentTypeFreeForm;
    private JComboBox contentTypeValues;
    private JRadioButton specifyContentTypeFromHeader;
    private JTextField getContentTypeFromProperty;
    private JCheckBox specifyContentTypeCheckBox;
    private JCheckBox isTemplateQueue;
    private JTextField jmsEndpointDescriptiveName;
    private JButton applyReset;
    private JComboBox destinationTypeComboBox;
    private JTable environmentPropertiesTable;
    private JButton addEnvironmentButton;
    private JButton editEnvironmentButton;
    private JButton removeEnvironmentButton;
    private JCheckBox showJndiPasswordCheckBox;
    private JCheckBox showQueuePasswordCheckBox;
    private JLabel jndiPasswordWarningLabel;
    private JLabel queuePasswordWarningLabel;
    private ByteLimitPanel byteLimitPanel;
    private SecurityZoneWidget zoneControl;
    private JSpinner dedicatedConsumerConnectionLimitSpinner;
    private JSpinner sessionPoolSizeSpinner;
    private JTextField sessionPoolMaxWaitTextField;
    private JLabel sessionPoolSizeLabel;
    private JLabel sessionPoolMaxWait;
    private JSpinner maxIdleSessionSpinner;
    private JLabel maxSessionIdleLabel;
    private JLabel jmsConsumerConnectionsLabel;


    private JmsConnection connection = null;
    private JmsEndpoint endpoint = null;
    private boolean isOk;
    private boolean outboundOnly = false;
    private FormAuthorizationPreparer securityFormAuthorizationPreparer;
    private Logger logger = Logger.getLogger(JmsQueuePropertiesDialog.class.getName());
    private ContentTypeComboBoxModel contentTypeModel;
    private SimpleTableModel<NameValuePair> environmentPropertiesTableModel;

    private final PermissionFlags endpointFlags;
    private final PermissionFlags connectionFlags;
    private InputValidator inputValidator;

    public ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }


    private static class ContentTypeComboBoxItem {
        private final ContentTypeHeader cType;

        private ContentTypeComboBoxItem( final ContentTypeHeader cType ) {
            if ( cType == null ) throw new IllegalArgumentException( "cType must not be null" );
            this.cType = cType;
        }

        public ContentTypeHeader getContentType() {
            return this.cType;
        }

        @Override
        public String toString() {
            return this.cType.getFullValue();
        }

        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }

            final ContentTypeComboBoxItem that = (ContentTypeComboBoxItem) o;

            return cType.getFullValue().equals( that.cType.getFullValue() );
        }

        public int hashCode() {
            return (cType != null ? cType.getFullValue().hashCode() : 0);
        }
    }

    private class ContentTypeComboBoxModel extends DefaultComboBoxModel {
        private ContentTypeComboBoxModel(ContentTypeComboBoxItem[] items) {
            super(items);
        }

        // implements javax.swing.MutableComboBoxModel
        @Override
        public void addElement(Object anObject) {
            if (anObject instanceof String) {
                String s = (String) anObject;
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(s);
                    super.addElement(new ContentTypeComboBoxItem(cth)) ;
                } catch (IOException e) {
                    logger.warning("Error parsing the content type " + s);
                }
            }
        }
    }

    private JmsQueuePropertiesDialog(Window parent) {
        super(parent, ModalityType.APPLICATION_MODAL);
        endpointFlags = PermissionFlags.get(EntityType.JMS_ENDPOINT);
        connectionFlags = PermissionFlags.get(EntityType.JMS_CONNECTION);
    }

    /**
     * Create a new JmsQueuePropertiesDialog, configured to adjust the JMS Destination defined by the union of the
     * specified connection and endpoint.  The connection and endpoint may be null, in which case a new
     * Destination will be created by the dialog.  After show() returns, check isCanceled() to see whether the user
     * OK'ed the changes.  If so, call getConnection() and getEndpoint() to read them.  If the dialog completes
     * successfully, the (possibly-new) connection and endpoint will already have been saved to the database.
     *
     * @param parent       the parent window for the new dialog.
     * @param connection   the JMS connection to edit, or null to create a new one for this Destination.
     * @param endpoint     the JMS endpoint to edit, or null to create a new one for this Destination.
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
        final AttemptedOperation attemptedOperation = (endpoint == null || endpoint.getGoid().equals(JmsEndpoint.DEFAULT_GOID))
            ? new AttemptedCreate(EntityType.JMS_ENDPOINT)
            : new AttemptedUpdate(EntityType.JMS_ENDPOINT, endpoint);
        that.securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, attemptedOperation);

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
     * Check how the dialog was closed.
     *
     * @return false iff. the dialog completed successfully via the "Save" button; otherwise true.
     */
    public boolean isCanceled() {
        return !isOk;
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


    public void selectNameField() {
        jmsEndpointDescriptiveName.requestFocus();
        jmsEndpointDescriptiveName.selectAll();
    }

    private void init() {
        setTitle(connection == null ? "Add JMS Destination" : "JMS Destination Properties");
        setContentPane(contentPane);
        setModal(true);

        inputValidator = new InputValidator(this, getTitle());

        inboundRadioButton.setEnabled(!isOutboundOnly());
        outboundRadioButton.setEnabled(!isOutboundOnly());
        isTemplateQueue.setEnabled(true);
        isTemplateQueue.addItemListener( enableDisableListener );

        inboundRadioButton.addItemListener(enableDisableListener);
        outboundRadioButton.addItemListener( enableDisableListener );


        initProviderReset();

        useJndiCredentialsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableJndiCredentials();
            }
        });
        Utilities.enableGrayOnDisabled(jndiUsernameTextField);
        Utilities.enableGrayOnDisabled(jndiPasswordField);

        destinationTypeComboBox.setModel( new DefaultComboBoxModel( new String[]{ TYPE_QUEUE, TYPE_TOPIC } ) );

        destinationTypeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableDedicatedConsumerConnections();
            }

        });
        Utilities.enableGrayOnDisabled(jmsConsumerConnectionsLabel);
        dedicatedConsumerConnectionLimitSpinner.setModel(new SpinnerNumberModel(Registry.getDefault().getJmsManager().getDefaultConsumerConnectionSize(), 1, 10000, 1));
        Utilities.enableGrayOnDisabled(dedicatedConsumerConnectionLimitSpinner);

        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(dedicatedConsumerConnectionLimitSpinner, jmsConsumerConnectionsLabel.getText()));

        sessionPoolSizeSpinner.setModel((new SpinnerNumberModel((Number) JmsConnection.DEFAULT_SESSION_POOL_SIZE, -1, 10000, 1)));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(sessionPoolSizeSpinner,sessionPoolSizeLabel.getText()));

        maxIdleSessionSpinner.setModel((new SpinnerNumberModel((Number) JmsConnection.DEFAULT_SESSION_POOL_SIZE, -1, 10000, 1)));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxIdleSessionSpinner,maxSessionIdleLabel.getText()));

        inputValidator.constrainTextFieldToNumberRange(sessionPoolMaxWait.getText(), sessionPoolMaxWaitTextField, -1, Long.MAX_VALUE);

        inputValidator.attachToButton(saveButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });


        useQueueCredentialsCheckBox.addItemListener(new ItemListener() {
            @Override
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
        jndiUsernameTextField.setDocument(new MaxLengthDocument(1024));
        jndiPasswordField.setDocument(new MaxLengthDocument(255));
        PasswordGuiUtils.configureOptionalSecurePasswordField(jndiPasswordField, showJndiPasswordCheckBox, jndiPasswordWarningLabel);
        // Case 2: in the Destination Tab
        qcfTextField.setDocument(new MaxLengthDocument(255));
        queueNameTextField.setDocument(new MaxLengthDocument(128));
        queueUsernameTextField.setDocument(new MaxLengthDocument(255));
        queuePasswordField.setDocument(new MaxLengthDocument(255));
        PasswordGuiUtils.configureOptionalSecurePasswordField(queuePasswordField, showQueuePasswordCheckBox, queuePasswordWarningLabel);
        // Case 3: in the Inbound or Outbound Options Tab
        inboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));
        failureQueueNameTextField.setDocument(new MaxLengthDocument(128));
        jmsMsgPropWithSoapActionTextField.setDocument(new MaxLengthDocument(255));
        outboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));

        jmsEndpointDescriptiveName.setDocument(new MaxLengthDocument(128));
        jmsEndpointDescriptiveName.getDocument().addDocumentListener( enableDisableListener );

        // Add a doc listener for those text fields that need to detect if input is validated or not.
        // Case 1: in the JNDI Tab
        icfTextField.getDocument().addDocumentListener( enableDisableListener );
        jndiUrlTextField.getDocument().addDocumentListener( enableDisableListener );
        environmentPropertiesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        // Case 2: in the Destination Tab
        qcfTextField.getDocument().addDocumentListener( enableDisableListener );
        queueNameTextField.getDocument().addDocumentListener( enableDisableListener );
        // Case 3: in the Inbound or Outbound Options Tab
        inboundReplySpecifiedQueueField.getDocument().addDocumentListener( enableDisableListener );
        failureQueueNameTextField.getDocument().addDocumentListener( enableDisableListener );
        jmsMsgPropWithSoapActionTextField.getDocument().addDocumentListener( enableDisableListener );
        outboundReplySpecifiedQueueField.getDocument().addDocumentListener( enableDisableListener );

        final ComponentEnabler inboundEnabler = new ComponentEnabler(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return inboundReplySpecifiedQueueRadioButton.isSelected() || inboundReplyAutomaticRadioButton.isSelected();
            }
        }, inboundCorrelationPanel, inboundMessageIdRadioButton, inboundCorrelationIdRadioButton);

        inboundReplySpecifiedQueueRadioButton.addActionListener(inboundEnabler);
        inboundReplyAutomaticRadioButton.addActionListener(inboundEnabler);
        inboundReplyNoneRadioButton.addActionListener(inboundEnabler);

        serviceNameCombo.setRenderer( TextListCellRenderer.<ServiceComboItem>basicComboBoxRenderer() );

        Utilities.enableGrayOnDisabled(contentTypeValues);
        Utilities.enableGrayOnDisabled(getContentTypeFromProperty);

        specifyContentTypeCheckBox.addActionListener(enableDisableListener);
        specifyContentTypeFreeForm.addActionListener(enableDisableListener);
        specifyContentTypeFromHeader.addActionListener(enableDisableListener);
        getContentTypeFromProperty.getDocument().addDocumentListener( enableDisableListener );

        final ComponentEnabler outboundEnabler = new ComponentEnabler(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return outboundReplySpecifiedQueueRadioButton.isSelected();
            }
        }, outboundReplySpecifiedQueueField, outboundCorrelationPanel, outboundMessageIdRadioButton, outboundCorrelationIdRadioButton);

        outboundReplySpecifiedQueueRadioButton.addActionListener(outboundEnabler);
        outboundReplyAutomaticRadioButton.addActionListener(outboundEnabler);
        outboundReplyNoneRadioButton.addActionListener(outboundEnabler);

        environmentPropertiesTableModel = TableUtil.configureTable(
                environmentPropertiesTable,
                TableUtil.column("Name",  50, 100, 100000, property("key"), String.class),
                TableUtil.column("Value", 50, 100, 100000, property("value"), String.class)
        );
        environmentPropertiesTable.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        environmentPropertiesTable.getTableHeader().setReorderingAllowed( false );
        environmentPropertiesTable.getTableHeader().setReorderingAllowed(false);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>( environmentPropertiesTableModel );
        sorter.setSortKeys( Arrays.asList( new RowSorter.SortKey(0, SortOrder.ASCENDING),  new RowSorter.SortKey(1, SortOrder.ASCENDING) ) );
        environmentPropertiesTable.setRowSorter( sorter );

        jndiExtraPropertiesOuterPanel.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (jndiExtraPropertiesPanel != null) {
                    jndiExtraPropertiesPanel.addChangeListener(enableDisableListener);
                }
                enableOrDisableComponents();
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                enableOrDisableComponents();
            }
        });

        queueExtraPropertiesOuterPanel.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (queueExtraPropertiesPanel != null) {
                    queueExtraPropertiesPanel.addChangeListener(enableDisableListener);
                }
                enableOrDisableComponents();
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                enableOrDisableComponents();
            }
        });

        acknowledgementModeComboBox.setModel(new DefaultComboBoxModel(values()));
        acknowledgementModeComboBox.setRenderer(new TextListCellRenderer<Object>(new Functions.Unary<String,Object>() {
            @Override
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
        acknowledgementModeComboBox.addActionListener( enableDisableListener );
        useQueueForFailedCheckBox.addActionListener( enableDisableListener );
        Utilities.enableGrayOnDisabled(failureQueueNameTextField);
        useJmsMsgPropAsSoapActionRadioButton.addItemListener( enableDisableListener );
        jmsMsgPropWithSoapActionTextField.getDocument().addDocumentListener( enableDisableListener );
        Utilities.enableGrayOnDisabled(jmsMsgPropWithSoapActionTextField);
        associateQueueWithPublishedService.addActionListener( enableDisableListener );

        addEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentAdd();
            }
        } );
        editEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentEdit();
            }
        } );
        removeEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentRemove();
            }
        } );

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // the SecurityZoneWidget is shared by both the JmsEndpoint and JmsConnection
        final OperationType operation;
        if (connection == null || connection.getGoid().equals(JmsConnection.DEFAULT_GOID)) {
            operation = OperationType.CREATE;
        } else if (Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.JMS_CONNECTION, connection))) {
            operation = OperationType.UPDATE;
        } else {
            operation = OperationType.READ;
        }
        zoneControl.configure(Arrays.asList(EntityType.JMS_CONNECTION, EntityType.JMS_ENDPOINT), operation,
                connection != null ? connection.getSecurityZone() : null);

        pack();
        initializeView();
        enableOrDisableComponents();
        applyFormSecurity();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void loadContentTypesModel() {
        if (contentTypeModel == null) {
            java.util.List<ContentTypeComboBoxItem> items = new ArrayList<ContentTypeComboBoxItem>();
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.XML_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.SOAP_1_2_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.TEXT_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.OCTET_STREAM_DEFAULT));

            contentTypeModel = new ContentTypeComboBoxModel(items.toArray(new ContentTypeComboBoxItem[items.size()]));
            contentTypeValues.setModel(contentTypeModel);
        }
    }

    private void enableContentTypeControls() {
        boolean specifyEnabled = specifyContentTypeCheckBox.isSelected();
        specifyContentTypeFreeForm.setEnabled(specifyEnabled);
        contentTypeValues.setEnabled(specifyEnabled && specifyContentTypeFreeForm.isSelected());

        specifyContentTypeFromHeader.setEnabled(specifyEnabled);
        getContentTypeFromProperty.setEnabled(specifyEnabled && specifyContentTypeFromHeader.isSelected());
    }


    private void initProviderReset() {
        EnumSet<JmsProviderType> providerTypes;
        try {
            providerTypes = Registry.getDefault().getJmsManager().getProviderTypes();
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain list of installed JMS provider types from Gateway", e);
        }

        JmsProviderType[] providerTypeArray = new JmsProviderType[providerTypes.size() + 1];
        System.arraycopy( providerTypes.toArray(new JmsProviderType[providerTypes.size()]), 0, providerTypeArray, 1, providerTypes.size() );
        providerComboBox.setModel(new DefaultComboBoxModel(providerTypeArray));
        providerComboBox.addItemListener( new ItemListener(){
            @Override
            public void itemStateChanged( final ItemEvent e ) {
                setExtraPropertiesPanels((JmsProviderType)providerComboBox.getSelectedItem(), connection == null ? null : connection.properties() );                
                enableDisableListener.itemStateChanged( e );
            }
        } );
        providerComboBox.setRenderer( new TextListCellRenderer<JmsProviderType>( new Functions.Unary<String,JmsProviderType>(){
            @Override
            public String call( final JmsProviderType jmsProviderType ) {
                return jmsProviderType == null ?
                        "Generic JMS" :
                        jmsProviderType.getName();
            }
        }, null, true ) );
        applyReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onProviderReset();
            }
        });
    }

    private void onProviderReset() {
        final JmsProviderType providerType = (JmsProviderType) providerComboBox.getSelectedItem();
        if (providerType == null)
            return;

        if (connection == null) {
            resetProviderProperties(providerType, true);
        } else if (providerType == connection.getProviderType()) {
            // same provider "type", offer overwrite/don't overwrite/cancel choice
            DialogDisplayer.showConfirmDialog(this,
                "Overwrite current JMS destination properties?",
                "JMS Destination Reset Confirmation",
                JOptionPane.YES_NO_CANCEL_OPTION,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        switch (option) {
                            case JOptionPane.YES_OPTION:
                                resetProviderProperties(providerType, true);
                                break;
                            case JOptionPane.NO_OPTION:
                                resetProviderProperties(providerType, false);
                                break;
                            default:
                                // cancel, don't do anything
                        }
                    }
                });

        } else {
            // different provider "type", just warn
            DialogDisplayer.showConfirmDialog(this,
                "Current JMS destination properties will be overwritten",
                "JMS Destination Reset Confirmation",
                JOptionPane.OK_CANCEL_OPTION,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (JOptionPane.OK_OPTION == option) {
                            resetProviderProperties(providerType, true);
                        }
                    }
                });
        }
    }

    /**
     * 
     */
    private void resetProviderProperties( final JmsProviderType providerType,
                                          final boolean overwrite ) {
        // Queue connection factory name, defaulting to destination factory name
        String qcfName = (connection != null && ! overwrite) ? connection.getQueueFactoryUrl() : providerType.getDefaultQueueFactoryUrl();
        if (qcfName == null || qcfName.length() < 1)
            qcfName = providerType.getDefaultDestinationFactoryUrl();
        if (qcfName != null)
            qcfTextField.setText(qcfName);

        String icfName = providerType.getInitialContextFactoryClass();
        if (icfName != null)
            icfTextField.setText(icfName);
    }

    /**
     * Inserts sub panels for extra settings according to the provider type selected.
     *
     * @param providerType          the provider type selected
     * @param extraProperties       data structure used by the sub panels to transmit settings
     */
    private void setExtraPropertiesPanels( final JmsProviderType providerType,
                                           final Properties extraProperties ) {
        jndiExtraPropertiesPanel = providerType==null ?
                null :
                getExtraPropertiesPanel(providerType.getJndiExtraPropertiesClass(), extraProperties);
        jndiExtraPropertiesOuterPanel.removeAll();  //clean out what's previous
        if (jndiExtraPropertiesPanel != null) {
            jndiExtraPropertiesOuterPanel.add(jndiExtraPropertiesPanel);    //set with new properties
        }

        queueExtraPropertiesPanel = providerType==null ?
                null :
                getExtraPropertiesPanel(providerType.getQueueExtraPropertiesClass(), extraProperties);
        queueExtraPropertiesOuterPanel.removeAll(); //clean out what's previous
        if (queueExtraPropertiesPanel != null) {
            queueExtraPropertiesOuterPanel.add(queueExtraPropertiesPanel);
        }

        JRootPane rootPane = contentPane.getRootPane();
        Container rootParent = rootPane.getParent();
        if (rootParent instanceof JInternalFrame) {
            JInternalFrame jif = (JInternalFrame)rootParent;
            Dimension newSize = contentPane.getPreferredSize();
            Dimension fullSize = new Dimension(10 + (int)newSize.getWidth(), 32 + (int)newSize.getHeight());
            jif.setSize(fullSize);
        } else {
            pack();
        }
    }

    private JmsExtraPropertiesPanel getExtraPropertiesPanel(String extraPropertiesClass, Properties extraProperties) {
        try {
            return extraPropertiesClass == null ? null : (JmsExtraPropertiesPanel) Class.forName(extraPropertiesClass).getDeclaredConstructor(Properties.class).newInstance(extraProperties);
        } catch (Exception e) {
            DialogDisplayer.showMessageDialog(this, "Error getting default settings for provider: " + extraPropertiesClass, "JMS Extra Properties Error", JOptionPane.ERROR_MESSAGE, null);
        }
        return null;
    }

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    /**
     * Extract information from the view and create a new JmsConnection object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     * <p/>
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsConnection with the current settings, or null if one could not be created.  The new connection
     *         will not yet have been saved to the database.
     */
    @SuppressWarnings({ "UseOfPropertiesAsHashtable" })
    private JmsConnection makeJmsConnectionFromView() throws IOException{
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "At minimum, the name, destination name, naming URL and factory URL are required.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsConnection conn;
        if (connection != null) {
            conn = new JmsConnection();
            conn.copyFrom(connection);
        } else {
            final JmsProviderType providerType = ((JmsProviderType)providerComboBox.getSelectedItem());
            if (providerType == null) {
                conn = new JmsConnection();
            } else {
                JmsProvider provider = providerType.createProvider();
                conn = provider.createConnection(queueNameTextField.getText(),
                  jndiUrlTextField.getText());
            }
            if (conn.getName()==null || conn.getName().trim().length()==0)
                conn.setName("Custom");
        }
        conn.setProviderType((JmsProviderType) providerComboBox.getSelectedItem());

        conn.setTemplate(viewIsTemplate());        

        Properties properties = new Properties();

        for ( final Map.Entry<String,String> entry : environmentPropertiesTableModel.getRows() ) {
            properties.setProperty( entry.getKey(), entry.getValue() );
        }

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

        if (associateQueueWithPublishedService.isSelected()) {
            PublishedService svc = ServiceComboBox.getSelectedPublishedService(serviceNameCombo);
            properties.setProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString());
            properties.setProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID, svc.getGoid().toString());
        } else {
            properties.setProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE, (Boolean.FALSE).toString());
        }

        if (specifyContentTypeCheckBox.isSelected()) {
            if (specifyContentTypeFreeForm.isSelected()) {
                //if none of the list is selected and there is a value in the content type, then we'll use the one
                //that was entered by the user
                ContentTypeHeader selectedContentType;
                if (contentTypeValues.getSelectedIndex() == -1 && contentTypeValues.getEditor().getItem() != null) {
                    String ctHeaderString = ((JTextField) contentTypeValues.getEditor().getEditorComponent()).getText();
                    selectedContentType = ContentTypeHeader.parseValue(ctHeaderString);

                    //check if the typed in content type matches to any one of the ones in our list
                    int foundIndex = findContentTypeInList(selectedContentType);
                    if (foundIndex != -1) {
                        selectedContentType = ((ContentTypeComboBoxItem) contentTypeModel.getElementAt(foundIndex)).getContentType();
                    }
                } else {
                    selectedContentType = ((ContentTypeComboBoxItem) contentTypeValues.getSelectedItem()).getContentType();
                }

                if (selectedContentType != null) {
                    properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_SOURCE, JmsConnection.CONTENT_TYPE_SOURCE_FREEFORM);
                    properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_VAL, selectedContentType.getFullValue());
                }
            } else {
                String propertyName = getContentTypeFromProperty.getText();
                if ((propertyName != null) && !"".equals(propertyName)) {
                    properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_SOURCE, JmsConnection.CONTENT_TYPE_SOURCE_HEADER);
                    properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_VAL, propertyName);
                }
            }
        } else {
            properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_SOURCE, "");
            properties.setProperty(JmsConnection.PROP_CONTENT_TYPE_VAL, "");
        }

        conn.setJndiUrl(getTextOrNull(jndiUrlTextField));
        conn.setInitialContextFactoryClassname(getTextOrNull(icfTextField));
        conn.setQueueFactoryUrl(getTextOrNull(qcfTextField));
        if (jndiExtraPropertiesPanel != null) {
            properties.putAll(jndiExtraPropertiesPanel.getProperties());
        }
        if (queueExtraPropertiesPanel != null) {
            properties.putAll(queueExtraPropertiesPanel.getProperties());
        }
        if (useJmsMsgPropAsSoapActionRadioButton.isSelected()) {
            properties.setProperty(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION, jmsMsgPropWithSoapActionTextField.getText());
        }
        if (inboundRadioButton.isSelected()) {
            if (TYPE_QUEUE.equals(destinationTypeComboBox.getSelectedItem())) {
                properties.setProperty(JmsConnection.PROP_IS_DEDICATED_CONSUMER, Boolean.TRUE.toString());
                properties.setProperty(JmsConnection.PROP_DEDICATED_CONSUMER_SIZE, dedicatedConsumerConnectionLimitSpinner.getValue().toString());
            }
        }
        else {
            properties.setProperty(JmsConnection.PROP_SESSION_POOL_SIZE, sessionPoolSizeSpinner.getValue().toString());
            properties.setProperty(JmsConnection.PROP_MAX_SESSION_IDLE, maxIdleSessionSpinner.getValue().toString());
            properties.setProperty(JmsConnection.PROP_SESSION_POOL_MAX_WAIT, sessionPoolMaxWaitTextField.getText());
        }
        conn.properties(properties);
        conn.setSecurityZone(zoneControl.getSelectedZone());
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
              "The destination name must be provided.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsEndpoint ep = new JmsEndpoint();
        if (endpoint != null)
            ep.copyFrom(endpoint);
        String name = jmsEndpointDescriptiveName.getText();
        ep.setName(name);
        ep.setQueue( TYPE_QUEUE.equals(destinationTypeComboBox.getSelectedItem()) );
        ep.setTemplate(viewIsTemplate());
        ep.setDestinationName(getTextOrNull(queueNameTextField));
        ep.setSecurityZone(zoneControl.getSelectedZone());

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
                    ep, viewIsTemplate()?"Outbound Template":"Outbound",
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

        // Save if the destination is disabled or not
        // save request limit size
        if (inboundRadioButton.isSelected()) {
            ep.setRequestMaxSize(byteLimitPanel.getLongValue());
            ep.setDisabled(disableListeningTheQueueCheckBox.isSelected());
        }

        return ep;
    }

    private static void configureEndpointReplyBehaviour(JmsEndpoint ep, String what, final JRadioButton autoButton, final JRadioButton noneButton, final JRadioButton specifiedButton, final JTextField specifiedField, JRadioButton messageIdRadioButton) {
        boolean isTemplate = ep.isTemplate();
        if (autoButton.isSelected()) {
            ep.setReplyType(JmsReplyType.AUTOMATIC);
            ep.setReplyToQueueName(null);
            if (ep.isMessageSource()) ep.setUseMessageIdForCorrelation(messageIdRadioButton.isSelected());
        } else if (noneButton.isSelected()) {
            ep.setReplyType(JmsReplyType.NO_REPLY);
            ep.setReplyToQueueName(null);
        } else if (specifiedButton.isSelected()) {
            ep.setReplyType(JmsReplyType.REPLY_TO_OTHER);
            final String t = getTextOrNull(specifiedField);
            ep.setUseMessageIdForCorrelation(messageIdRadioButton.isSelected());
            if (!isTemplate && (t == null || t.length() == 0) ) throw new IllegalStateException(what + " Specified Destination name must be set");
            ep.setReplyToQueueName(t);
        } else {
            throw new IllegalStateException(what + " was selected, but no reply type was selected");
        }
    }

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        boolean isHardWired = false;
        Goid hardWiredId = PersistentEntity.DEFAULT_GOID;
        loadContentTypesModel();
        if ( connection != null ) {
            isTemplateQueue.setSelected(connection.isTemplate());

            Properties props = connection.properties();

            // configure gui from connection
            qcfTextField.setText(connection.getQueueFactoryUrl());
            jndiUrlTextField.setText(connection.getJndiUrl());
            icfTextField.setText(connection.getInitialContextFactoryClassname());

            providerComboBox.setSelectedItem(connection.getProviderType());

            String jndiUsername = props.getProperty(Context.SECURITY_PRINCIPAL);
            String jndiPassword = props.getProperty(Context.SECURITY_CREDENTIALS);
            useJndiCredentialsCheckBox.setSelected(jndiUsername != null || jndiPassword != null);
            jndiUsernameTextField.setText(jndiUsername);
            jndiUsernameTextField.setCaretPosition( 0 );
            jndiPasswordField.setText(jndiPassword);
            jndiPasswordField.setCaretPosition( 0 );
            enableOrDisableJndiCredentials();

            useQueueCredentialsCheckBox.setSelected(connection.getUsername() != null || connection.getPassword() != null);
            queueUsernameTextField.setText(connection.getUsername());
            queueUsernameTextField.setCaretPosition( 0 );
            queuePasswordField.setText(connection.getPassword());
            queuePasswordField.setCaretPosition( 0 );
            enableOrDisableQueueCredentials();

            String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
            if (tmp != null) {
                if (Boolean.parseBoolean(tmp)) {
                    tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                    isHardWired = true;
                    hardWiredId = GoidUpgradeMapper.mapId(EntityType.SERVICE,tmp);
                }
            }

            String ctSource = props.getProperty(JmsConnection.PROP_CONTENT_TYPE_SOURCE);

            boolean shouldSelect = false;
            if(JmsConnection.CONTENT_TYPE_SOURCE_FREEFORM.equals(ctSource)) {
                shouldSelect = true;
                String ctStr = props.getProperty(JmsConnection.PROP_CONTENT_TYPE_VAL);
                if ((null != ctStr) && !"".equals(ctStr)) {
                    try {
                        //determine if the content type is a manually entered one, if so, we'll display this content type
                        //value in the editable box
                        ContentTypeHeader ctHeader = ContentTypeHeader.parseValue(ctStr);
                        if (findContentTypeInList(ctHeader) != -1) {
                            contentTypeValues.setSelectedItem(new ContentTypeComboBoxItem(ctHeader));
                        } else {
                            contentTypeValues.setSelectedItem(null);
                            contentTypeValues.getEditor().setItem(new ContentTypeComboBoxItem(ctHeader));
                        }
                        
                        specifyContentTypeFreeForm.setSelected(true);
                    } catch (IOException e1) {
                        logger.log(Level.WARNING,
                                MessageFormat.format("Error while parsing the Content-Type for JMS Destination {0}. Value was {1}", connection.toString(), ctStr),
                                ExceptionUtils.getMessage(e1));
                        shouldSelect = false;
                    }
                }
            } else if (JmsConnection.CONTENT_TYPE_SOURCE_HEADER.equals(ctSource)) {
                shouldSelect = true;
                String jmsPropertyName = props.getProperty(JmsConnection.PROP_CONTENT_TYPE_VAL);
                if ( (null != jmsPropertyName) && !"".equals(jmsPropertyName) ) {
                    getContentTypeFromProperty.setText(jmsPropertyName);
                    specifyContentTypeFromHeader.setSelected(true);
                }
            } else if (ctSource == null || "".equals(ctSource)) {
                shouldSelect = false;
            }
            specifyContentTypeCheckBox.setSelected(shouldSelect);

            setExtraPropertiesPanels( connection.getProviderType(), props );

            final java.util.List<NameValuePair> environmentProperties = new ArrayList<NameValuePair>();
            for ( final String propertyName : props.stringPropertyNames() ) {
                if ( !propertyName.startsWith( JmsConnection.PREFIX ) &&
                     !Context.SECURITY_PRINCIPAL.equals( propertyName ) &&
                     !Context.SECURITY_CREDENTIALS.equals( propertyName ) &&
                     !JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION.equals( propertyName ) &&
                     (jndiExtraPropertiesPanel == null || !jndiExtraPropertiesPanel.isKnownProperty( propertyName ) ) &&
                     (queueExtraPropertiesPanel == null || !queueExtraPropertiesPanel.isKnownProperty( propertyName ) ) ) {
                    environmentProperties.add( new NameValuePair( propertyName, props.getProperty( propertyName ) ) );
                }
            }
            environmentPropertiesTableModel.setRows( environmentProperties );
            String isDedicatedConsumer = props.getProperty(JmsConnection.PROP_IS_DEDICATED_CONSUMER);
            if (isDedicatedConsumer != null && Boolean.parseBoolean(isDedicatedConsumer)) {
                dedicatedConsumerConnectionLimitSpinner.setValue(getConsumerConnectionLimit(props));
            } else {
                dedicatedConsumerConnectionLimitSpinner.setValue(Registry.getDefault().getJmsManager().getDefaultConsumerConnectionSize());
            }

            sessionPoolSizeSpinner.setValue(getSessionPoolSize(props));
            maxIdleSessionSpinner.setValue(getMaxSessionIdle(props));
            sessionPoolMaxWaitTextField.setText(getSessionPoolMaxWait(props).toString());
        } else {
            // No connection is set
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
            environmentPropertiesTableModel.setRows( Collections.<NameValuePair>emptyList() );
            sessionPoolMaxWaitTextField.setText(String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT));
        }

        boolean associateQueue = ServiceComboBox.populateAndSelect(serviceNameCombo, isHardWired, hardWiredId);
        associateQueueWithPublishedService.setSelected(associateQueue);

        useQueueForFailedCheckBox.setSelected(false);
        failureQueueNameTextField.setText("");

        byteLimitPanel.setAllowContextVars(false);
        byteLimitPanel.addChangeListener(new RunOnChangeListener(){
            @Override
            protected void run(){
                enableOrDisableComponents();
            }
        });

        if (endpoint != null) {
            if ( endpoint.isTemplate() ) {
                isTemplateQueue.setSelected(true); // template if either endpoint or connection are templates
            }
            destinationTypeComboBox.setSelectedItem( endpoint.isQueue() ? TYPE_QUEUE : TYPE_TOPIC );
            jmsEndpointDescriptiveName.setText(endpoint.getName());
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

        byteLimitPanel.setValue(
            endpoint == null? null : Long.toString(endpoint.getRequestMaxSize()),
            Registry.getDefault().getJmsManager().getDefaultJmsMessageMaxBytes()
        );

        useJmsMsgPropAsSoapActionRadioButton.setSelected(false);
        jmsMsgPropWithSoapActionTextField.setText("");
        if (endpoint != null) {
            // Configure gui from endpoint
            queueNameTextField.setText(endpoint.getDestinationName());
            inboundRadioButton.setSelected(endpoint.isMessageSource());
            if (connection != null) {
                final String propName = connection.properties().getProperty(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION);
                if (propName != null) {
                    useJmsMsgPropAsSoapActionRadioButton.setSelected(true);
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

        enableOrDisableComponents();
    }

    private Integer getConsumerConnectionLimit(Properties props) {
        String val = props.getProperty(JmsConnection.PROP_DEDICATED_CONSUMER_SIZE);
        if(val == null ) return Registry.getDefault().getJmsManager().getDefaultConsumerConnectionSize();
        try{
            return new Integer(val);
        } catch (NumberFormatException ex) {
            return Registry.getDefault().getJmsManager().getDefaultConsumerConnectionSize();
        }
    }

    private Integer getSessionPoolSize(Properties props) {
        String val = props.getProperty(JmsConnection.PROP_SESSION_POOL_SIZE, String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_SIZE));
        if(val == null ) return JmsConnection.DEFAULT_SESSION_POOL_SIZE;
        try{
            return new Integer(val);
        } catch (NumberFormatException ex) {
            return JmsConnection.DEFAULT_SESSION_POOL_SIZE;
        }
    }

    private Integer getMaxSessionIdle(Properties props) {
        String val = props.getProperty(JmsConnection.PROP_MAX_SESSION_IDLE, String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_SIZE));
        if(val == null ) return JmsConnection.DEFAULT_SESSION_POOL_SIZE;
        try{
            return new Integer(val);
        } catch (NumberFormatException ex) {
            return JmsConnection.DEFAULT_SESSION_POOL_SIZE;
        }
    }

    private Long getSessionPoolMaxWait(Properties props) {
        String val = props.getProperty(JmsConnection.PROP_SESSION_POOL_MAX_WAIT, String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT));
        if(val == null ) return JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT;
        try{
            return new Long(val);
        } catch (NumberFormatException ex) {
            return JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT;
        }
    }

    private boolean viewIsTemplate() {
        return isTemplateQueue.isEnabled() && isTemplateQueue.isSelected();
    }

    /**
     * Get the text from the given field, null if empty.
     */
    private static String getTextOrNull( final JTextField textField ) {
        final String text = textField.getText();
        return (text == null || text.trim().isEmpty()) ? null : text;
    }

    /**
     * Returns true iff. the form has enough information to construct a JmsConnection.
     * @return true iff. form has enough info to construct a JmsConnection
     */
    @SuppressWarnings({ "RedundantIfStatement" })
    private boolean validateForm() {
        boolean isTemplate = viewIsTemplate();
        if (jmsEndpointDescriptiveName.getText().trim().length() == 0)
            return false;
        if (!isTemplate && queueNameTextField.getText().trim().length() == 0)
            return false;
        if (!isTemplate && jndiUrlTextField.getText().length() == 0)
            return false;
        if (!isTemplate && qcfTextField.getText().length() == 0)
            return false;
        if (!isTemplate && icfTextField.getText().length() == 0)
            return false;
        if (jndiExtraPropertiesPanel != null && !jndiExtraPropertiesPanel.validatePanel())
            return false;
        if (queueExtraPropertiesPanel != null && !queueExtraPropertiesPanel.validatePanel())
            return false;
        if (outboundRadioButton.isSelected() && !isOutboundPaneValid(isTemplate))
            return false;
        if (inboundRadioButton.isSelected() && !isInboundPaneValid())
            return false;
        return true;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    private boolean isOutboundPaneValid(boolean isTemplate) {
        if (outboundReplySpecifiedQueueField.isEnabled() &&
            (!isTemplate && outboundReplySpecifiedQueueField.getText().trim().length() == 0))
            return false;
        return true;
    }
    
    @SuppressWarnings({ "RedundantIfStatement" })
    private boolean isInboundPaneValid() {
        if (acknowledgementModeComboBox.getSelectedItem() == ON_COMPLETION &&
            useQueueForFailedCheckBox.isSelected() &&
            failureQueueNameTextField.getText().trim().length() == 0)
            return false;
        if (useJmsMsgPropAsSoapActionRadioButton.isSelected() &&
            jmsMsgPropWithSoapActionTextField.getText().trim().length() == 0)
            return false;
        if (inboundReplySpecifiedQueueField.isEnabled() &&
            inboundReplySpecifiedQueueField.getText().trim().length() == 0)
            return false;
        if (associateQueueWithPublishedService.isSelected() &&
                (serviceNameCombo == null || serviceNameCombo.getItemCount() <= 0))
            return false;
        if (specifyContentTypeCheckBox.isSelected() &&
            specifyContentTypeFromHeader.isSelected() &&
            getContentTypeFromProperty.getText().trim().length() == 0)
            return false;
        if (byteLimitPanel.validateFields()!=null)
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

    private void enableOrDisableDedicatedConsumerConnections() {
        boolean isEnabled = TYPE_QUEUE.equals(destinationTypeComboBox.getSelectedItem());
        dedicatedConsumerConnectionLimitSpinner.setEnabled(isEnabled);
        jmsConsumerConnectionsLabel.setEnabled(isEnabled);
    }

    /**
     * Adjust components based on the state of the form.
     */
    private void enableOrDisableComponents() {
        boolean canEdit = canEdit();

        if (inboundRadioButton.isSelected()) {
            useJmsMsgPropAsSoapActionRadioButton.setEnabled(canEdit);
            jmsMsgPropWithSoapActionLabel.setEnabled(canEdit && useJmsMsgPropAsSoapActionRadioButton.isSelected());
            jmsMsgPropWithSoapActionTextField.setEnabled(canEdit && useJmsMsgPropAsSoapActionRadioButton.isSelected());
            serviceNameLabel.setEnabled(canEdit && associateQueueWithPublishedService.isSelected());
            serviceNameCombo.setEnabled(canEdit && associateQueueWithPublishedService.isSelected());
            tabbedPane.setEnabledAt(3, true);
            tabbedPane.setEnabledAt(4, false);
            enableOrDisableAcknowledgementControls();
            final boolean specified = inboundReplySpecifiedQueueRadioButton.isSelected();
            final boolean auto = inboundReplyAutomaticRadioButton.isSelected();
            inboundReplySpecifiedQueueField.setEnabled(canEdit && specified);
            inboundMessageIdRadioButton.setEnabled(canEdit && (specified || auto));
            inboundCorrelationIdRadioButton.setEnabled(canEdit && (specified || auto));
        } else {
            tabbedPane.setEnabledAt(3, false);
            tabbedPane.setEnabledAt(4, true);
            outboundReplySpecifiedQueueField.setEnabled(canEdit && outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundMessageIdRadioButton.setEnabled(canEdit && outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundCorrelationIdRadioButton.setEnabled(canEdit && outboundReplySpecifiedQueueRadioButton.isSelected());
        }

        final boolean environmentPropSelected = environmentPropertiesTable.getSelectedRow() > -1;
        editEnvironmentButton.setEnabled( canEdit && environmentPropSelected );
        removeEnvironmentButton.setEnabled( canEdit && environmentPropSelected );
        
        isTemplateQueue.setEnabled(canEdit && outboundRadioButton.isSelected());
        applyReset.setEnabled( canEdit && providerComboBox.getSelectedItem() != null );


        final boolean valid = validateForm();
        saveButton.setEnabled(valid && canEdit);
        testButton.setEnabled(valid && !viewIsTemplate());
        enableContentTypeControls();
        enableOrDisableDedicatedConsumerConnections();
    }

    private boolean canEdit() {
        return (connectionFlags.canCreateSome() || connectionFlags.canUpdateSome() ||
                           endpointFlags.canCreateSome() || endpointFlags.canUpdateSome());
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
                useJmsMsgPropAsSoapActionRadioButton,
                jmsMsgPropWithSoapActionTextField,
                acknowledgementModeComboBox,
                useQueueForFailedCheckBox,
                failureQueueNameTextField,
                jmsEndpointDescriptiveName,
                isTemplateQueue,
                destinationTypeComboBox,
                addEnvironmentButton,
                editEnvironmentButton,
                removeEnvironmentButton,
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
              "The Gateway has verified the existence of this JMS Destination.",
              "JMS Connection Successful",
              JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            String errorMsg = "The Gateway was unable to find this JMS Destination.\n";
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              errorMsg,
              "JMS Connection Settings",
              JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        try {
              JmsConnection newConnection = makeJmsConnectionFromView();
              if (newConnection == null)
                  return;

            JmsEndpoint newEndpoint = makeJmsEndpointFromView();
            if (newEndpoint == null)
                return;

            // For the case where the destination name is changed, then the connection should be updated.
            newConnection.setName(newEndpoint.getName());

            Goid goid = Registry.getDefault().getJmsManager().saveConnection(newConnection);
            newConnection.setGoid(goid);
            newEndpoint.setConnectionGoid(newConnection.getGoid());
            goid = Registry.getDefault().getJmsManager().saveEndpoint(newEndpoint);
            newEndpoint.setGoid(goid);

            connection = Registry.getDefault().getJmsManager().findConnectionByPrimaryKey(newConnection.getGoid());
            endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(newEndpoint.getGoid());
            isOk = true;
            dispose();
        } catch (Exception e) {
            PermissionDeniedException pde = ExceptionUtils.getCauseIfCausedBy(e, PermissionDeniedException.class);
            if (pde != null) {
                EntityType type = pde.getType();
                String typeName = type == null ? "entity" : type.getName();
                JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
                        MessageFormat.format("Permission to {0} the {1} denied", pde.getOperation().getName(), typeName),
                        "Permission Denied", JOptionPane.OK_OPTION);
            } else if (ExceptionUtils.causedBy(e, IOException.class)) {
                String errorMsg = ExceptionUtils.getMessage(e, "Invalid JMS connection settings.");
                JOptionPane.showMessageDialog(this, errorMsg, "JMS Connection Settings", JOptionPane.ERROR_MESSAGE);
            } else if (ExceptionUtils.causedBy(e, VersionException.class)) {
                String errorMsg = ExceptionUtils.getMessage(e, "Failed to save JMS connection settings.");
                JOptionPane.showMessageDialog(this, errorMsg, "JMS Connection Settings", JOptionPane.ERROR_MESSAGE);
                onCancel();               
            } else {
                throw new RuntimeException("Unable to save changes to this JMS Destination", e);
            }
        }
    }

    private void onCancel() {
        dispose();
    }

    private void onEnvironmentAdd() {
        editEnvironmentProperty( null );
    }

    private void onEnvironmentEdit() {
        final int viewRow = environmentPropertiesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            final int modelRow = environmentPropertiesTable.convertRowIndexToModel(viewRow);
            editEnvironmentProperty( environmentPropertiesTableModel.getRowObject(modelRow  ) );
        }
    }

    private void editEnvironmentProperty( final NameValuePair nameValuePair ) {
        final SimplePropertyDialog dlg = nameValuePair == null ?
                new SimplePropertyDialog(this) : 
                new SimplePropertyDialog(this, new Pair<String,String>( nameValuePair.getKey(), nameValuePair.getValue() ) );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            /** @noinspection unchecked*/
            @Override
            public void run() {
                if ( dlg.isConfirmed() ) {
                    final Pair<String, String> property = dlg.getData();
                    for ( final NameValuePair pair : new ArrayList<NameValuePair>(environmentPropertiesTableModel.getRows()) ) {
                        if ( pair.getKey().equals(property.left) ) {
                            environmentPropertiesTableModel.removeRow( pair );
                        }
                    }
                    if ( nameValuePair != null ) environmentPropertiesTableModel.removeRow( nameValuePair );

                    environmentPropertiesTableModel.addRow( new NameValuePair( property.left, property.right ) );
                }
            }
        });

    }

    private void onEnvironmentRemove() {
        final int viewRow = environmentPropertiesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            environmentPropertiesTableModel.removeRowAt( environmentPropertiesTable.convertRowIndexToModel(viewRow) );
        }
    }

    private class ComponentEnabler implements ActionListener {
        private final Functions.Nullary<Boolean> f;
        private final JComponent[] components;

        private ComponentEnabler(Functions.Nullary<Boolean> f, JComponent... components) {
            this.f = f;
            this.components = components;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (JComponent component : components) {
                component.setEnabled(f.call());
            }
            final boolean valid = validateForm();
            saveButton.setEnabled(valid && canEdit());
            testButton.setEnabled(valid);
            enableOrDisableComponents();
        }
    }


    /**
     * Finds the index of the ContentTypeComboBoxItem from the model that matches to the specified content type header
     * @param ctHeader  The content type header
     * @return  -1 if not found in the list, otherwise the index location of the found match.
     */
    private int findContentTypeInList(ContentTypeHeader ctHeader) {
        for (int i = 0; i < contentTypeModel.getSize(); i++) {
            ContentTypeComboBoxItem contentTypeItem = (ContentTypeComboBoxItem) contentTypeModel.getElementAt(i);
            if (ctHeader.matches(contentTypeItem.getContentType()) && ctHeader.getParams().equals(contentTypeItem.getContentType().getParams())) {
                return i;
            }
        }
        return -1;
    }

    private static Functions.Unary<String,NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private static class NameValuePair extends AbstractMap.SimpleEntry<String,String>{
        NameValuePair( final String key, final String value ) {
            super( key, value );
        }
    }
}
