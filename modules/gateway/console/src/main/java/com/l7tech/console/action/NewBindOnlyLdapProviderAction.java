package com.l7tech.console.action;

import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Action to create a new simple bind-only LDAP identity provider.
 */
public class NewBindOnlyLdapProviderAction extends NewProviderAction {
    private static final Logger logger = Logger.getLogger(NewBindOnlyLdapProviderAction.class.getName());

    private BindOnlyLdapIdentityProviderConfig providerConfig;

    public NewBindOnlyLdapProviderAction() {
        super(null);
    }

    public NewBindOnlyLdapProviderAction(AbstractTreeNode node) {
        super(node);
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                BindOnlyLdapGeneralPanel configPanel = new BindOnlyLdapGeneralPanel(null, false);

                Frame f = TopComponents.getInstance().getTopParent();

                boolean readSettings = true;
                if (providerConfig == null) {
                    providerConfig = new BindOnlyLdapIdentityProviderConfig();
                    readSettings = false;
                } else {
                    providerConfig.setTypeVal(IdentityProviderType.LDAP.toVal());
                }

                Wizard w = new CreateIdentityProviderWizard(f, configPanel, providerConfig, readSettings);
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
