package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;

import javax.swing.*;


/**
 * The <code>AddAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddAssertionAction extends SecureAction {
    protected AbstractTreeNode paletteNode;
    protected AssertionTreeNode assertionNode;

    /**
     * @return the action name
     */
    public String getName() {
        return "Add assertion";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add assertion to the policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/assign.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (paletteNode == null || assertionNode == null) {
                    throw new IllegalStateException();
                }
                assertionNode.receive(paletteNode);
            }
        });
    }
}

