package com.l7tech.console.action;

import com.l7tech.console.panels.Utilities;
import com.l7tech.console.panels.FindDialog;
//import com.l7tech.console.tree.policy.IdentityPolicyView;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import java.awt.*;
import java.util.logging.Logger;


/**
 * The <code>FindIdentityAction</code> action invokes the searche identity
 * dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class FindIdentityAction extends BaseAction {
    static final Logger log = Logger.getLogger(FindIdentityAction.class.getName());


    /**
     * create the action
     */
    public FindIdentityAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Find";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Find Identities";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Find16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        Frame f =
          Registry.getDefault().getWindowManager().getMainWindow();
        FindDialog fd = new FindDialog(f, true);
        fd.pack();
        Utilities.centerOnScreen(fd);
        fd.show();
     /*   Frame f = Registry.getDefault().getWindowManager().getMainWindow();
        IdentityPolicyView pw = new IdentityPolicyView(f, assertion);
        pw.pack();
        Utilities.centerOnScreen(pw);
        pw.show();*/
    }
}
