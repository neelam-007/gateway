package com.l7tech.console.action;


import static com.l7tech.console.action.IdentityProviderPropertiesAction.showDuplicateProviderWarning;
import com.l7tech.console.event.EntityListener;
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
 * @author Emil Marceta
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
        final boolean readSettings;
        final LdapIdentityProviderConfig ldapConfig;
        if ( this.ldapConfig == null) {
            ldapConfig = LdapIdentityProviderConfig.newLdapIdentityProviderConfig();
            readSettings = false;
        } else {
            ldapConfig = this.ldapConfig;
            ldapConfig.setTypeVal(IdentityProviderType.LDAP.toVal());
            readSettings = true;
        }

        final Runnable duplicateCallback = new Runnable() {
            @Override
            public void run() {
                showDuplicateProviderWarning();
                edit( ldapConfig, this, true );
            }
        };

        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                edit( ldapConfig, duplicateCallback, readSettings );
            }
        } );
    }

    static LdapIdentityProviderConfigPanel buildPanels( final boolean typeSelectable ) {
        return new LdapIdentityProviderConfigPanel(
                new LdapGroupMappingPanel(
                        new LdapUserMappingPanel(
                                new LdapAdvancedConfigurationPanel(
                                        new LdapCertificateSettingsPanel(null)))), typeSelectable);
    }

    private void edit( final LdapIdentityProviderConfig config,
                       final Runnable duplicateCallback,
                       final boolean readSettings ) {
        final Frame f = TopComponents.getInstance().getTopParent();
        final Wizard w = new CreateIdentityProviderWizard(f, buildPanels(true), config, readSettings);
        w.addWizardListener( makeWizardAdapter(listener, duplicateCallback) );
        w.pack();

        // register itself to listen to the addEvent
        addEntityListener( listener );

        Utilities.centerOnParentWindow(w);
        DialogDisplayer.display(w);
    }

    private EntityListener listener = makeEntityListener();
}
