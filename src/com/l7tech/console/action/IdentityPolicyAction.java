package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.policy.IdentityAssertionTreeNode;
import com.l7tech.console.tree.policy.IdentityPolicyView;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * The <code>IdentityPolicyAction</code> action views the identity
 * policy for user or group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityPolicyAction extends SecureAction {
    static final Logger log = Logger.getLogger(IdentityPolicyAction.class.getName());
    IdentityAssertionTreeNode assertion;

    /**
     * create the action that shows the identity assertion policy
     * @param ia the identity assertion
     */
    public IdentityPolicyAction(IdentityAssertionTreeNode ia) {
        assertion = ia;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "View Identity policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View Identity policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        try {
            Frame f = TopComponents.getInstance().getMainWindow();
            IdentityPolicyView pw = new IdentityPolicyView(f, assertion);
            pw.pack();
            Utilities.centerOnScreen(pw);
            pw.show();
        } catch (FindException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
