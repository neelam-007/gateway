package com.l7tech.console.action;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import javax.swing.tree.TreeNode;

import com.l7tech.console.tree.policy.AssertionTreeNode;


/**
 * The <code>AssertionMoveDownAction</code> is the action that moves
 * the assertion down in the policy assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AssertionMoveDownAction extends SecureAction {
    protected AssertionTreeNode node;
    protected AssertionTreeNode[] nodes;

    public AssertionMoveDownAction(AssertionTreeNode node) {
        super(null);
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
        //return "Move the assertion down";
        return getName();
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
    protected void performAction() {
        if (node != null && nodes == null) {
            node.swap((AssertionTreeNode)node.getNextSibling());
        } else if (nodes != null) {
            Set onTheMove = new HashSet(Arrays.asList(nodes));
            Set processed = new HashSet();
            for (int n=0; n<nodes.length; n++) {
                AssertionTreeNode current = nodes[n];
                TreeNode parent = current.getParent();
                if (!processed.contains(parent)) {
                    processed.add(parent);
                    for (int c=parent.getChildCount()-1; c>=0; c--) {
                        TreeNode ctn = parent.getChildAt(c);
                        if (onTheMove.contains(ctn)) {
                            AssertionTreeNode moving = (AssertionTreeNode) ctn;
                            moving.swap((AssertionTreeNode)moving.getNextSibling());
                        }
                    }
                }
            }
        }
    }
}
