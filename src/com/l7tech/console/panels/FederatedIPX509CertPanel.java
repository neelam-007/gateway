package com.l7tech.console.panels;

import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.logging.Logger;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPX509CertPanel extends IdentityProviderStepPanel {

    private JPanel mainPanel;
    private JCheckBox sslCertCheckbox;
    private JCheckBox wssBSTCheckbox;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPX509CertPanel.class.getName());

    public FederatedIPX509CertPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String getDescription() {
        return "Select the usage of the X.509 certificate credential source.";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Configure X.509 Credential Source";
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

        wssBSTCheckbox.setSelected(iProviderConfig.getX509Config().isWssBinarySecurityToken());
        sslCertCheckbox.setSelected(iProviderConfig.getX509Config().isSslClientCert());

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

        iProviderConfig.getX509Config().setSslClientCert(sslCertCheckbox.isSelected());
        iProviderConfig.getX509Config().setWssBinarySecurityToken(wssBSTCheckbox.isSelected());
    }


}