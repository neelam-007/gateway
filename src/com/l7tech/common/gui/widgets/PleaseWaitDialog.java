/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

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
        doInit();
    }

    public PleaseWaitDialog(JFrame parentFrame) throws HeadlessException {
        super(parentFrame, MESS, false);
        doInit();
    }

    public PleaseWaitDialog(Dialog dialog, String message) throws HeadlessException {
        super(dialog, MESS, false);
        initialMess = message;
        doInit();
    }

    public PleaseWaitDialog(JFrame parentFrame, String message) throws HeadlessException {
        super(parentFrame, MESS, false);
        initialMess = message;
        doInit();
    }

    private void doInit() {
        this.setFocusableWindowState(false);
        Container c = this.getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(getMessageLabel(),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(15, 8, 15, 8), 0, 0));
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
