package com.l7tech.console.tree;

import java.util.Enumeration;
import java.util.Collections;
import com.l7tech.adminservicestub.ListResultEntry;

/**
 * The class represents an Entry gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class EntryTreeNode implements BasicTreeNode {
    /**
     * construct the <CODE>EntryTreeNode</CODE> instance for a given
     * <CODE>id</CODE>
     *
     * @param entry  the entry represented by this <CODE>EntryTreeNode</CODE>
     */
    public EntryTreeNode(ListResultEntry entry) {
        this.entry = entry;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns the children of the reciever as an Enumeration
     * of EntryTreeNodes.
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
     * Returns the Entry label, delegated to short name
     */
    public String getLabel() {
        return entry.getName();
    }

    public String getFqName() {
        return getLabel();
    }

    /**
     * Override toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName() + "\n").
                append(super.toString());
        return sb.toString();
    }

    private final ListResultEntry entry;
}
