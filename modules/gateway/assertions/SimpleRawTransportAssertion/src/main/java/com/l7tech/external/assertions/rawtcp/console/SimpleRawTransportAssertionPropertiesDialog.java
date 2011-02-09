package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;

import static com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion.*;

public class SimpleRawTransportAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<SimpleRawTransportAssertion> {
    private JPanel contentPane;
    private JComboBox requestSourceComboBox;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton saveAsContextVariableRadioButton; 
    private JPanel responseContextVariablePanel;
    private TargetVariablePanel responseContextVariableField;
    private JTextField targetHostField;
    private JTextField targetPortField;
    private JCheckBox limitMaximumResponseSizeCheckBox;
    private JTextField maximumResponseSizeField;
    private JCheckBox customTransmitTimeoutCheckBox;
    private JTextField transmitTimeoutField;
    private JCheckBox customReceiveTimeoutCheckBox;
    private JTextField receiveTimeoutField;
    private JTextField responseContentTypeField;

    public SimpleRawTransportAssertionPropertiesDialog(Window owner, SimpleRawTransportAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        maximumResponseSizeField.setDocument(new NumberField(10, Integer.MAX_VALUE));
        transmitTimeoutField.setDocument(new NumberField(9));
        receiveTimeoutField.setDocument(new NumberField(9));

        final Functions.Unary<String, MessageTargetable> nameAccessor = getMessageNameFunction("Default", null, "Context Variable: ");
        final TextListCellRenderer<MessageTargetable> renderer = new TextListCellRenderer<MessageTargetable>( nameAccessor, nameAccessor, false );
        renderer.setRenderClipped( true );
        renderer.setSmartTooltips( true );
        requestSourceComboBox.setRenderer( renderer );

        responseContextVariableField = new TargetVariablePanel();
        responseContextVariablePanel.setLayout(new BorderLayout());
        responseContextVariablePanel.add(responseContextVariableField, BorderLayout.CENTER);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        } );
        
        limitMaximumResponseSizeCheckBox.addActionListener(enableDisableListener);
        customTransmitTimeoutCheckBox.addActionListener(enableDisableListener);
        customReceiveTimeoutCheckBox.addActionListener(enableDisableListener);
        defaultResponseRadioButton.addActionListener(enableDisableListener);
        saveAsContextVariableRadioButton.addActionListener(enableDisableListener);
        responseContextVariableField.addChangeListener(enableDisableListener);

        Utilities.enableGrayOnDisabled(maximumResponseSizeField);
        Utilities.enableGrayOnDisabled(transmitTimeoutField);
        Utilities.enableGrayOnDisabled(receiveTimeoutField);

        enableOrDisableComponents();
        return contentPane;
    }

    private void enableOrDisableComponents() {
        maximumResponseSizeField.setEnabled(limitMaximumResponseSizeCheckBox.isSelected());
        transmitTimeoutField.setEnabled(customTransmitTimeoutCheckBox.isSelected());
        receiveTimeoutField.setEnabled(customReceiveTimeoutCheckBox.isSelected());
        responseContextVariableField.setEnabled(saveAsContextVariableRadioButton.isSelected());

        getOkButton().setEnabled(defaultResponseRadioButton.isSelected() || responseContextVariableField.isEntryValid());
    }

    private void populateAndUpdateRequestSourceComboBox(SimpleRawTransportAssertion assertion) {
        requestSourceComboBox.setModel( buildMessageSourceComboBoxModel( assertion, true, false ) );
        requestSourceComboBox.setSelectedItem( new MessageTargetableSupport(assertion.getRequestTarget()) );
    }

    @Override
    public void setData(SimpleRawTransportAssertion assertion) {
        targetHostField.setText(assertion.getTargetHost());
        targetPortField.setText(String.valueOf(assertion.getTargetPort()));
        responseContentTypeField.setText(assertion.getResponseContentType());

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
            responseContextVariableField.setVariable(responseTarget.getOtherTargetMessageVariable());
        } else {
            saveAsContextVariableRadioButton.setSelected(false);
            defaultResponseRadioButton.setSelected(true);
            responseContextVariableField.setVariable("");
        }
        responseContextVariableField.setAssertion(assertion,getPreviousAssertion());

        long responseLimit = assertion.getMaxResponseBytes();
        limitMaximumResponseSizeCheckBox.setSelected(responseLimit != DEFAULT_RESPONSE_SIZE_LIMIT);
        maximumResponseSizeField.setText(String.valueOf(responseLimit));

        enableOrDisableComponents();
    }

    @Override
    public SimpleRawTransportAssertion getData(SimpleRawTransportAssertion assertion) throws ValidationException {

        assertion.setResponseContentType(validNonEmptyString("Response content type", responseContentTypeField.getText()));
        assertion.setTargetHost(validNonEmptyString("Target host", targetHostField.getText()));
        assertion.setTargetPort(validInt("Port must be a number between 1 and 65535", 65535, targetPortField.getText()));
        assertion.setRequestTarget( (MessageTargetableSupport) requestSourceComboBox.getSelectedItem() );

        if (saveAsContextVariableRadioButton.isSelected()) {
            assertion.setResponseTarget(new MessageTargetableSupport(validVariableName("Response context variable name: ", responseContextVariableField.getVariable()), true));
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
