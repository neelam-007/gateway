package com.l7tech.console.action;

import com.l7tech.console.panels.PolicyAddIdentitiesDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;


/**
 * The <code>AddIdentityAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddIdentityAssertionAction extends BaseAction {
    private AssertionTreeNode node;

    public AddIdentityAssertionAction(AssertionTreeNode n) {
        node = n;
         if (!(node.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Add user or group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add user or group to the policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                JFrame f =
                          Registry.getDefault().getWindowManager().getMainWindow();
                JDialog d = new PolicyAddIdentitiesDialog(f, node);
                d.pack();
                Utilities.centerOnScreen(d);
                d.show();
            }
        });
    }
}
