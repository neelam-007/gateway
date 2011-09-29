package com.l7tech.console.action;


import com.l7tech.console.panels.CreateIdentityProviderWizard;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;

/**
 * The <code>CopyLdapProviderAction</code> action copies an LDAP provider
 */
public class CopyLdapProviderAction extends CopyIdentityProviderAction<LdapIdentityProviderConfig> {

    public CopyLdapProviderAction(IdentityProviderNode nodeIdentity) {
        super(nodeIdentity);

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
        final LdapIdentityProviderConfig newLdap = new LdapIdentityProviderConfig( getIdentityProviderConfig((EntityHeaderNode) node) );
        EntityUtils.updateCopy( newLdap );
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                edit( newLdap, true );
            }
        } );
    }

    @Override
    protected Wizard newWizard( final Frame parent,
                                final LdapIdentityProviderConfig config,
                                final boolean readSettings ) {
        return  new CreateIdentityProviderWizard(
                parent,
                NewLdapProviderAction.buildPanels( false ),
                config,
                readSettings );
    }
}
