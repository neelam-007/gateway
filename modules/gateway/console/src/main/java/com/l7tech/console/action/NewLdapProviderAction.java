package com.l7tech.console.action;


import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;


/**
 * The <code>NewLdapProviderAction</code> action adds the new provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewLdapProviderAction extends NewProviderAction {
    static final Logger log = Logger.getLogger(NewLdapProviderAction.class.getName());

    private LdapIdentityProviderConfig ldapConfig;

    public NewLdapProviderAction() {
        super(null);
    }
    
    public NewLdapProviderAction(AbstractTreeNode node) {
        super(node);
    }

    public NewLdapProviderAction(LdapIdentityProviderConfig ldapConfig) {
        super(null);
        this.ldapConfig = ldapConfig;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create LDAP Identity Provider";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Create a new LDAP Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                LdapIdentityProviderConfigPanel configPanel = (
                  new LdapIdentityProviderConfigPanel(new LdapGroupMappingPanel(new LdapUserMappingPanel(new LdapAdvancedConfigurationPanel(new LdapCertificateSettingsPanel(null)))), true));


                Frame f = TopComponents.getInstance().getTopParent();

                boolean readSettings = true;
                if (ldapConfig == null) {
                    ldapConfig = LdapIdentityProviderConfig.newLdapIdentityProviderConfig();
                    readSettings = false;
                } else {
                    ldapConfig.setTypeVal(IdentityProviderType.LDAP.toVal());
                }

                Wizard w = new CreateIdentityProviderWizard(f, configPanel, ldapConfig, readSettings);
                w.addWizardListener(wizardListener);

                // register itself to listen to the addEvent
                addEntityListener(listener);

                w.pack();
                Utilities.centerOnScreen(w);
                DialogDisplayer.display(w);
            }
        });

    }

    private EntityListener listener = makeEntityListener();
    private WizardListener wizardListener = makeWizardAdapter(listener);
}
