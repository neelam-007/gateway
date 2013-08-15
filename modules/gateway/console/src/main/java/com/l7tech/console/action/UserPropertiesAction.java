package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.FederatedUserPanel;
import com.l7tech.console.panels.GenericUserPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.util.NoSuchElementException;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class UserPropertiesAction extends NodeAction {

    public UserPropertiesAction(UserNode node) {
        super(node, SpecificUser.class);
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
                EntityHeader eh = ((EntityHeaderNode) node).getEntityHeader();
                IdentityHeader header;
                if (eh instanceof IdentityHeader) {
                    header = (IdentityHeader) eh;
                } else {
                    header = new IdentityHeader(config.getGoid(), eh);
                }
                UserPanel panel;
                if (UserPropertiesAction.this instanceof GenericUserPropertiesAction) {
                    panel = new GenericUserPanel();
                } else if (UserPropertiesAction.this instanceof FederatedUserPropertiesAction) {
                    panel = new FederatedUserPanel();
                } else {
                    throw new RuntimeException("Unsupported UserPropertiesAction.");
                }

                Frame f = TopComponents.getInstance().getTopParent();
                EditorDialog dialog = new EditorDialog(f, panel, true);
                try {
                    panel.edit(header, config);
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    DialogDisplayer.display(dialog);
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

    private IdentityProviderConfig config;
}
