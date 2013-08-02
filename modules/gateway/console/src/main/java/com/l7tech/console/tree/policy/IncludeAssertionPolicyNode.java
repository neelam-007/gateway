/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.objectmodel.FindException;

import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Policy node for {@link Include} assertions.  Note that this class deliberately does not extend
 * {@link LeafAssertionTreeNode}.
 */
public class IncludeAssertionPolicyNode extends AssertionTreeNode<Include> {
    private static final Logger logger = Logger.getLogger(IncludeAssertionPolicyNode.class.getName());

    private volatile Policy policy;
    private boolean permissionDenied;
    private boolean circularImport;

    public IncludeAssertionPolicyNode(Include assertion) {
        super(assertion);
        setAllowsChildren(true);
    }

    @Override
    protected void doLoadChildren() {
        Policy policy = getPolicy();
        if (policy == null) return;
        try {
            Assertion ass = policy.getAssertion();
            if (!(ass instanceof CompositeAssertion))
                throw new RuntimeException(MessageFormat.format("Top-level assertion in included policy #{0} ({1}) is not a CompositeAssertion", policy.getGoid(), policy.getName()));
            LoadChildrenStrategy strat = LoadChildrenStrategy.newStrategy(this);
            strat.loadChildren(this, (CompositeAssertion)ass);
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format("Couldn't parse included policy #{0} ({1}): {2}", policy.getGoid(), policy.getName(), ExceptionUtils.getMessage(e)), e);
        }
    }

    @Override
    public String getName(final boolean decorate) {
        if(!decorate) return assertion.meta().get(AssertionMetadata.PALETTE_NODE_NAME).toString();

        Policy policy = getPolicy();
        StringBuilder sb = new StringBuilder(assertion.meta().get(AssertionMetadata.PALETTE_NODE_NAME).toString()+": ");
        if (policy == null) {
            if ( permissionDenied ) {
                sb.append("Permission Denied");
            } else if (circularImport) {
                sb.append("Circular Import");
            } else {
                sb.append("Deleted");
            }
            sb.append(" Policy #");
            sb.append(assertion.getPolicyGuid());
            String name = assertion.getPolicyName();
            if (name != null) sb.append(" (").append(name).append(")");
        } else {
            sb.append(policy.getName());
        }
        return DefaultAssertionPolicyNode.addCommentToDisplayText(assertion, sb.toString());
    }

    @Override
    protected String iconResource(boolean open) {
        return assertion.meta().get(AssertionMetadata.POLICY_NODE_ICON).toString();
    }

    @Override
    public boolean accept(AbstractTreeNode node) {
        // Can't drag into an Include (yet?)
        return false;
    }

    /**
     * Allow the include policy node to add a new assertion node at the position below the include policy node.
     * This receive method is exactly same as the receive method of LeafAssertionTreeNode.
     * @param node: an assertion from the palette.
     * @return true if successfully receiving a node.
     */
    @Override
    public boolean receive(AbstractTreeNode node) {
        if (super.receive(node)) return true;

        JTree tree = (JTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion[] nass = node.asAssertions();
            if (nass != null) {
                for (int i = 0; i < nass.length; i++) {
                    Assertion nas = nass[i];
                    AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nas);
                    final MutableTreeNode parent = (MutableTreeNode)getParent();
                    int index = parent.getIndex(this);
                    if (index == -1) {
                        throw new IllegalStateException("Unknown node to the three model " + this);
                    }
                    model.insertNodeInto(as, parent, index + (i + 1));
                }
            } else {
                logger.log(Level.WARNING, "The node has no associated assertion " + node);
            }
        } else {
            logger.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    @Override
    public Policy getPolicy() {
        if (policy == null) {
            permissionDenied = false;
            circularImport = false;
            if ( isParentPolicy(assertion.getPolicyGuid(), assertion.getPolicyName()) ) {
                circularImport = true;
            } else {
                try {
                    if(assertion instanceof Include) {
                        policy = assertion.retrieveFragmentPolicy();
                    }
                    if(policy == null) {
                        policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(assertion.getPolicyGuid());
                    }
                } catch ( PermissionDeniedException pde ) {
                    logger.log(Level.WARNING, "Couldn't load included policy [permission denied]");
                    permissionDenied = true;
                } catch ( FindException e ) {
                    logger.log(Level.WARNING, "Couldn't load included policy", e);
                }
            }
        }
        return policy;
    }

    @Override
    public Action[] getActions() {
        final List<Action> actions = new ArrayList<Action>();
        final Policy policy = getPolicy();
        if (policy != null && !Goid.isDefault(policy.getGoid())) {
            final RootNode rootNode = TopComponents.getInstance().getRootNode();
            final AbstractTreeNode retrievedNode = rootNode.getNodeForEntity(policy.getGoid());
            if (retrievedNode instanceof EntityWithPolicyNode) {
                final EntityWithPolicyNode entityNode = (EntityWithPolicyNode) retrievedNode;
                // allow user to edit the included policy
                final EditPolicyAction editAction = new EditPolicyAction(entityNode);
                actions.add(editAction);
            }
        }
        actions.addAll(Arrays.asList(super.getActions()));
        return actions.toArray(new Action[actions.size()]);
    }

    private boolean isParentPolicy(final String policyGuid, final String policyName) {
        boolean found = false;
        TreeNode currentNode = getParent();

        while (currentNode != null) {
            if (currentNode instanceof IncludeAssertionPolicyNode) {
                IncludeAssertionPolicyNode includeTreeNode = (IncludeAssertionPolicyNode) currentNode;
                if (policyGuid == null && includeTreeNode.asAssertion().getPolicyName().equals(policyName) ||
                    policyGuid != null && includeTreeNode.asAssertion().getPolicyGuid().equals(policyGuid))
                {
                    found = true;
                    break;
                }
            }

            currentNode = currentNode.getParent();
        }

        return found;
    }
}
