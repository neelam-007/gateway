/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import org.apache.log4j.Category;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.PasswordAuthentication;

/**
 * GUI implementation of the CredentialManager.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:36:01 AM
 */
public class GuiCredentialManager implements CredentialManager {
    private static final Category log = Category.getInstance(GuiCredentialManager.class);
    private static GuiCredentialManager INSTANCE = new GuiCredentialManager();
    private PleaseWaitDialog pleaseWaitDialog;

    private GuiCredentialManager() {}

    public static GuiCredentialManager getInstance() {
        return INSTANCE;
    }

    private static class PromptState {
        public boolean credentialsObtained = false;
    }

    /**
     * Load credentials for this SSG.  If the SSG already contains credentials they will be
     * overwritten with new ones.  For the GUI, we'll pop up a login window on the main Swing thread
     * and hold here until it finishes.
     * @param ssg
     */
    public void getCredentials(final Ssg ssg) throws OperationCanceledException {
        // If this SSG isn't supposed to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword()) {
            log.info("Logon prompts disabled for SSG " + ssg);
            throw new OperationCanceledException("Logon prompts are disabled for SSG " + ssg);
        }

        long now = System.currentTimeMillis();
        synchronized(this) {
            // If another instance already updated the credentials while we were waiting, we've done our job
            if (ssg.credentialsUpdatedTime() > now)
                return;

            final PromptState promptState = new PromptState();
            log.info("Displaying logon prompt for SSG " + ssg);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(), ssg.toString(), ssg.getUsername());
                        if (pw == null) {
                            promptState.credentialsObtained = false;
                            if (ssg.incrementNumTimesLogonDialogCanceled() > 2) {
                                // This is the second time we've popped up a logon dialog and the user has impatiently
                                // canceled it.  We can take a hint -- we'll turn off logon prompts until the proxy is
                                // restarted or the user manually changes the password.
                                ssg.promptForUsernameAndPassword(false);
                                return;
                            }
                        }
                        ssg.setUsername(pw.getUserName());
                        ssg.password(pw.getPassword()); // TODO: encoding?
                        ssg.onCredentialsUpdated();
                        ssg.promptForUsernameAndPassword(true);
                        promptState.credentialsObtained = true;
                    }
                });
            } catch (InterruptedException e) {
                log.error(e);
            } catch (InvocationTargetException e) {
                log.error(e);
            }

            if (promptState.credentialsObtained)
                log.info("New credentials noted for SSG " + ssg);
            else{
                log.info("User canceled logon dialog for SSG " + ssg);
                throw new OperationCanceledException("User canceled logon dialog");
            }
        }
    }

    /**
     * Notify that the credentials for this SSG have been tried and found to be no good.
     * In the GUI, for now we'll just pop up an error dialog (yuck).
     */
    public void notifyInvalidCredentials(final Ssg ssg) {
        // If this SSG isn't suppose to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword())
            return;

        // Don't hassle if the username is unset or empty, or if the password is unset.
        // (Configuring an empty password when the service requires a different one should cause notification)
        if (ssg.getUsername() == null || ssg.getUsername().length() < 1)
            return;
        if (ssg.password() == null)
            return;

        long now = System.currentTimeMillis();
        synchronized(this) {

            // Avoid spamming the user with reports
            if (System.currentTimeMillis() > now + 1000)
                return;

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        Gui.getInstance().errorMessage("Invalid username or password for SSG " + ssg);
                    }
                });
            } catch (InterruptedException e) {
                log.error(e);
            } catch (InvocationTargetException e) {
                log.error(e);
            }
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
        synchronized(this) {

            // Avoid spamming the user with reports
            if (System.currentTimeMillis() > now + 1000)
                return;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        String msg = "The key store for the Gateway " + ssg + "\n is irrepairably damaged.\n\n" +
                                "Do you wish to delete it and build a new one?";

                        Gui.getInstance().getFrame().toFront();
                        int result = JOptionPane.showOptionDialog(Gui.getInstance().getFrame(),
                                                                  msg,
                                                                  "Key Store Corrupt",
                                                                  JOptionPane.YES_NO_CANCEL_OPTION,
                                                                  JOptionPane.ERROR_MESSAGE,
                                                                  null,
                                                                  null,
                                                                  null);
                        if (result == 0)
                            df.destroyKeystore = true;
                    }
                });
            } catch (InterruptedException e) {
                log.error(e);
            } catch (InvocationTargetException e) {
                log.error(e);
            }

            if (!df.destroyKeystore)
                throw new OperationCanceledException("KeyStore is corrupt, but user does not wish to delete it");
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
        if (!ssg.promptForUsernameAndPassword())
            return;

        long now = System.currentTimeMillis();
        synchronized(this) {

            // Avoid spamming the user with reports
            if (System.currentTimeMillis() > now + 1000)
                return;

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        Gui.getInstance().errorMessage("You need a client certificate to communicate with the Gateway " + ssg + ", \n" +
                                                       "but it has already issed a client certificate to this account and cannot issue\n" +
                                                       "a second one.  If you have lost your client certificate, you will need to\n" +
                                                       "contact your Gateway administrator and have them revoke your old one before\n" +
                                                       "you can obtain a new one.");
                    }
                });
            } catch (InterruptedException e) {
                log.error(e);
            } catch (InvocationTargetException e) {
                log.error(e);
            }
        }
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
