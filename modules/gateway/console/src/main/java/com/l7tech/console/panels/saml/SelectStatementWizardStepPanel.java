/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class SelectStatementWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JToggleButton authenticationStatementRadioButton;
    private JCheckBox authenticationStatementIncludeAuthnContextDecl;
    private JToggleButton authorizationDecisionStatementRadioButton;
    private JToggleButton attributeStatementRadioButton;
    private JPanel buttonsPanel;
    private final boolean issueMode;
    private boolean enableIncludeAuthnContextDecl;

    private final ActionListener changeListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableDisableComponents();
            notifyListeners();
        }
    };

    /**
     * Creates new form WizardPanel
     */
    public SelectStatementWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        super(next);
        this.issueMode = issueMode;
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    public SelectStatementWizardStepPanel(WizardStepPanel next) {
        this(next, false);
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        if (issueMode) {
            authenticationStatementRadioButton = new JCheckBox("Authentication Statement");
            authorizationDecisionStatementRadioButton = new JCheckBox("Authorization Decision Statement");
            attributeStatementRadioButton = new JCheckBox("Attribute Statement");            
            authenticationStatementIncludeAuthnContextDecl = new JCheckBox("Include Authentication Context Declaration");
        } else {
            authenticationStatementRadioButton = new JRadioButton("Authentication Statement");
            authorizationDecisionStatementRadioButton = new JRadioButton("Authorization Decision Statement");
            attributeStatementRadioButton = new JRadioButton("Attribute Statement");
            ButtonGroup bg = new ButtonGroup();
            bg.add(authenticationStatementRadioButton);
            bg.add(authorizationDecisionStatementRadioButton);
            bg.add(attributeStatementRadioButton);
            authenticationStatementRadioButton.setSelected(true);
        }

        buttonsPanel.add(authenticationStatementRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        buttonsPanel.add(authorizationDecisionStatementRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        buttonsPanel.add(attributeStatementRadioButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));

        if (issueMode) {
            buttonsPanel.add(authenticationStatementIncludeAuthnContextDecl, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        }

        authenticationStatementRadioButton.addActionListener(changeListener);
        authorizationDecisionStatementRadioButton.addActionListener(changeListener);
        attributeStatementRadioButton.addActionListener(changeListener);
    }

    @Override
    public boolean canAdvance() {
        return authenticationStatementRadioButton.isSelected() || authorizationDecisionStatementRadioButton.isSelected() || attributeStatementRadioButton.isSelected();
    }

    @Override
    public void readSettings(Object settings)
      throws IllegalArgumentException {
        SamlPolicyAssertion assertion = (SamlPolicyAssertion) settings;
        SamlAuthenticationStatement auths = assertion.getAuthenticationStatement();
        SamlAuthorizationStatement athz = assertion.getAuthorizationStatement();
        SamlAttributeStatement atts = assertion.getAttributeStatement();

        if (auths == null && athz == null && atts == null) authenticationStatementRadioButton.setSelected(true);
        if (auths != null) authenticationStatementRadioButton.setSelected(true);
        if (athz  != null) authorizationDecisionStatementRadioButton.setSelected(true);
        if (atts  != null) attributeStatementRadioButton.setSelected(true);
        if (issueMode) {
            if (auths != null ) authenticationStatementIncludeAuthnContextDecl.setSelected(auths.isIncludeAuthenticationContextDeclaration());
        }

        final Integer version = assertion.getVersion();
        enableIncludeAuthnContextDecl = version == null || version != 1;

        enableDisableComponents();
    }

    @Override
    public void storeSettings(Object settings)
      throws IllegalArgumentException {
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        if (authenticationStatementRadioButton.isSelected()) {
            SamlAuthenticationStatement atts = assertion.getAuthenticationStatement();
            if (atts == null) {
                assertion.setAuthenticationStatement(new SamlAuthenticationStatement());
            }
            if (issueMode) {
                assertion.getAuthenticationStatement().setIncludeAuthenticationContextDeclaration(
                    authenticationStatementIncludeAuthnContextDecl.isEnabled() &&
                    authenticationStatementIncludeAuthnContextDecl.isSelected() );
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

    private void enableDisableComponents() {
        if (issueMode) {
            authenticationStatementIncludeAuthnContextDecl.setEnabled( enableIncludeAuthnContextDecl && authenticationStatementRadioButton.isSelected() );
        }
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "SAML Statement Type";
    }

    @Override
    public String getDescription() {
        return "Select the SAML Statement Type you wish to configure.";
    }
}