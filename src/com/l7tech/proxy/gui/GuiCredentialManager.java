/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.dialogs.LogonDialog;
import com.l7tech.proxy.gui.dialogs.PleaseWaitDialog;
import com.l7tech.proxy.gui.dialogs.TrustCertificateDialog;
import com.l7tech.proxy.util.ClientLogger;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * GUI implementation of the CredentialManager.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:36:01 AM
 */
public class GuiCredentialManager extends CredentialManager {
    private static final ClientLogger log = ClientLogger.getInstance(GuiCredentialManager.class);
    private PleaseWaitDialog pleaseWaitDialog;
    private SsgManager ssgManager;

    private GuiCredentialManager(SsgManager ssgManager) {
        this.ssgManager = ssgManager;
    }

    public static GuiCredentialManager createGuiCredentialManager(SsgManager ssgManager) {
        return new GuiCredentialManager(ssgManager);
    }

    /**
     * Invoke the specified runnable and wait for it to finish.  If this is the event dispatch thread,
     * just runs it; otherwise uses SwingUtilities.invokeAndWait()
     * @param runnable  code that displays a modal dialog
     */
    private void invokeDialog(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                log.error("GuiCredentialManager: thread interrupted; reasserting interrupt and continuing");
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                log.error("GuiCredentialManager: dialog code threw an exception; continuing", e);
            }
        }
    }

    private static class CredHolder {
        private PasswordAuthentication pw = null;
        private String showUsername = null;
        private boolean lockUsername = false;
    }

    public PasswordAuthentication getCredentials(final Ssg ssg) throws OperationCanceledException {
        for (;;) {
            try {
                return getCredentials(ssg, false);
            } catch (KeyStoreCorruptException e) {
                notifyKeyStoreCorrupt(ssg);
                SsgKeyStoreManager.deleteStores(ssg);
                // FALLTHROUGH -- retry with newly-emptied keystore
            }
        }
    }

    public PasswordAuthentication getNewCredentials(final Ssg ssg) throws OperationCanceledException {
        for (;;) {
            try {
                return getCredentials(ssg, true);
            } catch (KeyStoreCorruptException e) {
                notifyKeyStoreCorrupt(ssg);
                SsgKeyStoreManager.deleteStores(ssg);
                // FALLTHROUGH -- retry with newly-emptied keystore
            }
        }
    }

    private PasswordAuthentication getCredentials(final Ssg ssg, final boolean oldOnesWereBad)
            throws OperationCanceledException, KeyStoreCorruptException
    {
        PasswordAuthentication pw = ssg.getCredentials();
        if (!oldOnesWereBad && pw != null)
            return pw;

        // If this SSG isn't supposed to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword()) {
            log.info("Logon prompts disabled for Gateway " + ssg);
            throw new OperationCanceledException("Logon prompts are disabled for Gateway " + ssg);
        }

        long now = System.currentTimeMillis();
        final CredHolder holder = new CredHolder();

        synchronized (ssg) {
            pw = ssg.getCredentials();
            if (ssg.credentialsUpdatedTime() > now && pw != null)
                return pw;

            // Check if username is locked into a client certificate
            holder.showUsername = ssg.getUsername();
            if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                X509Certificate cert = SsgKeyStoreManager.getClientCert(ssg);
                X500Principal certName = new X500Principal(cert.getSubjectDN().toString());
                String certNameString = certName.getName(X500Principal.RFC2253);
                holder.showUsername = certNameString.substring(3);
                ssg.setUsername(holder.showUsername);
                holder.lockUsername = true;
            }
        }

        log.info("Displaying logon prompt for Gateway " + ssg);
        invokeDialog(new Runnable() {
            public void run() {
                PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(),
                                                              ssg.toString(),
                                                              holder.showUsername,
                                                              holder.lockUsername,
                                                              oldOnesWereBad);
                if (pw == null) {
                    if (ssg.incrementNumTimesLogonDialogCanceled() > 2) {
                        // This is the second time we've popped up a logon dialog and the user has impatiently
                        // canceled it.  We can take a hint -- we'll turn off logon prompts until the proxy is
                        // restarted or the user manually changes the password.
                        ssg.promptForUsernameAndPassword(false);
                    }
                    return;
                }
                ssg.setUsername(pw.getUserName());
                ssg.cmPassword(pw.getPassword()); // TODO: encoding?
                ssg.onCredentialsUpdated();
                ssg.promptForUsernameAndPassword(true);
                holder.pw = pw;

                try {
                    ssgManager.save();
                } catch (IOException e) {
                    log.error("Unable to save Gateway configuration: ", e);
                    Gui.errorMessage("Unable to save Gateway configuration",
                                     "An error was encountered while writing your Gateway configuration to disk.",
                                     e);
                }
            }
        });

        if (holder.pw == null) {
            log.info("User canceled logon dialog for Gateway " + ssg);
            throw new OperationCanceledException("User canceled logon dialog");
        }

        log.info("New credentials noted for Gateway " + ssg);
        return holder.pw;
    }

    private static class DestructionFlag {
        public boolean destroyKeystore = false;
    }

    /**
     * Notify the user that the key store for the given Ssg has been damaged.
     * This should _not_ be used in cases where the user merely mistyped the password to decrypt his
     * private key.  The user should be given the option of either canceling, which will abort the operation,
     * or continuing, which will cause the invalid keystore to be deleted (which will require the SSG admin to revoke
     * the client cert, if the only copy of it's private key is now gone).
     *
     * Whatever decision the user makes should be remembered for the rest of this session.
     *
     * @param ssg
     * @throws OperationCanceledException if the user does not wish to delete the invalid keystore
     */
    public void notifyKeyStoreCorrupt(final Ssg ssg) throws OperationCanceledException {
        final DestructionFlag df = new DestructionFlag();
        df.destroyKeystore = false;

        long now = System.currentTimeMillis();

        // Avoid spamming the user with reports
        if (System.currentTimeMillis() > now + 1000)
            return;
        invokeDialog(new Runnable() {
            public void run() {
                String msg = "The certificate and/or key store for the Gateway " + ssg +
                        "\n is irrepairably damaged.\n\n" +
                        "Do you want to delete and rebuild them?";
                Gui.getInstance().getFrame().toFront();
                Object[] certoptions = { "Destroy Certificate and Key Stores", "Cancel" };                
                int result = JOptionPane.showOptionDialog(Gui.getInstance().getFrame(),
                                                          msg,
                                                          "Key Store Corrupt",
                                                          JOptionPane.YES_NO_CANCEL_OPTION,
                                                          JOptionPane.ERROR_MESSAGE,
                                                          null,
                                                          certoptions,
                                                          certoptions[1]);
                if (result == 0)
                    df.destroyKeystore = true;
            }
        });

        if (!df.destroyKeystore)
            throw new OperationCanceledException("KeyStore is corrupt, but user does not want to delete it");
    }

    /**
     * Notify the user that a client certificate has already been issued for his account.
     * At this point there is nothing the user can do except try a different account, or contact
     * his Gateway administrator and beg to have the lost certificate revoked from the database.
     *
     * @param ssg
     */
    public void notifyCertificateAlreadyIssued(final Ssg ssg) {
        // If this SSG isn't suppose to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword())
            return;

        long now = System.currentTimeMillis();

        // Avoid spamming the user with reports
        if (System.currentTimeMillis() > now + 1000)
            return;

        invokeDialog(new Runnable() {
            public void run() {
                Gui.errorMessage("You need a client certificate to communicate with the Gateway " + ssg + ", \n" +
                                 "but it has already issued a client certificate to this account and cannot issue\n" +
                                 "a second one.  If you have lost your client certificate, you will need to\n" +
                                 "contact your Gateway administrator and have them revoke your old one before\n" +
                                 "you can obtain a new one.");
            }
        });
    }

    /**
     * Notify the user that an SSL connection to the SSG could not be established because the hostname did not match
     * the one in the certificate.
     *
     * @param ssg
     * @param whatWeWanted  the expected hostname, equal to ssg.getSsgAddress()
     * @param whatWeGotInstead  the hostname in the peer's certificate
     */
    public void notifySsgHostnameMismatch(final Ssg ssg, final String whatWeWanted, final String whatWeGotInstead) {
        // If this SSG isn't suppose to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword())
            return;

        long now = System.currentTimeMillis();

        // Avoid spamming the user with reports
        if (System.currentTimeMillis() > now + 1000)
            return;

        invokeDialog(new Runnable() {
            public void run() {
                Gui.errorMessage(
                        "<HTML>The configured hostname for the Gateway " + ssg + " is \"" + whatWeWanted + "\", <br>" +
                        "but the server presented a certificate claiming its hostname is \"" + whatWeGotInstead + "\". <br>" +
                        "<p>Please double check the Gateway hostname for the Gateway " + ssg + ".");

            }
        });
    }

    public void notifySsgCertificateUntrusted(Ssg ssg, final X509Certificate certificate) throws OperationCanceledException {
        String mess = "The authenticity of this Gateway server certificate could not be verified using the " +
                "current username and password.  Do you want to trust the Gateway " + ssg + " using this server certificate?  " +
                "If you don't know, click Cancel.";
        final TrustCertificateDialog tcd = new TrustCertificateDialog(certificate,
                                                                      "Trust this Gateway certificate?",
                                                                      mess);
        invokeDialog(new Runnable() {
            public void run() {
                tcd.show();
            }
        });
        if (!tcd.isTrusted())
            throw new OperationCanceledException("The downloaded Gateway server certificate could not be verified with the current username and password.");
    }

    private PleaseWaitDialog getPleaseWaitDialog() {
        if (pleaseWaitDialog == null) {
            pleaseWaitDialog = new PleaseWaitDialog();
            Utilities.centerOnScreen(pleaseWaitDialog);
        }

        return pleaseWaitDialog;
    }

    public void notifyLengthyOperationStarting(Ssg ssg, final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getPleaseWaitDialog().setMessage(message);
                getPleaseWaitDialog().show();
            }
        });
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getPleaseWaitDialog().hide();
            }
        });
    }
}
