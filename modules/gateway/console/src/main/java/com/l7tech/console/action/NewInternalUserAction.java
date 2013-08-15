package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.NewInternalUserDialog;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>NewInternalUserAction</code> action adds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewInternalUserAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewInternalUserAction.class.getName());
    private AttemptedCreateSpecific attemptedCreateUser;

    public NewInternalUserAction(IdentityProviderNode node) {
        super(node, SpecificUser.class, null);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create User";
    }

    @Override
    public boolean isAuthorized() {
        if (attemptedCreateUser == null) {
            attemptedCreateUser = new AttemptedCreateSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "<new user>"));
        }
        return canAttemptOperation(attemptedCreateUser);
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
                Frame f = TopComponents.getInstance().getTopParent();
                NewInternalUserDialog dialog = new NewInternalUserDialog(f);
                dialog.setResizable(false);
                DialogDisplayer.display(dialog);
            }
        });
    }
}

