package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.Iterator;

/**
 * The <code>NodeAction</code> is the action that is
 * associated with tree nodes.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class NodeAction extends BaseAction {
    protected AbstractTreeNode node;
    protected JTree tree;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public NodeAction(AbstractTreeNode node) {
        this(node, null);
    }

    /**
     * full constructor. Construct the node action with the
     * node and the tree parameters.
     *
     * @param node the node that this action will act on
     * @param tree the tree where the node lives
     */
    public NodeAction(AbstractTreeNode node, JTree tree) {
        this.node = node;
        this.tree = tree;
    }

    /**
     * set the tree that this node is associated with
     *
     * @param tree the tree this node is associated with
     */
    public final void setTree(JTree tree) {
        JTree ot = tree;
        this.tree = tree;
        this.firePropertyChange("tree", ot, tree);
    }

    /**
     * @return the published service cookie or null if not founds
     */
    protected ServiceNode getServiceNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext(); ) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }

    /**
     * @return the identity provider or null if not found
     */
    public IdentityProvider getIdentityProvider(EntityHeaderNode node) {
        TreeNode parentNode = node.getParent();
        while (parentNode != null) {
            if (parentNode instanceof EntityHeaderNode) {
                EntityHeader header = ((EntityHeaderNode) parentNode).getEntityHeader();
                if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                    return Registry.getDefault().getIdentityProvider(header.getOid());
                }
            }
            parentNode = parentNode.getParent();
        }
        return null;
    }
}
