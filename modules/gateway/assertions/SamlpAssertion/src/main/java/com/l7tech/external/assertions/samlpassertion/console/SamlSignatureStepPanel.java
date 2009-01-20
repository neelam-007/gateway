package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;

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
    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next, null);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next, null);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
        initialize();
    }

    public SamlSignatureStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(settings);
        signRequestCheckBox.setSelected(assertion.isSignRequest());
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
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(settings);
        assertion.setSignRequest(signRequestCheckBox.isSelected());
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
    public String getStepLabel() {
        return "Digital Signatures";
    }

    public String getDescription() {
        return "<html>Specify whether the assertion will include a digital signature in the request.</html>";
    }
}