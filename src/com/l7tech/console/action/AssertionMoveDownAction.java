package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;


/**
 * The <code>AssertionMoveDownAction</code> is the action that moves
 * the assertion down in the policy assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AssertionMoveDownAction extends BaseAction {
    protected AssertionTreeNode node;


    public AssertionMoveDownAction(AssertionTreeNode node) {
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Move Assertion Down";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Move down the policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Down16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        node.swap((AssertionTreeNode)node.getNextSibling());
    }
}
