/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.AccessControlException;

/**
 * Simple "Please Wait.." dialog.
 * @author mike
 * @version 1.0
 */
public class CancelableOperationDialog extends JDialog {
    private final JLabel messageLabel = new JLabel();

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
    }

    public String getMessage() {
        return messageLabel.getText();
    }
}
