package com.l7tech.console.tree;

import com.l7tech.console.action.DeleteEntityAction;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.util.Comparator;

/**
 * The class represents an entity gui node element in the
 * TreeModel.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public abstract class EntityHeaderNode extends AbstractTreeNode {
    private IdentityProvider provider;

    /** The entity name comparator  */
    protected static final Comparator NAME_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof EntityHeaderNode && o2 instanceof EntityHeaderNode) {
                String name1 = ((EntityHeaderNode)o1).getEntityHeader().getName();
                String name2 = ((EntityHeaderNode)o2).getEntityHeader().getName();
                return name1.compareTo(name2);
            }
            throw new ClassCastException("Expected "+EntityHeaderNode.class +
                                         " received "+o1.getClass() + " and "+o2.getClass());
        }
    };

    /** The entity name comparator  */
       protected static final Comparator IGNORE_CASE_NAME_COMPARATOR = new Comparator() {
           public int compare(Object o1, Object o2) {
               if (o1 instanceof EntityHeaderNode && o2 instanceof EntityHeaderNode) {
                   String name1 = ((EntityHeaderNode)o1).getEntityHeader().getName();
                   String name2 = ((EntityHeaderNode)o2).getEntityHeader().getName();
                   return name1.compareToIgnoreCase(name2);
               }
               throw new ClassCastException("Expected "+EntityHeaderNode.class +
                                            " received "+o1.getClass() + " and "+o2.getClass());
           }
       };

    /**
     * construct the <CODE>EntityHeaderNode</CODE> instance for a given
     * <CODE>id</CODE>
     * 
     * @param e the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public EntityHeaderNode(EntityHeader e) {
        super(e);
        setAllowsChildren(false);
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
     * Get the set of actions associated with this node.
     * This returns actions that are used buy entity nodes
     * such .
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        final DeleteEntityAction deleteAction = new DeleteEntityAction(this, provider);
        deleteAction.setEnabled(canDelete());
        return new Action[]{deleteAction};
    }

    /**
     * Test if the node can be deleted. Default for entites
     * is <code>true</code>
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return super.canDelete();
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
     * @return the <code>IdentityProvider</code>
     */
    public IdentityProvider getProvider() {
        if (provider != null) {
            return provider;
        }
        try {
            long oid = getEntityHeader().getOid();
            provider = Registry.getDefault().getProviderConfigManager().getIdentityProvider(oid);
            return provider;
        } catch (FindException e) {
            throw new RuntimeException("Unable to locate the identity provider " + getEntityHeader().getName(), e);
        }
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
