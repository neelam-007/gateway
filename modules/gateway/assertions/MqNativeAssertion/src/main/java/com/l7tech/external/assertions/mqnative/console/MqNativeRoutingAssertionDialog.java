package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ByteLimitPanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.RoutingDialogUtils.*;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gui.util.Utilities.comboBoxModel;
import static com.l7tech.gui.util.Utilities.enableGrayOnDisabled;
import static com.l7tech.objectmodel.imp.PersistentEntityUtil.oid;
import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import static com.l7tech.util.Functions.*;
import com.l7tech.util.MutablePair;
import com.l7tech.util.Pair;
import static com.l7tech.util.TextUtils.truncStringMiddleExact;
import static com.l7tech.util.ValidationUtils.isValidInteger;
import static java.util.Collections.emptyList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assertion properties edit dialog for the MQ routing assertion.
 */
public class MqNativeRoutingAssertionDialog extends AssertionPropertiesOkCancelSupport<MqNativeRoutingAssertion> {

    //- PUBLIC

    public MqNativeRoutingAssertionDialog( final Window parent,
                                           final MqNativeRoutingAssertion assertion ) {
        super(MqNativeRoutingAssertion.class, parent, assertion, true);
        this.assertion = assertion;
        initComponents(false);
        setData( assertion );
    }

    @Override
    public MqNativeRoutingAssertion getData( final MqNativeRoutingAssertion assertion ) {
        viewToModel( assertion );
        return assertion;
    }

    @Override
    public void setData( final MqNativeRoutingAssertion assertion ) {
        modelToView( assertion );
    }

    //- PROTECTED

    @Override
    protected ActionListener createOkAction() {
        // returns a no-op action so we can add our own Ok listener
        return new RunOnChangeListener();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MqNativeRoutingAssertionDialog.class.getName());

    private JPanel mainPanel;
    private JButton newQueueButton;
    private JComboBox queueComboBox;
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JPanel dynamicPropertiesPanel;
    private JTextField dynamicDestQueueName;
    private JTextField dynamicReplyQueueName;
    private JTextField mqResponseTimeout;
    private JPanel responseByteLimitHolderPanel;
    private ByteLimitPanel responseByteLimitPanel;
    private JRadioButton putToQueueRadioButton;
    private JRadioButton getFromQueueRadioButton;
    private JComboBox messageSourceComboBox;
    private JComboBox messageTargetComboBox;
    private JPanel targetMessageVariableHolderPanel;
    private JCheckBox sourcePassThroughHeadersCheckBox;
    private JCheckBox targetPassThroughHeadersCheckBox;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JTable advancedPropertiesTable;
    private AdvancedPropertyTableModel advancedTableModel;
    private TargetVariablePanel targetMessageVariablePanel;

    private AbstractButton[] secHdrButtons = {wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };
    private MqNativeRoutingAssertion assertion;
    private Collection<SsgActiveConnector> queueItems;

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    /**
     * This method is called from within the static factory to
     * initialize the form.
     *
     * @param readOnly: all gui components are not editable if true
     */
    private void initComponents(boolean readOnly) {
        super.initComponents();

        Utilities.setEscKeyStrokeDisposes(this);

        enableGrayOnDisabled(
                dynamicDestQueueName,
                dynamicReplyQueueName );

        // queue info
        queueComboBox.setModel(comboBoxModel( getQueueItems() ));
        queueComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                populateDynamicPropertyFields();
                applyDynamicAssertionPropertyOverrides();
                enableOrDisableComponents();
            }
        });
        final TextListCellRenderer<SsgActiveConnector> renderer =  new TextListCellRenderer<SsgActiveConnector>( info() );
        renderer.setRenderClipped( true );
        renderer.setSmartTooltips( true );
        queueComboBox.setRenderer( renderer );
        newQueueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final MqNativePropertiesDialog mqQueuePropertiesDialog = MqNativePropertiesDialog.createInstance(MqNativeRoutingAssertionDialog.this, null, false);
                mqQueuePropertiesDialog.pack();
                Utilities.centerOnParentWindow( mqQueuePropertiesDialog );
                DialogDisplayer.display(mqQueuePropertiesDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (! mqQueuePropertiesDialog.isCanceled()) {
                            queueComboBox.setModel(comboBoxModel(loadQueueItems()));
                            queueComboBox.getModel().setSelectedItem(mqQueuePropertiesDialog.getTheMqResource());
                        }
                    }
                });
            }
        });

        InputValidator inputValidator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        inputValidator.ensureComboBoxSelection( "MQ Destination", queueComboBox );
        inputValidator.constrainTextFieldToBeNonEmpty( "Queue name", dynamicDestQueueName, null );
        inputValidator.constrainTextFieldToBeNonEmpty( "Reply queue name", dynamicReplyQueueName, null );
        inputValidator.constrainTextField(mqResponseTimeout, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String errMsg = null;
                final String uiResponseTimeout = mqResponseTimeout.getText().trim();
                if ( !isValidInteger( uiResponseTimeout, true, 1, Integer.MAX_VALUE ) &&
                     getReferencedNames( uiResponseTimeout ).length == 0  ) {
                    errMsg = "The value for the response timeout must be a valid positive number or use context variables.";
                }
                return errMsg;
            }
        });

        tagSecurityHeaderHandlingButtons( secHdrButtons );

        // Message Properties
        putToQueueRadioButton.addActionListener(enableDisableListener);
        getFromQueueRadioButton.addActionListener(enableDisableListener);

        messageSourceComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", "Message Variable"), null, true ) );
        messageSourceComboBox.addActionListener( enableDisableListener );
        messageTargetComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", "Message Variable"), null, true ) );
        messageTargetComboBox.addActionListener( enableDisableListener );

        targetMessageVariablePanel = new TargetVariablePanel();
        targetMessageVariableHolderPanel.setLayout(new BorderLayout());
        targetMessageVariableHolderPanel.add(targetMessageVariablePanel, BorderLayout.CENTER);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return targetMessageVariablePanel.getErrorMessage();
            }
        });

        inputValidator.attachToButton( getOkButton(), super.createOkAction() );

        advancedPropertiesTable.setModel( getAdvancedPropertyTableModel() );
        advancedPropertiesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        advancedPropertiesTable.getTableHeader().setReorderingAllowed( false );

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

        Utilities.setDoubleClickAction( advancedPropertiesTable, editButton );

        // Override message size
        responseByteLimitPanel = new ByteLimitPanel();
        responseByteLimitPanel.setAllowContextVars(true);
        responseByteLimitPanel.addChangeListener(new RunOnChangeListener() {
            @Override
            protected void run() {
                enableOrDisableComponents();
            }
        });

        responseByteLimitHolderPanel.setLayout(new BorderLayout());
        responseByteLimitHolderPanel.add(responseByteLimitPanel, BorderLayout.CENTER);

        getOkButton().setEnabled( !readOnly );

        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                MqNativeRoutingAssertionDialog.this.dispose();
            }
        });

        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {
        final boolean isPutToQueue = putToQueueRadioButton.isSelected();
        messageSourceComboBox.setEnabled(isPutToQueue);
        sourcePassThroughHeadersCheckBox.setEnabled(isPutToQueue);

        final boolean isMessageTargetEnabled =
                !isPutToQueue || isConfiguredForReply( (SsgActiveConnector) queueComboBox.getSelectedItem() );
        messageTargetComboBox.setEnabled(isMessageTargetEnabled);
        targetPassThroughHeadersCheckBox.setEnabled(isMessageTargetEnabled);
        targetMessageVariablePanel.setEnabled(
            (messageTargetComboBox.isEnabled() &&  messageTargetComboBox.getSelectedItem() != null && ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget() == TargetMessageType.OTHER)
        );

        final boolean enableAdvancedPropertyContextualControls = isPutToQueue && advancedPropertiesTable.getSelectedRow() >= 0;
        advancedPropertiesTable.setEnabled( isPutToQueue );
        addButton.setEnabled( isPutToQueue );
        editButton.setEnabled( enableAdvancedPropertyContextualControls );
        removeButton.setEnabled( enableAdvancedPropertyContextualControls );

        final boolean valid = responseByteLimitPanel.validateFields() == null;
        getOkButton().setEnabled(valid);
    }

    private boolean isConfiguredForReply(SsgActiveConnector connector) {
        if ( connector != null ) {
            final MqNativeReplyType mqNativeReplyType = connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class );
            return mqNativeReplyType == REPLY_AUTOMATIC || mqNativeReplyType == REPLY_SPECIFIED_QUEUE;
        }
        return false;
    }

    private void populateDynamicPropertyFields() {
        Utilities.setEnabled( dynamicPropertiesPanel, false );

        final SsgActiveConnector selected = (SsgActiveConnector) queueComboBox.getSelectedItem();
        if ( selected != null ) {
            final String selectedQueueName = selected.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, "" );
            final String selectedReplyQueueName = selected.getProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, "" );
            if ( selected.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE ) ) {
                Utilities.setEnabled(dynamicPropertiesPanel, true);
                final MqNativeReplyType mqNativeReplyType =
                        selected.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class );
                final boolean enableReplyQueueConfig = mqNativeReplyType == REPLY_SPECIFIED_QUEUE;
                setTextAndEnable( selectedQueueName, dynamicDestQueueName, true );
                setTextAndEnable( selectedReplyQueueName, dynamicReplyQueueName, enableReplyQueueConfig );
            } else {
                setText( selectedQueueName, dynamicDestQueueName );
                setText( selectedReplyQueueName, dynamicReplyQueueName );
            }
        }
    }

    private void setText( final String value, final JTextComponent textComponent ) {
        textComponent.setText( value );
        textComponent.setCaretPosition( 0 );
    }

    private void setTextAndEnable( final String value, final JTextField textField, final boolean canEnable ) {
        boolean templateValueSpecified = !value.trim().isEmpty();
        setText( value, textField );
        textField.setEnabled( canEnable && !templateValueSpecified );
    }

    private void setIfDynamic( final String value, final String defaultValue, final JTextField textField ) {
        String text = value==null ? defaultValue : value;
        if ( text != null && !text.isEmpty() && (textField.getText()==null || textField.getText().isEmpty()) ) {
            setText( value, textField );
        }
    }

    private void applyDynamicAssertionPropertyOverrides() {
        final MqNativeDynamicProperties mqNativeDynamicProperties = assertion != null ?
                assertion.getDynamicMqRoutingProperties() :
                null;
        if ( mqNativeDynamicProperties != null ) {
            final SsgActiveConnector selected = (SsgActiveConnector)queueComboBox.getSelectedItem();
            if ( selected != null ) {
                if ( assertion.getSsgActiveConnectorId()!=null && selected.getOid() == assertion.getSsgActiveConnectorId() ) {
                    setIfDynamic( mqNativeDynamicProperties.getQueueName(), "", dynamicDestQueueName );
                    setIfDynamic( mqNativeDynamicProperties.getReplyToQueue(), "", dynamicReplyQueueName );
                }
            }
        }
    }

    private Collection<SsgActiveConnector> loadQueueItems() {
        try {
            final TransportAdmin transportAdmin = Registry.getDefault().getTransportAdmin();
            return grep( transportAdmin.findSsgActiveConnectorsByType( ACTIVE_CONNECTOR_TYPE_MQ_NATIVE ),
                    negate( booleanProperty( PROPERTIES_KEY_IS_INBOUND ) ) );
        } catch ( IllegalStateException e ) {
            // no admin context available
            logger.info( "Unable to access queues from server." );
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading queues" );
        }
        return emptyList();
    }

    // This method will get a list of queue from the cache, so the queue list is probably stale.
    // To get a fresh list of queues, the method loadQueueItems should be used instead.
    private Collection<SsgActiveConnector> getQueueItems() {
        if (queueItems == null)
            queueItems = loadQueueItems();
        return queueItems;
    }

    private String getIfDynamicPropertyAllowed( final SsgActiveConnector connector,
                                                final String property,
                                                final JTextComponent component ) {
        return connector.getProperty( property, "" ).isEmpty() ? component.getText() : null;
    }

    private void viewToModel( final MqNativeRoutingAssertion assertion ) {
        configSecurityHeaderHandling( assertion, RoutingAssertion.CLEANUP_CURRENT_SECURITY_HEADER, secHdrButtons );
        SsgActiveConnector item = (SsgActiveConnector) queueComboBox.getSelectedItem();
        if ( item == null ) {
            assertion.setSsgActiveConnectorId( null );
            assertion.setSsgActiveConnectorName( null );
        } else {
            assertion.setSsgActiveConnectorId( item.getOidAsLong() );
            assertion.setSsgActiveConnectorName( item.getName() );

            MqNativeDynamicProperties dynProps = null;
            if ( item.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE ) ) {
                dynProps = new MqNativeDynamicProperties();
                dynProps.setQueueName( getIfDynamicPropertyAllowed( item,
                        PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, dynamicDestQueueName ) );
                dynProps.setReplyToQueue( getIfDynamicPropertyAllowed( item,
                        PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, dynamicReplyQueueName ) );
            }
            assertion.setDynamicMqRoutingProperties(dynProps);
        }

        assertion.setPutToQueue(putToQueueRadioButton.isSelected());
        final MessageTargetableSupport sourceMessageTargetable = new MessageTargetableSupport((MessageTargetable) messageSourceComboBox.getSelectedItem());
        assertion.setRequestTarget(sourceMessageTargetable);
        assertion.getRequestMqNativeMessagePropertyRuleSet().setPassThroughHeaders(sourcePassThroughHeadersCheckBox.isSelected());

        final MessageTargetableSupport targetMessageTargetable = new MessageTargetableSupport((MessageTargetable) messageTargetComboBox.getSelectedItem());
        if (targetMessageTargetable.getTarget() == TargetMessageType.OTHER) {
            targetMessageTargetable.setOtherTargetMessageVariable(targetMessageVariablePanel.getVariable());
            targetMessageTargetable.setSourceUsedByGateway(false);

            // target modified if 1) Get from Queue or 2) Put to Queue and reads a reply queue
            targetMessageTargetable.setTargetModifiedByGateway( !assertion.isPutToQueue() ||
                    (assertion.isPutToQueue() && isConfiguredForReply((SsgActiveConnector) queueComboBox.getSelectedItem())) );
        }
        assertion.setResponseTarget(targetMessageTargetable);
        assertion.getResponseMqNativeMessagePropertyRuleSet().setPassThroughHeaders(targetPassThroughHeadersCheckBox.isSelected());

        final Map<String,String> properties = advancedTableModel.toMap();
        assertion.setRequestMessageAdvancedProperties( properties.isEmpty() || !putToQueueRadioButton.isSelected() ? null : properties );

        String responseTimeoutOverride = mqResponseTimeout.getText();
        if (responseTimeoutOverride != null && ! responseTimeoutOverride.isEmpty()) {
            assertion.setResponseTimeout(responseTimeoutOverride);
        } else {
            assertion.setResponseTimeout(null);
        }

        assertion.setResponseSize(responseByteLimitPanel.getValue());
    }

    private void modelToView( final MqNativeRoutingAssertion assertion ) {
        configSecurityHeaderRadioButtons( assertion, -1, null, secHdrButtons );

        final Long endpointOid = assertion.getSsgActiveConnectorId();
        final SsgActiveConnector foundQueue = endpointOid != null ?
                grepFirst( getQueueItems(), equality( oid(), endpointOid ) ) :
                null ;

        if( foundQueue != null )
            queueComboBox.setSelectedItem(foundQueue);
        else
            queueComboBox.setSelectedIndex(-1);


        applyDynamicAssertionPropertyOverrides();

        // Message properties
        putToQueueRadioButton.setSelected(assertion.isPutToQueue());
        getFromQueueRadioButton.setSelected(!assertion.isPutToQueue());
        messageSourceComboBox.setModel(buildMessageSourceComboBoxModel(assertion));
        final MessageTargetableSupport sourceTargetable = new MessageTargetableSupport(assertion.getRequestTarget());
        messageSourceComboBox.setSelectedItem(sourceTargetable);
        sourcePassThroughHeadersCheckBox.setSelected(assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughHeaders());

        messageTargetComboBox.setModel(buildMessageTargetComboBoxModel(false));
        final MessageTargetableSupport targetTargetable = new MessageTargetableSupport(assertion.getResponseTarget());
        messageTargetComboBox.setSelectedItem(new MessageTargetableSupport( targetTargetable.getTarget()) );
        targetPassThroughHeadersCheckBox.setSelected( assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughHeaders() );
        targetMessageVariablePanel.setVariable(
            targetTargetable.getTarget() == TargetMessageType.OTHER ? targetTargetable.getOtherTargetMessageVariable() : ""
        );
        targetMessageVariablePanel.setAssertion(assertion, getPreviousAssertion());

        advancedTableModel.fromMap( assertion.getRequestMessageAdvancedProperties() );

        mqResponseTimeout.setText(assertion.getResponseTimeout()==null ? "":assertion.getResponseTimeout());

        responseByteLimitPanel.setValue(assertion.getResponseSize(), getMqNativeAdmin().getDefaultMqMessageMaxBytes());

        enableOrDisableComponents();
    }

    private static MqNativeAdmin getMqNativeAdmin() {
        return Registry.getDefault().getExtensionInterface(MqNativeAdmin.class, null);
    }

    private static String info( final SsgActiveConnector connector ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( truncStringMiddleExact( connector.getName(), 48 ) );
        builder.append( " [" );
        if ( connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE ) ) {
            builder.append( "<template>" );
        } else {
            builder.append( truncStringMiddleExact( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, "" ), 32 ) );
        }
        builder.append( " on " );
        builder.append( truncStringMiddleExact( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "" ), 32 ) );
        builder.append( ':' );
        builder.append( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_PORT, "" ) );
        builder.append( "]" );
        return builder.toString();
    }

    private static Unary<String,SsgActiveConnector> info() {
        return new Unary<String,SsgActiveConnector>(){
            @Override
            public String call( final SsgActiveConnector ssgActiveConnector ) {
                return info( ssgActiveConnector );
            }
        };
    }

    private void addAdvProp() {
        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(this, null, advancedTableModel.toMap());
        dialog.setTitle("Advanced Property");
        dialog.pack();
        Utilities.centerOnParentWindow( dialog );
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

        final String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        final String value = (String) advancedTableModel.getValueAt(viewRow, 1);

        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(this, new MutablePair<String, String>(name, value), advancedTableModel.toMap());
        dialog.setTitle( "Advanced Property" );
        dialog.pack();
        Utilities.centerOnParentWindow( dialog );
        DialogDisplayer.display( dialog, new Runnable() {
            @Override
            public void run() {
                if ( !dialog.isCanceled() ) {
                    updatePropertiesList( dialog.getTheProperty(), false );
                    dialog.dispose();
                }
            }
        } );
    }

    private void removeAdvProp() {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        String value = (String) advancedTableModel.getValueAt(viewRow, 1);
        updatePropertiesList(new Pair<String, String>(name, value), true);
    }

    private AdvancedPropertyTableModel getAdvancedPropertyTableModel() {
        if (advancedTableModel == null) {
            advancedTableModel = new AdvancedPropertyTableModel();
        }
        return advancedTableModel;
    }

    private void updatePropertiesList(final Pair<String, String> selectedProperty, boolean deleted) {
        ArrayList<String> keyset = new ArrayList<String>();
        int currentRow;

        if (deleted) {
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
            advancedTableModel.advancedPropertiesMap.remove(selectedProperty.left);
        } else {
            advancedTableModel.advancedPropertiesMap.put(selectedProperty.left, selectedProperty.right);
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
        }

        // Refresh the table
        advancedTableModel.fireTableDataChanged();

        // Refresh the selection highlight
        if (currentRow == advancedTableModel.advancedPropertiesMap.size()) currentRow--; // If the previous deleted row was the last row
        if (currentRow >= 0) advancedPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
    }

    private static class AdvancedPropertyTableModel extends AbstractTableModel {
        private static final int MAX_TABLE_COLUMN_NUM = 2;
        private Map<String, String> advancedPropertiesMap = new TreeMap<String,String>();

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
                    return "Name";
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

        @NotNull
        private Map<String,String> toMap() {
            return new HashMap<String,String>( advancedPropertiesMap );
        }

        private void fromMap( @Nullable final Map<String,String> properties ) {
            advancedPropertiesMap = new TreeMap<String, String>();

            if( properties != null ) {
                advancedPropertiesMap.putAll( properties );
            }
        }
    }
}