package com.l7tech.console.action;

import com.l7tech.console.panels.NewGroupDialog;
import com.l7tech.console.tree.AbstractTreeNode;

/**
 * The <code>NewGroupAction</code> action adds the new group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class NewGroupAction extends NodeAction {

    public NewGroupAction(AbstractTreeNode node) {
        super(node);
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create new group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create new group";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/group16.png";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        NewGroupDialog dialog = new NewGroupDialog(null);
        dialog.setResizable(false);
        dialog.show();
    }
}
