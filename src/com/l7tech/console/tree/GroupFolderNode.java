package com.l7tech.console.tree;


import com.l7tech.identity.GroupManager;

import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with groups.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class GroupFolderNode implements BasicTreeNode {
    private final GroupManager groupManager;

    /**
     * construct the <CODE>GroupFolderNode</CODE> instance for
     * a given entry.
     */
    public GroupFolderNode(GroupManager gm) {
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
     * Returns the children of the reciever as an Enumeration.
     *
     * @return the Enumeration of the child nodes.
     * @exception Exception thrown when an erro is encountered when
     *                      retrieving child nodes.
     */
    public Enumeration children() throws Exception {
         Enumeration e = new EntitiesEnumeration(new GroupEntitiesCollection(groupManager));
        return TreeNodeFactory.getTreeNodeEnumeration(e);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "Groups";
    }

}
