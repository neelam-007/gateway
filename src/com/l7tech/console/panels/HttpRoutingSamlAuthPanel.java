/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;

/**
 * @author alex
 */
public class HttpRoutingSamlAuthPanel extends JPanel {
    private final HttpRoutingAssertion assertion;

    private JPanel mainPanel;
    private JSpinner expirySpinner;
    private JLabel expiryLabel;

    public HttpRoutingSamlAuthPanel(final HttpRoutingAssertion assertion) {
        this.assertion = assertion;
        expirySpinner.setModel(new SpinnerNumberModel(new Integer(5), new Integer(1), new Integer(120), new Integer(1)));
        expiryLabel.setLabelFor(expirySpinner);

        //memebershipStatementCheck.setSelected(assertion.isGroupMembershipStatement()); // Bugzilla 1269
        int expiry = assertion.getSamlAssertionExpiry();
        if (expiry == 0) {
            expiry = 5;
        }

        expirySpinner.setValue(new Integer(expiry));
        
        add(mainPanel);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        expirySpinner.setEnabled(enabled);
        expiryLabel.setEnabled(enabled);
    }

    public JSpinner getExpirySpinner() {
        return expirySpinner;
    }

    public void updateModel() {
        final Integer sv = (Integer)expirySpinner.getValue();
        assertion.setSamlAssertionExpiry(sv.intValue());
    }
}
