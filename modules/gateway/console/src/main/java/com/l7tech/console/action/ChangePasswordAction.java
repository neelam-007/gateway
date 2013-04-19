package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.ChangePasswordDialog;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.awt.*;
import java.net.PasswordAuthentication;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

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
        return "My Account";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "View admin account info and roles and optionally change password";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    @Override
    public boolean isAuthorized() {
        boolean isAuthorized = false;
        try {
            isAuthorized = Registry.getDefault().isAdminContextPresent() && Registry.getDefault().getIdentityAdmin().currentUsersPasswordCanBeChanged();
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "Problem authorizing change password action: " + ExceptionUtils.getMessage(e));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Problem authorizing change password action: " + ExceptionUtils.getMessage(e));
        }

        return isAuthorized;
    }

    /**
     * 
     */
    @Override
    protected void performAction() {
        Registry registry = Registry.getDefault();

        if (registry.isAdminContextPresent()) {
            Frame owner = TopComponents.getInstance().getTopParent();
            SecurityProvider securityProvider = registry.getSecurityProvider();

            if (securityProvider != null) {
                User user = securityProvider.getUser();

                if (user != null && user.getLogin()!=null) {
                    showMyAccountPropertiesDialog(owner, user, securityProvider, null, false);
                }
            }
        }
    }

    private void showMyAccountPropertiesDialog(final Frame owner,
                                               final User user,
                                               final SecurityProvider securityProvider,
                                               final String message,
                                               final boolean modifyCancelBehaviour) {
        showMyAccountPropertiesDialog(owner, user, securityProvider, message, modifyCancelBehaviour, false);
    }

    private void showMyAccountPropertiesDialog(final Frame owner,
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
                                showMyAccountPropertiesDialog(owner, user, securityProvider, "Incorrect password.", modifyCancelBehaviour, true);
                            }
                            catch(InvalidPasswordException iae) {
                                showMyAccountPropertiesDialog(owner, user, securityProvider, formatErrors(iae), modifyCancelBehaviour, true);
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
