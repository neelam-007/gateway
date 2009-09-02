package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmlsec.VariableCredentialSourceAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class VariableCredentialSourceAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<VariableCredentialSourceAssertion> {
    private JPanel contentPane;
    private JTextField variableNameField;

    public VariableCredentialSourceAssertionPropertiesDialog(Frame parent, VariableCredentialSourceAssertion assertion) {
        super(assertion.getClass(), parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(VariableCredentialSourceAssertion assertion) {
        variableNameField.setText(assertion.getVariableName());
    }

    @Override
    public VariableCredentialSourceAssertion getData(VariableCredentialSourceAssertion assertion) throws ValidationException {
        assertion.setVariableName(variableNameField.getText());
        return assertion;
    }
}
