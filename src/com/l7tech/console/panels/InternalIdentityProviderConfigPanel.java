package com.l7tech.console.panels;

import javax.swing.*;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class InternalIdentityProviderConfigPanel extends WizardStepPanel{

        public InternalIdentityProviderConfigPanel(WizardStepPanel next, boolean showProviderType) {
        super(next);
        this.showProviderType = showProviderType;
        //initResources();
        //initComponents();
        //providerSettingsPanel = getLdapPanel(null);
       // add(providerSettingsPanel);
    }

    /** populate the form from the provider beans */
    public void readSettings(Object settings) throws IllegalArgumentException {
        {
            // kludge, we add the internal provider, as itmay show only in
            // edit dsabled mode
            providerTypesCombo.addItem("Internal Provider");

            for (int i = providerTypesCombo.getModel().getSize() - 1; i >= 0; i--) {
                Object toto = providerTypesCombo.getModel().getElementAt(i);
                if (toto instanceof String) {
                    if (toto.equals("Internal Provider")) {
                        providerTypesCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        providerTypesCombo.setEnabled(false);
    }

    private boolean showProviderType;
    private JComboBox providerTypesCombo = null;
}
