package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.IdentityProviderDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>NewProviderAction</code> action adds the new provider.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewSamlProviderAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewUserAction.class.getName());

    public NewSamlProviderAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "New SAML Provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create new SAML provider";
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
                EntityHeader header = new EntityHeader();
                header.setType(EntityType.ID_PROVIDER_CONFIG);
                IdentityProviderDialog dialog =
                  new IdentityProviderDialog(Registry.getDefault().
                  getComponentRegistry().getMainWindow(), header);
                dialog.addEntityListener(listener);

                dialog.setResizable(false);
                dialog.show();
            }
        });
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
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                        return;
                    }
                    if (tree.hasBeenExpanded(new TreePath(node.getPath()))) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        final int childCount = node.getChildCount();
                        int index = 0;
                        // finds the last provider node
                        for (int i = 0; i < childCount; i++) {
                            TreeNode n = node.getChildAt(i);
                            if (n instanceof ProviderNode) {
                                index = i + 1;
                            }
                        }
                        final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh);
                        model.insertNodeInto(newChildNode, node, index);
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
