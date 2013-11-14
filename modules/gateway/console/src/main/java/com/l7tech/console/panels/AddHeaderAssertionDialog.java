package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

public class AddHeaderAssertionDialog extends AssertionPropertiesOkCancelSupport<AddHeaderAssertion> {
    private JPanel contentPane;
    private JCheckBox replaceExistingValuesCheckBox;
    private JTextField headerNameTextField;
    private JTextField headerValueTextField;
    private JRadioButton addRadioButton;
    private JRadioButton removeRadioButton;
    private JCheckBox matchValueCheckBox;

    public AddHeaderAssertionDialog(Window owner, AddHeaderAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
        enableDisable();
    }

    @Override
    public void setData(AddHeaderAssertion assertion) {
        addRadioButton.setSelected(assertion.getOperation() == AddHeaderAssertion.Operation.ADD);
        removeRadioButton.setSelected(assertion.getOperation() == AddHeaderAssertion.Operation.REMOVE);
        matchValueCheckBox.setSelected(assertion.isMatchValueForRemoval());
        headerNameTextField.setText(assertion.getHeaderName() == null ? "" : assertion.getHeaderName());
        headerValueTextField.setText(assertion.getHeaderValue() == null ? "" : assertion.getHeaderValue());
        replaceExistingValuesCheckBox.setSelected(assertion.isRemoveExisting());
    }

    @Override
    public AddHeaderAssertion getData(AddHeaderAssertion assertion) throws ValidationException {
        assertion.setOperation(addRadioButton.isSelected() ? AddHeaderAssertion.Operation.ADD : AddHeaderAssertion.Operation.REMOVE);
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
        assertion.setMatchValueForRemoval(matchValueCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        final RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
            }
        });
        addRadioButton.addActionListener(listener);
        removeRadioButton.addActionListener(listener);
    }

    private void enableDisable() {
        replaceExistingValuesCheckBox.setEnabled(addRadioButton.isSelected());
        matchValueCheckBox.setEnabled(removeRadioButton.isSelected());
    }
}
