package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPGeneralPanel extends IdentityProviderStepPanel {

    private JTextField providerNameTextField;
    private JCheckBox x509CertCheckbox;
    private JCheckBox samlCheckbox;

    private JPanel mainPanel;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPGeneralPanel.class.getName());


    public FederatedIPGeneralPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String getDescription() {
        return "Enter the identity provider name, " +
                "and select the types of credential source this identity provider will handle.";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Enter Provider Information";
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {

        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        if (iProviderConfig.getOid() != -1) {
            providerNameTextField.setText(iProviderConfig.getName());
        }

        x509CertCheckbox.setSelected(iProviderConfig.isX509Supported());
        samlCheckbox.setSelected(iProviderConfig.isSamlSupported());
    }


    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        iProviderConfig.setSamlSupported(samlCheckbox.isSelected());
        iProviderConfig.setX509Supported(x509CertCheckbox.isSelected());
        iProviderConfig.setName(providerNameTextField.getText().trim());

    }

    public boolean onNextButton() {

        if(providerNameTextField.getText().length() < 3 || providerNameTextField.getText().length() > 24) {
            JOptionPane.showMessageDialog(mainPanel, resources.getString("providerNameTextField.error.empty"),
                            resources.getString("providerNameTextField.error.title"),
                            JOptionPane.ERROR_MESSAGE);

            return false;
        }

        java.util.ArrayList skippedPanels = new java.util.ArrayList();
        if (!samlCheckbox.isSelected()) skippedPanels.add(FederatedIPSamlPanel.class.getName());

        setSkippedPanels(skippedPanels.toArray());
        return true;
    }


}
