package com.l7tech.console.tree;

import java.util.Collections;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class ServicesFolderNode implements BasicTreeNode {
    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given entry.
     */
    public ServicesFolderNode() {

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
        return true;
    }

    /**
     * Returns the node FQ name.
     * Gui nodes have FQ name to facilitate handling in
     * hierarchical gui components such as JTree.
     *
     * @return the FQ name as a String
     */
    public String getFqName() {
        return getLabel();
    }

    /**
     * Returns the label
     */
    public String getLabel() {
        return "Services";
    }

}
