package com.l7tech.console.tree;


import com.l7tech.identity.GroupManager;

import javax.swing.tree.MutableTreeNode;
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

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given entry.
     */
    public GroupFolderNode(GroupManager gm) {
        super(null);
        groupManager = gm;

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
           insert((MutableTreeNode)e.nextElement(), index++);
       }
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
