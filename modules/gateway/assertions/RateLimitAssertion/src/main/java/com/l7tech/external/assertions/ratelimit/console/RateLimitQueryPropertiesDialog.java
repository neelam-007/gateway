package com.l7tech.external.assertions.ratelimit.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.ratelimit.RateLimitQueryAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetVariableAssertion;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.wsdl.Input;
import java.awt.*;

public class RateLimitQueryPropertiesDialog extends AssertionPropertiesOkCancelSupport<RateLimitQueryAssertion> {
    private JPanel contentPane;
    private JTextField counterNameField;
    private TargetVariablePanel variablePrefixField;
    private InputValidator validators;


    public RateLimitQueryPropertiesDialog(Window owner, RateLimitQueryAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        validators= new InputValidator(this,getTitle());
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return variablePrefixField.getErrorMessage();
            }
        });
        validators.constrainTextFieldToBeNonEmpty("Counter Name", counterNameField,null);
        variablePrefixField.setAcceptEmpty(true);
        initComponents();
        setData(assertion);
    }


    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(RateLimitQueryAssertion assertion) {
        String name = assertion.getCounterName();
        counterNameField.setText(name == null ? "" : name);

        variablePrefixField.setAssertion(assertion, getPreviousAssertion());
        variablePrefixField.setSuffixes(assertion.getVariableSuffixes());
        String prefix = assertion.getVariablePrefix();
        variablePrefixField.setVariable(prefix == null ? "" : prefix);
    }

    @Override
    public RateLimitQueryAssertion getData(RateLimitQueryAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }

        String name = counterNameField.getText();
        assertion.setCounterName(name);

        String prefix = variablePrefixField.getVariable();
        assertion.setVariablePrefix(prefix == null || prefix.trim().length() < 1 ? null : prefix);

        return assertion;
    }

}
