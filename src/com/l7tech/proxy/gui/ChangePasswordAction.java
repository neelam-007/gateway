/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.BadPasswordFormatException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.dialogs.PasswordDialog;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.util.SslUtils;

import javax.net.ssl.SSLException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side action that requests the Gateway to change a user's password and revoke their client cert (if any).
 */
class ChangePasswordAction extends AbstractAction {
    private static final Logger log = Logger.getLogger(ChangePasswordAction.class.getName());

    private SsgListPanel ssgListPanel;

    public ChangePasswordAction(SsgListPanel ssgListPanel) {
        super("Change Password/Revoke Certificate", IconManager.getCert());
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
                            title = "Revoke Client Certificate";
                            message = "You are about to send a request to\n" +
                                    "the Gateway \"" + ssg + "\" for it\n" +
                                    "to change your password for this account\n" +
                                    "and revoke your current Client Certificate.\n" +
                                    "Are you sure you wish to proceed?\n\n" +
                                    "This action cannot be undone.";
                        } else {
                            certoptions = new Object[] {"Change Password", "Cancel"};
                            title = "Change Password";
                            message = "You are about to send a request to\n" +
                                    "the Gateway \"" + ssg + "\" for it\n" +
                                    "to change your password for this account.\n" +
                                    "Are you sure you wish to proceed?\n\n" +
                                    "This action cannot be undone.";
                        }
                        int res2 = JOptionPane.showOptionDialog(null, message, title,
                                                                0, JOptionPane.WARNING_MESSAGE,
                                                                null, certoptions, certoptions[1]);
                        if (res2 != 0)
                            return;

                        prompted = true;
                    }

                    if (oldpass == null)
                        oldpass = ssg.getRuntime().getCredentialManager().getNewCredentials(ssg, false);
                    cmPasswdPotentiallyChanged = true;
                    if (newpass == null)
                        newpass = PasswordDialog.getPassword(Gui.getInstance().getFrame(), "New password");
                    if (newpass == null)
                        return;

                    SslUtils.changePasswordAndRevokeClientCertificate(ssg,
                                                                      oldpass.getUserName(),
                                                                      oldpass.getPassword(),
                                                                      newpass);

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
                } catch (OperationCanceledException e1) {
                    return;
                } catch (SSLException sslException) {
                    PasswordAuthentication credentials = null;
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
                        Gui.errorMessage("Unable to change your password", "Unable to negotiate an SSL connection " +
                                                                           "with the Gateway.", e1);
                        return;
                    }
                } catch (IOException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password", "The Gateway was unable to change " +
                                                                       "your password.", e1);
                    return;
                } catch (BadCredentialsException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to change your password -- the Gateway reports that your current " +
                                     "password or client certificate is not valid.");
                    return;
                } catch (KeyStoreException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    Gui.errorMessage("Unable to remove your existing client certificate",
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
