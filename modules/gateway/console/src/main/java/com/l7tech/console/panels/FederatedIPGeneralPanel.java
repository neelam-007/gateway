package com.l7tech.console.panels;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;

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
    private SecurityZoneWidget zoneControl;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPGeneralPanel.class.getName());


    public FederatedIPGeneralPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    public FederatedIPGeneralPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        applyFormSecurity();
    }

    public void applyFormSecurity() {
        boolean isEnabled = !isReadOnly();
        providerNameTextField.setEnabled(isEnabled);
        x509CertCheckbox.setEnabled(isEnabled);
        samlCheckbox.setEnabled(isEnabled);
    }

    public String getDescription() {
        return "Enter the Identity Provider name, then select one or more credential " +
               "source types that the Identity Provider will handle.";
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
        this.readSettings(settings, false);
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @param setNameField  TRUE to overwrite the provider name field from the 'settings' object no mattter what is in
     *                      the 'settings' parameter object.  FALSE, will set provider name only if ObjectID exists in 'settings'
     *                      parameter object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings, boolean setNameField) throws IllegalArgumentException {

        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        if (setNameField || iProviderConfig.getOid() != -1) {
            providerNameTextField.setText(iProviderConfig.getName());
        }

        x509CertCheckbox.setSelected(iProviderConfig.isX509Supported());
        samlCheckbox.setSelected(iProviderConfig.isSamlSupported());

        // select name field for clone
        if( iProviderConfig.getOid() == LdapIdentityProviderConfig.DEFAULT_OID)
        {
            providerNameTextField.requestFocus();
            providerNameTextField.selectAll();
            zoneControl.configure(OperationType.CREATE, iProviderConfig);
        } else {
            zoneControl.configure(isReadOnly() ? OperationType.READ : OperationType.UPDATE, iProviderConfig);
        }
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
        iProviderConfig.setSecurityZone(zoneControl.getSelectedZone());
    }

    public boolean onNextButton() {

        if(providerNameTextField.getText().length() < 1 || providerNameTextField.getText().length() > 128) {
            JOptionPane.showMessageDialog(mainPanel, resources.getString("providerNameTextField.length.error"),
                            resources.getString("providerNameTextField.error.title"),
                            JOptionPane.ERROR_MESSAGE);

            return false;
        }
        return true;
    }


}
