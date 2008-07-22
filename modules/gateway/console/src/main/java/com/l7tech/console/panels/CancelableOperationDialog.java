/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.lang.reflect.InvocationTargetException;

/**
 * Simple "Please Wait.." dialog.
 * @author mike
 * @version 1.0
 */
public class CancelableOperationDialog extends JDialog {
    private final JLabel messageLabel = new JLabel();
    private boolean wasCancel;

    public static CancelableOperationDialog newCancelableOperationDialog(Component component, String title, String message) {
        CancelableOperationDialog dialog;

        Window window = SwingUtilities.getWindowAncestor(component);
        if (window instanceof Dialog) {
            dialog = new CancelableOperationDialog((Dialog)window, title, message);
        }
        else {
            dialog = new CancelableOperationDialog((Frame)window, title, message);
        }

        Utilities.centerOnParentWindow(dialog);

        return dialog;
    }

    public CancelableOperationDialog(Frame owner, String title, String message, JProgressBar progressBar) {
        super(owner, title, true);
        doInit(message, progressBar);
    }

    public CancelableOperationDialog(Dialog parent, String title, String message) {
        super(parent, title, true);
        doInit(message, null);
    }

    public CancelableOperationDialog(Frame parent, String title, String message) {
        super(parent, title, true);
        doInit(message, null);
    }

    private void doInit(String message, JProgressBar progressBar) {
        wasCancel = false;

        setResizable(false);
        Utilities.setAlwaysOnTop(this, true);

        Container p = getContentPane();
        p.setLayout(new GridBagLayout());
        messageLabel.setText(message);
        p.add(messageLabel,
              new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.NONE,
                                     new Insets(15, 25, 15, 25), 0, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wasCancel = true;
                CancelableOperationDialog.this.dispose();
            }
        });
        p.add(cancelButton,
              new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 5), 0, 0));
        if (progressBar != null) {
            p.add(progressBar,
                  new GridBagConstraints(1, 2, GridBagConstraints.REMAINDER, 1, 1000.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(5, 15, 5, 15), 0, 0));
        }

        pack();
        Utilities.centerOnScreen(this);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
        pack();                             // Bug 3686
        Utilities.centerOnScreen(this);
    }

    public String getMessage() {
        return messageLabel.getText();
    }

    public boolean wasCancelled() {
        return wasCancel;
    }

    /**
     * Synchronously run the specified callable in a background thread, putting up a modal Cancel... dialog and returning
     * control to the user if the thread runs for longer than msBeforeDlg milliseconds.
     * <p/>
     * If the user cancels the dialog this will interrupt the background thread.  The callable may throw
     * InterruptedException to signal that this has occurred.
     * <p/>
     * This method must be called on the Swing event queue thread.
     * <p/>
     * This method makes use of {@link Utilities#doWithDelayedCancelDialog(java.util.concurrent.Callable, javax.swing.JDialog, long)}.
     *
     * @param callable      some work that may safely be done in a new thread.  Required.
     * @param dialogTitle   the title to display for the cancel dialog, if one is put up.  Required
     * @param dialogMessage the message to display inside the cancel dialog, if one is put up.  Required
     * @param msBeforeDlg  number of milliseconds to wait (blocking the event queue) before
     *                                            putting up the cancel dialog.  If less than one, defaults to 500ms.
     * @return the result of the callable.  May be null if the callable may return null.
     * @throws InterruptedException if the task was canceled by the user, or the Swing thread was interrupted
     * @throws java.lang.reflect.InvocationTargetException if the callable terminated with any exception other than InterruptedException
     */
    public static <T> T doWithDelayedCancelDialog(final Callable<T> callable, String dialogTitle, String dialogMessage, long msBeforeDlg)
            throws InterruptedException, InvocationTargetException
    {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        final CancelableOperationDialog cancelDialog =
                new CancelableOperationDialog(null, dialogTitle, dialogMessage, progressBar);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnScreen(cancelDialog);

        return Utilities.doWithDelayedCancelDialog(callable, cancelDialog, msBeforeDlg);
    }
}
