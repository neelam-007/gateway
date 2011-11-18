package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.assertion.SamlIssuerConfiguration;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Step to customize the Issuer value.
 */
public class IssuerWizardStepPanel extends WizardStepPanel {

    public IssuerWizardStepPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    @Override
    public boolean canAdvance() {

        if (version == 2) {
            if (!SquigglyFieldUtils.validateSquigglyFieldForVariableReference(nameQualifierSquigglyTextField)) {
                return false;
            }
        }

        if (issuerFromTemplateRadioButton.isSelected()) {
            final boolean hasIssuerValue = !issuerValueSquigglyTextField.getText().trim().isEmpty();
            if (!hasIssuerValue) {
                return false;
            }

            if (!SquigglyFieldUtils.validateSquigglyFieldForVariableReference(issuerValueSquigglyTextField)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getStepLabel() {
        return "Issuer";
    }

    @Override
    public String getDescription() {

        final String defaultValMsg = "subject distinguished name of the configured private key's corresponding public key.";
        final String defaultMsg;
        switch (version) {
            case 2:
                defaultMsg = "Default value for Issuer element is the " + defaultValMsg;
                return "<html>Configure the Issuer element value and attributes.<br><br>" +
                        defaultMsg + " No attributes are set by default.</html>";
            default:
                defaultMsg = "Default value for the Issuer attribute is the " + defaultValMsg;
                return "<html>Configure the Issuer attribute value.<br><br>" +
                        defaultMsg + "</html>";
        }
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        final SamlPolicyAssertion samlPolicyAssertion = (SamlPolicyAssertion) settings;
        version = samlPolicyAssertion.getVersion() == null ? 1 : samlPolicyAssertion.getVersion();

        final SamlIssuerConfiguration issuerConfig = (SamlIssuerConfiguration) samlPolicyAssertion;

        final String customIssuerValue = issuerConfig.getCustomIssuerValue();
        if (customIssuerValue != null) {
            issuerFromTemplateRadioButton.setSelected(true);
            issuerValueSquigglyTextField.setText(customIssuerValue);
        } else {
            issuerDefaultRadioButton.setSelected(true);
        }

        if (version == 2) {
            final String customIssuerFormat = issuerConfig.getCustomIssuerFormat();
            if (customIssuerFormat != null) {
                includeFormatAttributeCheckBox.setSelected(true);

                if (SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(customIssuerFormat)) {
                    unspecifiedRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(customIssuerFormat)) {
                    emailAddressRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(customIssuerFormat)) {
                    x509SubjectRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(customIssuerFormat)) {
                    windowsDomainRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_KERBEROS.equals(customIssuerFormat)) {
                    kerberosRadioButton.setSelected(true);
                } else {
                    entityFormatRadioButton.setSelected(true);
                }
            } else {
                //set selected so if Format check box is enabled there will be a default selection.
                entityFormatRadioButton.setSelected(true);
            }

            final String customNameQualifier = issuerConfig.getCustomIssuerNameQualifier();
            if (customNameQualifier != null) {
                nameQualifierSquigglyTextField.setText(customNameQualifier);
            }
        }

        enableDisable();
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerConfiguration issuerConfig = (SamlIssuerConfiguration) settings;

        if (!issuerDefaultRadioButton.isSelected()) {
            issuerConfig.setCustomIssuerValue(issuerValueSquigglyTextField.getText().trim());
        } else {
            issuerConfig.setCustomIssuerValue(null);
        }

        if (version == 2) {
            final String nameFormatUri;
            if (includeFormatAttributeCheckBox.isSelected()) {
                if (unspecifiedRadioButton.isSelected()) {
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;
                } else if (emailAddressRadioButton.isSelected()) {
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_EMAIL;
                } else if (x509SubjectRadioButton.isSelected()) {
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_X509_SUBJECT;
                } else if (windowsDomainRadioButton.isSelected()) {
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_WINDOWS;
                } else if (kerberosRadioButton.isSelected()) {
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_KERBEROS;
                } else {
                    //default is entity as per SAML Core 2.0
                    nameFormatUri = SamlConstants.NAMEIDENTIFIER_ENTITY;
                }
            } else {
                nameFormatUri = null;
            }
            issuerConfig.setCustomIssuerFormat(nameFormatUri);

            final String customNameQualifier = nameQualifierSquigglyTextField.getText().trim();
            if (!customNameQualifier.isEmpty()) {
                issuerConfig.setCustomIssuerNameQualifier(customNameQualifier);
            } else {
                issuerConfig.setCustomIssuerNameQualifier(null);
            }
        } else {
            issuerConfig.setCustomIssuerFormat(null);
            issuerConfig.setCustomIssuerNameQualifier(null);
        }
    }

    // - PRIVATE

    private JCheckBox includeFormatAttributeCheckBox;
    private JPanel formatsButtonPanel;
    private JRadioButton entityFormatRadioButton;
    private JRadioButton unspecifiedRadioButton;
    private JRadioButton emailAddressRadioButton;
    private JRadioButton x509SubjectRadioButton;
    private JRadioButton windowsDomainRadioButton;
    private JRadioButton kerberosRadioButton;
    private SquigglyTextField issuerValueSquigglyTextField;
    private SquigglyTextField nameQualifierSquigglyTextField;
    private JPanel mainPanel;
    private JPanel formatPanel;
    private JLabel nameQualifierLabel;
    private JLabel titleLabel;
    private JRadioButton issuerDefaultRadioButton;
    private JRadioButton issuerFromTemplateRadioButton;
    private JPanel nameQualifierPanel;
    private JPanel issuerPanel;
    private JLabel issuerValueLabel;
    private int version;

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        includeFormatAttributeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisable();
            }
        });

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(issuerValueSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(nameQualifierSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);

        final RunOnChangeListener onChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                notifyListeners();
            }
        });

        issuerDefaultRadioButton.addActionListener(onChangeListener);
        issuerFromTemplateRadioButton.addActionListener(onChangeListener);
    }

    private void enableDisable() {
        //can't do this in initialize as at that point we don't know what version were configuring for
        final boolean isVersion2 = version == 2;
        formatPanel.setVisible(isVersion2);
        nameQualifierPanel.setVisible(isVersion2);

        if (version == 2) {
            final boolean includeNameFormat = includeFormatAttributeCheckBox.isSelected();

            formatsButtonPanel.setEnabled(includeNameFormat);
            entityFormatRadioButton.setEnabled(includeNameFormat);
            unspecifiedRadioButton.setEnabled(includeNameFormat);
            emailAddressRadioButton.setEnabled(includeNameFormat);
            x509SubjectRadioButton.setEnabled(includeNameFormat);
            windowsDomainRadioButton.setEnabled(includeNameFormat);
            kerberosRadioButton.setEnabled(includeNameFormat);
        }

        issuerValueSquigglyTextField.setEnabled(issuerFromTemplateRadioButton.isSelected());
    }
}
