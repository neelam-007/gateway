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

    /**
     * Load credentials for this SSG.  If the SSG already contains credentials they will be
     * overwritten with new ones.  For the GUI, we'll pop up a login window on the main Swing thread
     * and hold here until it finishes.
     * @param ssg
     */
    public void getCredentials(final Ssg ssg) {
        // If this SSG isn't suppose to be hassling us with dialog boxes, stop now
        if (!ssg.isPromptForUsernameAndPassword())
            return;

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(), ssg.toString());
                    if (pw == null) {
                        if (ssg.incrementNumTimesLogonDialogCanceled() > 2) {
                            ssg.setPromptForUsernameAndPassword(false);
                            return;
                        }
                    }
                    ssg.setUsername(pw.getUserName());
                    ssg.setPassword(new String(pw.getPassword())); // TODO: encoding?
                }
            });
        } catch (InterruptedException e) {
            log.error(e);
        } catch (InvocationTargetException e) {
            log.error(e);
        }
    }

    /**
     * Notify that the credentials for this SSG have been tried and found to be no good.
     * In the GUI, for now we'll just pop up an error dialog (yuck).
     */
    public void notifyInvalidCredentials(final Ssg ssg) {
        // If this SSG isn't suppose to be hassling us with dialog boxes, stop now
        if (!ssg.isPromptForUsernameAndPassword())
            return;

        // Don't hassle if the username is unset or empty, or if the password is unset.
        // (Configuring an empty password when the service requires a different one should cause notification)
        if (ssg.getUsername() == null || ssg.getUsername().length() < 1)
            return;
        if (ssg.getPassword() == null)
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

    // Avoid pestering the user incessantly when the cert is updated.
    static long lastHassle = 0;
    static int timesHassled = 0;

    /**
     * Notify that the client must be restarted for a certificate change to take effect.
     */
    public synchronized void notifyCertificateUpdated(final Ssg ssg) {
        long now = System.currentTimeMillis();
        if (timesHassled > 2 || now - lastHassle < 1000 * 15)
            return;

        timesHassled++;
        lastHassle = now;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Gui.getInstance().errorMessage("Security certificate was updated for SSG " + ssg + ".\n" +
                                               "The Client Proxy must be restarted for the changes to take effect.");
            }
        });
    }
}
