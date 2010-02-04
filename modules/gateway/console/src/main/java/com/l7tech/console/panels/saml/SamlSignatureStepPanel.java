package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.SamlIssuerConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * The signature selection <code>WizardStepPanel</code>
 */
public class SamlSignatureStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JCheckBox signAssertionCheckBox;
    private JCheckBox decorateAddAssertionCheckBox;
    private JComboBox decorateRequestOrResponseCombo;
    private JCheckBox decorateSignatureIncludeBodyCheckBox;
    private JCheckBox decorateSignatureIncludeAssertionCheckBox;

    /**
     * Creates new form Version WizardPanel
     */
    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    public SamlSignatureStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next);
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
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerConfiguration issuerConfiguration = (SamlIssuerConfiguration) settings;
        signAssertionCheckBox.setSelected(issuerConfiguration.isSignAssertion());

        EnumSet<SamlIssuerConfiguration.DecorationType> dts = issuerConfiguration.getDecorationTypes();
        if (dts == null || dts.isEmpty()) return;

        decorateAddAssertionCheckBox.setSelected(dts.contains(SamlIssuerConfiguration.DecorationType.ADD_ASSERTION));
        if (dts.contains(SamlIssuerConfiguration.DecorationType.RESPONSE)) {
            decorateRequestOrResponseCombo.setSelectedItem("Response");
        } else {
            decorateRequestOrResponseCombo.setSelectedItem("Request");
        }
        decorateSignatureIncludeAssertionCheckBox.setSelected(dts.contains(SamlIssuerConfiguration.DecorationType.SIGN_ASSERTION));
        decorateSignatureIncludeBodyCheckBox.setSelected(dts.contains(SamlIssuerConfiguration.DecorationType.SIGN_BODY));
        enableDisable();
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
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerConfiguration issuerConfiguration = (SamlIssuerConfiguration) settings;
        issuerConfiguration.setSignAssertion(signAssertionCheckBox.isSelected());
        if (decorateAddAssertionCheckBox.isSelected()) {
            Set<SamlIssuerConfiguration.DecorationType> tempDts = new HashSet<SamlIssuerConfiguration.DecorationType>();
            tempDts.add(SamlIssuerConfiguration.DecorationType.ADD_ASSERTION);
            if (decorateRequestOrResponseCombo.getSelectedItem() == "Response") {
                tempDts.add(SamlIssuerConfiguration.DecorationType.RESPONSE);
            } else {
                tempDts.add(SamlIssuerConfiguration.DecorationType.REQUEST);
            }
            if (decorateSignatureIncludeBodyCheckBox.isSelected()) tempDts.add(SamlIssuerConfiguration.DecorationType.SIGN_BODY);
            if (decorateSignatureIncludeAssertionCheckBox.isSelected()) tempDts.add(SamlIssuerConfiguration.DecorationType.SIGN_ASSERTION);
            issuerConfiguration.setDecorationTypes(EnumSet.copyOf(tempDts));
        } else {
            issuerConfiguration.setDecorationTypes(EnumSet.noneOf(SamlIssuerConfiguration.DecorationType.class));
        }
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

        decorateAddAssertionCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisable();
            }
        });

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        enableDisable();
    }

    private void enableDisable() {
        boolean enable = decorateAddAssertionCheckBox.isSelected();
        decorateRequestOrResponseCombo.setEnabled(enable);
        decorateSignatureIncludeAssertionCheckBox.setEnabled(enable);
        decorateSignatureIncludeBodyCheckBox.setEnabled(enable);
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
        return "<html>Specify the digital signatures that the Gateway should create (if any) after issuing the Assertion.</html>";
    }
}
