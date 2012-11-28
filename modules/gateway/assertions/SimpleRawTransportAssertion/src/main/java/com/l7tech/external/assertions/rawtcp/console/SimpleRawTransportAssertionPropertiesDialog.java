package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ByteLimitPanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAdmin;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
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
    private JCheckBox customTransmitTimeoutCheckBox;
    private JTextField transmitTimeoutField;
    private JCheckBox customReceiveTimeoutCheckBox;
    private JTextField receiveTimeoutField;
    private JTextField responseContentTypeField;
    private JPanel responseLimitHolderPanel;
    private ByteLimitPanel responseLimitPanel;

    private InputValidator validator;

    public SimpleRawTransportAssertionPropertiesDialog(Window owner, SimpleRawTransportAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }
    @Override
    protected void initComponents() {
        super.initComponents();

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

        responseLimitPanel = new ByteLimitPanel();
        responseLimitPanel.setAllowContextVars(true);
        responseLimitHolderPanel.setLayout(new BorderLayout());
        responseLimitHolderPanel.add(responseLimitPanel, BorderLayout.CENTER);

        pack();

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        } );

        customTransmitTimeoutCheckBox.addActionListener(enableDisableListener);
        customReceiveTimeoutCheckBox.addActionListener(enableDisableListener);
        defaultResponseRadioButton.addActionListener(enableDisableListener);
        saveAsContextVariableRadioButton.addActionListener(enableDisableListener);
        responseContextVariableField.addChangeListener(enableDisableListener);

        validator = new InputValidator(this,getTitle());
        validator.constrainTextFieldToBeNonEmpty("Response content type", responseContentTypeField, null);
        validator.constrainTextFieldToBeNonEmpty("Target host", targetHostField, null);
        validator.constrainTextFieldToBeNonEmpty("Port", targetPortField, null);
        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String[] tmp = Syntax.getReferencedNames(targetPortField.getText(), false);
                if (tmp != null && tmp.length > 0) {
                    return null;
                }
                return validator.buildTextFieldNumberRangeValidationRule("Port", targetPortField, 1, 65535, false).getValidationError();
            }
        });
        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return responseLimitPanel.validateFields();
            }
        });
        validator.addRule( new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return saveAsContextVariableRadioButton.isSelected()?
                    VariableMetadata.validateName(responseContextVariableField.getVariable()):
                    null;
            }
        });
        validator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return responseContextVariableField.getErrorMessage();
            }
        });

        String responseLimitError = responseLimitPanel.validateFields();
        if(responseLimitError != null){
            throw new ValidationException(responseLimitError);
        }
        validator.constrainTextFieldToNumberRange("Transmit timeout", transmitTimeoutField,0,Integer.MAX_VALUE);
        validator.constrainTextFieldToNumberRange("Receive timeout",receiveTimeoutField, 0, Integer.MAX_VALUE);


        Utilities.enableGrayOnDisabled(transmitTimeoutField);
        Utilities.enableGrayOnDisabled(receiveTimeoutField);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void enableOrDisableComponents() {
        transmitTimeoutField.setEnabled(customTransmitTimeoutCheckBox.isSelected());
        receiveTimeoutField.setEnabled(customReceiveTimeoutCheckBox.isSelected());
        responseContextVariableField.setEnabled(saveAsContextVariableRadioButton.isSelected());
    }

    private void populateAndUpdateRequestSourceComboBox(SimpleRawTransportAssertion assertion) {
        requestSourceComboBox.setModel(buildMessageSourceComboBoxModel(assertion, true, false));
        requestSourceComboBox.setSelectedItem(new MessageTargetableSupport(assertion.getRequestTarget()));
    }

    @Override
    public void setData(SimpleRawTransportAssertion assertion) {
        targetHostField.setText(assertion.getTargetHost());
        targetPortField.setText(assertion.getTargetPort());
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

        SimpleRawTransportAdmin admin = Registry.getDefault().getExtensionInterface(SimpleRawTransportAdmin.class, null);
        responseLimitPanel.setValue(assertion.getMaxResponseBytesText(),admin.getDefaultResponseSizeLimit());

        enableOrDisableComponents();
    }

    @Override
    public SimpleRawTransportAssertion getData(SimpleRawTransportAssertion assertion) throws ValidationException {
        String validationError = validator.validate();
        if(validationError!=null){
            throw new ValidationException(validationError);
        }

        assertion.setResponseContentType( responseContentTypeField.getText());
        assertion.setTargetHost(targetHostField.getText());
        assertion.setTargetPort(targetPortField.getText());
        assertion.setRequestTarget( (MessageTargetableSupport) requestSourceComboBox.getSelectedItem() );

        if (saveAsContextVariableRadioButton.isSelected()) {
            assertion.setResponseTarget(new MessageTargetableSupport( responseContextVariableField.getVariable()));
        } else {
            assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));
        }

        String responseLimitError = responseLimitPanel.validateFields();
        if(responseLimitError != null){
            throw new ValidationException(responseLimitError);
        }
        assertion.setMaxResponseBytesText( responseLimitPanel.isSelected() ? responseLimitPanel.getValue() : Long.toString( DEFAULT_RESPONSE_SIZE_LIMIT ) );
        assertion.setWriteTimeoutMillis(customTransmitTimeoutCheckBox.isSelected() ?  Integer.parseInt(transmitTimeoutField.getText()) : DEFAULT_WRITE_TIMEOUT);
        assertion.setReadTimeoutMillis(customReceiveTimeoutCheckBox.isSelected() ? Integer.parseInt(receiveTimeoutField.getText()) : DEFAULT_READ_TIMEOUT);

        return assertion;
    }
}
