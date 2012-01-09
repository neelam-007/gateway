package com.l7tech.external.assertions.uuidgenerator.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Dialog for editing UUIDGeneratorAssertion properties.
 */
public class UUIDGeneratorPropertiesDialog extends AssertionPropertiesOkCancelSupport<UUIDGeneratorAssertion> {
    private static final String QUANTITY = "quantity";
    private static final String MAX_QUANTITY = "max quantity";
    private JPanel contentPane;
    private JPanel variableNamePanel;
    private JTextField quantityTextField;
    private JTextField maxQuantityTextField;
    private TargetVariablePanel targetVariablePanel;
    private InputValidator validators;
    private IntegerOrContextVariableValidationRule integerOrContextVariableRule;

    public UUIDGeneratorPropertiesDialog(final Frame parent, final UUIDGeneratorAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        integerOrContextVariableRule = new IntegerOrContextVariableValidationRule(UUIDGeneratorAssertion.MINIMUM_QUANTITY, assertion.getMaximumQuantity(), QUANTITY, quantityTextField);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(targetVariablePanel, BorderLayout.CENTER);

        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty(QUANTITY, quantityTextField, null);
        validators.constrainTextFieldToNumberRange(MAX_QUANTITY, maxQuantityTextField, UUIDGeneratorAssertion.MINIMUM_QUANTITY, Integer.MAX_VALUE);
        validators.addRule(integerOrContextVariableRule);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    public void setData(final UUIDGeneratorAssertion assertion) {
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getTargetVariable());
        quantityTextField.setText(assertion.getQuantity());
        maxQuantityTextField.setText(String.valueOf(assertion.getMaximumQuantity()));
    }

    public UUIDGeneratorAssertion getData(final UUIDGeneratorAssertion assertion) {
        int max = UUIDGeneratorAssertion.MAXIMUM_QUANTITY;
        try {
            max = Integer.valueOf(maxQuantityTextField.getText());
        } catch (final NumberFormatException e) {
            // error will be caught by number range validation
        }
        integerOrContextVariableRule.setMaximum(max);
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setTargetVariable(targetVariablePanel.getVariable());
        assertion.setQuantity(quantityTextField.getText().trim());
        assertion.setMaximumQuantity(Integer.valueOf(maxQuantityTextField.getText()));
        return assertion;
    }
}