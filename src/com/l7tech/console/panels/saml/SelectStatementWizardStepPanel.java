/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.*;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class SelectStatementWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JRadioButton authenticationStatementRadioButton;
    private JRadioButton authorizationDecisionStatementRadioButton;
    private JRadioButton attributeStatementRadioButton;

    /**
     * Creates new form WizardPanel
     */
    public SelectStatementWizardStepPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        ButtonGroup bg = new ButtonGroup();
        bg.add(authenticationStatementRadioButton);
        bg.add(authorizationDecisionStatementRadioButton);
        bg.add(attributeStatementRadioButton);
        authenticationStatementRadioButton.setSelected(true);
    }

    public void readSettings(Object settings)
      throws IllegalArgumentException {
        SamlPolicyAssertion assertion = (SamlPolicyAssertion) settings;
        SamlAuthenticationStatement auths = assertion.getAuthenticationStatement();
        SamlAuthorizationStatement athz = assertion.getAuthorizationStatement();
        SamlAttributeStatement atts = assertion.getAttributeStatement();

        if (auths == null && athz == null && atts == null) {
            authenticationStatementRadioButton.setSelected(true);
        } else if (auths !=null) {
            authenticationStatementRadioButton.setSelected(true);
        } else if (athz !=null) {
            authorizationDecisionStatementRadioButton.setSelected(true);
        } else
            attributeStatementRadioButton.setSelected(true);
    }

    public void storeSettings(Object settings)
      throws IllegalArgumentException {
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        if (authenticationStatementRadioButton.isSelected()) {
            SamlAuthenticationStatement atts = assertion.getAuthenticationStatement();
            if (atts == null) {
                assertion.setAuthenticationStatement(new SamlAuthenticationStatement());
            }
        } else {
            assertion.setAuthenticationStatement(null);
        }
        if (authorizationDecisionStatementRadioButton.isSelected()) {
            SamlAuthorizationStatement athzs = assertion.getAuthorizationStatement();
            if (athzs == null) {
                assertion.setAuthorizationStatement(new SamlAuthorizationStatement());
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
        return "SAML Statement Type";
    }

    public String getDescription() {
        return "Select the SAML Statement Type you wish to configure.";
    }
}