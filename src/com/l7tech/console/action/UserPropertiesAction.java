package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.GenericUserPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.panels.FederatedUserPanel;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.util.NoSuchElementException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class UserPropertiesAction extends NodeAction {

    public UserPropertiesAction(UserNode node) {
        super(node);
    }

   /**
     * @return the action name
     */
    public String getName() {
        return "User Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit User Properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
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
                if (config == null) {
                    config = getIdentityProviderConfig((EntityHeaderNode) node);
                }
                EntityHeader header = ((EntityHeaderNode) node).getEntityHeader();

                UserPanel panel;
                if (UserPropertiesAction.this instanceof GenericUserPropertiesAction) {
                    panel = new GenericUserPanel();
                } else if (UserPropertiesAction.this instanceof FederatedUserPropertiesAction) {
                    panel = new FederatedUserPanel();
                } else {
                    throw new RuntimeException("Unsupported UserPropertiesAction.");
                }

                JFrame f = TopComponents.getInstance().getMainWindow();
                EditorDialog dialog = new EditorDialog(f, panel);
                try {
                    panel.edit(header, config);
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    dialog.show();
                } catch (NoSuchElementException e) {
                    // Bugzilla #801 - removing the user from the tree should not be performed
                    // removeUserFromTree(header);
                }
            }
        });
    }


    public void setIdProviderConfig(IdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * Return the required roles for this action
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }

    private IdentityProviderConfig config;
}
