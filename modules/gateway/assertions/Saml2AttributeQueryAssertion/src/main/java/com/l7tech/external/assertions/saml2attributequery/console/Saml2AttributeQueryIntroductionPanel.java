/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: IntroductionWizardStepPanel.java 21105 2008-11-07 23:44:17Z megery $
 */
package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.WizardStepPanel;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML introduction <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class Saml2AttributeQueryIntroductionPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel greetingPanel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel extraTextLabel;

    /**
     * Creates new form WizardPanel
     */
    public Saml2AttributeQueryIntroductionPanel(WizardStepPanel next, boolean readonly) {
        super(next, readonly);
        setShowDescriptionPanel(false);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    public Saml2AttributeQueryIntroductionPanel(WizardStepPanel next) {
        this(next, false);
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        descriptionLabel.setText("<html>This wizard assists in configuring the details of the SAMLP attribute query processor.</html>");
        titleLabel.setText("Welcome to the SAMLP Attribute Query Processor wizard.");
        extraTextLabel.setVisible(false);
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Introduction";
    }
}