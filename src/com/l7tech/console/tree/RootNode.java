package com.l7tech.console.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The class represents an entry gui node element that
 * corresponds to the Root data element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class RootNode implements BasicTreeNode {
    /**
     * construct the <CODE>RootNode</CODE> instance
     */
    public RootNode(String title)
    throws IllegalArgumentException {
        if (title == null)
            throw new IllegalArgumentException();
        label = title;
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
     * @exception Exception thrown when an erroR is encountered when
     *                      retrieving child nodes.
     */
    public Enumeration children() throws Exception {
        List list =
                Arrays.asList(
                        new BasicTreeNode[]{
                            new ProvidersFolderNode(),
                            new PoliciesFolderNode(),
                            new ServicesFolderNode()
                        });
        return Collections.enumeration(list);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Returns the label; constant "System" is returned
     */
    public String getLabel() {
        return label;
    }

    public String getName() {
        return getLabel();
    }
    private String label;
}
