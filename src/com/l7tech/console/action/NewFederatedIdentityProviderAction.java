package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */

public class NewFederatedIdentityProviderAction extends NewProviderAction {
    static final Logger log = Logger.getLogger(NewFederatedIdentityProviderAction.class.getName());


    public NewFederatedIdentityProviderAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Federated Identity Provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Federated Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/providers16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                JFrame f = TopComponents.getInstance().getMainWindow();

                FederatedIPGeneralPanel configPanel = new FederatedIPGeneralPanel(new FederatedIPX509CertPanel(new FederatedIPSamlPanel(new FederatedIPTrustedCertsPanel(null))));
                CreateFederatedIPWizard w = new CreateFederatedIPWizard(f, configPanel);
                //FederatedIdentityProviderWindow w = new FederatedIdentityProviderWindow(f);

                // register itself to listen to the addEvent
                w.addWizardListener(wizardListener);

                // register itself to listen to the addEvent
                addEntityListener(listener);

                w.setSize(850, 500);
                Utilities.centerOnScreen(w);
                w.setVisible(true);

            }
        });

    }

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard) we.getSource();
            final IdentityProviderConfig iProvider = (IdentityProviderConfig) w.getCollectedInformation();

            if (iProvider != null) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        EntityHeader header = new EntityHeader();
                        header.setName(iProvider.getName());
                        header.setType(EntityType.ID_PROVIDER_CONFIG);
                        try {
                            header.setOid(getIdentityAdmin().saveIdentityProviderConfig(iProvider));
                        } catch (Exception e) {
                            ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving the new identity provider: " + header.getName());
                            header = null;
                        }
                        if (header != null) fireEventProviderAdded(header);
                    }
                });
            }
        }

    };

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            if (node == null) {
                log.fine("Parent node has not been set - skipping notificaiton.");
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EntityHeader eh = (EntityHeader) ev.getEntity();
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the identity tree.");
                        return;
                    }
                    if (tree.hasBeenExpanded(new TreePath(node.getPath()))) {
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                        final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh);
                        model.insertNodeInto(newChildNode, node, node.getInsertPosition(newChildNode));
                        TreeNode[] nodePath = model.getPathToRoot(newChildNode);
                        if (nodePath != null) {
                            tree.setSelectionPath(new TreePath(nodePath));
                        }
                    }
                }
            });
        }
    };
}
