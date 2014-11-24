package com.l7tech.console.panels;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.naming.Context;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;


/**
 * <code>JmsRoutingAssertionDialog</code> is the protected service
 * policy edit dialog for JMS routing assertions.
 *
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingAssertionDialog extends LegacyAssertionPropertyDialog {
    private static final int NOT_SELECTED_INDEX = -1;

    //- PUBLIC

    /**
     * Creates new form ServicePanel
     */
    public JmsRoutingAssertionDialog(Frame owner, JmsRoutingAssertion a, boolean readOnly) {
        super(owner, a, true);
        assertion = a;
        initComponents(readOnly);
        initFormData();
    }

    /** @return true unless the dialog was exited via the OK button. */
    public boolean isCanceled() {
        return !wasOkButtonPressed;
    }

    /**
     * add the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    @Override
    public void dispose() {
        super.dispose();

        try {
            if (newlyCreatedEndpoint != null) {
                Registry.getDefault().getJmsManager().deleteEndpoint(newlyCreatedEndpoint.getGoid());
                newlyCreatedEndpoint = null;
            }
            if (newlyCreatedConnection != null) {
                Registry.getDefault().getJmsManager().deleteConnection(newlyCreatedConnection.getGoid());
                newlyCreatedConnection = null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to roll back newly-created JMS Queue", e);
        }
    }

    //- PRIVATE

    private static class RequestSourceComboBoxItem {
        private boolean undefined = false;

        private final MessageTargetableSupport target;
        private RequestSourceComboBoxItem(MessageTargetableSupport target) {
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

    private static final Logger logger = Logger.getLogger(JmsRoutingAssertionDialog.class.getName());

    // model, etc
    private JmsRoutingAssertion assertion;
    private boolean wasOkButtonPressed = false;
    private EventListenerList listenerList = new EventListenerList();

    private JmsConnection newlyCreatedConnection = null;
    private JmsEndpoint newlyCreatedEndpoint = null;
    private JmsUtilities.QueueItem[] queueItems;

    // form items
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton newQueueButton;
    private JComboBox queueComboBox;
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JRadioButton authNoneRadio;
    private JRadioButton authSamlRadio;
    private JComboBox samlVersionComboBox;
    private JSpinner samlExpiryInMinutesSpinner;
    private JPanel samlPanel;
    private JmsMessagePropertiesPanel requestMsgPropsPanel;
    private JmsMessagePropertiesPanel responseMsgPropsPanel;
    private JPanel dynamicPropertiesPanel;
    private JTextField dynamicICF;
    private JTextField dynamicJndiUrl;
    private JTextField dynamicJndiUserName;
    private JLabel dynamicJndiPasswordWarning;
    private JPasswordField dynamicJndiPassword;
    private JCheckBox showDynamicJndiPassword;
    private JTextField dynamicQCF;
    private JTextField dynamicDestQueueName;
    private JTextField dynamicDestUserName;
    private JPasswordField dynamicDestPassword;
    private JCheckBox showDynamicDestPassword;
    private JLabel dynamicDestPasswordWarning;
    private JTextField dynamicReplyToName;
    private JTextField jmsResponseTimeout;
    private JComboBox requestTargetComboBox;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton saveAsContextVariableRadioButton;
    private TargetVariablePanel responseTargetVariable;
    private JPanel responseTargetVariablePanel;
    private JCheckBox useRequestSettingsCheckBox;
    private JLabel requestDeliveryModeLabel;
    private JComboBox requestDeliveryModeComboBox;
    private JLabel requestPriorityLabel;
    private JTextField requestPriorityTextField;
    private JLabel requestTimeToLiveLabel;
    private JTextField requestTimeToLiveTextField;
    private JLabel requestTimeToLiveMillisLabel;
    private ByteLimitPanel responseByteLimitPanel;

    private AbstractButton[] secHdrButtons = {wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };

    /**
     * notify the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        final CompositeAssertion parent = a.getParent();
        if (parent == null)
          return;

        SwingUtilities.invokeLater(
          new Runnable() {
              @Override
              public void run() {
                  int[] indices = new int[parent.getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for( EventListener listener : listeners ) {
                      ( (PolicyListener) listener ).assertionsChanged( event );
                  }
              }
          });
    }

    /**
     * Called by IntelliJ IDEA's UI initialization method to initialize
     * custom palette items.
     */
    public void createUIComponents() {
        requestMsgPropsPanel = new JmsMessagePropertiesPanel();
        responseMsgPropsPanel = new JmsMessagePropertiesPanel();
    }

    /**
     * This method is called from within the static factory to
     * initialize the form.
     */
    private void initComponents(boolean readOnly) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                                                                                                       
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);

        dynamicDestPasswordWarning.setVisible(false);
        dynamicJndiPasswordWarning.setVisible(false);
        queueComboBox.setModel(new DefaultComboBoxModel(getQueueItems()));
        queueComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dynamicDestPasswordWarning.setVisible(false);
                dynamicJndiPasswordWarning.setVisible(false);
                populateDynamicPropertyFields();
                applyDynamicAssertionPropertyOverrides();
            }
        });

        ActionListener enableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };

        defaultResponseRadioButton.addActionListener(enableListener);
        saveAsContextVariableRadioButton.addActionListener(enableListener);

        Utilities.enableGrayOnDisabled(dynamicICF);
        Utilities.enableGrayOnDisabled(dynamicJndiUrl);
        Utilities.enableGrayOnDisabled(dynamicJndiUserName);
        Utilities.enableGrayOnDisabled(dynamicJndiPassword);
        Utilities.enableGrayOnDisabled(showDynamicJndiPassword);
        Utilities.enableGrayOnDisabled(dynamicQCF);
        Utilities.enableGrayOnDisabled(dynamicDestQueueName);
        Utilities.enableGrayOnDisabled(dynamicDestUserName);
        Utilities.enableGrayOnDisabled(dynamicDestPassword);
        Utilities.enableGrayOnDisabled(showDynamicDestPassword);
        Utilities.enableGrayOnDisabled(dynamicReplyToName);

        ButtonGroup secButtonGroup = new ButtonGroup();
        secButtonGroup.add(authNoneRadio);
        secButtonGroup.add(authSamlRadio);
        samlVersionComboBox.setModel(new DefaultComboBoxModel(new String[]{"1.1", "2.0"}));
        samlExpiryInMinutesSpinner.setModel(new SpinnerNumberModel(5, 1, 120, 1));
        InputValidator inputValidator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        inputValidator.ensureComboBoxSelection("JMS Destination", queueComboBox);
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(samlExpiryInMinutesSpinner, "Ticket expiry"));
        inputValidator.constrainTextFieldToBeNonEmpty( "Initial Context Factory class name", dynamicICF, null );
        inputValidator.constrainTextFieldToBeNonEmpty( "JNDI URL", dynamicJndiUrl, null );
        inputValidator.constrainTextFieldToBeNonEmpty( "Queue Connection Factory Name", dynamicQCF, null );
        inputValidator.constrainTextFieldToBeNonEmpty( "Destination Queue Name", dynamicDestQueueName, null );
        inputValidator.constrainTextFieldToBeNonEmpty("Wait for Reply on specified queue", dynamicReplyToName, null);
        final InputValidator.ValidationRule priorityRule =
                inputValidator.buildTextFieldNumberRangeValidationRule( "Priority", requestPriorityTextField, 0L, 9L, false );
        inputValidator.constrainTextField( requestPriorityTextField,
                buildTextFieldContextVariableValidationRule( "Priority", requestPriorityTextField, priorityRule ) );
        final InputValidator.ValidationRule ttlRule =
                inputValidator.buildTextFieldNumberRangeValidationRule( "Time To Live", requestTimeToLiveTextField, 0L, Long.MAX_VALUE, false );
        inputValidator.constrainTextField( requestTimeToLiveTextField,
                buildTextFieldContextVariableValidationRule( "Time To Live", requestTimeToLiveTextField, ttlRule ) );
        inputValidator.constrainTextField(jmsResponseTimeout, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String uiResponseTimeout = jmsResponseTimeout.getText().trim();
                String errMsg = "The value for the response timeout must be a valid positive number, contain one context variable, or empty.";
                try {
                    if (! uiResponseTimeout.isEmpty()) {
                        int timeout = Integer.parseInt(uiResponseTimeout);
                        if (timeout <= 0) {
                            return errMsg;
                        }
                    }
                } catch (NumberFormatException e) {
                    //check if context var instead
                    String[] vars = Syntax.getReferencedNames(uiResponseTimeout);
                    if(vars.length == 1 &&  uiResponseTimeout.equals("${"+vars[0]+"}"))
                        return null;
                    else
                    {
                        return errMsg;
                    }
                }
                return null;
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

        requestDeliveryModeComboBox.setModel( new DefaultComboBoxModel( JmsDeliveryMode.values() ) );
        useRequestSettingsCheckBox.addActionListener( enableListener );
        Utilities.enableGrayOnDisabled(requestDeliveryModeLabel);
        Utilities.enableGrayOnDisabled(requestPriorityLabel);
        Utilities.enableGrayOnDisabled(requestTimeToLiveLabel);
        Utilities.enableGrayOnDisabled(requestTimeToLiveMillisLabel);

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!validateResMsgDest()) {
                    return "Invalid context variable for the message destination";
                }
                return null;
            }
        });

        responseByteLimitPanel.setAllowContextVars(true);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return responseByteLimitPanel.validateFields();
            }
        });

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

        authSamlRadio.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                samlPanel.setVisible(authSamlRadio.isSelected());
                samlExpiryInMinutesSpinner.setEnabled(authSamlRadio.isSelected());
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();
        for (AbstractButton button : secHdrButtons)
            buttonGroup.add(button);
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        newQueueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JmsEndpoint ep = newlyCreatedEndpoint;
                JmsConnection conn = newlyCreatedConnection;
                final JmsQueuePropertiesDialog pd = JmsQueuePropertiesDialog.createInstance(getOwner(), conn, ep, true);
                pd.pack();
                Utilities.centerOnScreen(pd);
                DialogDisplayer.display(pd, new Runnable() {
                    @Override
                    public void run() {
                        if (!pd.isCanceled()) {
                            // must validate well before calling pack()
                            // e.g. doesn't work if this line is right before pack()
                            JmsRoutingAssertionDialog.this.validate();

                            newlyCreatedEndpoint = pd.getEndpoint();
                            newlyCreatedConnection = pd.getConnection();
                            getQueueComboBox().setModel(new DefaultComboBoxModel(loadQueueItems()));
                            JmsUtilities.selectEndpoint(getQueueComboBox(), newlyCreatedEndpoint);

                            // resize dialog to fit new content
                            DialogDisplayer.pack(JmsRoutingAssertionDialog.this);
                        }
                    }
                });
            }
        });

        okButton.setEnabled( !readOnly );
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RoutingDialogUtils.configSecurityHeaderHandling(assertion, RoutingAssertion.CLEANUP_CURRENT_SECURITY_HEADER, secHdrButtons);

                JmsUtilities.QueueItem item = (JmsUtilities.QueueItem)getQueueComboBox().getSelectedItem();
                if ( item == null ) {
                    assertion.setEndpointOid((Goid)null);
                    assertion.setEndpointName(null);
                } else {
                    JmsEndpoint endpoint = item.getQueue().getEndpoint();
                    JmsConnection conn = item.getQueue().getConnection();

                    assertion.setEndpointOid(endpoint.getGoid());
                    assertion.setEndpointName(endpoint.getName());
                    JmsDynamicProperties dynProps = null;
                    if ( item.getQueue().isTemplate() ) {
                        dynProps = new JmsDynamicProperties();
                        if (isEmpty(endpoint.getDestinationName()))
                            dynProps.setDestQName(dynamicDestQueueName.getText());

                        if (isEmpty(endpoint.getUsername()))
                            dynProps.setDestUserName(dynamicDestUserName.getText());

                        if (isEmpty(endpoint.getPassword()))
                            dynProps.setDestPassword(new String(dynamicDestPassword.getPassword()));

                        if (isReplyToQueue(endpoint) && isEmpty(endpoint.getReplyToQueueName()))
                            dynProps.setReplytoQName(dynamicReplyToName.getText());

                        if (isEmpty(conn.getJndiUrl()))
                            dynProps.setJndiUrl(dynamicJndiUrl.getText()) ;

                        if (isEmpty((String) conn.properties().get(Context.SECURITY_PRINCIPAL)))
                            dynProps.setJndiUserName(dynamicJndiUserName.getText());

                        if (isEmpty((String) conn.properties().get(Context.SECURITY_CREDENTIALS)))
                            dynProps.setJndiPassword(new String(dynamicJndiPassword.getPassword()));

                        if (isEmpty(conn.getInitialContextFactoryClassname()))
                            dynProps.setIcfName(dynamicICF.getText());

                        if (isEmpty(conn.getQueueFactoryUrl()))
                            dynProps.setQcfName(dynamicQCF.getText());
                    }
                    assertion.setDynamicJmsRoutingProperties(dynProps);
                }

                assertion.setAttachSamlSenderVouches(authSamlRadio.isSelected());
                if (assertion.isAttachSamlSenderVouches()) {
                    assertion.setSamlAssertionVersion("1.1".equals(samlVersionComboBox.getSelectedItem()) ? 1 : 2);
                    assertion.setSamlAssertionExpiry((Integer)samlExpiryInMinutesSpinner.getValue());
                } else {
                    assertion.setSamlAssertionVersion(1);
                    assertion.setSamlAssertionExpiry(5);
                }

                assertion.setRequestJmsMessagePropertyRuleSet(requestMsgPropsPanel.getData());
                assertion.setRequestTarget(((RequestSourceComboBoxItem)requestTargetComboBox.getSelectedItem()).getTarget());

                if ( useRequestSettingsCheckBox.isSelected() ) {
                    assertion.setRequestDeliveryMode( (JmsDeliveryMode)requestDeliveryModeComboBox.getSelectedItem() );
                    assertion.setRequestPriority( requestPriorityTextField.getText() );
                    assertion.setRequestTimeToLive( requestTimeToLiveTextField.getText() );
                } else {
                    assertion.setRequestDeliveryMode( null );
                    assertion.setRequestPriority( null );
                    assertion.setRequestTimeToLive( null );
                }

                assertion.setResponseJmsMessagePropertyRuleSet(responseMsgPropsPanel.getData());
                if (saveAsContextVariableRadioButton.isSelected()) {
                    assertion.setResponseTarget(new MessageTargetableSupport(responseTargetVariable.getVariable(), true));
                } else {
                    assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));
                }
                assertion.setResponseSize(responseByteLimitPanel.getValue() );

                String responseTimeoutOverride = jmsResponseTimeout.getText();
                if (responseTimeoutOverride != null && ! responseTimeoutOverride.isEmpty()) {
                    assertion.setResponseTimeout(responseTimeoutOverride);
                } else {
                    assertion.setResponseTimeout(null);
                }

                fireEventAssertionChanged(assertion);
                wasOkButtonPressed = true;
                newlyCreatedConnection = null; // prevent disposal from deleting our new serviceQueue
                newlyCreatedEndpoint = null;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                JmsRoutingAssertionDialog.this.dispose();
            }
        });

        populateReqMsgSrcComboBox();
        initResponseTarget();
    }

    private void enableOrDisableComponents() {
        responseTargetVariable.setEnabled(saveAsContextVariableRadioButton.isSelected());

        boolean enableQosComponents = useRequestSettingsCheckBox.isSelected();
        requestDeliveryModeLabel.setEnabled( enableQosComponents );
        requestDeliveryModeComboBox.setEnabled( enableQosComponents );
        requestPriorityLabel.setEnabled( enableQosComponents );
        requestPriorityTextField.setEnabled( enableQosComponents );
        requestTimeToLiveLabel.setEnabled( enableQosComponents );
        requestTimeToLiveTextField.setEnabled( enableQosComponents );
        requestTimeToLiveMillisLabel.setEnabled( enableQosComponents );
    }

    private void populateReqMsgSrcComboBox() {
        requestTargetComboBox.removeAllItems();
        requestTargetComboBox.setSelectedIndex(NOT_SELECTED_INDEX);

        MessageTargetableSupport currentMessageSource = assertion.getRequestTarget();
        TargetMessageType sourceTarget = currentMessageSource != null ? currentMessageSource.getTarget() : null;
        String contextVariableSourceTarget = sourceTarget == TargetMessageType.OTHER ? currentMessageSource.getOtherTargetMessageVariable() : null;

        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.REQUEST, false)));
        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.RESPONSE, false)));

        if (sourceTarget == TargetMessageType.REQUEST)
            requestTargetComboBox.setSelectedIndex(0);
        else if (sourceTarget == TargetMessageType.RESPONSE)
            requestTargetComboBox.setSelectedIndex(1);

        final Map<String, VariableMetadata> predecessorVariables =
                (assertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors( assertion ) :
                (getPreviousAssertion() != null)? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( getPreviousAssertion() ) :
                Collections.<String, VariableMetadata>emptyMap();

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
        responseTargetVariable.setAssertion(assertion,getPreviousAssertion());
        if (responseTarget != null && responseTarget.getOtherTargetMessageVariable() != null) {
            defaultResponseRadioButton.setSelected(false);
            saveAsContextVariableRadioButton.setSelected(true);
            responseTargetVariable.setVariable(responseTarget.getOtherTargetMessageVariable());
        } else {
            saveAsContextVariableRadioButton.setSelected(false);
            defaultResponseRadioButton.setSelected(true);
            responseTargetVariable.setVariable("");
        }

        responseByteLimitPanel.setValue(assertion.getResponseSize(), Registry.getDefault().getJmsManager().getDefaultJmsMessageMaxBytes());
    }

    private boolean isReplyToQueue( final JmsEndpoint jmsEndpoint ) {
        return jmsEndpoint.getReplyType()==JmsReplyType.REPLY_TO_OTHER;
    }

    private void populateDynamicPropertyFields() {
        JmsUtilities.QueueItem selected = (JmsUtilities.QueueItem) getQueueComboBox().getSelectedItem();
        if (selected != null) {
            JmsEndpoint ep = selected.getQueue().getEndpoint();
            JmsConnection conn = selected.getQueue().getConnection();

            if ( selected.getQueue().isTemplate() ) {
                Utilities.setEnabled(dynamicPropertiesPanel, true);
                String destinationName = ep.getDestinationName();
                dynamicDestQueueName.setText(destinationName);
                dynamicDestQueueName.setCaretPosition( 0 );
                if (destinationName != null && !"".equals(destinationName.trim())) {
                    dynamicDestQueueName.setEnabled(false);
                }

                String destinationUserName = ep.getUsername();
                dynamicDestUserName.setText(destinationUserName);
                dynamicDestUserName.setCaretPosition( 0 );
                if (destinationUserName == null || !"".equals(destinationUserName)) {
                    dynamicDestUserName.setEnabled(false);
                }

                String destinationPassword = ep.getPassword();
                dynamicDestPassword.setText(destinationPassword);
                dynamicDestPassword.setCaretPosition( 0 );
                if (destinationPassword == null || !"".equals(destinationPassword)) {
                    dynamicDestPassword.setEnabled(false);
                    showDynamicDestPassword.setEnabled(false);
                } else {
                    dynamicDestPasswordWarning.setVisible(true);
                    PasswordGuiUtils.configureOptionalSecurePasswordField(dynamicDestPassword, showDynamicDestPassword, dynamicDestPasswordWarning);
                }

                String replyToQueueName = ep.getReplyToQueueName();
                dynamicReplyToName.setText(replyToQueueName);
                dynamicReplyToName.setCaretPosition( 0 );
                if ( !isReplyToQueue(ep) || replyToQueueName != null && !"".equals(replyToQueueName.trim())) {
                    dynamicReplyToName.setEnabled(false);
                }

                String jndiUrl = conn.getJndiUrl();
                dynamicJndiUrl.setText(jndiUrl);
                dynamicJndiUrl.setCaretPosition( 0 );
                if (jndiUrl != null && !"".equals(jndiUrl.trim())){
                    dynamicJndiUrl.setEnabled(false);
                }

                Properties jmsConnectionProperties = conn.properties();
                String jndiUserName = (String) jmsConnectionProperties.get(Context.SECURITY_PRINCIPAL);
                dynamicJndiUserName.setText(jndiUserName);
                dynamicJndiUserName.setCaretPosition( 0 );
                if (jndiUserName == null || !"".equals(jndiUserName)){
                    dynamicJndiUserName.setEnabled(false);
                }

                String jndiPassword = (String) jmsConnectionProperties.get(Context.SECURITY_CREDENTIALS);
                dynamicJndiPassword.setText(jndiPassword);
                dynamicJndiPassword.setCaretPosition( 0 );
                if (jndiPassword == null || !"".equals(jndiPassword)){
                    dynamicJndiPassword.setEnabled(false);
                    showDynamicJndiPassword.setEnabled(false);
                } else {
                    dynamicJndiPasswordWarning.setVisible(true);
                    PasswordGuiUtils.configureOptionalSecurePasswordField(dynamicJndiPassword, showDynamicJndiPassword, dynamicJndiPasswordWarning);
                }

                String icfClassName = conn.getInitialContextFactoryClassname();
                dynamicICF.setText(icfClassName);
                dynamicICF.setCaretPosition( 0 );
                if (icfClassName != null && !"".equals(icfClassName.trim())) {
                    dynamicICF.setEnabled(false);
                }

                String qcfName = conn.getQueueFactoryUrl();
                dynamicQCF.setText(qcfName);
                dynamicQCF.setCaretPosition( 0 );
                if (qcfName != null && !"".equals(qcfName.trim())) {
                    dynamicQCF.setEnabled(false);
                }
            } else {
                dynamicDestQueueName.setText(null);
                dynamicDestUserName.setText(null);
                dynamicDestPassword.setText(null);
                dynamicReplyToName.setText(null);
                dynamicJndiUrl.setText(null);
                dynamicJndiUserName.setText(null);
                dynamicJndiPassword.setText(null);
                dynamicICF.setText(null);
                dynamicQCF.setText(null);
                Utilities.setEnabled(dynamicPropertiesPanel, false);
            }
        } else {
            dynamicDestQueueName.setText(null);
            dynamicDestUserName.setText(null);
            dynamicDestPassword.setText(null);
            dynamicReplyToName.setText(null);
            dynamicJndiUrl.setText(null);
            dynamicJndiUserName.setText(null);
            dynamicJndiPassword.setText(null);
            dynamicICF.setText(null);
            dynamicQCF.setText(null);
            Utilities.setEnabled(dynamicPropertiesPanel, false);            
        }
    }

    private void applyDynamicAssertionPropertyOverrides() {
        if (assertion != null && assertion.getDynamicJmsRoutingProperties() != null) {
            JmsUtilities.QueueItem selected = (JmsUtilities.QueueItem) getQueueComboBox().getSelectedItem();
            if (selected != null) {
                if (selected.getQueue().getEndpoint().getGoid().equals( assertion.getEndpointOid())) {
                    if (assertion.getDynamicJmsRoutingProperties().getJndiUrl() != null && isDynamic(dynamicJndiUrl) ) {
                        dynamicJndiUrl.setText(assertion.getDynamicJmsRoutingProperties().getJndiUrl());
                        dynamicJndiUrl.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getJndiUserName() != null && isDynamic(dynamicJndiUserName) ) {
                        dynamicJndiUserName.setText(assertion.getDynamicJmsRoutingProperties().getJndiUserName());
                        dynamicJndiUserName.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getJndiPassword() != null && isDynamic(dynamicJndiPassword) ) {
                        dynamicJndiPassword.setText(assertion.getDynamicJmsRoutingProperties().getJndiPassword());
                        dynamicJndiPassword.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getDestQName() != null && isDynamic(dynamicDestQueueName) ) {
                        dynamicDestQueueName.setText(assertion.getDynamicJmsRoutingProperties().getDestQName());
                        dynamicDestQueueName.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getDestUserName() != null && isDynamic(dynamicDestUserName) ) {
                        dynamicDestUserName.setText(assertion.getDynamicJmsRoutingProperties().getDestUserName());
                        dynamicDestUserName.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getDestPassword() != null && isDynamic(dynamicDestPassword) ) {
                        dynamicDestPassword.setText(assertion.getDynamicJmsRoutingProperties().getDestPassword());
                        dynamicDestPassword.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getReplytoQName() != null && isDynamic(dynamicReplyToName) ) {
                        dynamicReplyToName.setText(assertion.getDynamicJmsRoutingProperties().getReplytoQName());
                        dynamicReplyToName.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getIcfName() != null && isDynamic(dynamicICF) ) {
                        dynamicICF.setText(assertion.getDynamicJmsRoutingProperties().getIcfName());
                        dynamicICF.setCaretPosition( 0 );
                    }

                    if (assertion.getDynamicJmsRoutingProperties().getQcfName() != null && isDynamic(dynamicQCF) ) {
                        dynamicQCF.setText(assertion.getDynamicJmsRoutingProperties().getQcfName());
                        dynamicQCF.setCaretPosition( 0 );
                    }
                }
            }
        }
    }

    private boolean isDynamic( final JTextField textField ) {
        return textField.getText()==null || textField.getText().isEmpty();
    }

    private JmsUtilities.QueueItem[] loadQueueItems() {
        return queueItems = JmsUtilities.loadQueueItems();
    }

    private JmsUtilities.QueueItem[] getQueueItems() {
        if (queueItems == null)
            queueItems = loadQueueItems();
        return queueItems;
    }

    private JComboBox getQueueComboBox() {
        return queueComboBox;
    }

    private void initFormData() {
        int expiry = assertion.getSamlAssertionExpiry();
        if (expiry == 0) {
            expiry = 5;
        }
        samlExpiryInMinutesSpinner.setValue(expiry);
        samlVersionComboBox.setSelectedItem(assertion.getSamlAssertionVersion()==1 ? "1.1" : "2.0");
        authNoneRadio.setSelected(!assertion.isAttachSamlSenderVouches());
        authSamlRadio.setSelected(assertion.isAttachSamlSenderVouches());
        samlPanel.setVisible(assertion.isAttachSamlSenderVouches());

        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);


        if(assertion.getEndpointOid()!=null){
            Goid endpointGoid = new Goid(assertion.getEndpointOid());
            try {
                JmsEndpoint serviceEndpoint = null;
                if (endpointGoid != null) {
                    serviceEndpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(endpointGoid);
                }
                JmsUtilities.selectEndpoint(getQueueComboBox(), serviceEndpoint);
                applyDynamicAssertionPropertyOverrides();

            } catch (Exception e) {
                throw new RuntimeException("Unable to look up JMS Queue for this routing assertion", e);
            }
        }
        else {
            queueComboBox.setSelectedIndex(NOT_SELECTED_INDEX);
        }

        requestMsgPropsPanel.setData(assertion.getRequestJmsMessagePropertyRuleSet());
        if ( assertion.getRequestDeliveryMode() != null &&
             assertion.getRequestPriority() != null &&
             assertion.getRequestTimeToLive() != null ) {
            useRequestSettingsCheckBox.setSelected( true );
            requestDeliveryModeComboBox.setSelectedItem( assertion.getRequestDeliveryMode() );
            requestPriorityTextField.setText( assertion.getRequestPriority() );
            requestTimeToLiveTextField.setText( assertion.getRequestTimeToLive() );
        }

        responseMsgPropsPanel.setData(assertion.getResponseJmsMessagePropertyRuleSet());
        jmsResponseTimeout.setText(assertion.getResponseTimeout()==null ? "":assertion.getResponseTimeout());
        jmsResponseTimeout.setCaretPosition( 0 );

        enableOrDisableComponents();
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

    @Override
    public void setPolicyPosition( final PolicyPosition policyPosition ) {
        super.setPolicyPosition( policyPosition );
        populateReqMsgSrcComboBox();
        initResponseTarget();
    }

    public InputValidator.ValidationRule buildTextFieldContextVariableValidationRule( final String fieldName,
                                                                           final JTextComponent comp,
                                                                           final InputValidator.ValidationRule nonVariableAdditionalConstraints ) {
        if (fieldName == null) throw new IllegalArgumentException("fieldName is required");
        if (comp == null) throw new IllegalArgumentException("comp is required");

        final String mess = "The " + fieldName + " field contains an invalid variable reference.";
        return new InputValidator.ComponentValidationRule(comp) {
            @Override
            public String getValidationError() {
                if ( getComponent().isEnabled() ) {
                    final String val = comp.getText();
                    if ( val!=null && !val.isEmpty() ) {
                        try {
                            final String[] names = Syntax.getReferencedNames( val );
                            if ( names.length > 0 ) {
                                return null; // don't run chained validators
                            }
                        } catch ( IllegalArgumentException e ) {
                            return mess;
                        }
                    }
                }
                return nonVariableAdditionalConstraints.getValidationError();
            }
        };
    }

    /**
     * A sub-panel to configure JMS message property propagation in either request
     * or response routing.
     *
     * @since SecureSpan 4.0
     * @author rmak
     */
    public class JmsMessagePropertiesPanel extends JPanel {
        public static final String PASS_THROUGH = "<original value>";

        @SuppressWarnings( { "UnusedDeclaration" } )
        private JPanel mainPanel;       // Not used but required by IntelliJ IDEA.        
        private JRadioButton passThroughAllRadioButton;
        private JRadioButton customizeRadioButton;
        private JPanel customPanel;
        private JTable customTable;
        private JButton addButton;
        private JButton removeButton;
        private JButton editButton;

        private DefaultTableModel customTableModel;

        public JmsMessagePropertiesPanel() {
            passThroughAllRadioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Utilities.setEnabled(customPanel, false);
                    customTable.clearSelection();
                }
            });

            customizeRadioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Utilities.setEnabled(customPanel, true);
                    removeButton.setEnabled(false);
                    editButton.setEnabled(false);
                }
            });

            final String[] columnNames = new String[]{"Name", "Value"};
            customTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            customTable.setModel(customTableModel);
            customTable.getTableHeader().setReorderingAllowed(false);
            customTable.setColumnSelectionAllowed(false);
            customTable.setRowSelectionAllowed(true);
            customTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            customTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    final int numSelected = customTable.getSelectedRows().length;
                    removeButton.setEnabled(numSelected >= 1);
                    editButton.setEnabled(numSelected == 1);
                }
            });

            customTable.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        editSelectedRow();
                    }
                }
                @Override
                public void keyTyped(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {}
            });

            customTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        editSelectedRow();
                }
            });

            // Provides sorting of the custom table by property name.
            final JTableHeader hdr = customTable.getTableHeader();
            hdr.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent event) {
                    final TableColumnModel tcm = customTable.getColumnModel();
                    final int viewColumnIndex = tcm.getColumnIndexAtX(event.getX());
                    final int modelColumnIndex = customTable.convertColumnIndexToModel(viewColumnIndex);
                    if (modelColumnIndex == 0) {
                        sortTable();
                    }
                }
            });

            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JmsMessagePropertyDialog editor = new JmsMessagePropertyDialog(JmsRoutingAssertionDialog.this, getExistingNames(), null);
                    editor.pack();
                    Utilities.centerOnScreen(editor);
                    editor.setVisible(true);
                    if (editor.isOKed()) {
                        JmsMessagePropertyRule newRule = editor.getData();
                        customTableModel.addRow(dataToRow(newRule));
                    }
                }
            });

            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    int[] selectedRows = customTable.getSelectedRows();
                    if (selectedRows != null && selectedRows.length > 0) {
                        for (int i = selectedRows.length - 1; i >= 0; --i) {
                            customTableModel.removeRow(selectedRows[i]);
                        }
                    }
                }
            });

            editButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    editSelectedRow();
                }
            });
        }

        /**
         * Initialize the view with the given data.
         *
         * @param ruleSet   the JMS message property rule set
         */
        public void setData(JmsMessagePropertyRuleSet ruleSet) {
            if (ruleSet == null || ruleSet.isPassThruAll()) {
                passThroughAllRadioButton.doClick();
            } else {
                customizeRadioButton.doClick();
                for (JmsMessagePropertyRule rule : ruleSet.getRules()) {
                    customTableModel.addRow(dataToRow(rule));
                }
                customTable.getSelectionModel().clearSelection();
            }
        }

        /**
         * @return data from the view
         */
        public JmsMessagePropertyRuleSet getData() {
            final int numRows = customTable.getRowCount();
            final JmsMessagePropertyRule[] rules = new JmsMessagePropertyRule[numRows];
            for (int row = 0; row < numRows; ++ row) {
                rules[row] = rowToData(row);
            }
            return new JmsMessagePropertyRuleSet( passThroughAllRadioButton.isSelected(), rules);
        }

        private void editSelectedRow() {
            final int row = customTable.getSelectedRow();
            if (row != -1) {
                final JmsMessagePropertyRule rule = rowToData(row);
                final JmsMessagePropertyDialog editor = new JmsMessagePropertyDialog(JmsRoutingAssertionDialog.this, getExistingNames(), rule);
                Utilities.centerOnScreen(editor);
                editor.pack();
                editor.setVisible(true);
                if (editor.isOKed()) {
                    final Object[] cells = dataToRow(rule);
                    customTable.setValueAt(cells[0], row, 0);
                    customTable.setValueAt(cells[1], row, 1);
                }
            }
        }

        private Object[] dataToRow(JmsMessagePropertyRule rule) {
            final String name = rule.getName();
            String value;
            if (rule.isPassThru()) {
                value = PASS_THROUGH;
            } else {
                value = rule.getCustomPattern();
            }
            return new Object[]{ name, value };
        }

        private JmsMessagePropertyRule rowToData(int row) {
            final TableModel model = customTable.getModel();
            final String name = (String)model.getValueAt(row, 0);
            final String value = (String)model.getValueAt(row, 1);
            boolean passThrough;
            String pattern;
            if ( PASS_THROUGH.equals(value)) {
                passThrough = true;
                pattern = null;
            } else {
                passThrough = false;
                pattern = value;
            }
            return new JmsMessagePropertyRule(name, passThrough, pattern);
        }

        private Set<String> getExistingNames() {
            final Set<String> existingNames = new HashSet<String>(customTable.getRowCount());
            for (int i = 0; i < customTable.getRowCount(); ++ i) {
                existingNames.add((String)customTable.getValueAt(i, 0));
            }
            return existingNames;
        }

        private boolean tableAscending = false;

        /**
         * Sort the rows of the custom table by property name in toggled order. 
         */
        private void sortTable() {
            final int rowCount = customTableModel.getRowCount();
            for (int i = 0; i < rowCount; ++ i) {
                for (int j = i + 1; j < rowCount; ++ j) {
                    final String name_i = customTable.getValueAt(i, 0).toString();
                    final String name_j = customTable.getValueAt(j, 0).toString();
                    if (tableAscending) {
                        if (name_i.compareTo(name_j) < 0) {
                            swapRows(i, j);
                        }
                    } else {
                        if (name_i.compareTo(name_j) > 0) {
                            swapRows(i, j);
                        }
                    }
                }
            }
            tableAscending = !tableAscending;
        }

        /**
         * Swaps the cell contents of two rows in the custom table.
         * @param row1  index of row 1
         * @param row2  index of row 2
         */
        private void swapRows(final int row1, final int row2) {
            for (int column = 0; column < customTable.getColumnCount(); ++ column) {
                final Object value_1 = customTable.getValueAt(row1, column);
                final Object value_2 = customTable.getValueAt(row2, column);
                customTable.setValueAt(value_2, row1, column);
                customTable.setValueAt(value_1, row2, column);
            }
        }
    }
}