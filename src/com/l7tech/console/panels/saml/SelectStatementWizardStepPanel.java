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
        setShowDescriptionPanel(false);
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
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Select the SAML Statement";
    }
}