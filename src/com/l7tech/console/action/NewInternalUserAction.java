package com.l7tech.console.action;

import com.l7tech.console.panels.NewInternalUserDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * The <code>NewInternalUserAction</code> action adds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewInternalUserAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewInternalUserAction.class.getName());

    public NewInternalUserAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Internal User";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Internal Identity Provider user";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();
                NewInternalUserDialog dialog = new NewInternalUserDialog(f);
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }
}

