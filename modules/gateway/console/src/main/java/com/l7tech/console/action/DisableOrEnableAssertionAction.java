package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * @author ghuang
 */
public abstract class DisableOrEnableAssertionAction extends NodeAction {
    protected static final int DISABLE_ASSERTION_ACTION_IDX = 0;
    protected static final int ENABLE_ASSERTION_ACTION_IDX = 1;

    private final PolicyTreeModel policyTreeModel;
    private Logger logger = Logger.getLogger(DisableOrEnableAssertionAction.class.getName());
    private int actionIdx;

    public DisableOrEnableAssertionAction(AssertionTreeNode node, int actionIdx) {
        super(node);
        this.actionIdx = actionIdx;
        policyTreeModel = (PolicyTreeModel) TopComponents.getInstance().getPolicyTree().getModel();
    }

    @Override
    public boolean supportMultipleSelection(){return true;}

    public void performAction() {
        // Perform a disable/enable action on selected assertion(s) - could be a single assertion or
        // multiple assertions.  Also, perform disable/enable on descendant or ancestor if applicable
        List<AssertionTreeNode> allSelectedAssertionNodes = getAllSelectedAssertionNodes();
        switch (actionIdx) {
            case DISABLE_ASSERTION_ACTION_IDX:
                for (AssertionTreeNode node: allSelectedAssertionNodes) {
                    // Disable the assertion
                    disableAssertion(node);

                    // Notify the assertion is disabled, then Save buttons will be activated.
                    notifyAssertionTreeNodeChanged(node);
                }
                break;
            case ENABLE_ASSERTION_ACTION_IDX:
                for (AssertionTreeNode node: allSelectedAssertionNodes) {
                    // Enable the assertion
                    enableAssertion(node);

                    // Notify the assertion is enabled, then Save buttons will be activated.
                    notifyAssertionTreeNodeChanged(node);
                }
                break;
            default:
                logger.warning("Invalid action index: " + actionIdx);
        }
    }

    /**
     * Collect all selected assertions.
     * @return all selected assertion.
     */
    private List<AssertionTreeNode> getAllSelectedAssertionNodes() {
        final JTree policyTree = TopComponents.getInstance().getPolicyTree();
        TreePath[] paths = policyTree.getSelectionPaths();
        List<AssertionTreeNode> nodeList = new ArrayList<AssertionTreeNode>();

        if (paths != null) {
            for (TreePath path : paths) {
                nodeList.add((AssertionTreeNode) path.getLastPathComponent());
            }
        }

        return nodeList;
    }

    /**
     * Disable the assertion and its descendant if applicable
     * @param node: an assertion node to be disabled.
     */
    private void disableAssertion(AssertionTreeNode node) {
        // Update the disable status of the assertion associated with the node.
        Assertion assertion = node.asAssertion();
        assertion.setEnabled(false);
        // Update the disable status of its descendant.
        if (assertion instanceof CompositeAssertion) {
            ((CompositeAssertion)assertion).disableDescendant();
        }
    }

    /**
     *  Enable the assertion and its ancestor.
     * @param node: an assertion node to be enabled.
     */
    private void enableAssertion(AssertionTreeNode node) {
        // Update the enable status of the assertion associated with the node.
        Assertion assertion = node.asAssertion();
        assertion.setEnabled(true);
        // Update the enable status of its ancestor.
        assertion.enableAncestor();
    }

    /**
     * Notify that there are changes on the properties of the assertion associated with node in
     * the policy tree.  It turns out that Save buttons will be re-activated.
     *
     * There are three places where we add treeModelListeners (two from {@link com.l7tech.console.poleditor.PolicyEditorPanel
     * and one from {@link com.l7tech.console.tree.policy.PolicyToolBar }.)
     */
    private void notifyAssertionTreeNodeChanged(AssertionTreeNode node) {
        switch (actionIdx) {
            case DISABLE_ASSERTION_ACTION_IDX:
                // Notify all child nodes.
                if (node.asAssertion() instanceof CompositeAssertion) {
                    for (Enumeration e = node.children(); e.hasMoreElements(); )
                        notifyAssertionTreeNodeChanged((AssertionTreeNode) e.nextElement());
                }
                // Notify itself.
                policyTreeModel.assertionTreeNodeChanged(node);
                break;
            case ENABLE_ASSERTION_ACTION_IDX:
                // Notify all parent nodes and itself..
                for (TreeNode tn: node.getPath()) {
                    if (tn.getParent() == null) continue;
                    policyTreeModel.assertionTreeNodeChanged((AssertionTreeNode)tn);
                }
                break;
            default:
                logger.warning("Invalid action index: " + actionIdx);
        }
    }
}