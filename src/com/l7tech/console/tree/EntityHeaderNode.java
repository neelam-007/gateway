package com.l7tech.console.tree;

import java.util.Enumeration;
import java.util.Collections;

import com.l7tech.objectmodel.EntityHeader;

/**
 * The class represents an Entry gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class EntityHeaderNode implements BasicTreeNode {
    /**
     * construct the <CODE>EntityHeaderNode</CODE> instance for a given
     * <CODE>id</CODE>
     *
     * @param e  the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public EntityHeaderNode(EntityHeader e) {
        this.entity = e;
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
     * Returns the entity this node contains.
     *
     * @return the <code>EntityHeader</code>
     */
    public EntityHeader getEntityHeader() {
        return entity;
    }


    /**
     * Returns the Entry label, delegated to short name
     */
    public String getLabel() {
        return entity.getName();
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

    private final EntityHeader entity;
}
