package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
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
                    policyTreeModel.assertionTreeNodeChanged(node);
                }
                break;
            case ENABLE_ASSERTION_ACTION_IDX:
                for (AssertionTreeNode node: allSelectedAssertionNodes) {
                    // Enable the assertion
                    enableAssertion(node);

                    // Notify the assertion is enabled, then Save buttons will be activated.
                    policyTreeModel.assertionTreeNodeChanged(node);
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
     * Disable the assertion and update ancestorDisabled attribute if applicable
     * @param node: an assertion node to be disabled.
     */
    private void disableAssertion(AssertionTreeNode node) {
        // Update the disable status of the assertion associated with the node.
        node.asAssertion().setEnabled(false);

        // If the node is a composite assertion, then update the attribute "ancestorDisable" of its descendants
        if (node instanceof CompositeAssertionTreeNode) {
            ((CompositeAssertionTreeNode) node).updateDescendantAttributeAncestorDisabled(true);
        }
    }

    /**
     *  Enable the assertion and its ancestor.
     * @param node: an assertion node to be enabled.
     */
    private void enableAssertion(AssertionTreeNode node) {
        // Update the enable status of the assertion associated with the node.
        node.asAssertion().setEnabled(true);
        node.setAncestorDisabled(false);

        // If the node is a composite assertion, then update the attribute "ancestorDisable" of its descendants
        if (node instanceof CompositeAssertionTreeNode) {
            ((CompositeAssertionTreeNode) node).updateDescendantAttributeAncestorDisabled(false);
        }

        // Enable its disabled ancestors
        node.enableAncestors();
    }
}