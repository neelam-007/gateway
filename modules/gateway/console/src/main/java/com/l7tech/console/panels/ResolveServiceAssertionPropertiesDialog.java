package com.l7tech.console.panels;

import com.l7tech.policy.assertion.ResolveServiceAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

public class ResolveServiceAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ResolveServiceAssertion> {

    private JPanel contentPane;
    private JTextField uriField;
    private JTextField resolvedServiceTextField;

    public ResolveServiceAssertionPropertiesDialog(Window owner, final ResolveServiceAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(ResolveServiceAssertion assertion) {
        uriField.setText(assertion.getUri());
        String prefixVariable = assertion.getPrefix();
        if ( prefixVariable == null )
            prefixVariable = ResolveServiceAssertion.DEFAULT_VARIABLE_PREFIX;
        resolvedServiceTextField.setText(prefixVariable);
    }

    @Override
    public ResolveServiceAssertion getData(ResolveServiceAssertion assertion) throws ValidationException {
        final String uri = uriField.getText();
        if (uri == null || uri.trim().length() < 1)
            throw new ValidationException("A URI path (or context variable expression) is required");
        if (Syntax.getReferencedNames(uri).length < 1 && !uri.startsWith("/"))
            throw new ValidationException("A URI path that does not use context variables must start with a forward slash");
        assertion.setUri(uri);
        final String prefix = resolvedServiceTextField.getText();
        if ( prefix == null | prefix.trim().length() < 1 )
            throw new ValidationException("The Variable Prefix cannot be empty.");
        assertion.setPrefix(resolvedServiceTextField.getText().trim());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
