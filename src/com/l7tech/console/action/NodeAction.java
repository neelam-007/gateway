package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.Iterator;
import java.util.logging.Level;

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
public abstract class NodeAction extends SecureAction {
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
    public IdentityProviderConfig getIdentityProviderConfig(EntityHeaderNode node) {

        if (node instanceof EntityHeaderNode) {
            EntityHeader header = ((EntityHeaderNode) node).getEntityHeader();
            if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                try {
                    return Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByPrimaryKey(header.getOid());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Couldn't find Identity Provider " + header.getOid(), e);
                }
            }
        }

        return null;
    }
}
