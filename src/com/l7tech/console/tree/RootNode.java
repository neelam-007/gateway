package com.l7tech.console.tree;

import com.l7tech.console.util.Registry;

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
        Registry r = Registry.getDefault();
        List list =
                Arrays.asList(
                        new BasicTreeNode[]{
                            new UserFolderNode(r.getInternalUserManager()),
                            new GroupFolderNode(r.getInternalGroupManager()),
                            new ProvidersFolderNode(),
                            new PoliciesFolderNode()
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
     *
     * @return the root name
     */
    public String getName() {
        return label;
    }
    private String label;
}
