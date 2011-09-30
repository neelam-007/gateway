package com.l7tech.console.panels;

import com.l7tech.policy.assertion.sla.ThroughputQuotaQueryAssertion;

import javax.swing.*;
import java.awt.*;

public class ThroughputQuotaQueryPropertiesDialog extends AssertionPropertiesOkCancelSupport<ThroughputQuotaQueryAssertion> {
    private JPanel contentPane;
    private JTextField counterNameField;
    private JTextField variablePrefixField;

    public ThroughputQuotaQueryPropertiesDialog(Window owner, ThroughputQuotaQueryAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
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

        String prefix = assertion.getVariablePrefix();
        variablePrefixField.setText(prefix == null ? "" : prefix);
    }

    @Override
    public ThroughputQuotaQueryAssertion getData(ThroughputQuotaQueryAssertion assertion) throws ValidationException {
        String name = counterNameField.getText();
        if (name == null || name.trim().length() < 1)
            throw new ValidationException("A counter name must be provided.");
        assertion.setCounterName(name);

        String prefix = variablePrefixField.getText();
        assertion.setVariablePrefix(prefix == null || prefix.trim().length() < 1 ? null : prefix);

        return assertion;
    }
}

