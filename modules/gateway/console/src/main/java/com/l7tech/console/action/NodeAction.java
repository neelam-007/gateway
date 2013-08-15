package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;
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
 * If a subclass would like to use meta data for the getName() value from SecureAction then simply do not
 * override getName() as the implementation in NodeAction will use the meta data. For this to work correctly the
 * subclass should call a constructor of NodeAction which takes the lazyActionValuesFlag
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class NodeAction extends SecureAction {
    protected AbstractTreeNode node;
    protected JTree tree;

    public NodeAction(AbstractTreeNode node) {
        this(node, null);
    }

    public NodeAction(AbstractTreeNode node, final String name, final String desc, final String img){
        this(node, null, name, desc, img);
    }

    /**
     * @param lazyActionValuesFlag regardless of value, BaseAction's setActionValues() method will not be called until
     * all of our instance variables required are set
     */
    public NodeAction(AbstractTreeNode node, Class requiredAssertionLicense, boolean lazyActionValuesFlag) {
        this(node, requiredAssertionLicense, null, lazyActionValuesFlag);
    }

    /**
     * @param lazyActionValuesFlag regardless of value, BaseAction's setActionValues() method will not be called until
     * all of our instance variables required are set
     */
    public NodeAction(AbstractTreeNode node, Class allowedAssertionLicenses, AttemptedOperation attemptedOperation, boolean lazyActionValuesFlag) {
        this(node, allowedAssertionLicenses == null ? null : Arrays.asList(allowedAssertionLicenses), attemptedOperation, lazyActionValuesFlag);
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

    public NodeAction(AbstractTreeNode node, Class requiredAssertionLicense, final String name, final String desc, final String img){
        this(node, requiredAssertionLicense, null, name, desc, img);
    }

    public NodeAction(AbstractTreeNode node, Class requiredAssertionLicense, final String name, final String desc, final Image img){
        this(node, requiredAssertionLicense, null, name, desc, img);
    }

    public NodeAction(AbstractTreeNode node, Class allowedAssertionLicenses, AttemptedOperation attemptedOperation) {
        this(node, allowedAssertionLicenses == null ? null : Arrays.asList(allowedAssertionLicenses), attemptedOperation);
    }

    public NodeAction(AbstractTreeNode node, Class allowedAssertionLicenses, AttemptedOperation attemptedOperation, final String name, final String desc, final String img) {
        this(node, allowedAssertionLicenses == null ? null : Arrays.asList(allowedAssertionLicenses), attemptedOperation, name, desc, img);
    }

    public NodeAction(AbstractTreeNode node, Class allowedAssertionLicenses, AttemptedOperation attemptedOperation, final String name, final String desc, final Image img) {
        this(node, allowedAssertionLicenses == null ? null : Arrays.asList(allowedAssertionLicenses), attemptedOperation, name, desc, img);
    }

    public NodeAction(AbstractTreeNode node, Collection<Class> allowedAssertionLicenses, AttemptedOperation attemptedOperation) {
        super(attemptedOperation, allowedAssertionLicenses);
        this.node = node;
    }

    public NodeAction(AbstractTreeNode node, Collection<Class> allowedAssertionLicenses, AttemptedOperation attemptedOperation, final String name, final String desc, final String img) {
        super(attemptedOperation, allowedAssertionLicenses, name, desc, img);
        this.node = node;
    }

    public NodeAction(AbstractTreeNode node, Collection<Class> allowedAssertionLicenses, AttemptedOperation attemptedOperation, final String name, final String desc, final Image img) {
        super(attemptedOperation, allowedAssertionLicenses, name, desc, img);
        this.node = node;
    }

    public NodeAction(AbstractTreeNode node, String featureSetNames, AttemptedOperation attemptedOperation) {
        super(featureSetNames, attemptedOperation);
        this.node = node;
    }

    /**
     * @param lazyActionValuesFlag regardless of value, BaseAction's setActionValues() method will not be called until
     * all of our instance variables required are set
     */
    public NodeAction(AbstractTreeNode node, String featureSetNames, AttemptedOperation attemptedOperation, boolean lazyActionValuesFlag) {
        super(attemptedOperation, featureSetNames, lazyActionValuesFlag);
        this.node = node;
        //now call setActionValues as we've got our node instance variable set
        setActionValues();
    }

    /**
     * @param lazyActionValuesFlag regardless of value, BaseAction's setActionValues() method will not be called until
     * all of our instance variables required are set
     */
    public NodeAction(AbstractTreeNode node, Collection<Class> allowedAssertionLicenses, AttemptedOperation attemptedOperation, boolean lazyActionValuesFlag) {
        super(attemptedOperation, allowedAssertionLicenses, lazyActionValuesFlag);
        this.node = node;
        //now call setActionValues as we've got our node instance variable set
        setActionValues();
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
    protected EntityWithPolicyNode getEntityWithPolicyNodeCookie() {
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
        Goid providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

        if (node != null) {
            EntityHeader header = node.getEntityHeader();
            if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                providerId = header.getGoid();
            }
        }

        try {
            return Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(providerId);
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't find Identity Provider " + providerId, e);
        }

        return null;
    }

    /**
     * Implementation of getName() all subclasses can use when they want to delegate the name of the context click
     * action to meta data
     * @return
     */
    @Override
    public String getName() {
        if(node == null) return super.getName();
        final Assertion as = node.asAssertion();
        return as.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString();
    }
}
