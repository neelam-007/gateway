package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.*;

/**
 * The class represents an entry gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class ProvidersFolderNode implements BasicTreeNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public ProvidersFolderNode() {
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
     * That is, the enumeration of Realm entries.
     *
     * @return the Enumeration of the child nodes.
     * @exception Exception thrown when an error is encountered when
     *                      retrieving child nodes.
     */
    public Enumeration children() throws Exception {
        EntityHeader[] res = new EntityHeader[0];
        List list =  new ArrayList();
        for (int i = 0;i<res.length;i++) {
            list.add(new ProviderNode(res[i]));
        }
        return Collections.enumeration(list);
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
    public String getName() {
        return "Identity providers";
    }

    /**
     * Returns the label; constant "Realms" is returned
     */
    public String getLabel() {
        return "Identity providers";
    }

}
