/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlpAuthorizationStatement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class SelectSamlpQueryWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JToggleButton authenticationStatementRadioButton;
    private JToggleButton authorizationDecisionStatementRadioButton;
    private JToggleButton attributeStatementRadioButton;
    private JPanel buttonsPanel;

    private final ActionListener changeListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            notifyListeners();
        }
    };

    /**
     * Creates new form WizardPanel
     */
    public SelectSamlpQueryWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        super(next, mode);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    public SelectSamlpQueryWizardStepPanel(WizardStepPanel next) {
        this(next, null);
    }

    private void initialize() {

        // title label
        if (isRequestMode()) {
            titleLabel.setText("Select the SAMLP query request");
        } else {
            titleLabel.setText("Select the expected SAMLP response to evaluate");
        }
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        // radio buttons
        if (isRequestMode()) {
            authenticationStatementRadioButton = new JRadioButton("Authentication Request");
            authorizationDecisionStatementRadioButton = new JRadioButton("Authorization Decision Request");
            attributeStatementRadioButton = new JRadioButton("Attribute Query Request");
        } else {
            authenticationStatementRadioButton = new JRadioButton("Authentication Response");
            authorizationDecisionStatementRadioButton = new JRadioButton("Authorization Decision Response");
            attributeStatementRadioButton = new JRadioButton("Attribute Query Response");
        }
        ButtonGroup bg = new ButtonGroup();
        bg.add(authenticationStatementRadioButton);
        bg.add(authorizationDecisionStatementRadioButton);
        bg.add(attributeStatementRadioButton);
        authenticationStatementRadioButton.setSelected(true);

        buttonsPanel.add(authenticationStatementRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        buttonsPanel.add(authorizationDecisionStatementRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        buttonsPanel.add(attributeStatementRadioButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));

        authenticationStatementRadioButton.addActionListener(changeListener);
        authorizationDecisionStatementRadioButton.addActionListener(changeListener);
        attributeStatementRadioButton.addActionListener(changeListener);
    }

    @Override
    public boolean canAdvance() {
        return authenticationStatementRadioButton.isSelected() || authorizationDecisionStatementRadioButton.isSelected() || attributeStatementRadioButton.isSelected();
    }

    public void readSettings(Object settings)
      throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);
        SamlAuthenticationStatement auths = assertion.getAuthenticationStatement();
        SamlpAuthorizationStatement athz = assertion.getAuthorizationStatement();
        SamlAttributeStatement atts = assertion.getAttributeStatement();

        if (assertion.getSamlVersion() == 2) {
            authenticationStatementRadioButton.setEnabled(true);
            if (auths == null && athz == null && atts == null) authenticationStatementRadioButton.setSelected(true);
            if (auths != null) authenticationStatementRadioButton.setSelected(true);
            if (athz  != null) authorizationDecisionStatementRadioButton.setSelected(true);
            if (atts  != null) attributeStatementRadioButton.setSelected(true);
        } else {
            // Authentication Request is not supported in SAMLP 1.1
            authenticationStatementRadioButton.setSelected(false);
            authenticationStatementRadioButton.setEnabled(false);
            if (athz == null && atts == null) authorizationDecisionStatementRadioButton.setSelected(true);
            if (athz  != null) authorizationDecisionStatementRadioButton.setSelected(true);
            if (atts  != null) attributeStatementRadioButton.setSelected(true);
        }
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {

        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);

        if (authenticationStatementRadioButton.isSelected()) {
            SamlAuthenticationStatement atts = assertion.getAuthenticationStatement();
            if (atts == null) {
                assertion.setAuthenticationStatement(new SamlAuthenticationStatement());
            }
        } else {
            assertion.setAuthenticationStatement(null);
        }
        if (authorizationDecisionStatementRadioButton.isSelected()) {
            SamlpAuthorizationStatement athzs = assertion.getAuthorizationStatement();
            if (athzs == null) {
                assertion.setAuthorizationStatement(new SamlpAuthorizationStatement());
            }
        } else {
            assertion.setAuthorizationStatement(null);
        }
        if (attributeStatementRadioButton.isSelected()) {
            SamlAttributeStatement atts = assertion.getAttributeStatement();
            if (atts == null) {
                assertion.setAttributeStatement(new SamlAttributeStatement());
            }
        } else {
            assertion.setAttributeStatement(null);
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {

        if (isRequestMode())
            return "SAMLP Request Type";
        return "SAMLP Response Type";
    }

    public String getDescription() {
        if (isRequestMode())
            return "Select the type of SAMLP query request you wish to configure.";
        return "Select the type of SAMLP response you wish to evaluate.";
    }
}