package com.l7tech.external.assertions.ratelimit.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.ratelimit.RateLimitQueryAssertion;

import javax.swing.*;
import java.awt.*;

public class RateLimitQueryPropertiesDialog extends AssertionPropertiesOkCancelSupport<RateLimitQueryAssertion> {
    private JPanel contentPane;
    private JTextField counterNameField;
    private JTextField variablePrefixField;

    public RateLimitQueryPropertiesDialog(Window owner, RateLimitQueryAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
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

        String prefix = assertion.getVariblePrefix();
        variablePrefixField.setText(prefix == null ? "" : prefix);
    }

    @Override
    public RateLimitQueryAssertion getData(RateLimitQueryAssertion assertion) throws ValidationException {
        String name = counterNameField.getText();
        if (name == null || name.trim().length() < 1)
            throw new ValidationException("A counter name must be provided.");
        assertion.setCounterName(name);

        String prefix = variablePrefixField.getText();
        assertion.setVariblePrefix(prefix == null || prefix.trim().length() < 1 ? null : prefix);

        return assertion;
    }

}
