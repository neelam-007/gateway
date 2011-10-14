package com.l7tech.console.panels;

import com.l7tech.policy.assertion.ResolveServiceAssertion;

import javax.swing.*;
import java.awt.*;

public class ResolveServiceAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ResolveServiceAssertion> {
    private JPanel contentPane;
    private JTextField uriField;

    public ResolveServiceAssertionPropertiesDialog(Window owner, final ResolveServiceAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(ResolveServiceAssertion assertion) {
        uriField.setText(assertion.getUri());
    }

    @Override
    public ResolveServiceAssertion getData(ResolveServiceAssertion assertion) throws ValidationException {
        final String uri = uriField.getText();
        if (uri == null || uri.trim().length() < 1)
            throw new ValidationException("A URI path (or context variable expression) is required");
        assertion.setUri(uri);
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
