package com.l7tech.console.tree;

import java.util.Collections;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with policies for a provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class PoliciesFolderNode implements BasicTreeNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public PoliciesFolderNode() {
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
     * @exception Exception thrown when an error is encountered when
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
     *
     * @return the name as a String
     */
    public String getName() {
        return "Policies";
    }

}
