package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.SamlIssuerAssertion;

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
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerAssertion sia = (SamlIssuerAssertion) settings;
        signAssertionCheckBox.setSelected(sia.isSignAssertion());

        EnumSet<SamlIssuerAssertion.DecorationType> dts = sia.getDecorationTypes();
        if (dts == null || dts.isEmpty()) return;

        decorateAddAssertionCheckBox.setSelected(dts.contains(SamlIssuerAssertion.DecorationType.ADD_ASSERTION));
        if (dts.contains(SamlIssuerAssertion.DecorationType.RESPONSE)) {
            decorateRequestOrResponseCombo.setSelectedItem("Response");
        } else {
            decorateRequestOrResponseCombo.setSelectedItem("Request");
        }
        decorateSignatureIncludeAssertionCheckBox.setSelected(dts.contains(SamlIssuerAssertion.DecorationType.SIGN_ASSERTION));
        decorateSignatureIncludeBodyCheckBox.setSelected(dts.contains(SamlIssuerAssertion.DecorationType.SIGN_BODY));
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
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerAssertion sia = (SamlIssuerAssertion) settings;
        sia.setSignAssertion(signAssertionCheckBox.isSelected());
        if (decorateAddAssertionCheckBox.isSelected()) {
            Set<SamlIssuerAssertion.DecorationType> tempDts = new HashSet<SamlIssuerAssertion.DecorationType>();
            tempDts.add(SamlIssuerAssertion.DecorationType.ADD_ASSERTION);
            if (decorateRequestOrResponseCombo.getSelectedItem() == "Response") {
                tempDts.add(SamlIssuerAssertion.DecorationType.RESPONSE);
            } else {
                tempDts.add(SamlIssuerAssertion.DecorationType.REQUEST);
            }
            if (decorateSignatureIncludeBodyCheckBox.isSelected()) tempDts.add(SamlIssuerAssertion.DecorationType.SIGN_BODY);
            if (decorateSignatureIncludeAssertionCheckBox.isSelected()) tempDts.add(SamlIssuerAssertion.DecorationType.SIGN_ASSERTION);
            sia.setDecorationTypes(EnumSet.copyOf(tempDts));
        } else {
            sia.setDecorationTypes(EnumSet.noneOf(SamlIssuerAssertion.DecorationType.class));
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
    public String getStepLabel() {
        return "Digital Signatures";
    }

    public String getDescription() {
        return "<html>Specify the digital signatures that the Gateway should create (if any) after issuing the Assertion.</html>";
    }
}