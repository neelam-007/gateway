package com.l7tech.console.tree;


import com.l7tech.console.action.NewGroupAction;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
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
    private final GroupManager groupManager;
    private long providerId;
    private String name;

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given provider.
     */
    public GroupFolderNode(GroupManager gm, long providerId) {
        this(gm, providerId, INTERNAL_GROUPS_NAME);
    }

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given provider.
     */
    public GroupFolderNode(GroupManager gm, long providerId, String name) {
        super(null);
        groupManager = gm;
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
     * subclasses override this method
     */
    protected void loadChildren() {
        Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(
            new EntitiesEnumeration(new GroupEntitiesCollection(groupManager)));
        int index = 0;

        children = null;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode)e.nextElement(), index++);
        }
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        final NewGroupAction newGroupAction = new NewGroupAction(this);
        newGroupAction.setEnabled(providerId == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        return new Action[]{newGroupAction};
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

}
