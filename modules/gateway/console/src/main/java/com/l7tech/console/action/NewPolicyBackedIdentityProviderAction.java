package com.l7tech.console.action;

import com.l7tech.console.event.EntityListener;
import com.l7tech.console.panels.CreateIdentityProviderWizard;
import com.l7tech.console.panels.PolicyBackedIdentityGeneralPanel;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;

import static com.l7tech.console.action.IdentityProviderPropertiesAction.showDuplicateProviderWarning;

/**
 *
 */
public class NewPolicyBackedIdentityProviderAction extends NewProviderAction {
    private final PolicyBackedIdentityProviderConfig providerConfig;

    public NewPolicyBackedIdentityProviderAction() {
        this( (AbstractTreeNode) null );
    }

    public NewPolicyBackedIdentityProviderAction(AbstractTreeNode node) {
        super(node);
        this.providerConfig = null;
    }

    public NewPolicyBackedIdentityProviderAction(PolicyBackedIdentityProviderConfig providerConfig) {
        super(null);
        this.providerConfig = providerConfig;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create Policy-Backed Identity Provider";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Create a new Policy-Backed Identity Provider";
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
        final PolicyBackedIdentityProviderConfig providerConfig;
        if ( this.providerConfig == null) {
            providerConfig = new PolicyBackedIdentityProviderConfig();
            readSettings = false;
        } else {
            providerConfig = this.providerConfig;
            providerConfig.setTypeVal(IdentityProviderType.POLICY_BACKED.toVal());
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
                edit(providerConfig, duplicateCallback, readSettings);
            }
        });
    }

    private void edit( final PolicyBackedIdentityProviderConfig providerConfig,
                       final Runnable duplicateCallback,
                       final boolean readSettings ) {
        final Frame f = TopComponents.getInstance().getTopParent();

        final PolicyBackedIdentityGeneralPanel configPanel = new PolicyBackedIdentityGeneralPanel(null, false);
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
