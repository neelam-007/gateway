package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.PauseListenerAdapter;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

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

        boolean fieldsAreValid = true;
        if (version == 2) {
            fieldsAreValid = SquigglyFieldUtils.validateSquigglyFieldForVariableReference(nameQualifierSquigglyTextField);
        }

        if (!issuerValueSquigglyTextField.getText().trim().equals(autoString)) {
            fieldsAreValid = fieldsAreValid && SquigglyFieldUtils.validateSquigglyFieldForVariableReference(issuerValueSquigglyTextField);
        }

        return fieldsAreValid;
    }

    @Override
    public String getStepLabel() {
        return "Customize Issuer";
    }

    @Override
    public String getDescription() {
        final String defaultMsg = "Default value is the subject distinguished name of the configured private key's corresponding public key.";
        switch (version) {
            case 2:
                return "<html>Customize the Issuer element.<br>" +
                        "Note: If enabled a Name Format attribute will always be added to the Issuer element.<br><br>" +
                        defaultMsg + "</html>";
            default:
                return "<html>Customize the Issuer attribute.<br><br>" +
                        defaultMsg + "</html>";
        }
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        final SamlPolicyAssertion samlPolicyAssertion = (SamlPolicyAssertion) settings;
        version = samlPolicyAssertion.getVersion() == null ? 1 : samlPolicyAssertion.getVersion();

        final SamlIssuerConfiguration issuerConfig = (SamlIssuerConfiguration) samlPolicyAssertion;

        final String customNameFormatUri = issuerConfig.getCustomizedIssuerNameFormat();
        final String customIssuerValueTest = issuerConfig.getCustomizedIssuerValue();
        final String customIssuerValue = (customIssuerValueTest != null)? customIssuerValueTest : autoString;
        final boolean hasCustomIssuer = (customNameFormatUri != null && version == 2) || customIssuerValueTest != null;
        customizeIssuerCheckBox.setSelected(hasCustomIssuer);

        issuerValueSquigglyTextField.setText(customIssuerValue);

        if (version == 2) {
            if (SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(customNameFormatUri)) {
                unspecifiedRadioButton.setSelected(true);
            } else if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(customNameFormatUri)) {
                emailAddressRadioButton.setSelected(true);
            } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(customNameFormatUri)) {
                x509SubjectRadioButton.setSelected(true);
            } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(customNameFormatUri)) {
                windowsDomainRadioButton.setSelected(true);
            } else if (SamlConstants.NAMEIDENTIFIER_KERBEROS.equals(customNameFormatUri)) {
                kerberosRadioButton.setSelected(true);
            } else {
                entityFormatRadioButton.setSelected(true);
            }

            final String customNameQualifier = issuerConfig.getCustomizedIssuerNameQualifier();
            if (customNameQualifier != null) {
                nameQualifierSquigglyTextField.setText(customNameQualifier);
            }
        }

        enableDisable();
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlIssuerConfiguration issuerConfig = (SamlIssuerConfiguration) settings;
        final boolean customize = customizeIssuerCheckBox.isSelected();
        if (customize) {

            final String issuerCustomValue = issuerValueSquigglyTextField.getText().trim();
            issuerConfig.setCustomizedIssuerValue((issuerCustomValue.isEmpty() || issuerCustomValue.equals(autoString)) ? null : issuerCustomValue);

            final String nameQualifier = nameQualifierSquigglyTextField.getText().trim();
            issuerConfig.setCustomizedIssuerNameQualifier((nameQualifier.isEmpty() || version != 2) ? null : nameQualifier);

            final String nameFormatUri;
            if (version != 2) {
                nameFormatUri = null;
            } else if (unspecifiedRadioButton.isSelected()) {
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

            issuerConfig.setCustomizedIssuerNameFormat(nameFormatUri);


        } else {
            issuerConfig.setCustomizedIssuerValue(null);
            issuerConfig.setCustomizedIssuerNameFormat(null);
            issuerConfig.setCustomizedIssuerNameQualifier(null);
        }
    }

    // - PRIVATE

    private JCheckBox customizeIssuerCheckBox;
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
    private JPanel customizePanel;
    private JLabel nameQualifierLabel;
    private JLabel titleLabel;
    private JLabel issuerValueLabel;
    private int version;
    private static final String autoString = "<auto>";

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        customizeIssuerCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisable();
            }
        });

        final FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                final Object source = e.getSource();
                if (!(source instanceof JTextField)) return;
                final JTextField sourceField = (JTextField) source;

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(sourceField.getText().trim().isEmpty()){
                            sourceField.setText(autoString);
                        }
                    }
                });
            }

            @Override
            public void focusGained(FocusEvent e) {
                final Object source = e.getSource();
                if (!(source instanceof JTextField)) return;
                final JTextField sourceField = (JTextField) source;

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(sourceField.getText().equals(autoString)){
                            sourceField.setText("");
                        }
                    }
                });
            }
        };

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(issuerValueSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300, focusAdapter);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(nameQualifierSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);
    }

    private void enableDisable() {
        boolean enablePanel = customizeIssuerCheckBox.isSelected();
        customizePanel.setEnabled(enablePanel);

        if (version == 2) {
            formatsButtonPanel.setVisible(true);
            nameQualifierSquigglyTextField.setVisible(true);
            nameQualifierLabel.setVisible(true);

            formatsButtonPanel.setEnabled(enablePanel);
            nameQualifierSquigglyTextField.setEnabled(enablePanel);
            nameQualifierLabel.setEnabled(enablePanel);

            entityFormatRadioButton.setEnabled(enablePanel);
            unspecifiedRadioButton.setEnabled(enablePanel);
            emailAddressRadioButton.setEnabled(enablePanel);
            x509SubjectRadioButton.setEnabled(enablePanel);
            windowsDomainRadioButton.setEnabled(enablePanel);
            kerberosRadioButton.setEnabled(enablePanel);
        } else {
            formatsButtonPanel.setVisible(false);
            nameQualifierSquigglyTextField.setVisible(false);
            nameQualifierLabel.setVisible(false);
        }

        issuerValueLabel.setEnabled(enablePanel);
        issuerValueSquigglyTextField.setEnabled(enablePanel);
    }
}
