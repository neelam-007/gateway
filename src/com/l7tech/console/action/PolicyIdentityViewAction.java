package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.MainWindow;
import com.l7tech.service.PublishedService;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;


/**
 * The <code>PolicyIdentityViewAction</code> action switches the policy
 * view to the identity based policy view.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyIdentityViewAction extends BaseAction {

    public PolicyIdentityViewAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Identity view";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View the identity policy view";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/identity.png";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
    }
}
