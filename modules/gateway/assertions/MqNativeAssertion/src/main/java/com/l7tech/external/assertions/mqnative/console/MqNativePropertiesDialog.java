package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.mqnative.MqNativeMessageFormatType;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.MutablePair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType.ON_COMPLETION;
import static com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType.values;

/**
 * Dialog for configuring a MQ Native queue.
 */
public class MqNativePropertiesDialog extends JDialog {
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
    private JTextField mqConnectionName;
    private JTextField hostNameTextBox;
    private JTextField portNumberTextBox;
    private JTextField channelTextBox;
    private JTextField queueManagerNameTextBox;
    private JTextField queueNameTextBox;
    private JCheckBox credentialsAreRequiredToCheckBox;
    private JCheckBox enableSSLCheckBox;
    private JTextField authUserNameTextBox;

    private JTable advancedPropertiesTable;
    private AdvancedPropertyTableModel advancedTableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
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

    private SsgActiveConnector mqNativeActiveConnector;

    private boolean isOk;
    private boolean outboundOnly = false;
    private FormAuthorizationPreparer securityFormAuthorizationPreparer;
    private Logger logger = Logger.getLogger(MqNativePropertiesDialog.class.getName());
    private ContentTypeComboBoxModel contentTypeModel;

    private Map<String, String> advancedPropertiesMap;

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

    private class AdvancedPropertyTableModel extends AbstractTableModel {
        private static final int MAX_TABLE_COLUMN_NUM = 2;

        AdvancedPropertyTableModel(){
            initAdvancedProperties();
        }

        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public int getRowCount() {
            return advancedPropertiesMap.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Property Name";
                case 1:
                    return "Value";
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            String name = (String) advancedPropertiesMap.keySet().toArray()[row];

            switch (col) {
                case 0:
                    return name;
                case 1:
                    return advancedPropertiesMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        private void initAdvancedProperties() {
            advancedPropertiesMap = new TreeMap<String, String>();

            if(mqNativeActiveConnector != null) {
                for (String property : mqNativeActiveConnector.getPropertyNames()) {
                    if (property.startsWith( PROPERTIES_KEY_MQ_NATIVE_ADVANCED_PROPERTY_PREFIX) &&
                        property.length() > PROPERTIES_KEY_MQ_NATIVE_ADVANCED_PROPERTY_PREFIX.length() + 1) {
                        advancedPropertiesMap.put(
                            property.substring(PROPERTIES_KEY_MQ_NATIVE_ADVANCED_PROPERTY_PREFIX.length()),
                            mqNativeActiveConnector.getProperty(property)
                        );
                    }
                }
            }
        }
    }

    //The 'flags' that are commented out below are related to access control for user permissions to create and edit
    //the items in this dialog.  This dialog was originally mostly from JMS impl, this feature should be implemented
    //for MQ native as well.  Time permitting we will handle it.
    private MqNativePropertiesDialog(Frame parent) {
        super(parent, true);
    }

    private MqNativePropertiesDialog(Dialog parent) {
        super(parent, true);
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
    public static MqNativePropertiesDialog createInstance(Window parent, @Nullable SsgActiveConnector mqConnection, boolean outboundOnly) {
        MqNativePropertiesDialog that;
        if (parent instanceof Frame)
            that = new MqNativePropertiesDialog((Frame)parent);
        else if (parent instanceof Dialog)
            that = new MqNativePropertiesDialog((Dialog)parent);
        else
            throw new IllegalArgumentException("parent must be derived from either Frame or Dialog");
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        that.securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, new AttemptedCreate(EntityType.JMS_ENDPOINT));
        that.mqNativeActiveConnector = mqConnection;
        that.setOutboundOnly(outboundOnly);

        if(that.mqNativeActiveConnector ==null)
            that.mqNativeActiveConnector = new SsgActiveConnector();

        that.init();

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


    private void init() {
        setTitle(mqNativeActiveConnector == null ? "Add MQ Native Queue" : "MQ Native Queue Properties");
        setContentPane(contentPane);
        setModal(true);

        testButton.setVisible(false);
        outboundMessagePanel.setVisible(false);
                                                                                                        
        hostNameTextBox.setDocument(new MaxLengthDocument(255));
        hostNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        portNumberTextBox.setDocument(new MaxLengthDocument(255));
        portNumberTextBox.getDocument().addDocumentListener( enableDisableListener );
        channelTextBox.setDocument(new MaxLengthDocument(255));
        channelTextBox.setText("SYSTEM.DEF.SVRCONN");
        channelTextBox.getDocument().addDocumentListener( enableDisableListener );

        queueManagerNameTextBox.setDocument(new MaxLengthDocument(255));
        queueManagerNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        queueNameTextBox.setDocument(new MaxLengthDocument(255));
        queueNameTextBox.getDocument().addDocumentListener( enableDisableListener );

        managePasswordsButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {

                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                securePasswordComboBox.reloadPasswordList();
                DialogDisplayer.pack(MqNativePropertiesDialog.this);
            }
        });

        Utilities.enableGrayOnDisabled(modelQueueNameTextField);        
        credentialsAreRequiredToCheckBox.addActionListener( enableDisableListener );

        authUserNameTextBox.setDocument(new MaxLengthDocument(255));
        authUserNameTextBox.getDocument().addDocumentListener( enableDisableListener );
        Utilities.enableGrayOnDisabled(authUserNameTextBox);

        cipherSpecCombo.setModel(new DefaultComboBoxModel(getCipherSuites()));

        boolean isSslEnabled = mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED);
        enableSSLCheckBox.setSelected(isSslEnabled);
        cipherLabel.setEnabled(isSslEnabled);
        cipherSpecCombo.setEnabled(isSslEnabled);

        boolean isSslKeyStoreUsed = isSslEnabled && mqNativeActiveConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED);
        keystoreLabel.setEnabled(isSslKeyStoreUsed);
        keystoreComboBox.setEnabled(isSslKeyStoreUsed);

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

        outboundReplyNoneRadioButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                    modelQueueNameLabel.setEnabled(false);
                    modelQueueNameTextField.setEnabled(false);
            }
        });

        outboundReplySpecifiedQueueRadioButton.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                            modelQueueNameLabel.setEnabled(false);
                            modelQueueNameTextField.setEnabled(false);
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

        saveButton.addActionListener(new ActionListener() {
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

        advancedPropertiesTable.setModel(getAdvancedPropertyTableModel());

        advancedPropertiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if(advancedPropertiesTable.getSelectedRow()<0){
                        editButton.setEnabled(false);
                        removeButton.setEnabled(false);
                    }
                    else{
                        editButton.setEnabled(true);
                        removeButton.setEnabled(true);
                    }
                }
            });

        advancedPropertiesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == 1)
                    editAdvProp();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addAdvProp();
            }
        });

        editButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent event) {
                 editAdvProp();
             }
        });
        editButton.setEnabled(false);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeAdvProp();
            }
        });
        removeButton.setEnabled(false);

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
        keystoreComboBox.selectDefaultSsl();
        keystoreComboBox.setEnabled(false);


        pack();
        initializeView();
        enableOrDisableComponents();
        applyFormSecurity();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void addAdvProp() {
        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(null, advancedPropertiesMap);
        dialog.setTitle("Advanced Property");
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (!dialog.isCanceled()) {
                    updatePropertiesList(dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        });
    }

    private void editAdvProp() {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        String value = advancedPropertiesMap.get(name);

        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(new MutablePair<String, String>(name, value), advancedPropertiesMap);
        dialog.setTitle("Advanced Property");
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (!dialog.isCanceled()) {
                    updatePropertiesList(dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        });
    }

    private void removeAdvProp() {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        String value = advancedPropertiesMap.get(name);
        updatePropertiesList(new MutablePair<String, String>(name, value), true);
    }

    private AdvancedPropertyTableModel getAdvancedPropertyTableModel() {
        if (advancedTableModel == null) {
            advancedTableModel = new AdvancedPropertyTableModel();
        }
        return advancedTableModel;
    }

    private void updatePropertiesList(final MutablePair<String, String> selectedProperty, boolean deleted) {
        ArrayList<String> keyset = new ArrayList<String>();
        int currentRow;

        if (deleted) {
            keyset.addAll(advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
            advancedPropertiesMap.remove(selectedProperty.left);
        } else {
            advancedPropertiesMap.put(selectedProperty.left, selectedProperty.right);
            keyset.addAll(advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
        }

        // Refresh the table
        advancedTableModel.fireTableDataChanged();

        // Refresh the selection highlight
        if (currentRow == advancedPropertiesMap.size()) currentRow--; // If the previous deleted row was the last row
        if (currentRow >= 0) advancedPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
    }

    private void loadContentTypesModel() {
        if (contentTypeModel == null) {
            List<ContentTypeComboBoxItem> items = new ArrayList<ContentTypeComboBoxItem>();
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.XML_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.SOAP_1_2_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.TEXT_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.OCTET_STREAM_DEFAULT));
            try {
                items.add(new ContentTypeComboBoxItem(ContentTypeHeader.parseValue("application/fastinfoset")));
            } catch (IOException e) {
                logger.warning("Error trying to initialize content-type application/fastinfoset");
            }
            try {
                items.add(new ContentTypeComboBoxItem(ContentTypeHeader.parseValue("application/soap+fastinfoset")));
            } catch (IOException e) {
                logger.warning("Error trying to initialize content-type application/soap+fastinfoset");
            }

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

    private MqNativeExtraPropertiesPanel getExtraPropertiesPanel(String extraPropertiesClass, Properties extraProperties) {
        try {
            return extraPropertiesClass == null ? null : (MqNativeExtraPropertiesPanel)
                    Class.forName(extraPropertiesClass).getDeclaredConstructor(Properties.class).newInstance(extraProperties);
        } catch (Exception e) {
            DialogDisplayer.showMessageDialog(this, "Error getting default settings for provider: " + extraPropertiesClass, "MQ Native Extra Properties Error", JOptionPane.ERROR_MESSAGE, null);
        }
        return null;
    }

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

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
            if (!isTemplate && (t == null || t.length() == 0) ) throw new IllegalStateException(what + " Specified Queue name must be set");
            ep.setReplyToQueueName(t);
        } else {
            throw new IllegalStateException(what + " was selected, but no reply type was selected");
        }
    }

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        loadContentTypesModel();
        boolean populatedTheServiceList = false;
        if ( mqNativeActiveConnector != null ) {
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
            if(isSslEnabled) {
                cipherLabel.setEnabled(true);
                cipherSpecCombo.setEnabled(true);
                clientAuthCheckbox.setEnabled(true);

                final String cipherSuite = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE);
                if(!StringUtils.isEmpty(cipherSuite)){
                    //select the cipher suite from the list
                    cipherSpecCombo.setSelectedItem(cipherSuite);
                }

                final boolean isSslKeyStoreUsed = mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED);
                if (isSslKeyStoreUsed) {
                    clientAuthCheckbox.setSelected(true);
                    keystoreComboBox.setEnabled(true);

                    final String sslKeyStoreAlias = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS);
                    final long sslKeyStoreId = mqNativeActiveConnector.getLongProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, -1L);
                    if (!StringUtils.isEmpty(sslKeyStoreAlias) && sslKeyStoreId > -1L) {
                        keystoreComboBox.select(sslKeyStoreId, sslKeyStoreAlias);
                    }
                }
            }

            hostNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME));
            if(mqNativeActiveConnector.getOid() > -1L) {
                channelTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL));
            }
            isTemplateQueue.setSelected(mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE));
            portNumberTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_PORT));
            queueManagerNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME));
            queueNameTextBox.setText(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME));
            mqConnectionName.setText(mqNativeActiveConnector.getName());

            final boolean isInbound = mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_INBOUND);
            inboundRadioButton.setSelected(isInbound);
            outboundRadioButton.setSelected(!isInbound);

            // inbound options
            if (isInbound) {
                JmsAcknowledgementType acknowledgementType = JmsAcknowledgementType.valueOf(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE));
                if (acknowledgementType != null) {
                    acknowledgementModeComboBox.setSelectedItem(acknowledgementType);
                    if ( acknowledgementType == ON_COMPLETION ) {
                        String failQueueName = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME);
                        if (!StringUtils.isEmpty(failQueueName)) {
                            useQueueForFailedCheckBox.setSelected(true);
                            failureQueueNameTextField.setText(failQueueName);
                        }
                    }
                }

                MqNativeReplyType replyType = MqNativeReplyType.valueOf(mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE));
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

                String contentType = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE);
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
                                MessageFormat.format("Error while parsing the Content-Type for Mq Destination {0}. Value was {1}", mqNativeActiveConnector.getName(), contentType),
                                ExceptionUtils.getMessage(e1));
                    }
                }

                disableListeningTheQueueCheckBox.setSelected(!mqNativeActiveConnector.isEnabled());
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

                String replyTypeProp = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE);
                if (replyTypeProp != null) {
                    MqNativeReplyType replyType = MqNativeReplyType.valueOf(replyTypeProp);
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
                        default:
                            logger.log( Level.WARNING, "Bad state - unknown MQ native replyType = " + replyType );
                            break;
                    }
                } else {
                    outboundReplyNoneRadioButton.setSelected(true);
                    modelQueueNameLabel.setEnabled(false);
                    modelQueueNameTextField.setEnabled(false);
                }
            }
        }

        if(!populatedTheServiceList)
             ServiceComboBox.populateAndSelect(serviceNameCombo, true, 0);
        enableOrDisableComponents();
    }


    /**
     * Get the text from the given field, null if empty.
     * @param textField a JTextField
     * @return a string or null
     */
    private static String getTextOrNull( final JTextField textField ) {
        final String text = textField.getText();
        return (text == null || text.trim().isEmpty()) ? null : text;
    }

    /**
     * Returns true if the form has enough information to construct a MQ native connection.
     * @return true if form has enough info to construct a MQ native connection
     */
    private boolean validateForm() {
        boolean isTemplate = viewIsTemplate();
        if (mqConnectionName.getText().trim().length() == 0)
            return false;
        if (queueManagerNameTextBox.getText().trim().length() == 0)
            return false;
        if (!isTemplate && queueNameTextBox.getText().trim().length() == 0)
            return false;
        if (portNumberTextBox.getText().trim().length() == 0 || !(isValidNumber(portNumberTextBox.getText().trim())))
            return false;
        if (hostNameTextBox.getText().trim().length() == 0)
            return false;
        if (!isTemplate && channelTextBox.getText().trim().length() == 0)
            return false;
        if (credentialsAreRequiredToCheckBox.isSelected() && (authUserNameTextBox.getText().trim().length()==0))
            return false;
        return true;
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
            tabbedPane.setEnabledAt(2, true);
            tabbedPane.setEnabledAt(3, false);
            enableOrDisableAcknowledgementControls();
            final boolean specified = inboundReplySpecifiedQueueRadioButton.isSelected();
            final boolean auto = inboundReplyAutomaticRadioButton.isSelected();
            inboundReplySpecifiedQueueField.setEnabled(specified);
            inboundMessageIdRadioButton.setEnabled(specified || auto);
            inboundCorrelationIdRadioButton.setEnabled(specified || auto);
        } else {
            isTemplateQueue.setEnabled(true);
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setEnabledAt(3, true);
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
        securityFormAuthorizationPreparer.prepare(new Component[]{
                outboundRadioButton,
                inboundRadioButton,
                queueNameTextBox,
                credentialsAreRequiredToCheckBox,
                authUserNameTextBox,
                useJmsMsgPropAsSoapActionRadioButton,
                jmsMsgPropWithSoapActionTextField,
                acknowledgementModeComboBox,
                useQueueForFailedCheckBox,
                failureQueueNameTextField,
                mqConnectionName,
                isTemplateQueue,
        });
        securityFormAuthorizationPreparer.prepare(inboundOptionsPanel);
        securityFormAuthorizationPreparer.prepare(outboundOptionsPanel);
    }

    private void onTest() {
//        try {
//            final JmsConnection newConnection = makeMQConnectionFromView();
//            if (newConnection == null)
//                return;
//
//            final JmsEndpoint newEndpoint = makeMQEndpointFromView();
//            if (newEndpoint == null)
//                return;

//            Registry.getDefault().getJmsManager().testEndpoint(newConnection, newEndpoint);
//            JOptionPane.showMessageDialog(MqNativeConnectorPropertiesDialog.this,
//              "The Gateway has verified the existence of this WebSphere MQ Queue.",
//              "WebSphere MQ Connection Successful",
//              JOptionPane.INFORMATION_MESSAGE);
//        } catch (Exception ex) {
//            String errorMsg = (ExceptionUtils.causedBy(ex, JmsNotSupportTopicException.class))?
//                    ex.getMessage() : "The Gateway was unable to find this WebSphere MQ Queue.\n";
//            JOptionPane.showMessageDialog(MqNativeConnectorPropertiesDialog.this,
//              errorMsg,
//              "WebSphere MQ Connection Settings",
//              JOptionPane.ERROR_MESSAGE);
//        }
    }


    private void viewToModel(final SsgActiveConnector connector) throws IOException {
        connector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        connector.setName(mqConnectionName.getText());

        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, hostNameTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, portNumberTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL, channelTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, queueManagerNameTextBox.getText());
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, queueNameTextBox.getText());

        //Set security info
        boolean isCredentialsRequired = credentialsAreRequiredToCheckBox.isSelected();
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED, Boolean.toString(isCredentialsRequired));
        if (isCredentialsRequired) {
            if(authUserNameTextBox.getText().trim().length() > 0) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_USERID, authUserNameTextBox.getText());
            }
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID,
                    Long.toString(securePasswordComboBox.getSelectedSecurePassword().getOid()));
        } else {
            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_USERID);
            connector.removeProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID);
        }
        if(enableSSLCheckBox.isSelected()){
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, Boolean.TRUE.toString());
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE, (String) cipherSpecCombo.getSelectedItem());
            if(clientAuthCheckbox.isSelected()){
                String keyAlias = keystoreComboBox.getSelectedKeyAlias();
                long keyStoreId = keystoreComboBox.getSelectedKeystoreId();
                if ( keyAlias != null && keyStoreId != -1 ) {
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS, keyAlias);
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, Long.toString(keyStoreId));
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED, Boolean.TRUE.toString());
                }
            }
        }

        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_INBOUND, Boolean.toString(inboundRadioButton.isSelected()));
        if (inboundRadioButton.isSelected()) {
            JmsAcknowledgementType acknowledgementType = (JmsAcknowledgementType) acknowledgementModeComboBox.getSelectedItem();
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, acknowledgementType.toString());

                if (useQueueForFailedCheckBox.isSelected() && failureQueueNameTextField.getText().trim().length() > 0) {
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME, failureQueueNameTextField.getText());
                }

                if(inboundReplyAutomaticRadioButton.isSelected())
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_AUTOMATIC.toString());
                else if (inboundReplyNoneRadioButton.isSelected())
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_NONE.toString());
                else if (inboundReplySpecifiedQueueRadioButton.isSelected()){
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_SPECIFIED_QUEUE.toString());
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, inboundReplySpecifiedQueueField.getText());
                }

                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST, Boolean.toString(inboundCorrelationIdRadioButton.isSelected()));

                if(associateQueueWithPublishedService.isSelected()){
                     PublishedService svc = ServiceComboBox.getSelectedPublishedService(serviceNameCombo);
                     if(svc != null) {
                        connector.setHardwiredServiceOid(svc.getOid());
                     }
                }

                if (specifyContentTypeCheckBox.isSelected() && specifyContentTypeFromHeader.isSelected()) {
                    connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE_FROM_PROPERTY, getContentTypeFromProperty.getText());
                } else if (specifyContentTypeCheckBox.isSelected() && specifyContentTypeFreeForm.isSelected()) {
                    //if none of the list is selected and there is a value in the content type,
                    //then we'll use the one that was entered by the user
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
                        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE, selectedContentType.getFullValue());
                    }
                }

            connector.setEnabled(!disableListeningTheQueueCheckBox.isSelected());
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

            if (outboundReplyAutomaticRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_AUTOMATIC.toString());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN, modelQueueNameTextField.getText());
            } else if (outboundReplySpecifiedQueueRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_SPECIFIED_QUEUE.toString());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, outboundReplySpecifiedQueueField.getText());
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST, Boolean.toString(outboundCorrelationIdRadioButton.isSelected()));
            } else if (outboundReplyNoneRadioButton.isSelected()) {
                connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_NONE.toString());
            }
        }

        // Save advanced properties
        for (String name: advancedPropertiesMap.keySet()) {
            connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_ADVANCED_PROPERTY_PREFIX + name, advancedPropertiesMap.get(name));
        }
    }

    private void onSave() {
        final TransportAdmin admin = getTransportAdmin();
        if ( admin != null ) {
            try {
                viewToModel(mqNativeActiveConnector);
                admin.saveSsgActiveConnector( mqNativeActiveConnector );
            }  catch (Throwable t) {
                DialogDisplayer.showMessageDialog(this, "Cannot save MQ Native Queue: " + ExceptionUtils.getMessage(t), "Error Saving MQ Native Queue", JOptionPane.ERROR_MESSAGE, null);
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

    public void selectField() {
        mqConnectionName.requestFocus();
        mqConnectionName.selectAll();
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
        return new String[]{"SSL_RSA_WITH_NULL_MD5", "SSL_RSA_WITH_NULL_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5", "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT1024_WITH_RC4_56_SHA", "SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_FIPS_WITH_DES_CBC_SHA", "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA"};
    }
}