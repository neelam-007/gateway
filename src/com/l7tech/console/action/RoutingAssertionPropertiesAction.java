package com.l7tech.console.action;

import com.l7tech.console.panels.IdentityProviderDialog;
import com.l7tech.console.panels.RoutingAssertionDialog;
import com.l7tech.console.panels.Utilities;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.policy.RoutingAssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;

/**
 * The <code>RoutingAssertionPropertiesAction</code> edits the
 * protected service properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RoutingAssertionPropertiesAction extends NodeAction {

    public RoutingAssertionPropertiesAction(RoutingAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Routing properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit routing properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
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
                JFrame f = Registry.getDefault().getWindowManager().getMainWindow();
                RoutingAssertionDialog d =
                  new RoutingAssertionDialog(f, (RoutingAssertion)node.asAssertion());
                d.pack();
                Utilities.centerOnScreen(d);
                d.show();
            }
        });
    }
}
