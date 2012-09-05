package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.HasOptionalSamlSignature;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RequireEmbeddedSignatureWizardStepPanel extends WizardStepPanel {
    private final boolean showTitleLabel;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JCheckBox requireSignatureCheckBox;

    public RequireEmbeddedSignatureWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        this(next, showTitleLabel, null);
    }

    public RequireEmbeddedSignatureWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, @Nullable JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        if (owner != null) {
            setOwner(owner);
        }
        initialize();
    }

    public RequireEmbeddedSignatureWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        HasOptionalSamlSignature samlAssertion = (HasOptionalSamlSignature) settings;
        samlAssertion.setRequireDigitalSignature(requireSignatureCheckBox.isSelected());
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        HasOptionalSamlSignature samlAssertion = (HasOptionalSamlSignature) settings;
        requireSignatureCheckBox.setSelected(samlAssertion.isRequireDigitalSignature());
    }

    @Override
    public String getStepLabel() {
        return "Embedded Signature";
    }

    @Override
    public String getDescription() {
        return "Configure whether the SAML assertion must contain an embedded signature or\n" +
                "not. If configured then there must be a Signature element which is a child of\n" +
                "the Assertion element that signs the SAML Assertion.";
    }

    // - PRIVATE

    private void initialize() {
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);

        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }
    }
}

