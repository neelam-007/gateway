/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog box that says "Please wait..."
 */
public class PleaseWaitDialog extends JDialog {
    private JLabel messageLabel;
    private String initialMess = "";
    private static final String MESS = "Please wait...";

    public PleaseWaitDialog(Dialog dialog) throws HeadlessException {
        super(dialog, MESS, false);
        doInit("", null);
    }

    public PleaseWaitDialog(JFrame parentFrame) throws HeadlessException {
        super(parentFrame, MESS, false);
        doInit("", null);
    }

    public PleaseWaitDialog(Dialog dialog, String message) throws HeadlessException {
        super(dialog, MESS, false);
        doInit(message, null);
    }

    public PleaseWaitDialog(JFrame parentFrame, String message) throws HeadlessException {
        super(parentFrame, MESS, false);
        doInit(message, null);
    }

    public PleaseWaitDialog(Dialog dialog, String message, JProgressBar progressBar) throws HeadlessException {
        super(dialog, MESS, false);
        doInit(message, progressBar);
    }

    public PleaseWaitDialog(JFrame parentFrame, String message, JProgressBar progressBar) throws HeadlessException {
        super(parentFrame, MESS, false);
        doInit(message, progressBar);
    }

    private void doInit(String initialMess, JProgressBar progressBar) {
        this.initialMess = initialMess == null ? "" : initialMess;

        final boolean progress = progressBar != null;

        this.setUndecorated(progress);
        this.setFocusableWindowState(false);
        JPanel c = new JPanel();
        this.setContentPane(c);
        c.setLayout(new GridBagLayout());
        c.add(getMessageLabel(),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(15, 8, 15, 8), 0, 0));

        if (progress) {
            c.add(progressBar,
                  new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 1000.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(5, 15, 5, 15), 0, 0));
            c.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));
        }

        pack();
    }

    private JLabel getMessageLabel() {
        if (messageLabel == null) {
            messageLabel = new JLabel(initialMess);
        }
        return messageLabel;
    }

    public void setMessage(String mess) {
        getMessageLabel().setText(mess);
        pack();
    }
}
