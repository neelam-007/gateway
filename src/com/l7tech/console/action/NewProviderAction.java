package com.l7tech.console.action;

import com.l7tech.console.panels.NewProviderDialog;
import com.l7tech.console.tree.AbstractTreeNode;

/**
 * The <code>NewProviderAction</code> action adds the new provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class NewProviderAction extends NodeAction {

    public NewProviderAction(AbstractTreeNode node) {
        super(node);
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "New provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create new identioty provider";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/providers16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        NewProviderDialog dialog = new NewProviderDialog(null);
        dialog.setResizable(false);
        dialog.show();
    }
}
