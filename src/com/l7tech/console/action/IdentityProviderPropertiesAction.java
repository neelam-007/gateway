package com.l7tech.console.action;

import com.l7tech.console.panels.IdentityProviderDialog;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.util.Registry;

import javax.swing.*;

/**
 * The <code>IdentityProviderPropertiesAction</code> edits the
 * identity provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPropertiesAction extends NodeAction {

    public IdentityProviderPropertiesAction(ProviderNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Provider properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit provider properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
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
                IdentityProviderDialog d =
                  new IdentityProviderDialog(f, ((EntityHeaderNode)node).getEntityHeader());
                d.setResizable(false);
                d.show();
            }
        });
    }
}
