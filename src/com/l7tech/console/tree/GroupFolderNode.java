package com.l7tech.console.tree;


import com.l7tech.console.action.NewGroupAction;
import com.l7tech.identity.GroupManager;

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
    public final static String NAME = "Groups";
    private final GroupManager groupManager;
    private long providerId;

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given entry.
     */
    public GroupFolderNode(GroupManager gm, long providerId) {
        super(null);
        groupManager = gm;
        this.providerId = providerId;
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
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode) e.nextElement(), index++);
        }
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{new NewGroupAction(this)};
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
        return NAME;
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
