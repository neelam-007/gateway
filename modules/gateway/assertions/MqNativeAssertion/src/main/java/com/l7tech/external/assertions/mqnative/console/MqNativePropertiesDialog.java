package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.*;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin.MqNativeTestException;
import com.l7tech.external.assertions.mqnative.MqNativeMessageFormatType;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.CancelableOperationDialog.doWithDelayedCancelDialog;
import static com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType.AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType.ON_COMPLETION;
import static com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType.values;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gui.util.DialogDisplayer.showMessageDialog;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.sort;

/**
 * Dialog for configuring a MQ Native queue.
 */
public class MqNativePropertiesDialog extends JDialog {

    private static final int TAB_INBOUND = 1;
    private static final int TAB_OUTBOUND = 2;
    private static final int DEFAULT_CONCURRENT_LISTENER_SIZE = 20;

    private JPanel contentPane;
    private JRadioButton outboundRadioButton;
    private JRadioButton inboundRadioButton;
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
    private JTextField mqConnectionName;
    private JTextField hostNameTextBox;
    private JTextField portNumberTextField;
    private JTextField channelTextBox;
    private JTextField queueManagerNameTextBox;
    private JTextField queueNameTextBox;
    private JCheckBox credentialsAreRequiredToCheckBox;
    private JCheckBox enableSSLCheckBox;
    private JTextField authUserNameTextBox;

    private JCheckBox clientAuthCheckbox;
    private PrivateKeysComboBox keystoreComboBox;
    private JLabel keystoreLabel;
    private JComboBox cipherSpecCombo;
    private JLabel cipherLabel;
    private JPanel outboundMessagePanel;
    private JTextField modelQueueNameTextField;
    private JLabel modelQueueNameLabel;
    private JButton managePasswordsButton;
    private SecurePasswordComboBox securePasswordComboBox;
    private JPanel byteLimitHolderPanel;
    private JCheckBox enabledCheckBox;
    private JSpinner concurrentListenerSizeSpinner;
    private JCheckBox useConcurrencyCheckBox;
    private JLabel concurrentListenerSizeLabel;
    private SecurityZoneWidget zoneControl;
    private ByteLimitPanel byteLimitPanel;

    private SsgActiveConnector mqNativeActiveConnector;

    private boolean isOk;
    private boolean outboundOnly = false;
    private boolean isClone = false;
    private FormPreparer.ComponentPreparer securityFormAuthorizationPreparer;
    private Logger logger = Logger.getLogger(MqNativePropertiesDialog.class.getName());
    private ContentTypeComboBoxModel contentTypeModel;


    private static final AttemptedOperation CREATE_OPERATION = new AttemptedCreateSpecific(EntityType.SSG_ACTIVE_CONNECTOR, SsgActiveConnector.newWithType(ACTIVE_CONNECTOR_TYPE_MQ_NATIVE));
    private static final AttemptedOperation UPDATE_OPERATION = new AttemptedUpdate(EntityType.SSG_ACTIVE_CONNECTOR, SsgActiveConnector.newWithType(ACTIVE_CONNECTOR_TYPE_MQ_NATIVE));

    //these permissions will have to be added for adding and editing MQ native connections.
//    private PermissionFlags flags;

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
            return this.cType.getMainValue();
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
            return (cType != null ? cType.hashCode() : 0);
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

    //The 'flags' that are commented out below are related to access control for user permissions to create and edit
    //the items in this dialog.  This dialog was originally mostly from JMS impl, this feature should be implemented
    //for MQ native as well.  Time permitting we will handle it.
    private MqNativePropertiesDialog(Window parent) {
        super(parent, DEFAULT_MODALITY_TYPE);
    }

    /**
     * Create a new MqNativePropertiesDialog, configured to adjust the queue defined by the union of the
     * specified connection and endpoint.  The connection and endpoint may be null, in which case a new
     * Queue will be created by the dialog.  After show() returns, check isCanceled() to see whether the user
     * OK'ed the changes.  If so, call getConnection() and getEndpoint() to read them.  If the dialog completes
     * successfully, the (possibly-new) connection and endpoint will already have been saved to the database.
     *
     * @param parent       the parent window for the new dialog.
     * @param mqConnection     the MQ connection endpoint to edit, or null to create a new one for this Queue.
     * @param outboundOnly if true, the direction will be locked and defaulted to Outbound only.
     * @return the new instance
     */
    public static MqNativePropertiesDialog createInstance(Window parent, @Nullable SsgActiveConnector mqConnection, boolean outboundOnly, boolean isClone) {
        final MqNativePropertiesDialog that = new MqNativePropertiesDialog(parent);
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        FormPreparer.ComponentPreparer create = new FormAuthorizationPreparer.GrantToRolePreparer(CREATE_OPERATION, provider);
        FormPreparer.ComponentPreparer update = new FormAuthorizationPreparer.GrantToRolePreparer(UPDATE_OPERATION, provider);

        that.securityFormAuthorizationPreparer = new FormPreparer.CompositePreparer(new FormPreparer.ComponentPreparer[]{create, update});

        that.mqNativeActiveConnector = mqConnection;
        that.setOutboundOnly(outboundOnly);
        that.isClone = isClone;

        final String title;
        if (that.mqNativeActiveConnector == null) {
            that.mqNativeActiveConnector = new SsgActiveConnector();
            that.mqNativeActiveConnector.setEnabled(true); // Initially the queue is set to be enabled by default
            title = "New MQ Native Queue";
        } else {
            title = "MQ Native Queue Properties";
        }

        that.init(title);

        return that;
    }

    private void setOutboundOnly(boolean outboundOnly) {
        this.outboundOnly = outboundOnly;
    }

    private boolean isOutboundOnly() {
        return outboundOnly;
    }

    public SsgActiveConnector getTheMqResource(){
        return mqNativeActiveConnector;
    }

    /**
     * Check how the dialog was closed.
     *
     * @return false iff. the dialog completed successfully via the "Save" button; otherwise true.
     */
    public boolean isCanceled() {
        return !isOk;
    }


    private void init(final String title) {
        setTitle(title);
        setContentPane(contentPane);
        setModal(true);

        outboundMessagePanel.setVisible(false);

        isTemplateQueue.addActionListener(enableDisableListener);

        hostNameTextBox.setDocument(new MaxLengthDocument(255));
        hostNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        portNumberTextField.setDocument(new MaxLengthDocument(255));
        portNumberTextField.getDocument().addDocumentListener( enableDisableListener );
        channelTextBox.setDocument(new MaxLengthDocument(255));
        channelTextBox.getDocument().addDocumentListener( enableDisableListener );

        queueManagerNameTextBox.setDocument(new MaxLengthDocument(255));
        queueManagerNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        queueNameTextBox.setDocument(new MaxLengthDocument(255));
        queueNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        securePasswordComboBox.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());
        securePasswordComboBox.addActionListener(enableDisableListener);

        managePasswordsButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {

                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        securePasswordComboBox.reloadPasswordList();
                        enableOrDisableComponents();
                        DialogDisplayer.pack(MqNativePropertiesDialog.this);
                    }
                });
            }
        });

        Utilities.enableGrayOnDisabled(modelQueueNameTextField);
        credentialsAreRequiredToCheckBox.addActionListener( enableDisableListener );

        authUserNameTextBox.setDocument(new MaxLengthDocument(255));
        authUserNameTextBox.getDocument().addDocumentListener( enableDisableListener );
        Utilities.enableGrayOnDisabled(authUserNameTextBox);

        cipherSpecCombo.setModel(new DefaultComboBoxModel(getCipherSuites()));

        boolean isQueueCredentialRequired = mqNativeActiveConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED);
        credentialsAreRequiredToCheckBox.setSelected(isQueueCredentialRequired);
        authUserNameTextBox.setEnabled(isQueueCredentialRequired);
        securePasswordComboBox.setEnabled(isQueueCredentialRequired);

        inboundRadioButton.setEnabled(!isOutboundOnly());
        outboundRadioButton.setEnabled(!isOutboundOnly());

        inboundRadioButton.addItemListener( enableDisableListener );
        outboundRadioButton.addItemListener( enableDisableListener );

        Utilities.enableGrayOnDisabled(inboundReplySpecifiedQueueField);
        Utilities.enableGrayOnDisabled(outboundReplySpecifiedQueueField);

        inboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));
        failureQueueNameTextField.setDocument(new MaxLengthDocument(128));
        jmsMsgPropWithSoapActionTextField.setDocument(new MaxLengthDocument(255));
        outboundReplySpecifiedQueueField.setDocument(new MaxLengthDocument(128));

        mqConnectionName.setDocument(new MaxLengthDocument(128));
        mqConnectionName.getDocument().addDocumentListener( enableDisableListener );

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
        specifyContentTypeFromHeader.setVisible(false);
        getContentTypeFromProperty.getDocument().addDocumentListener( enableDisableListener );
        getContentTypeFromProperty.setVisible(false);

        byteLimitPanel = new ByteLimitPanel();
        byteLimitPanel.setAllowContextVars(false);
        byteLimitPanel.addChangeListener(new RunOnChangeListener() {
            @Override
            protected void run() {
                enableOrDisableComponents();
            }
        });

        byteLimitHolderPanel.setLayout(new BorderLayout());
        byteLimitHolderPanel.add(byteLimitPanel, BorderLayout.CENTER);

        useConcurrencyCheckBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                concurrentListenerSizeSpinner.setEnabled(useConcurrencyCheckBox.isSelected());
                concurrentListenerSizeLabel.setEnabled(concurrentListenerSizeSpinner.isEnabled());
            }
        });
        concurrentListenerSizeSpinner.setModel(new SpinnerNumberModel(DEFAULT_CONCURRENT_LISTENER_SIZE, 2, 10000, 1));

        final ComponentEnabler outboundEnabler = new ComponentEnabler(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return outboundReplySpecifiedQueueRadioButton.isSelected();
            }
        }, outboundReplySpecifiedQueueField, outboundCorrelationPanel, outboundMessageIdRadioButton, outboundCorrelationIdRadioButton);

        outboundReplySpecifiedQueueRadioButton.addActionListener(outboundEnabler);
        outboundReplyAutomaticRadioButton.addActionListener(outboundEnabler);
        outboundReplyAutomaticRadioButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(outboundReplyAutomaticRadioButton.isSelected()){
                    modelQueueNameLabel.setEnabled(true);
                    modelQueueNameTextField.setEnabled(true);
                }
            }
        });

        outboundReplyNoneRadioButton.addActionListener(outboundEnabler);

        outboundReplyNoneRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modelQueueNameLabel.setEnabled(false);
                modelQueueNameTextField.setEnabled(false);
            }
        });

        outboundReplySpecifiedQueueRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modelQueueNameLabel.setEnabled(false);
                modelQueueNameTextField.setEnabled(false);
            }
        });

        acknowledgementModeComboBox.setModel(new DefaultComboBoxModel(values()));
        acknowledgementModeComboBox.setRenderer(new TextListCellRenderer<MqNativeAcknowledgementType>(new Functions.Unary<String, MqNativeAcknowledgementType>() {
            @Override
            public String call(final MqNativeAcknowledgementType type) {
                String text;

                switch (type) {
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
        acknowledgementModeComboBox.addActionListener(enableDisableListener);
        useQueueForFailedCheckBox.addActionListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(failureQueueNameTextField);
        useJmsMsgPropAsSoapActionRadioButton.addItemListener(enableDisableListener);
        jmsMsgPropWithSoapActionTextField.getDocument().addDocumentListener(enableDisableListener);
        useJmsMsgPropAsSoapActionRadioButton.setVisible(false);
        jmsMsgPropWithSoapActionTextField.setVisible(false);
        jmsMsgPropWithSoapActionLabel.setVisible(false);
        Utilities.enableGrayOnDisabled(jmsMsgPropWithSoapActionTextField);
        associateQueueWithPublishedService.addActionListener( enableDisableListener );

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

        InputValidator inputValidator = new InputValidator(this, title);
        inputValidator.constrainTextFieldToNumberRange("Port number", portNumberTextField, 1L, 65535L );
        inputValidator.attachToButton(saveButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(concurrentListenerSizeSpinner, "Concurrency"));

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        clientAuthCheckbox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(clientAuthCheckbox.isSelected()){
                    keystoreComboBox.setEnabled(true);
                    keystoreLabel.setEnabled(true);
                }else{
                    keystoreComboBox.setEnabled(false);
                    keystoreLabel.setEnabled(false);
                }
            }
        });

        enableSSLCheckBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(enableSSLCheckBox.isSelected()){
                    clientAuthCheckbox.setEnabled(true);
                    cipherLabel.setEnabled(true);
                    cipherSpecCombo.setEnabled(true);
                    if(clientAuthCheckbox.isSelected()){
                        keystoreComboBox.setEnabled(true);
                        keystoreLabel.setEnabled(true);
                    }
                }else{
                    clientAuthCheckbox.setEnabled(false);
                    keystoreComboBox.setEnabled(false);
                    keystoreLabel.setEnabled(false);
                    cipherLabel.setEnabled(false);
                    cipherSpecCombo.setEnabled(false);
                }
            }
        });
        clientAuthCheckbox.setEnabled(false);
        keystoreComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        keystoreComboBox.selectDefaultSsl();
        keystoreComboBox.setEnabled(false);
        zoneControl.configure(EntityType.SSG_ACTIVE_CONNECTOR,
                mqNativeActiveConnector.getOid() == SsgActiveConnector.DEFAULT_OID ? OperationType.CREATE : OperationType.UPDATE,
                mqNativeActiveConnector.getSecurityZone());

        pack();
        initializeView();
        enableOrDisableComponents();
        applyFormSecurity();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void loadContentTypesModel() {
        if (contentTypeModel == null) {
            List<ContentTypeComboBoxItem> items = new ArrayList<ContentTypeComboBoxItem>();
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

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        loadContentTypesModel();
        boolean populatedTheServiceList = false;
        if ( mqNativeActiveConnector != null ) {
            enabledCheckBox.setSelected(mqNativeActiveConnector.isEnabled());

            final String userId = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_USERID);
            final long passwordOid = mqNativeActiveConnector.getLongProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, -1L);

            credentialsAreRequiredToCheckBox.setSelected(!StringUtils.isEmpty(userId) || passwordOid > -1L);
            authUserNameTextBox.setText(userId);
            authUserNameTextBox.setCaretPosition( 0 );
            if(passwordOid > -1L) {
                securePasswordComboBox.setSelectedSecurePassword(passwordOid);
            }

            final boolean isSslEnabled = mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED);
            enableSSLCheckBox.setSelected(isSslEnabled);
            if (isSslEnabled) {
                cipherLabel.setEnabled(true);
                cipherSpecCombo.setEnabled(true);
                clientAuthCheckbox.setEnabled(true);

                final String cipherSuite = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE);
                if(!StringUtils.isEmpty(cipherSuite)){
                    //select the cipher suite from the list
                    cipherSpecCombo.setSelectedItem(cipherSuite);
                }

                final boolean isSslKeyStoreUsed = mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED);
                clientAuthCheckbox.setSelected(isSslKeyStoreUsed);
                if (isSslKeyStoreUsed) {
                    keystoreLabel.setEnabled(true);
                    keystoreComboBox.setEnabled(true);

                    final String sslKeyStoreAlias = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS);
                    final long sslKeyStoreId = mqNativeActiveConnector.getLongProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, -1L);
                    if (!StringUtils.isEmpty(sslKeyStoreAlias) && sslKeyStoreId > -1L) {
                        keystoreComboBox.select(sslKeyStoreId, sslKeyStoreAlias);
                    } else {
                        keystoreComboBox.selectDefaultSsl();
                    }
                } else {
                    keystoreLabel.setEnabled(false);
                    keystoreComboBox.setEnabled(false);
                }
            } else {
                cipherLabel.setEnabled(false);
                cipherSpecCombo.setEnabled(false);
                clientAuthCheckbox.setEnabled(false);
                keystoreLabel.setEnabled(false);
                keystoreComboBox.setEnabled(false);
            }

            hostNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME));
            if(mqNativeActiveConnector.getOid() > -1L || isClone) {
                channelTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL));
            } else {
                channelTextBox.setText("SYSTEM.DEF.SVRCONN");
            }
            isTemplateQueue.setSelected(mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE));
            portNumberTextField.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_PORT));
            queueManagerNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME));
            queueNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME));
            mqConnectionName.setText(mqNativeActiveConnector.getName());

            final boolean isInbound = mqNativeActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND );
            inboundRadioButton.setSelected(isInbound);
            outboundRadioButton.setSelected(!isInbound);

            // inbound options
            if (isInbound) {
                final MqNativeAcknowledgementType acknowledgementType = mqNativeActiveConnector.getEnumProperty(
                        PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, AUTOMATIC, MqNativeAcknowledgementType.class );
                acknowledgementModeComboBox.setSelectedItem(acknowledgementType);
                if ( acknowledgementType == ON_COMPLETION ) {
                    useQueueForFailedCheckBox.setSelected(mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED));
                    String failQueueName = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME);
                    if (!StringUtils.isEmpty(failQueueName)) {
                        failureQueueNameTextField.setText(failQueueName);
                    }
                }

                MqNativeReplyType replyType = mqNativeActiveConnector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class );
                switch(replyType) {
                    case REPLY_NONE:
                        inboundReplyNoneRadioButton.setSelected(true);
                        break;
                    case REPLY_AUTOMATIC:
                        inboundReplyAutomaticRadioButton.setSelected(true);
                        break;
                    case REPLY_SPECIFIED_QUEUE:
                        inboundReplySpecifiedQueueRadioButton.setSelected(true);
                        inboundReplySpecifiedQueueField.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME));
                        break;
                    default:
                        logger.log( Level.WARNING, "Bad state - unknown MQ native replyType = " + replyType );
                        break;
                }

                if(mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST)) {
                    inboundCorrelationIdRadioButton.setSelected(true);
                } else {
                    inboundMessageIdRadioButton.setSelected(true);
                }

                boolean associateQueue;
                if (mqNativeActiveConnector.getHardwiredServiceOid() != null && mqNativeActiveConnector.getHardwiredServiceOid() > -1L){
                    associateQueue = ServiceComboBox.populateAndSelect(serviceNameCombo, true, mqNativeActiveConnector.getHardwiredServiceOid());
                }else{
                    associateQueue = ServiceComboBox.populateAndSelect(serviceNameCombo, true, 0);
                }
                if(associateQueue) {
                    associateQueueWithPublishedService.setSelected(true);
                }
                populatedTheServiceList = true;

                String contentTypeFromProperty = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE_FROM_PROPERTY);
                if(!StringUtils.isEmpty(contentTypeFromProperty)) {
                    specifyContentTypeCheckBox.setSelected(true);
                    specifyContentTypeFromHeader.setSelected(true);
                    getContentTypeFromProperty.setText(contentTypeFromProperty);
                }

                String contentType = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE);
                if (!StringUtils.isEmpty(contentType)) {
                    specifyContentTypeCheckBox.setSelected(true);
                    specifyContentTypeFreeForm.setSelected(true);
                    try {
                        //determine if the content type is a manually entered one, if so, we'll display this content type
                        //value in the editable box
                        ContentTypeHeader ctHeader = ContentTypeHeader.parseValue(contentType);
                        if (findContentTypeInList(ctHeader) != -1) {
                            contentTypeValues.setSelectedItem(new ContentTypeComboBoxItem(ctHeader));
                        } else {
                            contentTypeValues.setSelectedItem(null);
                            contentTypeValues.getEditor().setItem(new ContentTypeComboBoxItem(ctHeader));
                        }

                        specifyContentTypeFreeForm.setSelected(true);
                    } catch (IOException e1) {
                        logger.log(Level.WARNING,
                            MessageFormat.format("Error while parsing the Content-Type for MQ Native queue {0}. Value was {1}", mqNativeActiveConnector.getName(), contentType),
                            getMessage( e1 ));
                    }
                }
                
                if (StringUtils.isEmpty(contentTypeFromProperty) && StringUtils.isEmpty(contentType)) {
                    specifyContentTypeCheckBox.setSelected(false);
                }

                useConcurrencyCheckBox.setSelected(false);
                concurrentListenerSizeSpinner.setValue(DEFAULT_CONCURRENT_LISTENER_SIZE);
                Integer concurrentSize = mqNativeActiveConnector.getIntegerProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE, 1);
                if (concurrentSize > 1) {
                    useConcurrencyCheckBox.setSelected(true);
                    concurrentListenerSizeSpinner.setValue(concurrentSize);
                }
            // outbound options
            } else {
                String msgFormatProp = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_MESSAGE_FORMAT);
                if (msgFormatProp != null) {
                    MqNativeMessageFormatType messageFormatType = MqNativeMessageFormatType.valueOf(msgFormatProp);
                    switch(messageFormatType) {
                        case AUTOMATIC:
                            outboundFormatAutoRadioButton.setSelected(true);
                            break;
                        case BYTES:
                            outboundFormatBytesRadioButton.setSelected(true);
                            break;
                        case TEXT:
                            outboundFormatTextRadioButton.setSelected(true);
                            break;
                        default:
                            logger.log( Level.WARNING, "Bad state - unknown MQ native messageFormatType = " + messageFormatType );
                            break;
                    }
                } else {
                    outboundFormatAutoRadioButton.setSelected(true);
                }

                MqNativeReplyType replyType = mqNativeActiveConnector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE, MqNativeReplyType.class );
                switch(replyType) {
                    case REPLY_NONE:
                        outboundReplyNoneRadioButton.setSelected(true);
                        modelQueueNameLabel.setEnabled(false);
                        modelQueueNameTextField.setEnabled(false);
                        break;
                    case REPLY_AUTOMATIC:
                        outboundReplyAutomaticRadioButton.setSelected(true);
                        modelQueueNameTextField.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN));
                        break;
                    case REPLY_SPECIFIED_QUEUE:
                        modelQueueNameLabel.setEnabled(false);
                        modelQueueNameTextField.setEnabled(false);
                        outboundReplySpecifiedQueueRadioButton.setSelected(true);
                        outboundReplySpecifiedQueueField.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME));
                        if (mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST)) {
                            outboundCorrelationIdRadioButton.setSelected(true);
                        } else {
                            outboundMessageIdRadioButton.setSelected(true);
                        }
                        break;
                    default:
                        logger.log( Level.WARNING, "Bad state - unknown MQ native replyType = " + replyType );
                        break;
                }
            }
        } else {
            enabledCheckBox.setSelected(true);
        }

        byteLimitPanel.setValue(
            mqNativeActiveConnector == null? null : mqNativeActiveConnector.getProperty(PROPERTIES_KEY_REQUEST_SIZE_LIMIT),
            getMqNativeAdmin().getDefaultMqMessageMaxBytes()
        );

        if(!populatedTheServiceList)
            ServiceComboBox.populateAndSelect(serviceNameCombo, true, 0);
        enableOrDisableComponents();
    }

    /**
     * Returns true if the form has enough information to construct a MQ native connection.
     * @return true if form has enough info to construct a MQ native connection
     */
    private boolean validateForm() {
        boolean isValid = true;
        final boolean isTemplate = viewIsTemplate();

        if (mqConnectionName.getText().trim().length() == 0) {
            isValid = false;
        } else if (queueManagerNameTextBox.getText().trim().length() == 0) {
            isValid =  false;
        } if (!isTemplate && queueNameTextBox.getText().trim().length() == 0) {
            isValid =  false;
        } else if (portNumberTextField.getText().trim().length() == 0 || !(isValidNumber(portNumberTextField.getText().trim()))) {
            isValid =  false;
        } else if (hostNameTextBox.getText().trim().length() == 0) {
            isValid =  false;
        } else if (channelTextBox.getText().trim().length() == 0) {
            isValid =  false;
        } else if (credentialsAreRequiredToCheckBox.isSelected() && securePasswordComboBox.getSelectedItem() == null) {
            isValid =  false;
        } else if (outboundRadioButton.isSelected() && !isOutboundPaneValid(isTemplate)) {
            isValid =  false;
        } else if (inboundRadioButton.isSelected() && !isInboundPaneValid()) {
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidNumber(String num){
        try{
            Integer.parseInt(num);
            return true;
        }catch(NumberFormatException nfe){
            return false;
        }
    }

    private boolean viewIsTemplate() {
        return isTemplateQueue.isEnabled() && isTemplateQueue.isSelected();
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

        if (byteLimitPanel.validateFields() != null)
            return false;

        return true;
    }

    /**
     * Adjust components based on the state of the form.
     */
    private void enableOrDisableComponents() {
        if (inboundRadioButton.isSelected()) {
            isTemplateQueue.setEnabled(false);
            useJmsMsgPropAsSoapActionRadioButton.setEnabled(true);
            jmsMsgPropWithSoapActionLabel.setEnabled(useJmsMsgPropAsSoapActionRadioButton.isSelected());
            jmsMsgPropWithSoapActionTextField.setEnabled(useJmsMsgPropAsSoapActionRadioButton.isSelected());
            serviceNameLabel.setEnabled(associateQueueWithPublishedService.isSelected());
            serviceNameCombo.setEnabled(associateQueueWithPublishedService.isSelected());
            tabbedPane.setEnabledAt(TAB_INBOUND, true);
            tabbedPane.setEnabledAt(TAB_OUTBOUND, false);
            enableOrDisableAcknowledgementControls();
            final boolean specified = inboundReplySpecifiedQueueRadioButton.isSelected();
            final boolean auto = inboundReplyAutomaticRadioButton.isSelected();
            inboundReplySpecifiedQueueField.setEnabled(specified);
            inboundMessageIdRadioButton.setEnabled(specified || auto);
            inboundCorrelationIdRadioButton.setEnabled(specified || auto);
            concurrentListenerSizeSpinner.setEnabled(useConcurrencyCheckBox.isEnabled() && useConcurrencyCheckBox.isSelected());
            concurrentListenerSizeLabel.setEnabled(concurrentListenerSizeSpinner.isEnabled());
        } else {
            isTemplateQueue.setEnabled(true);
            tabbedPane.setEnabledAt(TAB_INBOUND, false);
            tabbedPane.setEnabledAt(TAB_OUTBOUND, true);
            outboundReplySpecifiedQueueField.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundMessageIdRadioButton.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
            outboundCorrelationIdRadioButton.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());
        }

        boolean isCredentialsRequired = credentialsAreRequiredToCheckBox.isSelected();
        authUserNameTextBox.setEnabled(isCredentialsRequired);
        securePasswordComboBox.setEnabled(isCredentialsRequired);
        managePasswordsButton.setEnabled(isCredentialsRequired);

        final boolean valid = validateForm();
        saveButton.setEnabled(valid);// && (flags.canCreateSome() || flags.canUpdateSome()));
        testButton.setEnabled(valid && !viewIsTemplate());
        enableContentTypeControls();
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

        securityFormAuthorizationPreparer.prepare(outboundRadioButton);
        securityFormAuthorizationPreparer.prepare(inboundRadioButton);
        securityFormAuthorizationPreparer.prepare(queueNameTextBox);
        securityFormAuthorizationPreparer.prepare(credentialsAreRequiredToCheckBox);
        securityFormAuthorizationPreparer.prepare(authUserNameTextBox);
        securityFormAuthorizationPreparer.prepare(useJmsMsgPropAsSoapActionRadioButton);
        securityFormAuthorizationPreparer.prepare(jmsMsgPropWithSoapActionTextField);
        securityFormAuthorizationPreparer.prepare(acknowledgementModeComboBox);
        securityFormAuthorizationPreparer.prepare(useQueueForFailedCheckBox);
        securityFormAuthorizationPreparer.prepare(failureQueueNameTextField);
        securityFormAuthorizationPreparer.prepare(mqConnectionName);
        securityFormAuthorizationPreparer.prepare(isTemplateQueue);

        securityFormAuthorizationPreparer.prepare(inboundOptionsPanel);
        securityFormAuthorizationPreparer.prepare(outboundOptionsPanel);
    }

    private void onTest() {
        final SsgActiveConnector settings = new SsgActiveConnector();
        try {
            viewToModel(settings);
        } catch ( final MqNativeSettingsException e ) {
            showMessageDialog(
                    this,
                    "Queue settings invalid: " + getMessage( e ),
                    "Error Testing MQ Native Queue",
                    JOptionPane.ERROR_MESSAGE,
                    null );
            return;
        }

        try {
            final Option<? extends Exception> error = doWithDelayedCancelDialog(
                    new Callable<Option<? extends Exception>>() {
                        @Override
                        public Option<? extends Exception> call() {
                            Option<? extends Exception> result = none();
                            try {
                                getMqNativeAdmin().testSettings( settings );
                            } catch ( MqNativeTestException e ) {
                                result = some( e );
                            }

                            // ensure interrupted status is cleared
                            Thread.interrupted();

                            return result;
                        }
                    },
                    this,
                    "Testing Queue Settings",
                    "Testing Queue settings, please wait ...",
                    5000L );
            if ( error.isSome() ) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to verify this MQ Native setting: " + getMessage( error.some() ),
                        "MQ Native Test Failed",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "The Gateway has successfully verified this MQ Native setting.",
                        "MQ Native Test Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch ( InterruptedException e ) {
            // cancelled
        } catch ( InvocationTargetException e ) {
            throw ExceptionUtils.wrap( e.getTargetException() );
        }
    }

    private void viewToModel( final SsgActiveConnector connector ) throws MqNativeSettingsException {
        connector.setEnabled(enabledCheckBox.isSelected());
        connector.setSecurityZone(zoneControl.getSelectedZone());
        connector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        connector.setName(mqConnectionName.getText());

        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, hostNameTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, portNumberTextField.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL, channelTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, queueManagerNameTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, queueNameTextBox.getText());

        //Set security info
        boolean isCredentialsRequired = credentialsAreRequiredToCheckBox.isSelected();
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED, Boolean.toString(isCredentialsRequired));
        if (isCredentialsRequired) {
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_USERID, authUserNameTextBox.getText());
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID,
                Long.toString(securePasswordComboBox.getSelectedSecurePassword().getOid()));
        } else {
            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_USERID);
            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID);
        }

        final boolean sslEnabled = enableSSLCheckBox.isSelected();
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, Boolean.toString(sslEnabled));
        if (sslEnabled) {
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE, (String) cipherSpecCombo.getSelectedItem());

            final boolean isSslKeyStoreUsed = clientAuthCheckbox.isSelected();
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED, Boolean.toString(isSslKeyStoreUsed));
            if (isSslKeyStoreUsed) {
                String keyAlias = keystoreComboBox.getSelectedKeyAlias();
                long keyStoreId = keystoreComboBox.getSelectedKeystoreId();
                if ( keyAlias != null && keyStoreId != -1 ) {
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS, keyAlias);
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, Long.toString(keyStoreId));
                } else {
                    connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS);
                    connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID);
                }
            }
        }

        connector.setProperty( PROPERTIES_KEY_IS_INBOUND, Boolean.toString(inboundRadioButton.isSelected()));
        if (inboundRadioButton.isSelected()) {
            final MqNativeAcknowledgementType acknowledgementType = (MqNativeAcknowledgementType) acknowledgementModeComboBox.getSelectedItem();
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, acknowledgementType.toString());

            boolean isFailedQueueUsed = useQueueForFailedCheckBox.isSelected();
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED, Boolean.toString(isFailedQueueUsed));
            String failedQueueName = failureQueueNameTextField.getText();
            if (!StringUtils.isEmpty(failedQueueName)) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME, failedQueueName);
            } else {
                connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME);
            }

            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME);
            if(inboundReplyAutomaticRadioButton.isSelected())
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC.toString());
            else if (inboundReplyNoneRadioButton.isSelected())
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE.toString());
            else if (inboundReplySpecifiedQueueRadioButton.isSelected()){
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_SPECIFIED_QUEUE.toString());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, inboundReplySpecifiedQueueField.getText());
            }

            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST, Boolean.toString(inboundCorrelationIdRadioButton.isSelected()));

            if(associateQueueWithPublishedService.isSelected()){
                PublishedService svc = ServiceComboBox.getSelectedPublishedService(serviceNameCombo);
                if(svc != null) {
                    connector.setHardwiredServiceOid(svc.getOid());
                }
            } else {
                connector.setHardwiredServiceOid(null);
            }

            if (specifyContentTypeCheckBox.isSelected()) {
                if (specifyContentTypeFromHeader.isSelected()) {
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE_FROM_PROPERTY, getContentTypeFromProperty.getText());
                } else if (specifyContentTypeFreeForm.isSelected()) {
                    //if none of the list is selected and there is a value in the content type,
                    //then we'll use the one that was entered by the user
                    ContentTypeHeader selectedContentType;
                    if (contentTypeValues.getSelectedIndex() == -1 && contentTypeValues.getEditor().getItem() != null) {
                        String ctHeaderString = ((JTextField) contentTypeValues.getEditor().getEditorComponent()).getText();
                        // If the content type is not specified, it will be set as the default type, "text/xml".
                        try {
                            selectedContentType = StringUtils.isEmpty(ctHeaderString)?
                                ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.parseValue(ctHeaderString);
                        } catch ( IOException e ) {
                            throw new MqNativeSettingsException( getMessage( e ), e );
                        }

                        //check if the typed in content type matches to any one of the ones in our list
                        int foundIndex = findContentTypeInList(selectedContentType);
                        if (foundIndex != -1) {
                            selectedContentType = ((ContentTypeComboBoxItem) contentTypeModel.getElementAt(foundIndex)).getContentType();
                        }
                    } else {
                        selectedContentType = ((ContentTypeComboBoxItem) contentTypeValues.getSelectedItem()).getContentType();
                    }

                    if (selectedContentType != null) {
                        connector.setProperty(PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE, selectedContentType.getFullValue());
                    }
                }
            } else {
                connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE_FROM_PROPERTY);
                connector.removeProperty(PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE);
            }

            connector.setProperty(PROPERTIES_KEY_REQUEST_SIZE_LIMIT, byteLimitPanel.getValue());

            if (useConcurrencyCheckBox.isEnabled() && useConcurrencyCheckBox.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE, concurrentListenerSizeSpinner.getValue().toString());
            } else {
                connector.removeProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE);
            }
        } else {
            // else outbound
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE, Boolean.toString(isTemplateQueue.isSelected()));

            if (outboundFormatAutoRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_MESSAGE_FORMAT, MqNativeMessageFormatType.AUTOMATIC.toString());
            } else if (outboundFormatBytesRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_MESSAGE_FORMAT, MqNativeMessageFormatType.BYTES.toString());
            } else if (outboundFormatTextRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_MESSAGE_FORMAT, MqNativeMessageFormatType.TEXT.toString());
            }

            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME);
            if (outboundReplyAutomaticRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC.toString());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN, modelQueueNameTextField.getText());
            } else if (outboundReplySpecifiedQueueRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_SPECIFIED_QUEUE.toString());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, outboundReplySpecifiedQueueField.getText());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST, Boolean.toString(outboundCorrelationIdRadioButton.isSelected()));
            } else if (outboundReplyNoneRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE.toString());
            }
        }
    }

    private void onSave() {
        final TransportAdmin admin = getTransportAdmin();
        if ( admin != null ) {
            try {
                viewToModel(mqNativeActiveConnector);
                admin.saveSsgActiveConnector( mqNativeActiveConnector );
            }  catch (Throwable t) {
                showMessageDialog( this, "Cannot save MQ Native Queue: " + getMessage( t ), "Error Saving MQ Native Queue", JOptionPane.ERROR_MESSAGE, null );
                return;
            }

            isOk = true;
            dispose();
        }
    }

    private TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Admin context not present.");
            return null;
        }
        return registry.getTransportAdmin();
    }

    private static MqNativeAdmin getMqNativeAdmin() {
        return Registry.getDefault().getExtensionInterface(MqNativeAdmin.class, null);
    }

    private void onCancel() {
        dispose();
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
            saveButton.setEnabled(valid);// && (flags.canCreateSome() || flags.canUpdateSome()));
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
            if (ctHeader.equals(contentTypeItem.getContentType())) {
                return i;
            }
        }
        return -1;
    }

    private String[] getCipherSuites(){
        final String[] suites = CipherSuiteGuiUtil.getCipherSuiteNames();
        sort( suites, CASE_INSENSITIVE_ORDER );
        return suites;
    }

    public void selectNameField() {
        mqConnectionName.requestFocus();
        mqConnectionName.selectAll();
    }

    private static final class MqNativeSettingsException extends Exception {
        public MqNativeSettingsException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }
}