/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;

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
        if (!ssg.promptForUsernameAndPassword())
            return false;

        final PromptState promptState = new PromptState();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(), ssg.toString());
                    if (pw == null) {
                        promptState.credentialsObtained = false;
                        if (ssg.incrementNumTimesLogonDialogCanceled() > 2) {
                            ssg.promptForUsernameAndPassword(false);
                            return;
                        }
                    }
                    ssg.setUsername(pw.getUserName());
                    ssg.password(pw.getPassword()); // TODO: encoding?
                    promptState.credentialsObtained = true;
                }
            });
        } catch (InterruptedException e) {
            log.error(e);
        } catch (InvocationTargetException e) {
            log.error(e);
        }

        return promptState.credentialsObtained;
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
