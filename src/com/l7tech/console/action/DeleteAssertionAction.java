package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.Functions;
import com.l7tech.common.policy.Policy;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;


/**
 * The <code>DeleteAssertionAction</code> action deletes
 * the assertion from the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteAssertionAction extends SecureAction {
    protected AssertionTreeNode node;
    protected AssertionTreeNode[] nodes;

    public DeleteAssertionAction() {
        super(null);
    }

    public DeleteAssertionAction(AssertionTreeNode node) {
        super(null);
        this.node = node;
    }

    public DeleteAssertionAction(AssertionTreeNode node, AssertionTreeNode[] nodes) {
        super(null);
        this.node = node;
        this.nodes = nodes;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete Assertion";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        //return "Delete the assertion from the policy";
        return getName();
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }

        if (nodes == null) {
            delete(node);
        } else if (nodes.length < 2) {
            delete(nodes[0]);
        } else {
            delete(nodes);
        }
    }

    @Override
    public boolean isAuthorized() {
        if (node == null) return true;
        try {
            // Case 1: if the node is associated to a published service
            PublishedService svc = node.getService();
            boolean authorized = canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, svc));

            // Case 2: if the node is associated to a policy fragment
            if (svc == null && !authorized) {
                Policy policy = node.getPolicy();
                authorized = canAttemptOperation(new AttemptedUpdate(EntityType.POLICY, policy));
            }
            return authorized;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current service or policy", e);
        }
    }

    private void delete(final AssertionTreeNode treeNode) {
        Actions.deleteAssertion(treeNode, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean deleted) {
                if (deleted) {
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.removeNodeFromParent(treeNode);
                }
            }
        });
    }

    private void delete(final AssertionTreeNode[] treeNodes) {
        Actions.deleteAssertions(treeNodes, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean deleted) {
                if (deleted) {
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    for (AssertionTreeNode treeNode : treeNodes) {
                        model.removeNodeFromParent(treeNode);
                    }
                }
            }
        });
    }
}
