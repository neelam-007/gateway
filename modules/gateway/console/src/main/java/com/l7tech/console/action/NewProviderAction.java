package com.l7tech.console.action;

import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
abstract public class NewProviderAction extends NodeAction {
    private static final Logger log = Logger.getLogger(NewProviderAction.class.getName());

    protected EventListenerList listenerList = new EventListenerList();

    public NewProviderAction(AbstractTreeNode node) {
        super(node, LIC_AUTH_ASSERTIONS, new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG));
    }

    protected WizardAdapter makeWizardAdapter(final EntityListener listener) {
        return new WizardAdapter() {
            @Override
            public void wizardCanceled(WizardEvent e) {
                removeEntityListener(listener);
            }

            /**
             * Invoked when the wizard has finished.
             *
             * @param we the event describing the wizard finish
             */
            @Override
            public void wizardFinished(WizardEvent we) {

                // update the provider
                Wizard w = (Wizard)we.getSource();
                final IdentityProviderConfig iProvider = (IdentityProviderConfig)w.getWizardInput();

                if (iProvider != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
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
                            if (header == null) {
                                removeEntityListener(listener);
                            } else {
                                fireEventProviderAdded(header);
                            }
                        }
                    });
                }
            }

        };
    }

    protected EntityListenerAdapter makeEntityListener() {
        final EntityListenerAdapter listener[] = { null };
        listener[0] = new EntityListenerAdapter() {
            /**
             * Fired when an new entity is added.
             *
             * @param ev event describing the action
             */
            @Override
            public void entityAdded(final EntityEvent ev) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EntityHeader eh = (EntityHeader) ev.getEntity();
                        IdentityProvidersTree tree = (IdentityProvidersTree) TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                        if (tree == null) {
                            log.log(Level.WARNING, "Unable to reach the identity tree.");
                            return;
                        }
                        AbstractTreeNode targetNode = node != null ? node : tree.getRootNode();
                        if (tree.hasBeenExpanded(new TreePath(targetNode.getPath()))) {
                            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                            final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh, null);
                            model.insertNodeInto(newChildNode, targetNode, targetNode.getInsertPosition(newChildNode));
                            TreeNode[] nodePath = model.getPathToRoot(newChildNode);
                            if (nodePath != null) {
                                tree.setSelectionPath(new TreePath(nodePath));
                            }
                        }
                        removeEntityListener(listener[0]);
                    }
                });
            }
        };
        return listener[0];
    }

    /**
     * notfy the listeners that the entity has been added
     *
     * @param header
     */
    protected void fireEventProviderAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityAdded(event);
        }
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    protected void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    protected static IdentityAdmin getIdentityAdmin()
            throws RuntimeException {
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        if (admin == null) {
            throw new RuntimeException("Could not find registered " + IdentityAdmin.class);
        }

        return admin;
    }
}
