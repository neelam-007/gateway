package com.l7tech.console.tree;


import com.l7tech.identity.GroupManager;
import com.l7tech.console.action.NewUserAction;
import com.l7tech.console.action.NewGroupAction;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.*;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with groups.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class GroupFolderNode extends AbstractTreeNode {
    private final GroupManager groupManager;
    private DefaultTreeModel model;

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given entry.
     */
    public GroupFolderNode(GroupManager gm, DefaultTreeModel model) {
        super(null);
        groupManager = gm;
        this.model = model;

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
        return new Action[]{new NewGroupAction(this, model)};
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "Groups";
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
