package com.l7tech.console.tree;

import com.l7tech.console.action.DeleteEntityAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.Comparator;
import java.util.logging.Level;

/**
 * The class represents an entity gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public abstract class EntityHeaderNode<HT extends EntityHeader> extends AbstractTreeNode {
    private IdentityProviderConfig config;

    /**
     * The entity name comparator
     */
    public static final Comparator<TreeNode> NAME_COMPARATOR = new Comparator<TreeNode>() {
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof EntityHeaderNode && o2 instanceof EntityHeaderNode) {
                String name1 = ((EntityHeaderNode)o1).getEntityHeader().getName();
                String name2 = ((EntityHeaderNode)o2).getEntityHeader().getName();
                return name1.compareTo(name2);
            }
            throw new ClassCastException("Expected " + EntityHeaderNode.class +
              " received " + o1.getClass() + " and " + o2.getClass());
        }
    };

    /**
     * The entity name comparator
     */
    public static final Comparator<TreeNode> IGNORE_CASE_NAME_COMPARATOR = new Comparator<TreeNode>() {
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof EntityHeaderNode && o2 instanceof EntityHeaderNode) {
                String name1 = ((EntityHeaderNode)o1).getEntityHeader().getName();
                String name2 = ((EntityHeaderNode)o2).getEntityHeader().getName();
                return name1.compareToIgnoreCase(name2);
            }
            throw new ClassCastException("Expected " + EntityHeaderNode.class +
              " received " + (o1 == null ? null : o1.getClass().getSimpleName()) + " and " + (o2 == null ? null : o2.getClass().getSimpleName()));
        }
    };

    /**
     * construct the <CODE>EntityHeaderNode</CODE> instance for a given
     * <CODE>id</CODE>
     *
     * @param e the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public EntityHeaderNode(HT e) {
        super(e);
        setAllowsChildren(false);
    }

    public EntityHeaderNode(HT e, Comparator<TreeNode> c){
        super(e, c);
        setAllowsChildren(false);
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
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
    @Override
    public Action[] getActions() {
        final DeleteEntityAction deleteAction = new DeleteEntityAction(this, config);
        deleteAction.setEnabled(canDelete());
        TopComponents.getInstance().addPermissionRefreshListener(deleteAction);
        return new Action[]{deleteAction};
    }

    /**
     * Test if the node can be deleted. Default for entites
     * is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    @Override
    public boolean canDelete() {
        return super.canDelete();
    }

    /**
     * Returns the entity this node contains.
     *
     * @return the <code>EntityHeader</code>
     */
    @SuppressWarnings({"unchecked"})
    public HT getEntityHeader() {
        return (HT)getUserObject();
    }

    /**
     * @return the <code>IdentityProviderConfig</code>
     */
    public IdentityProviderConfig getProviderConfig() {
        if (config != null) {
            return config;
        }
        try {
            long oid = getEntityHeader().getOid();
            config = getIdentityAdmin().findIdentityProviderConfigByID(oid);
            return config;
        }
        catch (Exception e) {
            ErrorManager.getDefault().
                    notify(Level.WARNING, e, "Unable to locate the identity provider " + getEntityHeader().getName());
            return null;
        }
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
    }

    @Override
    public String getName() {
        return getEntityHeader().getName();
    }

    /**
     * Override toString
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append("\n").
          append(super.toString());
        return sb.toString();
    }
}
