package com.l7tech.console.tree;

import java.util.Enumeration;

/**
 * The interface represents an common interface for TreeModel
 * nodes.
 *
 * The interface will be replaced with the custom swing
 * <CODE>MutableTreeNode</CODE> implementation. Currently it
 * contains an esential  subset of node operations (there are no
 * siblings or parent/child relationship ops implemented).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version $1.1
 * @see com.l7tech.console.tree.EntityHeaderNode
 */
public interface BasicTreeNode {

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    boolean isLeaf();

    /**
     * Returns the children of the reciever as an Enumeration.
     *
     * @return the Enumeration of the child nodes.
     * @exception Exception thrown when an erro is encountered when
     *                      retrieving child nodes.
     */
    Enumeration children() throws Exception;

    /**
     * Returns true if the receiver allows children.
     */
    boolean getAllowsChildren();

    /**
     * Returns the node label (shown in GUI elements such as tree, table etc).
     */
    String getLabel();

    /**
     * Returns the node FQ name
     */
    String getFqName();
}
