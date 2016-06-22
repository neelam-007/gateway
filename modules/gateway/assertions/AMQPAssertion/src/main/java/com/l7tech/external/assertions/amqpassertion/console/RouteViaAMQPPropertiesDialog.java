package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.RouteViaAMQPAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/14/12
 * Time: 1:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class RouteViaAMQPPropertiesDialog extends AssertionPropertiesEditorSupport<RouteViaAMQPAssertion> {
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

    private JPanel mainPanel;
    private JComboBox destinationComboBox;
    private JTextField routingKeyField;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton saveAsContextVariableRadioButton;
    private TargetVariablePanel responseMessageContextVariableField;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox messageSourceComboBox;

    private RouteViaAMQPAssertion assertion;
    private boolean confirmed = false;

    public RouteViaAMQPPropertiesDialog(final Frame owner, final RouteViaAMQPAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
        initComponents();
        enableDisableComponents();
    }

    private void initComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        messageSourceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                messageSourceChanged(e);
            }
        });
        AMQPDestination[] destinations = AMQPDestinationHelper.restoreAmqpDestinations();
        if (destinations == null) {
            destinations = new AMQPDestination[0];
        }
        destinationComboBox.setModel(new DefaultComboBoxModel(destinations));
        destinationComboBox.setRenderer(new TextListCellRenderer<AMQPDestination>(new Functions.Unary<String, AMQPDestination>() {
            @Override
            public String call(AMQPDestination destination) {
                return destination.getName();
            }
        }));

        if (assertion.getSsgActiveConnectorGoid() != null) {
            for (int i = 0; i < destinationComboBox.getItemCount(); i++) {
                AMQPDestination destination = (AMQPDestination) destinationComboBox.getItemAt(i);
                if (destination.getGoid().equals(assertion.getSsgActiveConnectorGoid())) {
                    destinationComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        destinationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        responseMessageContextVariableField.setAcceptEmpty(false);
        responseMessageContextVariableField.setValueWillBeWritten(true);

        defaultResponseRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                responseMessageContextVariableField.setEnabled(false);
            }
        });

        saveAsContextVariableRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                responseMessageContextVariableField.setEnabled(true);
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validProperties()) {
                    if (destinationComboBox.getSelectedItem() == null) {
                        JOptionPane.showMessageDialog(RouteViaAMQPPropertiesDialog.this, "An AMQP destination is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (saveAsContextVariableRadioButton.isSelected() && responseMessageContextVariableField.getVariable().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(RouteViaAMQPPropertiesDialog.this, "The name of the response context variable is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    getData(assertion);
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(okButton, cancelButton);

        setContentPane(mainPanel);
        Utilities.setRequestFocusOnOpen(this);
    }

    private void messageSourceChanged(ActionEvent e) {
        JComboBox jb = (JComboBox) e.getSource();
        Object obj = jb.getSelectedItem();
        if (obj != null) {
            RequestSourceComboBoxItem temp = (RequestSourceComboBoxItem) obj;
            assertion.setRequestTarget(temp.getTarget());
        }
    }

    private void populateReqMsgSrcComboBox() {
        messageSourceComboBox.removeAllItems();
        messageSourceComboBox.setSelectedIndex(-1);

        MessageTargetableSupport currentMessageSource = assertion.getRequestTarget();
        TargetMessageType sourceTarget = currentMessageSource != null ? currentMessageSource.getTarget() : null;
        String contextVariableSourceTarget = sourceTarget == TargetMessageType.OTHER ? currentMessageSource.getOtherTargetMessageVariable() : null;

        messageSourceComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.REQUEST, false)));
        messageSourceComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.RESPONSE, false)));

        if (sourceTarget == TargetMessageType.REQUEST)
            messageSourceComboBox.setSelectedIndex(0);
        else if (sourceTarget == TargetMessageType.RESPONSE)
            messageSourceComboBox.setSelectedIndex(1);

        final Map<String, VariableMetadata> predecessorVariables =
                (assertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion) :
                        (getPreviousAssertion() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(getPreviousAssertion()) :
                                Collections.<String, VariableMetadata>emptyMap();

        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName : predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final RequestSourceComboBoxItem item = new RequestSourceComboBoxItem(new MessageTargetableSupport(variableName));
                messageSourceComboBox.addItem(item);
                if (variableName.equals(contextVariableSourceTarget)) {
                    messageSourceComboBox.setSelectedItem(item);
                }
            }
        }

        if (contextVariableSourceTarget != null && !predecessorVariableNames.contains(contextVariableSourceTarget)) {
            RequestSourceComboBoxItem current = new RequestSourceComboBoxItem(new MessageTargetableSupport(contextVariableSourceTarget));
            current.setUndefined(true);
            messageSourceComboBox.addItem(current);
            messageSourceComboBox.setSelectedItem(current);
        }
    }

    private void enableDisableComponents() {
        okButton.setEnabled(destinationComboBox.getSelectedItem() != null);
    }

    private boolean validProperties() {
        if (destinationComboBox.getSelectedItem() == null) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(final RouteViaAMQPAssertion assertion) {
        this.assertion = assertion;

        if (assertion.getSsgActiveConnectorGoid() != null) {
            for (int i = 0; i < destinationComboBox.getItemCount(); i++) {
                AMQPDestination destination = (AMQPDestination) destinationComboBox.getItemAt(i);
                if (destination.getGoid().equals(assertion.getSsgActiveConnectorGoid())) {
                    destinationComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        routingKeyField.setText(assertion.getRoutingKeyExpression() == null ? "" : assertion.getRoutingKeyExpression());

        populateReqMsgSrcComboBox();

        if (assertion.getResponseTarget().getTarget() == TargetMessageType.RESPONSE) {
            defaultResponseRadioButton.setSelected(true);
            responseMessageContextVariableField.setEnabled(false);
        } else {
            saveAsContextVariableRadioButton.setSelected(true);
            responseMessageContextVariableField.setEnabled(true);
            responseMessageContextVariableField.setVariable(assertion.getResponseTarget().getOtherTargetMessageVariable() == null ? "" : assertion.getResponseTarget().getOtherTargetMessageVariable());
        }

        responseMessageContextVariableField.setAssertion(assertion, this.getPreviousAssertion());

    }

    @Override
    public RouteViaAMQPAssertion getData(final RouteViaAMQPAssertion assertion) {
        assertion.setSsgActiveConnectorGoid(((AMQPDestination) destinationComboBox.getSelectedItem()).getGoid());
        assertion.setSsgActiveConnectorName(((AMQPDestination) destinationComboBox.getSelectedItem()).getExchangeName());
        assertion.setRoutingKeyExpression(routingKeyField.getText().trim());
        assertion.setRequestTarget(((RequestSourceComboBoxItem) messageSourceComboBox.getSelectedItem()).getTarget());
        if (defaultResponseRadioButton.isSelected()) {
            assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));
        } else if (saveAsContextVariableRadioButton.isSelected()) {
            MessageTargetableSupport mts = new MessageTargetableSupport(TargetMessageType.OTHER, true);
            mts.setOtherTargetMessageVariable(responseMessageContextVariableField.getVariable().trim());
            assertion.setResponseTarget(mts);
        }
        return assertion;
    }
}
