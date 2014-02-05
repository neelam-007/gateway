package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

public class AddHeaderAssertionDialog extends AssertionPropertiesOkCancelSupport<AddHeaderAssertion> {
    private static final String ADD = "Add";
    private static final String ADD_OR_REPLACE = "Add or Replace";
    private static final String REMOVE = "Remove";
    private JPanel contentPane;
    private JTextField headerNameTextField;
    private JTextField headerValueTextField;
    private JCheckBox nameExpressionCheckBox;
    private JCheckBox valueExpressionCheckBox;
    private JComboBox operationComboBox;

    public AddHeaderAssertionDialog(final Window owner, final AddHeaderAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
        enableDisable();
    }

    @Override
    public void setData(final AddHeaderAssertion assertion) {
        if (assertion.getOperation() == AddHeaderAssertion.Operation.REMOVE) {
            operationComboBox.setSelectedItem(REMOVE);
        } else if (assertion.getOperation() == AddHeaderAssertion.Operation.ADD) {
            if (assertion.isRemoveExisting()) {
                operationComboBox.setSelectedItem(ADD_OR_REPLACE);
            } else {
                operationComboBox.setSelectedItem(ADD);
            }
        }
        headerNameTextField.setText(assertion.getHeaderName() == null ? "" : assertion.getHeaderName());
        headerValueTextField.setText(assertion.getHeaderValue() == null ? "" : assertion.getHeaderValue());
        nameExpressionCheckBox.setSelected(assertion.isEvaluateNameAsExpression());
    }

    @Override
    public AddHeaderAssertion getData(final AddHeaderAssertion assertion) throws ValidationException {
        if (REMOVE.equals(operationComboBox.getSelectedItem())) {
            assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        } else if (ADD.equals(operationComboBox.getSelectedItem()) || ADD_OR_REPLACE.equals(operationComboBox.getSelectedItem())) {
            assertion.setOperation(AddHeaderAssertion.Operation.ADD);
            assertion.setRemoveExisting(ADD_OR_REPLACE.equals(operationComboBox.getSelectedItem()));
        }
        final String name = headerNameTextField.getText();

        if (null == name || name.trim().length() < 1)
            throw new ValidationException("Header name may not be empty");

        try {
            Syntax.getReferencedNames(headerNameTextField.getText());
        } catch (IllegalArgumentException iae) {
            throw new ValidationException("Error with header name variable, '" + iae.getMessage() + "'.");
        }

        assertion.setHeaderName(name);

        final String value = headerValueTextField.getText();

        if (null != value) {
            try {
                Syntax.getReferencedNames(headerValueTextField.getText());
            } catch (IllegalArgumentException iae) {
                throw new ValidationException("Error with header value variable, '" + iae.getMessage() + "'.");
            }
        }

        assertion.setHeaderValue(value);

        assertion.setEvaluateNameAsExpression(nameExpressionCheckBox.isSelected());
        assertion.setEvaluateValueExpression(valueExpressionCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        operationComboBox.setModel(new DefaultComboBoxModel(new String[]{ADD, ADD_OR_REPLACE, REMOVE}));
        operationComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
            }
        }));
    }

    private void enableDisable() {
        nameExpressionCheckBox.setEnabled(REMOVE.equals(operationComboBox.getSelectedItem()));
        valueExpressionCheckBox.setEnabled(REMOVE.equals(operationComboBox.getSelectedItem()));
    }
}
