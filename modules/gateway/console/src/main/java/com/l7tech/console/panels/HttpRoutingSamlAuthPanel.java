/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;

/**
 * @author alex
 */
public class HttpRoutingSamlAuthPanel extends JPanel {
    private final HttpRoutingAssertion assertion;

    private JPanel mainPanel;
    private JSpinner expirySpinner;
    private JLabel expiryLabel;
    private JComboBox samlVersionComboBox;
    private JLabel samlVersionLabel;

    public HttpRoutingSamlAuthPanel(final HttpRoutingAssertion assertion) {
        this.assertion = assertion;
        expirySpinner.setModel(new SpinnerNumberModel(5, 1, 120, 1));
        expiryLabel.setLabelFor(expirySpinner);

        samlVersionComboBox.setModel(new DefaultComboBoxModel(new String[]{"1.1", "2.0"}));
        samlVersionLabel.setLabelFor(samlVersionComboBox);

        //memebershipStatementCheck.setSelected(assertion.isGroupMembershipStatement()); // Bugzilla 1269
        int expiry = assertion.getSamlAssertionExpiry();
        if (expiry == 0) {
            expiry = 5;
        }

        expirySpinner.setValue(expiry);

        samlVersionComboBox.setSelectedItem(assertion.getSamlAssertionVersion()==1 ? "1.1" : "2.0");

        add(mainPanel);
    }

    public HttpRoutingSamlAuthPanel(final HttpRoutingAssertion assertion, final InputValidator inputValidator) {
        this(assertion);

        // Add an input validator to expirySpinner
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(expirySpinner, "Ticket expiry"));
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        expirySpinner.setEnabled(enabled);
        expiryLabel.setEnabled(enabled);
        samlVersionComboBox.setEnabled(enabled);
        samlVersionLabel.setEnabled(enabled);
    }

    public void updateModel() {
        final Integer sv = (Integer) expirySpinner.getValue();        
        assertion.setSamlAssertionVersion("1.1".equals(samlVersionComboBox.getSelectedItem()) ? 1 : 2);
        assertion.setSamlAssertionExpiry(sv);
    }
}
