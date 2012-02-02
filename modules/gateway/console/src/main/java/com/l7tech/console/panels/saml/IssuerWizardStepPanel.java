package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.SamlIssuerConfig;
import com.l7tech.policy.assertion.SamlElementGenericConfig;
import com.l7tech.util.Functions;

import java.awt.*;

/**
 * Step to customize the Issuer value.
 */
public class IssuerWizardStepPanel extends WizardStepPanel {

    public IssuerWizardStepPanel(WizardStepPanel next, boolean showIssuerCheckBox) {
        super(next);
        initialize(showIssuerCheckBox);
    }

    @Override
    public boolean canAdvance() {
        try {
            samlIssuerPanel.validateData();
        } catch (AssertionPropertiesOkCancelSupport.ValidationException e) {
            return false;
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
        final SamlIssuerConfig issuerElmConfig = (SamlIssuerConfig) settings;
        samlIssuerPanel.setData(issuerElmConfig);

        version = issuerElmConfig.getVersion() == null ? 1 : issuerElmConfig.getVersion();
        // Version 1.1 does not have an Issuer.
        this.setSkipped(issuerElmConfig.samlProtocolUsage() && version != 2);
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlElementGenericConfig issuerElmConfig = (SamlElementGenericConfig) settings;
        samlIssuerPanel.getData(issuerElmConfig);
    }

    // - PRIVATE

    private SamlIssuerPanel samlIssuerPanel;
    private int version;

    private void initialize(final boolean showIssuerCheckBox) {
        samlIssuerPanel = new SamlIssuerPanel(true, showIssuerCheckBox);
        samlIssuerPanel.setConfigListener(new Functions.Nullary<Void>() {
            @Override
            public Void call() {
                notifyListeners();
                return null;
            }
        });

        setLayout(new BorderLayout());
        add(samlIssuerPanel, BorderLayout.CENTER);
    }
}
