package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.TreeNode;

/**
 * The <code>UserPropertiesAction</code> edits the user entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class UserPropertiesAction extends NodeAction {

    public UserPropertiesAction(UserNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "User properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit user properties";
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
                if (idProvider == null) {
                    idProvider = getIdentityProvider((EntityHeaderNode)node);
                }
                EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                UserPanel panel = new UserPanel();
                JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
                EditorDialog dialog = new EditorDialog(f, panel);

                panel.edit(header, idProvider);
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.show();
            }
        });
    }

    public void setIdProvider(IdentityProvider idProvider) {
        this.idProvider = idProvider;
    }

    private IdentityProvider idProvider;
}
