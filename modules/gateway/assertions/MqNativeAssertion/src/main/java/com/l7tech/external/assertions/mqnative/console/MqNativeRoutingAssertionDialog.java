package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ByteLimitPanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mqnative.*;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.RoutingDialogUtils.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.MQ_MESSAGE_DESCRIPTORS;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gui.util.Utilities.comboBoxModel;
import static com.l7tech.gui.util.Utilities.enableGrayOnDisabled;
import static com.l7tech.objectmodel.EntityUtil.name;
import static com.l7tech.objectmodel.imp.PersistentEntityUtil.goid;
import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import static com.l7tech.util.TextUtils.truncStringMiddleExact;
import static com.l7tech.util.ValidationUtils.isValidInteger;
import static java.util.Collections.emptyList;
import static com.l7tech.util.Functions.*;

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
    private JTabbedPane tabbedPane;
    private MqNativeAdvancedPropertiesPanel requestMessageDescriptorHeaderPanel;
    private MqNativeAdvancedPropertiesPanel responseMessageDescriptorHeaderPanel;
    private JCheckBox requestPropertiesPassThroughCheckBox;
    private JCheckBox requestHeadersPassThroughCheckBox;
    private JCheckBox requestCopyHeaderToPropertyCheckBox;
    private JCheckBox requestCopyPropertyToHeaderCheckBox;
    private JLabel overrideAdditionalHeaderLabel;
    private JComboBox requestOverrideMqHeaderTypeComboBox;
    private JCheckBox responsePropertiesPassThroughCheckBox;
    private JCheckBox responseHeadersPassThroughCheckBox;
    private JCheckBox responseCopyHeaderToPropertyCheckBox;
    private JCheckBox responseCopyPropertyToHeaderCheckBox;
    private JComboBox responseOverrideMqHeaderTypeComboBox;
    private TargetVariablePanel targetMessageVariablePanel;

    private AbstractButton[] secHdrButtons = {wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };
    private MqNativeRoutingAssertion assertion;
    private List<SsgActiveConnector> queueItems;

    private RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableOrDisableComponents();
        }
    };

    /**
     * Called by IDEA's UI initialization when "Custom Create" is checked for custom palette item.
     */
    private void createUIComponents() {
        requestMessageDescriptorHeaderPanel = new MqNativeAdvancedPropertiesPanel(MqNativeRoutingAssertionDialog.this, MQ_MESSAGE_DESCRIPTORS);
        requestMessageDescriptorHeaderPanel.setTitleAndLabels("MQ Message Descriptors", "Pass through all message descriptors", "Customize message descriptors:");

        responseMessageDescriptorHeaderPanel = new MqNativeAdvancedPropertiesPanel(MqNativeRoutingAssertionDialog.this, MQ_MESSAGE_DESCRIPTORS);
        responseMessageDescriptorHeaderPanel.setTitleAndLabels("MQ Message Descriptors", "Pass through all message descriptors", "Customize message descriptors:");

    }

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
                final MqNativePropertiesDialog mqQueuePropertiesDialog =
                        MqNativePropertiesDialog.createInstance(MqNativeRoutingAssertionDialog.this, null, true, false);
                mqQueuePropertiesDialog.pack();
                Utilities.centerOnParentWindow( mqQueuePropertiesDialog );
                DialogDisplayer.display(mqQueuePropertiesDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (! mqQueuePropertiesDialog.isCanceled()) {
                            final String newQueueName = mqQueuePropertiesDialog.getTheMqResource().getName();
                            List<SsgActiveConnector> newQueues = loadQueueItems();
                            sortQueueList(newQueues);
                            queueComboBox.setModel(comboBoxModel(newQueues));
                            queueComboBox.getModel().setSelectedItem(
                                    grepFirst(newQueues, equality(name(),newQueueName))
                            );
                        }
                    }
                });
            }
        });

        InputValidator inputValidator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        inputValidator.ensureComboBoxSelection( "MQ Native Queues", queueComboBox );
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

        // Message direction
        putToQueueRadioButton.addActionListener(enableDisableListener);
        getFromQueueRadioButton.addActionListener(enableDisableListener);

        // Request tab
        messageSourceComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", null), null, true ) );
        messageSourceComboBox.addActionListener( enableDisableListener );

        // Response tab
        messageTargetComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction("Default", "Message Variable"), null, true ) );
        messageTargetComboBox.addActionListener(enableDisableListener);
        targetMessageVariablePanel = new TargetVariablePanel();
        targetMessageVariableHolderPanel.setLayout(new BorderLayout());
        targetMessageVariableHolderPanel.add(targetMessageVariablePanel, BorderLayout.CENTER);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return targetMessageVariablePanel.getErrorMessage();
            }
        });

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

        inputValidator.attachToButton( getOkButton(), super.createOkAction() );
        getOkButton().setEnabled( !readOnly );
        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                MqNativeRoutingAssertionDialog.this.dispose();
            }
        });

        DefaultComboBoxModel requestMqHeaderTypesComboBoxModel = new DefaultComboBoxModel(MqNativeMessageHeaderType.values());
        requestOverrideMqHeaderTypeComboBox.setModel(requestMqHeaderTypesComboBoxModel);

        DefaultComboBoxModel responseMqHeaderTypesComboBoxModel = new DefaultComboBoxModel(MqNativeMessageHeaderType.values());
        responseOverrideMqHeaderTypeComboBox.setModel(responseMqHeaderTypesComboBoxModel);

        enableOrDisableComponents();
    }


    private void enableOrDisableComponents() {
        final boolean isPutToQueue = putToQueueRadioButton.isSelected();
        tabbedPane.setEnabledAt( 1, isPutToQueue );
        messageSourceComboBox.setEnabled(isPutToQueue);
        targetMessageVariablePanel.setEnabled(
            (messageTargetComboBox.isEnabled() &&  messageTargetComboBox.getSelectedItem() != null && ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget() == TargetMessageType.OTHER)
        );
        final boolean valid = responseByteLimitPanel.validateFields() == null;

        getOkButton().setEnabled(valid);
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
                if ( assertion.getSsgActiveConnectorId()!=null && selected.getGoid().equals(assertion.getSsgActiveConnectorId())) {
                    setIfDynamic( mqNativeDynamicProperties.getQueueName(), "", dynamicDestQueueName );
                    setIfDynamic( mqNativeDynamicProperties.getReplyToQueue(), "", dynamicReplyQueueName );
                }
            }
        }
    }

    private List<SsgActiveConnector> loadQueueItems() {
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
    private List<SsgActiveConnector> getQueueItems() {
        if (queueItems == null)
            queueItems = loadQueueItems();

        sortQueueList(queueItems);

        return queueItems;
    }
    
    private void sortQueueList(final List<SsgActiveConnector> queues) {
        Collections.sort(queues, new Comparator<SsgActiveConnector>() {
            @Override
            public int compare(SsgActiveConnector o1, SsgActiveConnector o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
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
            assertion.setSsgActiveConnectorGoid( null );
            assertion.setSsgActiveConnectorName( null );
        } else {
            assertion.setSsgActiveConnectorGoid( item.getGoid() );
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
        MqNativeMessagePropertyRuleSet requestRuleSet = assertion.getRequestMqNativeMessagePropertyRuleSet();
        requestRuleSet.setPassThroughHeaders(requestMessageDescriptorHeaderPanel.isPassThrough());
        requestRuleSet.setPassThroughMqMessageHeaders(requestHeadersPassThroughCheckBox.isSelected());
        requestRuleSet.setPassThroughMqMessageProperties(requestPropertiesPassThroughCheckBox.isSelected());

        final MessageTargetableSupport targetMessageTargetable = new MessageTargetableSupport((MessageTargetable) messageTargetComboBox.getSelectedItem());
        if (targetMessageTargetable.getTarget() == TargetMessageType.OTHER) {
            targetMessageTargetable.setOtherTargetMessageVariable(targetMessageVariablePanel.getVariable());
            targetMessageTargetable.setSourceUsedByGateway(false);

            // target modified if 1) Get from Queue or 2) Put to Queue and reads a reply queue
            targetMessageTargetable.setTargetModifiedByGateway( true );
        }
        assertion.setResponseTarget(targetMessageTargetable);
        MqNativeMessagePropertyRuleSet responseRuleSet = assertion.getResponseMqNativeMessagePropertyRuleSet();
        responseRuleSet.setPassThroughHeaders(responseMessageDescriptorHeaderPanel.isPassThrough());
        responseRuleSet.setPassThroughMqMessageHeaders(responseHeadersPassThroughCheckBox.isSelected());
        responseRuleSet.setPassThroughMqMessageProperties(responsePropertiesPassThroughCheckBox.isSelected());

        assertion.setRequestMqHeaderType((MqNativeMessageHeaderType) requestOverrideMqHeaderTypeComboBox.getSelectedItem());
        assertion.setResponseMqHeaderType((MqNativeMessageHeaderType) responseOverrideMqHeaderTypeComboBox.getSelectedItem());
        final Map<String,String> requestDescriptors = requestMessageDescriptorHeaderPanel.getAdvancedPropertiesTableModelAsMap();
        assertion.setRequestMessageAdvancedProperties(requestDescriptors.isEmpty() || !putToQueueRadioButton.isSelected() ? null : requestDescriptors);

        final Map<String,String> responseDescriptor = responseMessageDescriptorHeaderPanel.getAdvancedPropertiesTableModelAsMap();
        assertion.setResponseMessageAdvancedProperties(responseDescriptor.isEmpty() ? null : responseDescriptor);

        String responseTimeoutOverride = mqResponseTimeout.getText();
        if (responseTimeoutOverride != null && ! responseTimeoutOverride.isEmpty()) {
            assertion.setResponseTimeout(responseTimeoutOverride);
        } else {
            assertion.setResponseTimeout(null);
        }

        assertion.setResponseSize(responseByteLimitPanel.getValue());

        assertion.setRequestCopyHeaderToProperty(requestCopyHeaderToPropertyCheckBox.isSelected());
        assertion.setRequestCopyPropertyToHeader(requestCopyPropertyToHeaderCheckBox.isSelected());
        assertion.setResponseCopyHeaderToProperty(responseCopyHeaderToPropertyCheckBox.isSelected());
        assertion.setResponseCopyPropertyToHeader(responseCopyPropertyToHeaderCheckBox.isSelected());

    }

    private void modelToView( final MqNativeRoutingAssertion assertion ) {
        configSecurityHeaderRadioButtons( assertion, -1, null, secHdrButtons );

        final Goid endpointOid = assertion.getSsgActiveConnectorId();
        final SsgActiveConnector foundQueue = endpointOid != null ?
                        grepFirst( getQueueItems(), equality( goid(), endpointOid ) ) :
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
        MqNativeMessagePropertyRuleSet requestRuleSet = assertion.getRequestMqNativeMessagePropertyRuleSet();
        requestMessageDescriptorHeaderPanel.setPassThrough(requestRuleSet.isPassThroughHeaders());
        requestHeadersPassThroughCheckBox.setSelected(requestRuleSet.isPassThroughMqMessageHeaders());
        requestPropertiesPassThroughCheckBox.setSelected(requestRuleSet.isPassThroughMqMessageProperties());

        messageTargetComboBox.setModel(buildMessageTargetComboBoxModel(false));
        final MessageTargetableSupport targetTargetable = new MessageTargetableSupport(assertion.getResponseTarget());
        messageTargetComboBox.setSelectedItem(new MessageTargetableSupport( targetTargetable.getTarget()) );
        MqNativeMessagePropertyRuleSet responseRuleSet = assertion.getResponseMqNativeMessagePropertyRuleSet();
        responseMessageDescriptorHeaderPanel.setPassThrough(responseRuleSet.isPassThroughHeaders());
        responseHeadersPassThroughCheckBox.setSelected(responseRuleSet.isPassThroughMqMessageHeaders());
        responsePropertiesPassThroughCheckBox.setSelected(responseRuleSet.isPassThroughMqMessageProperties());
        targetMessageVariablePanel.setVariable(
            targetTargetable.getTarget() == TargetMessageType.OTHER ? targetTargetable.getOtherTargetMessageVariable() : ""
        );
        targetMessageVariablePanel.setAssertion(assertion, getPreviousAssertion());

        requestOverrideMqHeaderTypeComboBox.setSelectedItem(assertion.getRequestMqHeaderType());
        responseOverrideMqHeaderTypeComboBox.setSelectedItem(assertion.getResponseMqHeaderType());

        requestMessageDescriptorHeaderPanel.setAdvancedPropertiesTableModel(assertion.getRequestMessageAdvancedProperties());
        responseMessageDescriptorHeaderPanel.setAdvancedPropertiesTableModel(assertion.getResponseMessageAdvancedProperties());

        requestCopyHeaderToPropertyCheckBox.setSelected(assertion.isRequestCopyHeaderToProperty());
        requestCopyPropertyToHeaderCheckBox.setSelected(assertion.isRequestCopyPropertyToHeader());

        responseCopyHeaderToPropertyCheckBox.setSelected(assertion.isResponseCopyHeaderToProperty());
        responseCopyPropertyToHeaderCheckBox.setSelected(assertion.isResponseCopyPropertyToHeader());

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
}