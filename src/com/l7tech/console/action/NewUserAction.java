package com.l7tech.console.action;

import com.l7tech.console.panels.NewUserDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * The <code>NewUserAction</code> action adds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewUserAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewUserAction.class.getName());

    public NewUserAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create new user";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create new user";
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
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f =
                  Registry.getDefault().getComponentRegistry().getMainWindow();
                NewUserDialog dialog = new NewUserDialog(f);
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }
}

