package com.l7tech.console.tree;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.console.action.NewUserAction;
import com.l7tech.console.action.RefreshAction;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.*;
import java.util.Enumeration;
import java.awt.*;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class UserFolderNode extends AbstractTreeNode {
    public static String INTERNAL_USERS_NAME = "Internal Users";

    private UserManager userManager;
    private long providerId;
    private String name;

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the user manager
     */
    public UserFolderNode(UserManager um, long providerId) {
        this(um, providerId, INTERNAL_USERS_NAME);
    }

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the user manager
     * @param providerId the provider id
     * @param name the folder name
     */
    public UserFolderNode(UserManager um, long providerId, String name) {
        super(null);
        userManager = um;
        this.providerId = providerId;
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
        final NewUserAction newUserAction = new NewUserAction(this);
        newUserAction.setEnabled(providerId == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        RefreshAction ra = new UserFolderRefreshAction(this);

        return new Action[]{newUserAction, ra};
    }

    /**
     * Returns the provider id for the users.
     *
     * @return the provider id
     */
    public long getProviderId() {
        return providerId;
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
     * subclasses override this method
     */
    protected void loadChildren() {
        Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(
            new EntitiesEnumeration(new UserEntitiesCollection(userManager)));
        int index = 0;
        children = null;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode)e.nextElement(), index++);
        }
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
     * the refresh users action class
     */
    class UserFolderRefreshAction extends RefreshAction {
        public UserFolderRefreshAction(UserFolderNode node) {
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
                        tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        TreePath treePath = new TreePath(UserFolderNode.this.getPath());
                        if (tree.isExpanded(treePath)) {
                            UserFolderNode.this.hasLoadedChildren = false;
                            model.reload(UserFolderNode.this);
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
