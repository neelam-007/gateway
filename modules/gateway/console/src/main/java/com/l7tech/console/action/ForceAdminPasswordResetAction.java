package com.l7tech.console.action;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * The <code>ForceAdminPasswordResetAction</code> forces all administrative users in the
 * identity provider to reset password
 *
 * @author wlui
 */
public class ForceAdminPasswordResetAction extends SecureAction {
    public ForceAdminPasswordResetAction() {
        super(new AttemptedUpdate(EntityType.USER, new InternalUser()), LIC_AUTH_ASSERTIONS);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Force Administrative Passwords Reset";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Force all administrative users in the identity provider to reset their passwords";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Refresh16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {

        Frame f = TopComponents.getInstance().getTopParent();
        int result = JOptionPane.showConfirmDialog(
                f, "Are you sure you want to force all administrative users in the identity provider to reset their passwords ", getName(), JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            try {
                getIdentityAdmin().forceAdminUsersResetPassword(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
            } catch (ObjectModelException e) {
                logger.log(Level.WARNING, "Failed to force password change: " + ExceptionUtils.getMessage(e), e);
                DialogDisplayer.showMessageDialog(f, "Failed to force password change: " + ExceptionUtils.getMessage(e), "Edit Failed", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }
}
