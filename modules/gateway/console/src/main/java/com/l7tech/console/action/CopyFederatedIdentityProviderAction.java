package com.l7tech.console.action;

import com.l7tech.console.panels.*;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class CopyFederatedIdentityProviderAction extends CopyIdentityProviderAction<FederatedIdentityProviderConfig> {

    public CopyFederatedIdentityProviderAction( final IdentityProviderNode node ) {
        super(node);
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
        final FederatedIdentityProviderConfig fipConfig =
                new FederatedIdentityProviderConfig(getIdentityProviderConfig((EntityHeaderNode) node));
        fipConfig.setOid(FederatedIdentityProviderConfig.DEFAULT_OID);
        fipConfig.setVersion( 0 );

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                edit( fipConfig, true );
            }
        });
    }

    @Override
    protected Wizard newWizard( final Frame parent,
                                final FederatedIdentityProviderConfig config,
                                final boolean readSettings ) {
        final FederatedIPGeneralPanel configPanel = new FederatedIPGeneralPanel(new FederatedIPTrustedCertsPanel(new IdentityProviderCertificateValidationConfigPanel(null)));
        return new CreateFederatedIPWizard( parent, configPanel, config );
    }
}
