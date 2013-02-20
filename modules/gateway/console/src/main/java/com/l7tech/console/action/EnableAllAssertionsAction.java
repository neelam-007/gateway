package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * Enable all descendant assertions in a composite assertion, which can be either enabled or disabled.
 * Note: The action should be only applied onto a composite assertion tree node.
 *
 * @author ghuang
 */
public class EnableAllAssertionsAction extends NodeAction {
    public EnableAllAssertionsAction(AssertionTreeNode node) {
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
    public boolean supportMultipleSelection() {
        return true;
    }

    @Override
    protected void performAction() {
        List<AssertionTreeNode> allSelectedAssertionNodes = getAllSelectedAssertionNodes();
        for (AssertionTreeNode node: allSelectedAssertionNodes) {
            if (! (node instanceof CompositeAssertionTreeNode)) {
                continue;
            }

            node.asAssertion().setEnabled(true);
            node.setAncestorDisabled(false);

            // If the composite assertion is to be enabled, then enable all descendants and all ancestors.
            ((CompositeAssertionTreeNode) node).enableAllDescendants();
            node.enableAncestors();

            // Notify the assertion node is enabled, then Save buttons will be activated.
            notifyAssertionTreeNodeChanged(node);
        }
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

    /**
     * Collect all selected assertions.
     * @return all selected assertion.
     */
    private List<AssertionTreeNode> getAllSelectedAssertionNodes() {
        final JTree policyTree = TopComponents.getInstance().getPolicyTree();
        TreePath[] paths = policyTree.getSelectionPaths();
        List<AssertionTreeNode> nodeList = new ArrayList<>();

        if (paths != null) {
            for (TreePath path : paths) {
                nodeList.add((AssertionTreeNode) path.getLastPathComponent());
            }
        }

        return nodeList;
    }
}