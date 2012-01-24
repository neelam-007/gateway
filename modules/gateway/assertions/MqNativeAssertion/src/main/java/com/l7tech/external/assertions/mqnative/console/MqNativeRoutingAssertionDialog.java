package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import static com.l7tech.console.panels.RoutingDialogUtils.configSecurityHeaderHandling;
import static com.l7tech.console.panels.RoutingDialogUtils.configSecurityHeaderRadioButtons;
import static com.l7tech.console.panels.RoutingDialogUtils.tagSecurityHeaderHandlingButtons;
import com.l7tech.console.panels.ByteLimitPanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.*;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.booleanProperty;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import static com.l7tech.gui.util.Utilities.comboBoxModel;
import static com.l7tech.gui.util.Utilities.enableGrayOnDisabled;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import static com.l7tech.objectmodel.imp.PersistentEntityUtil.oid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.equality;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.grepFirst;
import static com.l7tech.util.Functions.negate;
import static com.l7tech.util.TextUtils.truncStringMiddleExact;
import static com.l7tech.util.ValidationUtils.isValidInteger;
import static java.util.Collections.emptyList;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // form items
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
    private JComboBox requestTargetComboBox;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton saveAsContextVariableRadioButton;
    private TargetVariablePanel responseTargetVariable;
    private JLabel messageDestinationStatusLabel;
    private JPanel responseTargetVariablePanel;
    private JCheckBox responsePassThroughHeadersCheckBox;
    private JCheckBox requestPassThroughHeadersCheckBox;
    private JPanel responseByteLimitHolderPanel;
    private ByteLimitPanel responseByteLimitPanel;

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
                Utilities.centerOnScreen(mqQueuePropertiesDialog);
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
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (((RequestSourceComboBoxItem) requestTargetComboBox.getSelectedItem()).isUndefined()) {
                    return "Undefined context variable for message source: " + ((RequestSourceComboBoxItem) requestTargetComboBox.getSelectedItem()).getTarget().getTargetName();
                }
                return null;
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!validateResMsgDest()) {
                    return "Message destination error: " + messageDestinationStatusLabel.getText();
                }
                return null;
            }
        });
        inputValidator.attachToButton( getOkButton(), super.createOkAction() );

        saveAsContextVariableRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                validateResMsgDest();
            }
        });
        responseTargetVariable = new TargetVariablePanel();
        responseTargetVariablePanel.setLayout(new BorderLayout());
        responseTargetVariablePanel.add(responseTargetVariable, BorderLayout.CENTER);
        responseTargetVariable.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                validateResMsgDest();
            }
        });
        validateResMsgDest();

        tagSecurityHeaderHandlingButtons( secHdrButtons );

        // response properties
        defaultResponseRadioButton.addActionListener(enableDisableListener);
        saveAsContextVariableRadioButton.addActionListener(enableDisableListener);

        // Override message size
        responseByteLimitPanel = new ByteLimitPanel();
        responseByteLimitPanel.setAllowContextVars(false);
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

        populateReqMsgSrcComboBox();
        initResponseTarget();

        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {
        responseTargetVariable.setEnabled( saveAsContextVariableRadioButton.isSelected() );

        final boolean valid = responseByteLimitPanel.validateFields() == null;
        getOkButton().setEnabled(valid);
    }

    private void populateReqMsgSrcComboBox() {
        requestTargetComboBox.removeAllItems();
        requestTargetComboBox.setSelectedIndex(-1);

        MessageTargetableSupport currentMessageSource = assertion.getRequestTarget();
        TargetMessageType sourceTarget = currentMessageSource != null ? currentMessageSource.getTarget() : null;
        String contextVariableSourceTarget = sourceTarget == TargetMessageType.OTHER ? currentMessageSource.getOtherTargetMessageVariable() : null;

        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.REQUEST, false)));
        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.RESPONSE, false)));

        if (sourceTarget == TargetMessageType.REQUEST)
            requestTargetComboBox.setSelectedIndex(0);
        else if (sourceTarget == TargetMessageType.RESPONSE)
            requestTargetComboBox.setSelectedIndex(1);

        final Map<String, VariableMetadata> predecessorVariables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName : predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final RequestSourceComboBoxItem item = new RequestSourceComboBoxItem(new MessageTargetableSupport(variableName));
                requestTargetComboBox.addItem(item);
                if ( variableName.equals(contextVariableSourceTarget)) {
                    requestTargetComboBox.setSelectedItem(item);
                }
            }
        }

        if (contextVariableSourceTarget != null && ! predecessorVariableNames.contains(contextVariableSourceTarget)) {
            RequestSourceComboBoxItem current = new RequestSourceComboBoxItem(new MessageTargetableSupport(contextVariableSourceTarget));
            current.setUndefined(true);
            requestTargetComboBox.addItem(current);
            requestTargetComboBox.setSelectedItem(current);
        }
    }

    private void initResponseTarget() {
        MessageTargetableSupport responseTarget = assertion.getResponseTarget();
        if (responseTarget.getOtherTargetMessageVariable() != null) {
            defaultResponseRadioButton.setSelected(false);
            saveAsContextVariableRadioButton.setSelected( true );
            responseTargetVariable.setVariable( responseTarget.getOtherTargetMessageVariable() );
            responseTargetVariable.setAssertion( assertion, getPreviousAssertion() );
        } else {
            saveAsContextVariableRadioButton.setSelected(false);
            defaultResponseRadioButton.setSelected(true);
            responseTargetVariable.setVariable( "" );
        }
    }


    private void populateDynamicPropertyFields() {
        Utilities.setEnabled( dynamicPropertiesPanel, false );

        final SsgActiveConnector selected = (SsgActiveConnector) queueComboBox.getSelectedItem();
        if ( selected != null ) {
            final String selectedChannelName = selected.getProperty( PROPERTIES_KEY_MQ_NATIVE_CHANNEL, "" );
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
                    negate( booleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_INBOUND ) ) );
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

        assertion.getRequestMqNativeMessagePropertyRuleSet().setPassThroughHeaders(requestPassThroughHeadersCheckBox.isSelected());
        assertion.setRequestTarget( ((RequestSourceComboBoxItem) requestTargetComboBox.getSelectedItem()).getTarget() );

        assertion.getResponseMqNativeMessagePropertyRuleSet().setPassThroughHeaders(responsePassThroughHeadersCheckBox.isSelected());
        if (saveAsContextVariableRadioButton.isSelected()) {
            assertion.setResponseTarget(new MessageTargetableSupport(responseTargetVariable.getVariable(), true));
        } else {
            assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));
        }

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

        requestPassThroughHeadersCheckBox.setSelected(assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughHeaders());
        responsePassThroughHeadersCheckBox.setSelected( assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughHeaders() );
        mqResponseTimeout.setText(assertion.getResponseTimeout()==null ? "":assertion.getResponseTimeout());

        responseByteLimitPanel.setValue(assertion.getResponseSize(), getMqNativeAdmin().getDefaultMqMessageMaxBytes());
    }

    private static MqNativeAdmin getMqNativeAdmin() {
        return Registry.getDefault().getExtensionInterface(MqNativeAdmin.class, null);
    }

    /**
     * Validates the response message destination; with the side effect of setting the status icon and text.
     *
     * @return <code>true</code> if response messge destination is valid, <code>false</code> if invalid
     */
    private boolean validateResMsgDest() {
        boolean ok = (defaultResponseRadioButton.isSelected() || responseTargetVariable.isEntryValid());
        refreshDialog();
        return ok;
    }

    /**
     * Resize the dialog due to some components getting extended.
     */
    private void refreshDialog() {
        if (getSize().width < mainPanel.getMinimumSize().width) {
            setSize(mainPanel.getMinimumSize().width, getSize().height);
        }
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

    private static class RequestSourceComboBoxItem {
        private boolean undefined = false;

        private final MessageTargetableSupport target;
        public RequestSourceComboBoxItem(MessageTargetableSupport target) {
            this.target = target;
        }

        public MessageTargetableSupport getTarget() {
            return target;
        }

        public void setUndefined(boolean undefined) {
            this.undefined = undefined;
        }

        public boolean isUndefined() {
            return undefined;
        }

        @Override
        public String toString() {
            return (undefined ? "Undefined: " : "") + target.getTargetName();
        }
    }
}