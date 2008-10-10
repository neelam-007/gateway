/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.CertUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.BadPasswordFormatException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.client.gui.dialogs.ChangePasswordDialog;
import com.l7tech.client.gui.util.IconManager;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.util.SslUtils;

import javax.net.ssl.SSLException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side action that requests the Gateway to change a user's password and revoke their client cert (if any).
 */
class ChangePasswordAction extends AbstractAction {
    private static final Logger log = Logger.getLogger(ChangePasswordAction.class.getName());

    private SsgListPanel ssgListPanel;

    public ChangePasswordAction(SsgListPanel ssgListPanel) {
        super("Change Account Password", IconManager.getCert());
        this.ssgListPanel = ssgListPanel;
    }

    public void actionPerformed(final ActionEvent e) {
        final Ssg ssg = ssgListPanel.getSelectedSsg();
        if (ssg == null) return;
        final char[] currentCMPasswd = ssg.getRuntime().getCachedPassword();
        boolean cmPasswdPotentiallyChanged = false;
        boolean changeCompleted = false;
        if (ssg != null) {
            if (ssg.isFederatedGateway()) {
                Gui.errorMessage("This is a Federated Gateway.  You must perform this\n" +
                                 "action on the corresponding Trusted Gateway instead." );
                return;
            }

            boolean retry = true;
            boolean prompted = false;
            PasswordAuthentication oldpass = null;
            char[] newpass = null;
            while (retry) {
                retry = false;
                try {
                    if (!prompted) {
                        Object[] certoptions;
                        String title;
                        String message;
                        if (ssg.getClientCertificate() != null) {
                            certoptions = new Object[] {"Revoke Certificate", "Cancel"};
                            title = "Change Account Password";
                            message = "WARNING: Changing your account password\n" +
                                      "on gateway \"" + ssg + "\" will also revoke\n" +
                                      "your current client certificate and cannot\n" +
                                      "be undone. Are you sure you wish to proceed?";

                        } else {
                            certoptions = new Object[] {"Change Password", "Cancel"};
                            title = "Change Account Password";
                            message = "WARNING: Changing your account password\n" +
                                      "on gateway \"" + ssg + "\" cannot be undone.\n" +
                                      "Are you sure you wish to proceed?";
                        }
                        int res2 = JOptionPane.showOptionDialog(null, message, title,
                                                                0, JOptionPane.WARNING_MESSAGE,
                                                                null, certoptions, certoptions[1]);
                        if (res2 != 0)
                            return;

                        prompted = true;
                    }

                    String username = ssg.getUsername();
                    boolean usernameEditable = true;
                    final X509Certificate cert = ssg.getClientCertificate();
                    if (cert != null) {
                        try {
                            username = CertUtils.extractSingleCommonNameFromCertificate(cert);
                            usernameEditable = false;
                        } catch (CertUtils.MultipleCnValuesException e1) {
                            // Custom cert.  Fallthrough and let username be editable.
                        }
                    }
                    String message = "Change password for gateway: " + ssg.getSsgAddress();
                    ChangePasswordDialog cpd = new ChangePasswordDialog(Gui.getInstance().getFrame(), message, username, usernameEditable);
                    //Bug 5416 - it will re-display the change password dialog due to server cert untrusted exception
                    //but since we still have the new pass and old pass info already, we dont need to ask for it again
                    if (newpass == null && oldpass == null) {
                        cpd.setVisible(true);
                    }

                    if (cpd.wasOk()) {
                        cmPasswdPotentiallyChanged = true;
                        oldpass = cpd.getCurrentPasswordAuthentication();
                        newpass = cpd.getNewPasswordAuthentication().getPassword();
                        if (usernameEditable && (username==null || !username.equals(oldpass.getUserName()))) {
                            ssg.setUsername(oldpass.getUserName());
                        }
                        ssg.getRuntime().setCachedPassword(oldpass.getPassword());
                    } else if (newpass != null && oldpass != null) {    //bug 5416
                        cmPasswdPotentiallyChanged = true;
                        if (usernameEditable && (username==null || !username.equals(oldpass.getUserName()))) {
                            ssg.setUsername(oldpass.getUserName());
                        }
                        ssg.getRuntime().setCachedPassword(oldpass.getPassword());
                    } else {
                        return;
                    }

                    try {
                        SslUtils.changePasswordAndRevokeClientCertificate(ssg,
                                                                          oldpass.getUserName(),
                                                                          oldpass.getPassword(),
                                                                          newpass);
                    } catch (IOException e1) {
                        // un-hide wrapped SSLException so server cert disco can be triggered if needed.
                        SSLException sse = ExceptionUtils.getCauseIfCausedBy(e1, SSLException.class);
                        if (sse != null)
                            throw (SSLException)new SSLException(ExceptionUtils.getMessage(sse)).initCause(e1);
                        throw e1;
                    }

                    // Succeeded, so update password and client cert
                    ssg.getRuntime().setCachedPassword(newpass);
                    ssg.getRuntime().getSsgKeyStoreManager().deleteClientCert();
                    SsgManagerImpl.getSsgManagerImpl().save();
                    changeCompleted = true;
                } catch (KeyStoreCorruptException e1) {
                    try {
                        ssg.getRuntime().handleKeyStoreCorrupt();
                        retry = true;
                        // FALLTHROUGH -- retry with newly-emptied keystore
                    } catch (OperationCanceledException e2) {
                        return; // cancel the password change as well
                    }
                } catch (SSLException sslException) {
                    PasswordAuthentication credentials;
                    try {
                        credentials = ssg.getRuntime().getCredentialManager().getCredentials(ssg);
                        MessageProcessor.handleSslException(ssg, credentials, sslException);
                        retry = true;
                        // FALLTHROUGH -- retry with newly-(re?)learned server certificate

                    } catch (OperationCanceledException e1) {
                        return;
                    } catch (BadCredentialsException e1) {
                        log.log(Level.WARNING, e1.getMessage(), e1);
                        Gui.errorMessage("Unable to change your password -- the Gateway reports that your\ncurrent " +
                                         "password or client certificate is not valid.");
                        return;
                    } catch (Exception e1) {
                        log.log(Level.WARNING, e1.getMessage(), e1);
                        Gui.errorMessage("Unable to change your password",
                                         "Password change failed.",
                                         "Unable to negotiate an SSL connection with the Gateway.", e1);
                        return;
                    }
                } catch (IOException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password",
                                     "Password change failed.",
                                     "The Gateway was unable to change your password.", e1);
                    return;
                } catch (BadCredentialsException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password -- the Gateway reports that your current " +
                                     "password or client certificate is not valid.");
                    return;
                } catch (KeyStoreException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.criticalErrorMessage("Unable to remove your existing client certificate",
                                     "There was an error while attempting to remove our local copy of your " +
                                     "newly-revoked client certificate.", e1);
                    return;
                } catch (BadPasswordFormatException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password. The Gateway has rejected your " +
                                     "new password. \n" + e1.getMessage());
                    return;
                } catch (SslUtils.PasswordNotWritableException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password -- the Gateway is unable to change " +
                                     "the password for this account");
                    return;
                } finally {
                    CurrentSslPeer.clear();
                    if (!changeCompleted && cmPasswdPotentiallyChanged && currentCMPasswd != null) {
                        ssg.getRuntime().setCachedPassword(currentCMPasswd);
                    }
                }
            }

            JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                          "Your password has been changed successfully.",
                                          "Password change succeeded",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
