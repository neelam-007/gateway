package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Collections;
import java.util.Enumeration;

/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with admins for Root, Realm or
 * Company.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class AdminFolderNode implements BasicTreeNode {
    /**
     * construct the <CODE>AdminFolderNode</CODE> instance for
     * a given entry.
     * The parameter entry must be Root, Realm or Company, otherwise
     * the runtime IllegalArgumentException is thrown.
     *
     * @param entry  the Entry instance
     * @exception IllegalArgumentException
     *                   thrown if the Entry instance is not a Root,
     *                   Realm or Company Entry
     */
    public AdminFolderNode(EntityHeader entry) {
        this.entry = entry;
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
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns the node name.
     * @return the name as a String
     */
    public String getName() {
        return "Administrators";
    }

    private EntityHeader entry;
    private String fqName;

}
