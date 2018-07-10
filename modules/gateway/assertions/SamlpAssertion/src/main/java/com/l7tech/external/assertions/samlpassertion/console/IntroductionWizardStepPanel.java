/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: IntroductionWizardStepPanel.java 21105 2008-11-07 23:44:17Z megery $
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class IntroductionWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel extraTextLabel;

    /**
     * Creates new form WizardPanel
     */
    public IntroductionWizardStepPanel(WizardStepPanel next, AssertionMode mode, Assertion prevAssertion) {
        super(next, mode, prevAssertion);
        setShowDescriptionPanel(false);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    public IntroductionWizardStepPanel(WizardStepPanel next, Assertion prevAssertion) {
        this(next, null, prevAssertion);
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        if (isRequestMode()) {
            descriptionLabel.setText("<html>This wizard assists in specifying the details of the SAMLP assertion to be issued.</html>");
            titleLabel.setText("Welcome to the SAML Protocol Request Wizard");
            extraTextLabel.setVisible(false);
        } else {
            descriptionLabel.setText("<html>This wizard assists in specifying a SAMLP Evaluator assertion " +
                                     "and any<br> associated requirements and/or conditions against which the " +
                                     "assertion is validated.</html>");
            titleLabel.setText("Welcome to the SAML Protocol Response Wizard");
            extraTextLabel.setVisible(false);
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Introduction";
    }
}