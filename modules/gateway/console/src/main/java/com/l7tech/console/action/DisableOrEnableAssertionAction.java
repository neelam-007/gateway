package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * @auther: ghuang
 */
public abstract class DisableOrEnableAssertionAction extends NodeAction {
    protected static final int DISABLE_ASSERTION_ACTION_IDX = 0;
    protected static final int ENABLE_ASSERTION_ACTION_IDX = 1;

    private Logger logger = Logger.getLogger(DisableOrEnableAssertionAction.class.getName());
    private int actionIdx;

    public DisableOrEnableAssertionAction(AssertionTreeNode node, int actionIdx) {
        super(node);
        this.actionIdx = actionIdx;
    }

    public void performAction() {
        // Step1:
        // Perform a disable/enable action on selected assertion(s) - could be a single assertion or
        // multiple assertions.  Also, perform disable/enable on descendant or ancestor if appliably
        Assertion[] allSelectedAssertions = getAllSelectedAssertions();
        switch (actionIdx) {
            case DISABLE_ASSERTION_ACTION_IDX:
                for (Assertion currAssertion: allSelectedAssertions) disableAssertion(currAssertion);
                break;
            case ENABLE_ASSERTION_ACTION_IDX:
                for (Assertion currAssertion: allSelectedAssertions) enableAssertion(currAssertion);
                break;
            default:
                logger.warning("Invalid action index: " + actionIdx);
                return;
        }

        // Step2: notify the assertion is disabled/enabled, then Save buttons will be activated.
        notifyAssertionTreeNodeChanged();

        // Step3: notify the assertion is disabled/enabled, then validate the policy.
        notifyPolicyValidation();
    }

    /**
     * Collect all selected assertions.
     * @return all selected assertion.
     */
    private Assertion[] getAllSelectedAssertions() {
        final JTree policyTree = TopComponents.getInstance().getPolicyTree();
        TreePath[] paths = policyTree.getSelectionPaths();
        java.util.List<Assertion> assertions = new ArrayList<Assertion>();

        if (paths != null) {
            for (TreePath path : paths) {
                AssertionTreeNode assertionTreeNode = (AssertionTreeNode) path.getLastPathComponent();
                assertions.add(assertionTreeNode.asAssertion());
            }
        }

        return assertions.toArray(new Assertion[assertions.size()]);
    }

    /**
     * Disable the assertion and its descendant if appliable
     * @param assertion: an assertion to be diabled.
     */
    private void disableAssertion(Assertion assertion) {
        // Update the disable status of the assertion associated with the node.
        assertion.setEnabled(false);
        // Update the diable status of its descendant.
        if (assertion instanceof CompositeAssertion) {
            ((CompositeAssertion)assertion).disableDescendant();
        }
    }

    /**
     *  Enable the assertion and its ancestor.
     * @param assertion: an assertion to be enabled.
     */
    private void enableAssertion(Assertion assertion) {
        // Update the enable status of the assertion associated with the node.
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
    private void notifyAssertionTreeNodeChanged() {
        final JTree tree = TopComponents.getInstance().getPolicyTree();
        final PolicyTreeModel policyTreeModel = (PolicyTreeModel)tree.getModel();

        if (node instanceof AssertionTreeNode) {
            policyTreeModel.assertionTreeNodeChanged((AssertionTreeNode)node);
        }
    }

    /**
     *  Notify the validator to validate the policy after there are some changes on the disable/enable status of some assertions.
     */
    private void notifyPolicyValidation() {
        WorkSpacePanel currentWorkSpace = TopComponents.getInstance().getCurrentWorkspace();
        final JComponent currentPanel = currentWorkSpace.getComponent();
        if(currentPanel == null || !(currentPanel instanceof PolicyEditorPanel)) {
            logger.warning("Internal error: current workspace is not a PolicyEditorPanel instance");
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // The reason of not using full validatition is because it would cause lots of overhead for connecting
                    // the server, when user disables and enables assertions back and forth before the finalization.
                    // After user finalizes the policy modification, press Validate or Save buttons to make a full validation. 
                    ((PolicyEditorPanel)currentPanel).validatePolicy();
                }
            });
        }
    }
}
