package com.l7tech.console.action;

import com.l7tech.console.panels.NewGroupDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * The <code>NewGroupAction</code> action adds the new group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewGroupAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewGroupAction.class.getName());

    public NewGroupAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Internal Identity Provider group";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/group16.png";
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
                JFrame f = TopComponents.getInstance().getMainWindow();
                NewGroupDialog dialog = new NewGroupDialog(f, getIdentityProviderConfig((EntityHeaderNode) node));
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }
}
