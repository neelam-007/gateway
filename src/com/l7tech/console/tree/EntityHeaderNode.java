package com.l7tech.console.tree;

import com.l7tech.console.action.DeleteEntityAction;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;

/**
 * The class represents an entity gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
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
     * Get the set of actions associated with this node.
     * This returns actions that are used buy entity nodes
     * such .
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{new DeleteEntityAction(this)};
    }

    /**
     *Test if the node can be deleted. Default for entites
     * is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Returns the entity this node contains.
     *
     * @return the <code>EntityHeader</code>
     */
    public EntityHeader getEntityHeader() {
        return (EntityHeader) getUserObject();
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
