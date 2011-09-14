package com.l7tech.console.action;

import com.l7tech.console.panels.BindOnlyLdapGeneralPanel;
import com.l7tech.console.panels.CreateIdentityProviderWizard;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Action to copy a bind-only LDAP identity provider
 */
public class CopyBindOnlyLdapProviderAction extends CopyIdentityProviderAction<BindOnlyLdapIdentityProviderConfig> {

    public CopyBindOnlyLdapProviderAction( final  IdentityProviderNode nodeIdentity ) {
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
        final BindOnlyLdapIdentityProviderConfig newLdap = new BindOnlyLdapIdentityProviderConfig( getIdentityProviderConfig((EntityHeaderNode) node) );
        newLdap.setOid( LdapIdentityProviderConfig.DEFAULT_OID );
        newLdap.setVersion( 0 );
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                edit( newLdap, true );
            }
        } );
    }

    @Override
    protected Wizard newWizard( final Frame parent,
                                final BindOnlyLdapIdentityProviderConfig config,
                                final boolean readSettings ) {
        return  new CreateIdentityProviderWizard(
                parent,
                new BindOnlyLdapGeneralPanel(null, false),
                config,
                readSettings );
    }
}
