package com.l7tech.console.tree;


import com.l7tech.console.action.NewGroupAction;
import com.l7tech.console.action.RefreshAction;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with groups.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class GroupFolderNode extends AbstractTreeNode {
    public final static String INTERNAL_GROUPS_NAME = "Internal Groups";
    private IdentityProvider identityProvider;
    private String name;

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given provider.
     */
    public GroupFolderNode(IdentityProvider ip) {
        this(ip, INTERNAL_GROUPS_NAME);
    }

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given provider.
     */
    public GroupFolderNode(IdentityProvider ip, String name) {
        super(null);
        identityProvider = ip;
        this.name = name;
    }


    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        final IdentityEntitiesCollection collection =
                new IdentityEntitiesCollection(identityProvider, new EntityType[] {EntityType.GROUP});

        Enumeration e =
          TreeNodeFactory.getTreeNodeEnumeration(new EntitiesEnumeration(collection));

        int index = 0;
        children = null;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode)e.nextElement(), index++);
        }
    }

    /**
     * @return true as this node children can be refreshed
     */
    public boolean canRefresh() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        final NewGroupAction newGroupAction = new NewGroupAction(this);
        final long oid = identityProvider.getConfig().getOid();
        newGroupAction.setEnabled(oid == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        RefreshAction ra = new GroupFolderRefreshAction(this);

        return new Action[]{newGroupAction, ra};
    }

    /**
     * Returns the provider id for the groups.
     *
     * @return the provider id
     */
    public long getProviderId() {
        return identityProvider.getConfig().getOid();
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }


    /**
     * the refresh groups action class
     */
    class GroupFolderRefreshAction extends RefreshAction {
        public GroupFolderRefreshAction(GroupFolderNode node) {
            super(node);

        }

        public void performAction() {
            if (tree == null) {
                logger.warning("No tree assigned, ignoring the refresh action");
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        SwingUtilities.getWindowAncestor(tree);
                        tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        TreePath treePath = new TreePath(GroupFolderNode.this.getPath());
                        if (tree.isExpanded(treePath)) {
                            GroupFolderNode.this.hasLoadedChildren = false;
                            model.reload(GroupFolderNode.this);
                        }
                    } finally {
                        tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            };
            SwingUtilities.invokeLater(runnable);
        }
    }


}
