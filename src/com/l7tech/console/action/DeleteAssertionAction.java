package com.l7tech.console.action;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;


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
        } else {
            delete(nodes);
        }
    }

    private void delete(AssertionTreeNode treeNode) {
        boolean deleted = Actions.deleteAssertion(treeNode);
        if (deleted) {
            JTree tree = (JTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.removeNodeFromParent(treeNode);
        }
    }

    private void delete(AssertionTreeNode[] treeNodes) {
        boolean deleted = Actions.deleteAssertions(treeNodes);
        if (deleted) {
            JTree tree = (JTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            for (int n=0; n<treeNodes.length; n++) {
                model.removeNodeFromParent(treeNodes[n]);
            }
        }
    }
}
