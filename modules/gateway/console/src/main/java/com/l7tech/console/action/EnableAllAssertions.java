package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.tree.TreeNode;

/**
 * Enable all descendant assertions in a composite assertion, which can be either enabled or disabled.
 * Note: The action should be only applied onto a composite assertion tree node.
 *
 * @author ghuang
 */
public class EnableAllAssertions extends NodeAction {
    public EnableAllAssertions(AssertionTreeNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "Enable All Assertions";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/enableAllAssertions16.gif";
    }

    @Override
    protected void performAction() {
        if (! (node instanceof CompositeAssertionTreeNode)) {
            return;
        }

        node.asAssertion().setEnabled(true);
        ((AssertionTreeNode)node).setAncestorDisabled(false);

        // If the composite assertion is to be enabled, then enable all descendants and all ancestors.
        ((CompositeAssertionTreeNode) node).enableAllDescendants();
        ((AssertionTreeNode)node).enableAncestors();

        // Notify the assertion node is enabled, then Save buttons will be activated.
        notifyAssertionTreeNodeChanged((AssertionTreeNode)node);
    }

    /**
     * Notify that there are changes on the properties of the assertion associated with node in
     * the policy tree.  It turns out that Save buttons will be re-activated.
     *
     * There are three places where we add treeModelListeners (two from {@link com.l7tech.console.poleditor.PolicyEditorPanel
     * and one from {@link com.l7tech.console.tree.policy.PolicyToolBar }.)
     */
    private void notifyAssertionTreeNodeChanged(AssertionTreeNode node) {
        final PolicyTreeModel policyTreeModel = (PolicyTreeModel) TopComponents.getInstance().getPolicyTree().getModel();

        // Notify all parent nodes and itself..
        for (TreeNode tn: node.getPath()) {
            if (tn.getParent() == null) continue;
            policyTreeModel.assertionTreeNodeChanged((AssertionTreeNode)tn);
        }
    }
}