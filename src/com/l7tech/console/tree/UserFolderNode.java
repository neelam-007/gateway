package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class UserFolderNode implements BasicTreeNode {
    private final Enumeration children;

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param ce the children enumeration
     */
    public UserFolderNode(Enumeration ce) {
        children = ce;
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
       return children;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
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
