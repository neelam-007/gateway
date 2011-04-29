package com.l7tech.console.action;

import java.awt.Frame;
import java.net.PasswordAuthentication;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.security.auth.login.LoginException;
import javax.swing.*;

import com.l7tech.console.panels.ChangePasswordDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

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
    @Override
    public String getName() {
        return "Change Password";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Change Password";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * Method will display the change password dialog to force the user to change their password.  The change password
     * dialog has exactly the same behaviour as the usual change password behaviour with the exception that the cancel
     * button and the "x" button on the top right will disconnect from SSG.
     */
    public void doForceChangePasswordAction() {
        Registry registry = Registry.getDefault();
        String msg = "Password expired, please change your password.";
        if (registry != null && registry.isAdminContextPresent()) {
            Frame owner = TopComponents.getInstance().getTopParent();
            SecurityProvider securityProvider = registry.getSecurityProvider();

            if (securityProvider != null) {
                User user = securityProvider.getUser();

                if ( user != null && user.getLogin() != null ) {
                    changePassword(owner, user, securityProvider, msg, true);
                }
            }
        }

    }

    /**
     * 
     */
    @Override
    protected void performAction() {
        Registry registry = Registry.getDefault();

        if (registry != null && registry.isAdminContextPresent()) {
            Frame owner = TopComponents.getInstance().getTopParent();
            SecurityProvider securityProvider = registry.getSecurityProvider();

            if (securityProvider != null) {
                User user = securityProvider.getUser();

                if (user != null && user.getLogin()!=null) {
                    changePassword(owner, user, securityProvider, null, false);
                }
            }
        }
    }

    private void changePassword(final Frame owner,
                                final User user,
                                final SecurityProvider securityProvider,
                                final String message,
                                final boolean modifyCancelBehaviour) {
        changePassword(owner, user, securityProvider, message, modifyCancelBehaviour, false);
    }

    private void changePassword(final Frame owner,
                                final User user,
                                final SecurityProvider securityProvider,
                                final String message,
                                final boolean modifyCancelBehaviour,
                                final boolean reentrant) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(!reentrant){
                    //check if the user has a cert, don't show if user has already seen this message whilst trying to change their password
                    try {
                        final boolean userHasCert = Registry.getDefault().getIdentityAdmin().doesCurrentUserHaveCert();
                        if(userHasCert){
                            Locale locale = Locale.getDefault();
                            ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PasswordDialog", locale);

                            int res = JOptionPane.showConfirmDialog(
                                    null,
                                    resources.getString("revokeClientCertCurrentUser.question"),
                                    resources.getString("revokeClientCert.title"),
                                    JOptionPane.YES_NO_OPTION);
                            if (res != JOptionPane.YES_OPTION) {
                                return;
                            }

                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Could not check if user '" + user.getLogin() + "'has a cert: " +
                                ExceptionUtils.getMessage(e));
                    } catch (AuthenticationException e) {
                        logger.log(Level.WARNING, "Could not check if user '" + user.getLogin() + "'has a cert: " +
                                ExceptionUtils.getMessage(e));
                    }
                }

                final ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog(owner, message, user.getLogin(), false);
                if ( modifyCancelBehaviour ) changePasswordDialog.changeCancelBehaviourToDisconnectBehaviour();
                changePasswordDialog.pack();
                changePasswordDialog.setModal(true);
                Utilities.centerOnScreen(changePasswordDialog);
                DialogDisplayer.display(changePasswordDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (changePasswordDialog.wasOk()) {
                            PasswordAuthentication oldPass = changePasswordDialog.getCurrentPasswordAuthentication();
                            PasswordAuthentication newPass = changePasswordDialog.getNewPasswordAuthentication();
                            try {
                                securityProvider.changePassword(oldPass, newPass);
                            }
                            catch(LoginException le) {
                                changePassword(owner, user, securityProvider, "Incorrect password.", modifyCancelBehaviour, true);
                            }
                            catch(InvalidPasswordException iae) {
                                changePassword(owner, user, securityProvider, formatErrors( iae ), modifyCancelBehaviour, true);
                            }
                            catch(IllegalStateException iae) {
                                DialogDisplayer.showMessageDialog(owner, "Error changing password.", iae.getMessage(), null);
                            }
                            catch(Exception e) {
                                ErrorManager.getDefault().notify(Level.WARNING, e, "Error while changing password.");
                            }
                        } else if ( !changePasswordDialog.wasOk() && !changePasswordDialog.isCancel() &&
                                !changePasswordDialog.isHelp() && modifyCancelBehaviour) {
                            TopComponents tc = TopComponents.getInstance();
                            if ( !tc.isDisconnected() ){
                                tc.disconnectFromGateway();
                            }
                        }
                    }
                });
            }
        });
    }

    public static String formatErrors( final InvalidPasswordException e ) {
        if ( e.getPasswordErrors().isEmpty() ) {
            return "Invalid password '"+e.getMessage()+"'.";
        } else if ( e.getPasswordErrors().size() == 1 ) {
            return "Invalid password '"+e.getPasswordErrors().iterator().next()+"'.";
        } else {
            return "<html>Invalid password:<ul><li>" + TextUtils.join( "</li><li>", e.getPasswordErrors() ) + "</li></ul></html>";
        }
    }
}
