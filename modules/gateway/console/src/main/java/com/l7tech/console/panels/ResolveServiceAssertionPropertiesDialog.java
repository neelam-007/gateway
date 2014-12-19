package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.ResolveServiceAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

public class ResolveServiceAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ResolveServiceAssertion> {
    private JPanel contentPane;
    private JTextField uriField;
    private TargetVariablePanel prefixVariablePanel;

    private InputValidator inputValidator;

    public ResolveServiceAssertionPropertiesDialog(Window owner, final ResolveServiceAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        // prefix variable panel
        prefixVariablePanel.setValueWillBeRead(false);
        prefixVariablePanel.setValueWillBeWritten(true);

        // input validation
        inputValidator = new InputValidator(this, "Resolve Service Assertion Validation");

        // URI is required and must begin with a slash or be a valid context variable
        inputValidator.addRule(new InputValidator.ComponentValidationRule(uriField) {
            @Override
            public String getValidationError() {
                String uri = uriField.getText().trim();

                if (uri.isEmpty()) {
                    return "A URI path (or context variable expression) is required.";
                }

                if (Syntax.getReferencedNames(uri).length < 1 && !uri.startsWith("/")) {
                    return "A URI path that does not use context variables must start with a forward slash";
                }

                return null;
            }
        });

        // validate prefix
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return prefixVariablePanel.getErrorMessage();
            }
        });
    }

    @Override
    public void setData(ResolveServiceAssertion assertion) {
        uriField.setText(assertion.getUri());

        String prefixVariable = assertion.getPrefix();

        if ( prefixVariable == null )
            prefixVariable = ResolveServiceAssertion.DEFAULT_VARIABLE_PREFIX;

        prefixVariablePanel.setAssertion(assertion, getPreviousAssertion());
        prefixVariablePanel.setVariable(prefixVariable);
    }

    @Override
    public ResolveServiceAssertion getData(ResolveServiceAssertion assertion) throws ValidationException {
        // perform validation
        final String error = inputValidator.validate();

        if (error != null) {
            throw new ValidationException(error);
        }

        assertion.setUri(uriField.getText());
        assertion.setPrefix(prefixVariablePanel.getVariable());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
