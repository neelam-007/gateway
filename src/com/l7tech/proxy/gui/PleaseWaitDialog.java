/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog box that says "Please wait..."
 * User: mike
 * Date: Aug 18, 2003
 * Time: 2:59:32 PM
 */
public class PleaseWaitDialog extends JDialog {
    private JLabel messageLabel;

    public PleaseWaitDialog() {
        super(Gui.getInstance().getFrame(), "Please wait...", false);
        this.setFocusableWindowState(false);
        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());
        c.add(getMessageLabel(), BorderLayout.CENTER);
    }

    private JLabel getMessageLabel() {
        if (messageLabel == null) {
            messageLabel = new JLabel();
        }
        return messageLabel;
    }

    public void setMessage(String mess) {
        getMessageLabel().setText(mess);
        pack();
    }
}
