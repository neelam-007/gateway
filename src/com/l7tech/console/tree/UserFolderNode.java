package com.l7tech.console.tree;

import com.l7tech.identity.UserManager;

import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class UserFolderNode implements BasicTreeNode {
    private UserManager userManager;

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the children enumeration
     */
    public UserFolderNode(UserManager um) {
        userManager = um;
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
        Enumeration e = new EntitiesEnumeration(new UserEntitiesCollection(userManager));
        return TreeNodeFactory.getTreeNodeEnumeration(e);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * hierarchical gui components such as JTree.
     *
     * @return the FQ name as a String
     */
    public String getName() {
        return getLabel();
    }

    /**
     * Returns the label; constant "Users" is returned
     */
    public String getLabel() {
        return "Users";
    }

}
