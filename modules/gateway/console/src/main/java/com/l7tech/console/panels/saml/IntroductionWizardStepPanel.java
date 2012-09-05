/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class IntroductionWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel extraTextLabel;
    private boolean issueMode;
    private final boolean isSoap;

    /**
     * Creates new form WizardPanel
     */
    public IntroductionWizardStepPanel(WizardStepPanel next, boolean issueMode, boolean isSoap) {
        super(next);
        this.issueMode = issueMode;
        this.isSoap = isSoap;
        setShowDescriptionPanel(false);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    public IntroductionWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        this(next, issueMode, true);
    }

    public IntroductionWizardStepPanel(WizardStepPanel next) {
        this(next, false);
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        if (issueMode) {
            descriptionLabel.setText("<html>This wizard assists in specifying the details of the SAML assertion to be issued.</html>");
            titleLabel.setText("Welcome to the SAML wizard.");
            extraTextLabel.setVisible(false);
        } else {
            if (isSoap) {
                titleLabel.setText("Welcome to the SAML Token Profile Wizard.");
            } else {
                titleLabel.setText("Welcome to the Validate SAML Assertion Wizard.");
            }

            descriptionLabel.setText("<html>This wizard assists in specifying a SAML assertion and any " +
                    "associated <br>requirements and/or conditions against which the " +
                    "assertion is validated.</html>");
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Introduction";
    }
}