package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

public class AddHeaderAssertionDialog extends AssertionPropertiesOkCancelSupport<AddHeaderAssertion> {
    private JPanel contentPane;
    private JCheckBox replaceExistingValuesCheckBox;
    private JTextField headerNameTextField;
    private JTextField headerValueTextField;

    public AddHeaderAssertionDialog(Window owner, AddHeaderAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(AddHeaderAssertion assertion) {
        headerNameTextField.setText(assertion.getHeaderName() == null ? "" : assertion.getHeaderName());
        headerValueTextField.setText(assertion.getHeaderValue() == null ? "" : assertion.getHeaderValue());
        replaceExistingValuesCheckBox.setSelected(assertion.isRemoveExisting());
    }

    @Override
    public AddHeaderAssertion getData(AddHeaderAssertion assertion) throws ValidationException {
        final String name = headerNameTextField.getText();

        if (null == name || name.trim().length() < 1)
            throw new ValidationException("Header name may not be empty");

        try {
            Syntax.getReferencedNames(headerNameTextField.getText());
        } catch (IllegalArgumentException iae) {
            throw new ValidationException("Error with header name variable, '"+ iae.getMessage() +"'.");
        }

        assertion.setHeaderName(name);

        final String value = headerValueTextField.getText();

        if(null != value) {
            try {
                Syntax.getReferencedNames(headerValueTextField.getText());
            } catch (IllegalArgumentException iae) {
                throw new ValidationException("Error with header value variable, '"+ iae.getMessage() +"'.");
            }
        }

        assertion.setHeaderValue(value);

        assertion.setRemoveExisting(replaceExistingValuesCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
