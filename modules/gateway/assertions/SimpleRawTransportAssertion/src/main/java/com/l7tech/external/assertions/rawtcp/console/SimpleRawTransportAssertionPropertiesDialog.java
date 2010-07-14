package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion.DEFAULT_READ_TIMEOUT;
import static com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion.DEFAULT_RESPONSE_SIZE_LIMIT;
import static com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion.DEFAULT_WRITE_TIMEOUT;

public class SimpleRawTransportAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<SimpleRawTransportAssertion> {
    private JPanel contentPane;
    private JComboBox requestSourceComboBox;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton saveAsContextVariableRadioButton;
    private JTextField responseContextVariableField;
    private JTextField targetHostField;
    private JTextField targetPortField;
    private JCheckBox limitMaximumResponseSizeCheckBox;
    private JTextField maximumResponseSizeField;
    private JCheckBox customTransmitTimeoutCheckBox;
    private JTextField transmitTimeoutField;
    private JCheckBox customReceiveTimeoutCheckBox;
    private JTextField receiveTimeoutField;

    public SimpleRawTransportAssertionPropertiesDialog(Window owner, SimpleRawTransportAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        maximumResponseSizeField.setDocument(new NumberField(10, Integer.MAX_VALUE));
        transmitTimeoutField.setDocument(new NumberField(9));
        receiveTimeoutField.setDocument(new NumberField(9));

        ActionListener enableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };
        limitMaximumResponseSizeCheckBox.addActionListener(enableListener);
        customTransmitTimeoutCheckBox.addActionListener(enableListener);
        customReceiveTimeoutCheckBox.addActionListener(enableListener);
        saveAsContextVariableRadioButton.addActionListener(enableListener);

        Utilities.enableGrayOnDisabled(maximumResponseSizeField);
        Utilities.enableGrayOnDisabled(transmitTimeoutField);
        Utilities.enableGrayOnDisabled(receiveTimeoutField);
        Utilities.enableGrayOnDisabled(responseContextVariableField);

        enableOrDisableComponents();
        return contentPane;
    }

    private void enableOrDisableComponents() {
        maximumResponseSizeField.setEnabled(limitMaximumResponseSizeCheckBox.isSelected());
        transmitTimeoutField.setEnabled(customTransmitTimeoutCheckBox.isSelected());
        receiveTimeoutField.setEnabled(customReceiveTimeoutCheckBox.isSelected());
        responseContextVariableField.setEnabled(saveAsContextVariableRadioButton.isSelected());
    }

    private static class RequestSourceComboBoxItem {
        final String variableName;
        final String displayName;
        RequestSourceComboBoxItem(String variableName, String displayName) {
            this.variableName = variableName;
            this.displayName = displayName;
        }
        @Override
        public String toString() { return displayName; }
    }

    private void populateAndUpdateRequestSourceComboBox(SimpleRawTransportAssertion assertion) {
        requestSourceComboBox.removeAllItems();
        requestSourceComboBox.addItem(new RequestSourceComboBoxItem(null, "Default Request"));
        final Map<String, VariableMetadata> predecessorVariables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                RequestSourceComboBoxItem item =
                        new RequestSourceComboBoxItem(variableName, String.format("Context Variable: %s%s%s", Syntax.SYNTAX_PREFIX, variableName, Syntax.SYNTAX_SUFFIX));
                requestSourceComboBox.addItem(item);
                MessageTargetableSupport requestTarget = assertion.getRequestTarget();
                if (requestTarget != null && variableName.equals(requestTarget.getOtherTargetMessageVariable())) {
                    requestSourceComboBox.setSelectedItem(item);
                }
            }
        }
    }

    @Override
    public void setData(SimpleRawTransportAssertion assertion) {
        targetHostField.setText(assertion.getTargetHost());
        targetPortField.setText(String.valueOf(assertion.getTargetPort()));

        populateAndUpdateRequestSourceComboBox(assertion);

        int writeTimeout = assertion.getWriteTimeoutMillis();
        customTransmitTimeoutCheckBox.setSelected(writeTimeout != DEFAULT_WRITE_TIMEOUT);
        transmitTimeoutField.setText(String.valueOf(writeTimeout));

        int readTimeout = assertion.getReadTimeoutMillis();
        customReceiveTimeoutCheckBox.setSelected(readTimeout != DEFAULT_READ_TIMEOUT);
        receiveTimeoutField.setText(String.valueOf(readTimeout));

        MessageTargetableSupport responseTarget = assertion.getResponseTarget();
        if (responseTarget != null && responseTarget.getOtherTargetMessageVariable() != null) {
            defaultResponseRadioButton.setSelected(false);
            saveAsContextVariableRadioButton.setSelected(true);
            responseContextVariableField.setText(responseTarget.getOtherTargetMessageVariable());
        } else {
            saveAsContextVariableRadioButton.setSelected(false);
            defaultResponseRadioButton.setSelected(true);
            responseContextVariableField.setText("");
        }

        long responseLimit = assertion.getMaxResponseBytes();
        limitMaximumResponseSizeCheckBox.setSelected(responseLimit != DEFAULT_RESPONSE_SIZE_LIMIT);
        maximumResponseSizeField.setText(String.valueOf(responseLimit));

        enableOrDisableComponents();
    }

    @Override
    public SimpleRawTransportAssertion getData(SimpleRawTransportAssertion assertion) throws ValidationException {

        assertion.setTargetHost(validNonEmptyString("Target host", targetHostField.getText()));
        assertion.setTargetPort(validInt("Port must be a number between 1 and 65535", 65535, targetPortField.getText()));

        RequestSourceComboBoxItem requestSourceItem = (RequestSourceComboBoxItem) requestSourceComboBox.getSelectedItem();
        if (requestSourceItem != null && requestSourceItem.variableName != null) {
            assertion.setRequestTarget(new MessageTargetableSupport(validVariableName("Request context variable name: ", requestSourceItem.variableName), false));
        } else {
            assertion.setRequestTarget(new MessageTargetableSupport(TargetMessageType.REQUEST, false));
        }

        if (saveAsContextVariableRadioButton.isSelected()) {
            assertion.setResponseTarget(new MessageTargetableSupport(validVariableName("Response context variable name: ", responseContextVariableField.getText()), true));
        } else {
            assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));
        }

        assertion.setMaxResponseBytes(limitMaximumResponseSizeCheckBox.isSelected() ? validLong("Maximum response size ", maximumResponseSizeField.getText()) : DEFAULT_RESPONSE_SIZE_LIMIT);
        assertion.setWriteTimeoutMillis(customTransmitTimeoutCheckBox.isSelected() ? validInt("Transmit timeout must be a positive number of milliseconds", Integer.MAX_VALUE, transmitTimeoutField.getText()) : DEFAULT_WRITE_TIMEOUT);
        assertion.setReadTimeoutMillis(customReceiveTimeoutCheckBox.isSelected() ? validInt("Receive timeout must be a positive number of milliseconds", Integer.MAX_VALUE, receiveTimeoutField.getText()) : DEFAULT_READ_TIMEOUT);

        return assertion;
    }

    private String validNonEmptyString(String fieldName, String text) throws ValidationException {
        if (text == null || text.trim().length() < 1)
            throw new ValidationException(fieldName + " is required");
        return text;
    }

    private long validLong(String fieldDesc, String text) throws ValidationException {
        try {
            long val = Long.parseLong(text);
            if (val >= 0)
                return val;
        } catch (NumberFormatException nfe) {
            /* FALLTHROUGH and throw */
        }
        throw new ValidationException(fieldDesc + " must be a positive number of bytes");
    }

    private int validInt(String message, int maximum, String portString) throws ValidationException {
        try {
            int port = Integer.parseInt(portString);
            if (port > 0 && port < maximum)
                return port;
        } catch (NumberFormatException nfe) {
            /* FALLTHROUGH and throw */
        }
        throw new ValidationException(message);
    }

    private String validVariableName(String prefix, String variableName) throws ValidationException {
        String result = VariableMetadata.validateName(variableName);
        if (result != null)
            throw new ValidationException(prefix + result);
        return variableName;
    }
}
