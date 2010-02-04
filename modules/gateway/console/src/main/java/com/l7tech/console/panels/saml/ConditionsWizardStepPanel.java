/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.SamlIssuerConfiguration;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private JSpinner notBeforeSpinner;
    private JSpinner notOnOrAfterSpinner;
    private JPanel validityPanel;
    private JLabel notBeforeLabel;
    private JLabel notOnOrAfterLabel;
    private JCheckBox validityCheckbox;

    private final boolean showTitleLabel;
    private final boolean issueMode;

    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = false;
        initialize();
    }


    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        this(next, showTitleLabel, issueMode, null);
    }

    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlPolicyAssertion samlAssertion = (SamlPolicyAssertion) settings;
        samlAssertion.setAudienceRestriction(textFieldAudienceRestriction.getText());
        if (issueMode) {
            SamlIssuerConfiguration issuerConfiguration = (SamlIssuerConfiguration) samlAssertion;
            issuerConfiguration.setConditionsNotBeforeSecondsInPast(validityCheckbox.isSelected() ? (Integer)notBeforeSpinner.getValue() : -1);
            issuerConfiguration.setConditionsNotOnOrAfterExpirySeconds(validityCheckbox.isSelected() ? (Integer)notOnOrAfterSpinner.getValue() : -1);
        } else {
            ((RequireWssSaml)settings).setCheckAssertionValidity(checkBoxCheckAssertionValidity.isSelected());
        }
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlPolicyAssertion samlAssertion = (SamlPolicyAssertion)settings;
        textFieldAudienceRestriction.setText(samlAssertion.getAudienceRestriction());

        if (issueMode) {
            SamlIssuerConfiguration issuerConfiguration = (SamlIssuerConfiguration) samlAssertion;
            final int secondsInPast = issuerConfiguration.getConditionsNotBeforeSecondsInPast();
            final int onOrAfterExpirySeconds = issuerConfiguration.getConditionsNotOnOrAfterExpirySeconds();
            if (secondsInPast != -1 || onOrAfterExpirySeconds != -1) {
                validityCheckbox.setSelected(true);
                notBeforeLabel.setEnabled(true);
                notBeforeSpinner.setEnabled(true);
                notOnOrAfterLabel.setEnabled(true);
                notOnOrAfterSpinner.setEnabled(true);
            }

            if (secondsInPast != -1) notBeforeSpinner.setValue(secondsInPast);
            if (onOrAfterExpirySeconds != -1) notOnOrAfterSpinner.setValue(onOrAfterExpirySeconds);
        } else {
            RequireWssSaml ass = (RequireWssSaml) samlAssertion;
            checkBoxCheckAssertionValidity.setSelected(ass.isCheckAssertionValidity());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (issueMode) {
            checkBoxCheckAssertionValidity.setVisible(false);
            notBeforeSpinner.setModel(new SpinnerNumberModel(120, 0, 3600, 1));
            validationRules.add(new InputValidator.NumberSpinnerValidationRule(notBeforeSpinner, "Not Before seconds in past"));
            notOnOrAfterSpinner.setModel(new SpinnerNumberModel(300, 30, 3600, 1));
            validationRules.add(new InputValidator.NumberSpinnerValidationRule(notOnOrAfterSpinner, "Not On Or After seconds in future"));
            validityCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final boolean b = validityCheckbox.isSelected();
                    notBeforeLabel.setEnabled(b);
                    notBeforeSpinner.setEnabled(b);
                    notOnOrAfterLabel.setEnabled(b);
                    notOnOrAfterSpinner.setEnabled(b);
                }
            });
        } else {
            validityPanel.setVisible(false);
            validityCheckbox.setVisible(false);
            notBeforeLabel.setVisible(false);
            notBeforeSpinner.setVisible(false);
            notOnOrAfterLabel.setVisible(false);
            notOnOrAfterSpinner.setVisible(false);
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Conditions";
    }

    @Override
    public String getDescription() {
        if (issueMode) {
            return
          "<html>Specify SAML statement conditions such as assertion validity period and Audience Restriction</html>";
        } else {
            return
          "<html>Specify SAML statement conditions such as Audience Restriction [optional] " +
                "and whether to check the assertion validity [optional]</html>";
        }
    }
}
