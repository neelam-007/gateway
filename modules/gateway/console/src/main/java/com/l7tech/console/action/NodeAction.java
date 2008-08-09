package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
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

    public NodeAction(AbstractTreeNode node) {
        this(node, null);
    }

    /**
     * constructor accepting the node that this action will act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     * @param requiredAssertionLicense assertion class that must be licensed, or null to allow action regardless of licensing
     */
    public NodeAction(AbstractTreeNode node, Class requiredAssertionLicense) {
        this(node, requiredAssertionLicense, null);
    }


    public NodeAction(AbstractTreeNode node, Collection<Class> allowedAssertionLicenses, AttemptedOperation attemptedOperation) {
        super(attemptedOperation, allowedAssertionLicenses);
        this.node = node;
    }

    public NodeAction(AbstractTreeNode node, Class allowedAssertionLicenses, AttemptedOperation attemptedOperation) {
        this(node, allowedAssertionLicenses == null ? null : Arrays.asList(allowedAssertionLicenses), attemptedOperation);
    }

    /**
     * set the tree that this node is associated with
     *
     * @param tree the tree this node is associated with
     */
    public final void setTree(JTree tree) {
        this.tree = tree;
        this.firePropertyChange("tree", tree, tree);
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
     * @return the policy cookie or null if not founds
     */
    protected EntityWithPolicyNode getPolicyNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext(); ) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof EntityWithPolicyNode) return (EntityWithPolicyNode)value;
        }
        return null;
    }

    /**
     * @return the identity provider or null if not found
     */
    public IdentityProviderConfig getIdentityProviderConfig(EntityHeaderNode node) {
        long providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        if (node != null) {
            EntityHeader header = node.getEntityHeader();
            if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                providerId = header.getOid();
            }
        }

        try {
            return Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(providerId);
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't find Identity Provider " + providerId, e);
        }

        return null;
    }
}
