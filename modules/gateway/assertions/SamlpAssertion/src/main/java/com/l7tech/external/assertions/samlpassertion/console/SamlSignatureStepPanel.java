package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;

/**
 * The signature selection <code>WizardStepPanel</code>
 */
public class SamlSignatureStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JCheckBox signRequestCheckBox;

    /**
     * Creates new form Version WizardPanel
     */
    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel, Assertion prevAssertion) {
        super(next, null, prevAssertion);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner, Assertion prevAssertion) {
        super(next, null, prevAssertion);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
        initialize();
    }

    public SamlSignatureStepPanel(WizardStepPanel next, Assertion prevAssertion) {
        this(next, true, prevAssertion);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(settings);
        signRequestCheckBox.setSelected(assertion.isSignAssertion());
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(settings);
        assertion.setSignAssertion(signRequestCheckBox.isSelected());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Digital Signatures";
    }

    @Override
    public String getDescription() {
        return "<html>Specify whether the assertion will include a digital signature in the request.</html>";
    }
}