/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.CertUtils;
import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.dialogs.LogonDialog;
import com.l7tech.proxy.gui.dialogs.PleaseWaitDialog;
import com.l7tech.proxy.gui.dialogs.TrustCertificateDialog;
import com.l7tech.proxy.ssl.SslPeer;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI implementation of the CredentialManager.
 */
class GuiCredentialManager extends CredentialManager {
    private static final Logger log = Logger.getLogger(GuiCredentialManager.class.getName());
    private PleaseWaitDialog pleaseWaitDialog;
    private SsgManager ssgManager;

    private GuiCredentialManager(SsgManager ssgManager) {
        this.ssgManager = ssgManager;
    }

    static GuiCredentialManager createGuiCredentialManager(SsgManager ssgManager) {
        return new GuiCredentialManager(ssgManager);
    }

    /**
     * Invoke the specified runnable and wait for it to finish.  If this is the event dispatch thread,
     * just runs it; otherwise uses SwingUtilities.invokeAndWait()
     * @param runnable  code that displays a modal dialog
     */
    private void invokeOnSwingThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                log.log(Level.WARNING, "GuiCredentialManager: thread interrupted; reasserting interrupt and continuing");
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                log.log(Level.WARNING, "GuiCredentialManager: dialog code threw an exception; continuing", e);
            }
        }
    }

    private static class CredHolder {
        private PasswordAuthentication pw = null;
        private String showUsername = null;
        private boolean lockUsername = false;
    }

    public PasswordAuthentication getCredentials(final Ssg ssg) throws OperationCanceledException {
        return getCredentials(ssg, "", false, false);
    }

    public PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg,
                                                               CredentialManager.ReasonHint hint,
                                                               boolean disregardExisting,
                                                               boolean reportBadPassword)
            throws OperationCanceledException
    {
        return getCredentials(ssg, hint.toString(), disregardExisting, reportBadPassword);
    }

    public PasswordAuthentication getNewCredentials(final Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
        return getCredentials(ssg, "", true, displayBadPasswordMessage);
    }

    private PasswordAuthentication getCredentials(final Ssg ssg, final String reasonHint, boolean mustGetNewOnes, final boolean oldOnesWereBad)
            throws OperationCanceledException
    {
        if (oldOnesWereBad) mustGetNewOnes = true;

        if (ssg.isFederatedGateway())
            throw new OperationCanceledException("Not permitted to send password credentials to a Federated Gateway");

        PasswordAuthentication pw = ssg.getRuntime().getCredentials();
        if (!mustGetNewOnes && pw != null)
            return pw;

        // If this SSG isn't supposed to be hassling us with dialog boxes, stop now
        if (!ssg.getRuntime().promptForUsernameAndPassword()) {
            log.info("Logon prompts disabled for Gateway " + ssg);
            throw new OperationCanceledException("Logon prompts are disabled for Gateway " + ssg);
        }

        long now = System.currentTimeMillis();
        final CredHolder holder = new CredHolder();

        synchronized (ssg) {
            pw = ssg.getRuntime().getCredentials();
            if (ssg.getRuntime().getCredentialsLastUpdatedTime() > now && pw != null)
                return pw;

            // Check if username is locked into a client certificate
            holder.showUsername = ssg.getUsername();
            final X509Certificate cert = ssg.getClientCertificate();
            if (cert != null) {
                holder.showUsername = CertUtils.extractCommonNameFromClientCertificate(cert);
                holder.lockUsername = true;
            }
        }

        log.info("Displaying logon prompt for Gateway " + ssg);
        invokeOnSwingThread(new Runnable() {
            public void run() {
                PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(),
                                                              ssg.toString(),
                                                              holder.showUsername,
                                                              holder.lockUsername,
                                                              oldOnesWereBad,
                                                              reasonHint);
                if (pw == null) {
                    if (ssg.getRuntime().incrementNumTimesLogonDialogCanceled() > 2) {
                        // This is the second time we've popped up a logon dialog and the user has impatiently
                        // canceled it.  We can take a hint -- we'll turn off logon prompts until the proxy is
                        // restarted or the user manually changes the password.
                        ssg.getRuntime().promptForUsernameAndPassword(false);
                    }
                    return;
                }
                ssg.setUsername(pw.getUserName());
                ssg.getRuntime().setCachedPassword(pw.getPassword()); // TODO: encoding?
                ssg.getRuntime().onCredentialsUpdated();
                ssg.getRuntime().promptForUsernameAndPassword(true);
                holder.pw = pw;

                doSsgManagerSave();
            }
        });

        if (holder.pw == null) {
            log.info("User canceled logon dialog for Gateway " + ssg);
            throw new OperationCanceledException("User canceled logon dialog");
        }

        log.info("New credentials noted for Gateway " + ssg);
        ssg.getRuntime().resetSslContext();
        return holder.pw;
    }

    public void saveSsgChanges(Ssg ssg) {
        // Ignore ssg and just save them all, since it's all we can do anyway
        invokeOnSwingThread(new Runnable() {
            public void run() {
                doSsgManagerSave();
            }
        });
    }

    private void doSsgManagerSave() {
        try {
            ssgManager.save();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to save Gateway configuration: ", e);
            Gui.errorMessage("Unable to save Gateway configuration",
                             "An error was encountered while writing your Gateway configuration to disk.",
                             e);
        }
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
        invokeOnSwingThread(new Runnable() {
            public void run() {
                String msg = "Your password is incorrect for this client certificate, or the certificate\n" +
                        "and/or key store for the Gateway " + ssg +
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

        if (!df.destroyKeystore) {
            ssg.getRuntime().setCachedPassword(null); // forget cached credentials
            throw new OperationCanceledException("Unable to read the key store.");
        }
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
        if (!ssg.getRuntime().promptForUsernameAndPassword())
            return;

        long now = System.currentTimeMillis();

        // Avoid spamming the user with reports
        if (System.currentTimeMillis() > now + 1000)
            return;

        invokeOnSwingThread(new Runnable() {
            public void run() {
                Gui.errorMessage("You need a client certificate to communicate with the Gateway " + ssg + ", \n" +
                                 "but it has already issued a client certificate to this account and cannot issue\n" +
                                 "a second one.  If you have lost your client certificate, you will need to\n" +
                                 "contact your Gateway administrator and have them revoke your old one before\n" +
                                 "you can obtain a new one.");
            }
        });
    }

    public void notifySslHostnameMismatch(final String server, final String whatWeWanted, final String whatWeGotInstead) {
        long now = System.currentTimeMillis();

        // Avoid spamming the user with reports
        if (System.currentTimeMillis() > now + 1000)
            return;

        invokeOnSwingThread(new Runnable() {
            public void run() {
                Gui.errorMessage(
                        "<HTML>The configured hostname for " + server + " is \"" + whatWeWanted + "\", <br>" +
                        "but the server presented a certificate claiming its hostname is \"" + whatWeGotInstead + "\". <br>" +
                        "<p>Please double check the hostname for " + server + ".");

            }
        });
    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, final X509Certificate untrustedCertificate) throws OperationCanceledException {
        final String msg = "The authenticity of the SSL server certificate for " + serverDesc + " could not be automatically established.  ";
        String mess = msg +
                "Do you want to trust " + serverDesc + " using this server certificate?  " +
                "If you don't know, click Cancel.";
        final TrustCertificateDialog tcd = new TrustCertificateDialog(untrustedCertificate,
                                                                      "Trust Certificate for " + serverDesc,
                                                                      mess);
        invokeOnSwingThread(new Runnable() {
            public void run() {
                tcd.show();
            }
        });
        if (!tcd.isTrusted())
            throw new OperationCanceledException(msg);
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
