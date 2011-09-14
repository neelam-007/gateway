package com.l7tech.console.action;

import static com.l7tech.console.action.IdentityProviderPropertiesAction.showDuplicateProviderWarning;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Action to create a new simple bind-only LDAP identity provider.
 */
public class NewBindOnlyLdapProviderAction extends NewProviderAction {
    private final BindOnlyLdapIdentityProviderConfig providerConfig;

    public NewBindOnlyLdapProviderAction() {
        this( (AbstractTreeNode) null );
    }

    public NewBindOnlyLdapProviderAction(AbstractTreeNode node) {
        super(node);
        this.providerConfig = null;
    }

    public NewBindOnlyLdapProviderAction(BindOnlyLdapIdentityProviderConfig providerConfig) {
        super(null);
        this.providerConfig = providerConfig;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create Simple LDAP Identity Provider";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Create a new Simple LDAP (Bind-Only) Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    @Override
    protected void performAction() {
        final boolean readSettings;
        final BindOnlyLdapIdentityProviderConfig providerConfig;
        if ( this.providerConfig == null) {
            providerConfig = new BindOnlyLdapIdentityProviderConfig();
            readSettings = false;
        } else {
            providerConfig = this.providerConfig;
            providerConfig.setTypeVal(IdentityProviderType.LDAP.toVal());
            readSettings = true;
        }

        final Runnable duplicateCallback = new Runnable() {
            @Override
            public void run() {
                showDuplicateProviderWarning();
                edit( providerConfig, this, true );
            }
        };

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                edit( providerConfig, duplicateCallback, readSettings );
            }
        });
    }

    private void edit( final BindOnlyLdapIdentityProviderConfig providerConfig,
                       final Runnable duplicateCallback,
                       final boolean readSettings ) {
        final Frame f = TopComponents.getInstance().getTopParent();

        final BindOnlyLdapGeneralPanel configPanel = new BindOnlyLdapGeneralPanel(null, false);
        final Wizard w = new CreateIdentityProviderWizard(f, configPanel, providerConfig, readSettings);
        w.addWizardListener( makeWizardAdapter( listener, duplicateCallback ) );
        w.pack();

        // register itself to listen to the addEvent
        addEntityListener(listener);

        Utilities.centerOnParentWindow(w);
        DialogDisplayer.display(w);
    }

    private EntityListener listener = makeEntityListener();
}
