package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.sla.ThroughputQuotaQueryAssertion;

import javax.swing.*;
import java.awt.*;

public class ThroughputQuotaQueryPropertiesDialog extends AssertionPropertiesOkCancelSupport<ThroughputQuotaQueryAssertion> {
    private JPanel contentPane;
    private JTextField counterNameField;
    private TargetVariablePanel variablePrefixPanel;
    private InputValidator validators;

    public ThroughputQuotaQueryPropertiesDialog(Window owner, ThroughputQuotaQueryAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        validators= new InputValidator(this,getTitle());
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return variablePrefixPanel.getErrorMessage();
            }
        });
        validators.constrainTextFieldToBeNonEmpty("Counter Name", counterNameField,null);
        variablePrefixPanel.setAcceptEmpty(true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(ThroughputQuotaQueryAssertion assertion) {
        String name = assertion.getCounterName();
        counterNameField.setText(name == null ? "" : name);

        variablePrefixPanel.setAssertion(assertion, getPreviousAssertion());
        variablePrefixPanel.setSuffixes(assertion.getVariableSuffixes());
        String prefix = assertion.getVariablePrefix();
        variablePrefixPanel.setVariable(prefix == null ? "" : prefix);
    }

    @Override
    public ThroughputQuotaQueryAssertion getData(ThroughputQuotaQueryAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }

        String name = counterNameField.getText();
        assertion.setCounterName(name);

        String prefix = variablePrefixPanel.getVariable();
        assertion.setVariablePrefix(prefix == null || prefix.trim().length() < 1 ? null : prefix);

        return assertion;
    }
}

