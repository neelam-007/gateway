package com.l7tech.console.action;

import java.awt.Frame;
import java.net.PasswordAuthentication;
import java.util.logging.Level;

import javax.security.auth.login.LoginException;

import com.l7tech.console.panels.ChangePasswordDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.identity.User;

/**
 * The <code>ChangePasswordAction</code> changes the administrators password.
 *
 * @author Steve Jones
 */
public class ChangePasswordAction extends SecureAction {

    /**
     * 
     */
    public ChangePasswordAction() {
        super(null);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Change Password";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Change Password";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * 
     */
    protected void performAction() {
        Registry registry = Registry.getDefault();

        if (registry != null && registry.isAdminContextPresent()) {
            Frame owner = TopComponents.getInstance().getTopParent();
            SecurityProvider securityProvider = registry.getSecurityProvider();

            if (securityProvider != null) {
                User user = securityProvider.getUser();

                if (user != null && user.getLogin()!=null) {
                    changePassword(owner, user, securityProvider, null);
                }
            }
        }
    }

    private void changePassword(final Frame owner,
                                final User user,
                                final SecurityProvider securityProvider,
                                final String message) {
        final ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog(owner, message, user.getLogin(), false);
        changePasswordDialog.pack();
        Utilities.centerOnScreen(changePasswordDialog);
        DialogDisplayer.display(changePasswordDialog, new Runnable() {
            public void run() {
                if (changePasswordDialog.wasOk()) {
                    PasswordAuthentication oldPass = changePasswordDialog.getCurrentPasswordAuthentication();
                    PasswordAuthentication newPass = changePasswordDialog.getNewPasswordAuthentication();
                    try {
                        securityProvider.changePassword(oldPass, newPass);
                    }
                    catch(LoginException le) {
                        changePassword(owner, user, securityProvider, "Incorrect password.");    
                    }
                    catch(IllegalArgumentException iae) {
                        changePassword(owner, user, securityProvider, "Invalid password '"+iae.getMessage()+"'.");    
                    }
                    catch(IllegalStateException iae) {
                        DialogDisplayer.showMessageDialog(owner, "Error changing password.", iae.getMessage(), null);
                    }
                    catch(Exception e) {
                        ErrorManager.getDefault().notify(Level.WARNING, e, "Error while changing password.");
                    }
                }
            }
        });
    }
}
