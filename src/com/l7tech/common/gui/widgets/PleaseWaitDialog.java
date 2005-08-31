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

    public PleaseWaitDialog(JFrame parentFrame) {
        super(parentFrame, "Please wait...", false);
        this.setFocusableWindowState(false);
        Container c = this.getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(getMessageLabel(),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(15, 8, 15, 8), 0, 0));
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
