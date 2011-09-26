package com.l7tech.console.action;

import static com.l7tech.console.action.IdentityProviderPropertiesAction.showDuplicateProviderWarning;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;

import java.awt.*;

/**
 * Superclass for actions that copy identity providers
 */
public abstract class CopyIdentityProviderAction<CT extends IdentityProviderConfig> extends NewProviderAction {

    public CopyIdentityProviderAction( final IdentityProviderNode nodeIdentity ) {
        super(nodeIdentity);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Clone Identity Provider";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Clone Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    /**
     * Edit the given config using the given panel(s) with re-run on duplicate name
     */
    protected void edit( final CT providerConfig,
                         final boolean readSettings ) {
        final Frame f = TopComponents.getInstance().getTopParent();

        final Wizard w = newWizard( f, providerConfig, readSettings );
        w.addWizardListener( makeWizardAdapter( listener, new Runnable(){
            @Override
            public void run() {
                showDuplicateProviderWarning();
                edit( providerConfig, true );
            }
        } ) );
        w.pack();

        // register itself to listen to the addEvent
        addEntityListener(listener);

        Utilities.centerOnParentWindow( w );
        DialogDisplayer.display( w );
    }

    /**
     * Overridden to create the appropriate wizard type.
     */
    protected abstract Wizard newWizard( final Frame parent, final CT config, final boolean readSettings );

    protected EntityListener listener = makeEntityListener(true);
}
