package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.event.*;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.event.EventListenerList;
import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */

public class NewPKIProviderAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewPKIProviderAction.class.getName());
    private EventListenerList listenerList = new EventListenerList();

    public NewPKIProviderAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create PKI Identity Provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new PKI Identity Provider";
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
    public void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                JFrame f = TopComponents.getInstance().getMainWindow();
                PKIIdentityProviderWindow w = new PKIIdentityProviderWindow(f);

                // register itself to listen to the addEvent
                w.addEntityListener(listener);

                w.setSize(550, 400);
                Utilities.centerOnScreen(w);
                w.setVisible(true);

            }
        });

    }

    private IdentityProviderConfigManager getProviderConfigManager()
            throws RuntimeException {
        IdentityProviderConfigManager ipc =
                (IdentityProviderConfigManager) Locator.
                getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

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
                    EntityHeader eh = (EntityHeader)ev.getEntity();
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the identity tree.");
                        return;
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
                }
            });
        }
    };
}
