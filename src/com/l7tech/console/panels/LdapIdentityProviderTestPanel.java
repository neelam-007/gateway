package com.l7tech.console.panels;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;

import javax.swing.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Locale;
import java.util.ResourceBundle;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LdapIdentityProviderTestPanel extends WizardStepPanel {

    /** Create a test panel */
    public LdapIdentityProviderTestPanel(WizardStepPanel next) {
        super(next);

        initResources();
        initComponents();
    }


    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();

        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    private void initComponents() {

    }

/*
    private void getNewSettings(IdentityProviderConfig config) {

        if (config == null || !(config instanceof LdapIdentityProviderConfig)) {
            throw new RuntimeException("unhandled provider config type");
        }
        IdentityProviderType type = config.type();
        config.setTypeVal(type.toVal());
        LdapIdentityProviderConfig convertedcfg = (LdapIdentityProviderConfig) config;
        convertedcfg.setBindDN(ldapBindDNTextField.getText());
        convertedcfg.setBindPasswd(ldapBindPassTextField.getText());
        convertedcfg.setLdapUrl(ldapHostTextField.getText());
        convertedcfg.setSearchBase(ldapSearchBaseTextField.getText());
    }

    private void testSettings() {
        Object type = providerTypesCombo.getSelectedItem();
        IdentityProviderConfig tmp = null;
        if (type instanceof LdapIdentityProviderConfig) {
            try {
                tmp = new LdapIdentityProviderConfig((LdapIdentityProviderConfig)type);
            } catch (IOException e) {
                log.log(Level.SEVERE, "cannot instantiate new provider config based on template", e);
                return;
            }
        } else {
            log.severe("unhandled provider type");
            return;
        }

        tmp.setName(providerNameTextField.getText());
        getNewSettings(tmp);
        String errorMsg = null;
        try {
            getProviderConfigManager().test(tmp);
        } catch (InvalidIdProviderCfgException e) {
            errorMsg = e.getMessage();
        } catch (RuntimeException e) {
            errorMsg = resources.getString("test.error.runtime") + "\n" + e.getMessage();
        }
        if (errorMsg == null) {
            JOptionPane.showMessageDialog(this, resources.getString("test.res.ok"),
                    resources.getString("test.res.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, errorMsg,
                    resources.getString("test.res.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
*/

    private ResourceBundle resources = null;
    private String CMD_TEST = "cmd.test";
}
