/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class ConditionsWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JCheckBox checkBoxCheckAssertionValidity;
    private JTextField textFieldAudienceRestriction;

    /**
     * Creates new form WizardPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlStatementAssertion statement = (SamlStatementAssertion)settings;
        statement.setAudienceRestriction(textFieldAudienceRestriction.getText());
        statement.setCheckAssertionValidity(checkBoxCheckAssertionValidity.isSelected());
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlStatementAssertion statement = (SamlStatementAssertion)settings;
        textFieldAudienceRestriction.setText(statement.getAudienceRestriction());
        checkBoxCheckAssertionValidity.setSelected(statement.isCheckAssertionValidity());
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Conditions";
    }

    public String getDescription() {
        return
          "<html>Specify SAML statement conditions such as Audience Restriction [optional]<br>" +
            "and whether to check the assertion validity [optional]</html>";

    }
}