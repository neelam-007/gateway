package com.l7tech.console.action;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.CreateFederatedIPWizard;
import com.l7tech.console.panels.FederatedIPGeneralPanel;
import com.l7tech.console.panels.FederatedIPTrustedCertsPanel;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.IdentityProviderCertificateValidationConfigPanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.DuplicateObjectException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

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
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
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

                Frame f = TopComponents.getInstance().getTopParent();

                FederatedIPGeneralPanel configPanel = new FederatedIPGeneralPanel(new FederatedIPTrustedCertsPanel(new IdentityProviderCertificateValidationConfigPanel(null)));
                CreateFederatedIPWizard w = new CreateFederatedIPWizard(f, configPanel);
                //FederatedIdentityProviderWindow w = new FederatedIdentityProviderWindow(f);

                // register itself to listen to the addEvent
                w.addWizardListener(wizardListener);

                // register itself to listen to the addEvent
                addEntityListener(listener);

                w.pack();
                Utilities.centerOnScreen(w);
                DialogDisplayer.display(w);
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
            Wizard w = (Wizard)we.getSource();
            final IdentityProviderConfig iProvider = (IdentityProviderConfig)w.getWizardInput();

            if (iProvider != null) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        EntityHeader header = new EntityHeader();
                        header.setName(iProvider.getName());
                        header.setType(EntityType.ID_PROVIDER_CONFIG);
                        try {
                            header.setOid(getIdentityAdmin().saveIdentityProviderConfig(iProvider));
                            // Refresh permission cache so that newly created IdP is usable
                            Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        } catch (DuplicateObjectException doe) {
                            String msg = "An Identity Provider with the same name already exists.\nThe new Identity Provider has not been saved.";
                            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                          msg,
                                                          "Error Saving Identity Provider",
                                                          JOptionPane.WARNING_MESSAGE);
                            header = null;
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
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EntityHeader eh = (EntityHeader)ev.getEntity();
                    IdentityProvidersTree tree = (IdentityProvidersTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the identity tree.");
                        return;
                    }
                    if (node == null) {
                        node = tree.getRootNode();
                    }
                    if (tree.hasBeenExpanded(new TreePath(node.getPath()))) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

                        final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh);
                        model.insertNodeInto(newChildNode, node, node.getInsertPosition(newChildNode));
                        TreeNode[] nodePath = model.getPathToRoot(newChildNode);
                        if (nodePath != null) {
                            tree.setSelectionPath(new TreePath(nodePath));
                        }
                    }
                    removeEntityListener(listener);
                }
            });
        }
    };

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        node = null;
    }
}
