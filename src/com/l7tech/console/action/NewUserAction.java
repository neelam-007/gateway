package com.l7tech.console.action;

import com.l7tech.console.panels.NewUserDialog;
import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.*;

/**
 * The <code>NewUserAction</code> action adds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class NewUserAction extends NodeAction {

    public NewUserAction(AbstractTreeNode node) {
        super(node);
        this.node = node;
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
        return "com/l7tech/console/resources/New16.gif";
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
                NewUserDialog dialog = new NewUserDialog(null);
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }
}
