/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import java.net.PasswordAuthentication;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Category;

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
    public boolean getCredentials(final Ssg ssg) {
        // If this SSG isn't supposed to be hassling us with dialog boxes, stop now
        if (!ssg.promptForUsernameAndPassword()) {
            log.info("Logon prompts disabled for SSG " + ssg);
            return false;
        }

        long now = System.currentTimeMillis();
        synchronized(this) {
            // If another instance already updated the credentials while we were waiting, we've done our job
            if (ssg.credentialsUpdatedTime() > now)
                return true;

            final PromptState promptState = new PromptState();
            log.info("Displaying logon prompt for SSG " + ssg);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(), ssg.toString(), ssg.getUsername());
                        if (pw == null) {
                            promptState.credentialsObtained = false;
                            if (ssg.incrementNumTimesLogonDialogCanceled() > 2) {
                                ssg.promptForUsernameAndPassword(false);
                                return;
                            }
                        }
                        ssg.setUsername(pw.getUserName());
                        ssg.password(pw.getPassword()); // TODO: encoding?
                        ssg.onCredentialsUpdated();
                        promptState.credentialsObtained = true;
                    }
                });
            } catch (InterruptedException e) {
                log.error(e);
            } catch (InvocationTargetException e) {
                log.error(e);
            }

            log.info(promptState.credentialsObtained ? "New credentials noted for SSG " + ssg
                                                     : "User canceled logon dialog for SSG " + ssg);
            return promptState.credentialsObtained;
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
