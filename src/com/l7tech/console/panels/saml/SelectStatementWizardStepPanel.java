/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

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
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
        SamlAuthenticationStatement auths = requestWssSaml.getAuthenticationStatement();
        SamlAuthorizationStatement athz = requestWssSaml.getAuthorizationStatement();
        SamlAttributeStatement atts = requestWssSaml.getAttributeStatement();

        if (auths == null && athz == null && atts == null) {
            authenticationStatementRadioButton.setSelected(true);
        } else if (auths !=null) {
            authenticationStatementRadioButton.setSelected(true);
        } else if (athz !=null) {
            authorizationDecisionStatementRadioButton.setSelected(true);
        } else if (atts !=null) {
            attributeStatementRadioButton.setSelected(true);
        }
    }

    public void storeSettings(Object settings)
      throws IllegalArgumentException {
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
        if (authenticationStatementRadioButton.isSelected()) {
            SamlAuthenticationStatement atts = requestWssSaml.getAuthenticationStatement();
            if (atts == null) {
                requestWssSaml.setAuthenticationStatement(new SamlAuthenticationStatement());
            }
        } else {
            requestWssSaml.setAuthenticationStatement(null);
        }
        if (authorizationDecisionStatementRadioButton.isSelected()) {
            SamlAuthorizationStatement athzs = requestWssSaml.getAuthorizationStatement();
            if (athzs == null) {
                requestWssSaml.setAuthorizationStatement(new SamlAuthorizationStatement());
            }
        } else {
            requestWssSaml.setAuthorizationStatement(null);
        }
        if (attributeStatementRadioButton.isSelected()) {
            SamlAttributeStatement atts = requestWssSaml.getAttributeStatement();
            if (atts == null) {
                requestWssSaml.setAttributeStatement(new SamlAttributeStatement());
            }
        } else {
            requestWssSaml.setAttributeStatement(null);
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

    public boolean canFinish() {
        return false;
    }
}