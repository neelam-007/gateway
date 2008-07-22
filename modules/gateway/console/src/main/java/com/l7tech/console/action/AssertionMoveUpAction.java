package com.l7tech.console.action;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

import javax.swing.tree.TreeNode;

import com.l7tech.console.tree.policy.AssertionTreeNode;


/**
 * The <code>AssertionMoveUpAction</code> is the action that moves
 * the assertion up in the policy tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AssertionMoveUpAction extends SecureAction {
    protected AssertionTreeNode node;
    protected AssertionTreeNode[] nodes;

    public AssertionMoveUpAction(AssertionTreeNode node) {
        super(null);
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Move Assertion Up";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        //return "Move up the assertion";
        return getName();
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Up16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        if (node != null && nodes == null) {
            node.swap((AssertionTreeNode)node.getPreviousSibling());
        } else if (nodes != null) {
            Set onTheMove = new HashSet(Arrays.asList(nodes));
            Set processed = new HashSet();
            for (int n=0; n<nodes.length; n++) {
                AssertionTreeNode current = nodes[n];
                TreeNode parent = current.getParent();
                if (!processed.contains(parent)) {
                    processed.add(parent);
                    for (int c=1; c<parent.getChildCount(); c++) {
                        TreeNode ctn = parent.getChildAt(c);
                        if (onTheMove.contains(ctn)) {
                            AssertionTreeNode moving = (AssertionTreeNode) ctn;
                            moving.swap((AssertionTreeNode)moving.getPreviousSibling());
                        }
                    }
                }
            }
        }
    }
}
