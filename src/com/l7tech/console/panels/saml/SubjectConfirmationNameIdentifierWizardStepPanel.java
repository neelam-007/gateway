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
 * The SAML Subject Confirmatioin selections <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class SubjectConfirmationNameIdentifierWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JTextField textFieldNameQualifier;
    private JCheckBox checkBoxFormatX509SubjectName;
    private JCheckBox checkBoxEmailAddress;
    private JCheckBox checkBoxWindowsDomainQualifiedName;
    private JCheckBox checkBoxUnspecified;

    /**
     * Creates new form WizardPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Name Identifier";
    }
}