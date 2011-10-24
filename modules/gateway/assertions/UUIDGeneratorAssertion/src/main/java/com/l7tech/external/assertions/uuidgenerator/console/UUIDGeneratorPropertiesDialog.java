package com.l7tech.external.assertions.uuidgenerator.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.external.assertions.uuidgenerator.server.ServerUUIDGeneratorAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Dialog for editing UUIDGeneratorAssertion properties.
 */
public class UUIDGeneratorPropertiesDialog extends AssertionPropertiesOkCancelSupport<UUIDGeneratorAssertion> {
    private static final String AMOUNT = "amount";
    private JPanel contentPane;
    private JPanel variableNamePanel;
    private JTextField amountTextField;
    private TargetVariablePanel targetVariablePanel;
    private InputValidator validators;
    private IntegerOrContextVariableValidationRule integerOrContextVariableRule;

    public UUIDGeneratorPropertiesDialog(Frame parent, UUIDGeneratorAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(targetVariablePanel, BorderLayout.CENTER);

        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty(AMOUNT, amountTextField, null);
        integerOrContextVariableRule = new IntegerOrContextVariableValidationRule(UUIDGeneratorAssertion.MINIMUM_AMOUNT, Integer.MAX_VALUE, AMOUNT);
        validators.addRule(integerOrContextVariableRule);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    public void setData(UUIDGeneratorAssertion assertion) {
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getTargetVariable());
        amountTextField.setText(assertion.getAmount());
    }

    public UUIDGeneratorAssertion getData(UUIDGeneratorAssertion assertion) {
        integerOrContextVariableRule.setTextToValidate(amountTextField.getText());
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }
        assertion.setTargetVariable(targetVariablePanel.getVariable());
        assertion.setAmount(amountTextField.getText().trim());
        return assertion;
    }
}
