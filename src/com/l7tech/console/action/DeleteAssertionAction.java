package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;


/**
 * The <code>DeleteAssertionAction</code> action deletes
 * the assertion from the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteAssertionAction extends BaseAction {
    protected AssertionTreeNode node;

    public DeleteAssertionAction() {
    }

    public DeleteAssertionAction(AssertionTreeNode node) {
        this.node = node;
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
        return "Delete the assertion from the policy assertion tree";
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
    public void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }
            boolean deleted = Actions.deleteAssertion(node);
        if (deleted) {
            JTree tree =
              (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.removeNodeFromParent(node);
        }
    }
}
