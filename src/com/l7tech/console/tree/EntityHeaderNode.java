package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Collections;
import java.util.Enumeration;

/**
 * The class represents an entity gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public abstract class EntityHeaderNode extends AbstractTreeNode {
    /**
     * construct the <CODE>EntityHeaderNode</CODE> instance for a given
     * <CODE>id</CODE>
     *
     * @param e  the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public EntityHeaderNode(EntityHeader e) {
        super(e);
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
        return (EntityHeader)getUserObject();
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
    }

    public String getName() {
        return getEntityHeader().getName();
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
}
