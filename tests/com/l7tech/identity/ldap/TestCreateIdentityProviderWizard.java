package com.l7tech.identity.ldap;

import com.l7tech.console.panels.*;
import com.l7tech.common.gui.util.Utilities;

/**
 * Tests the wizard that creates a new ldap id prov config.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Oct 4, 2004<br/>
 */
public class TestCreateIdentityProviderWizard {
    public static void main(String[] args) throws Exception {
        // enable the registrystub
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        //
        LdapIdentityProviderConfigPanel configPanel = (
                  new LdapIdentityProviderConfigPanel(new LdapGroupMappingPanel(new LdapUserMappingPanel(null)), true));
        Wizard w = new CreateIdentityProviderWizard(null, configPanel);
        w.pack();
        w.setSize(780, 560);
        Utilities.centerOnScreen(w);
        w.setVisible(true);
    }
}
