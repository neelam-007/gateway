/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * About Box for the client.
 */
public class AboutBox extends JDialog {
    public AboutBox() {
        super(Gui.getInstance().getFrame(), "About the " + Gui.APP_NAME);
        setModal(true);
        Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());
        JLabel thinger = new JLabel(IconManager.getSmallSplashImageIcon());
        thinger.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        pane.add(thinger,
                 new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(8, 20, 0, 20), 0, 0));

        JLabel version = new JLabel(BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber());
        pane.add(version,
                 new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets(0, 20, 8, 0), 0, 0));

        JButton okButton = new JButton(" Close ");
        getRootPane().setDefaultButton(okButton);

        final Action closeAction = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        AboutBox.this.dispose();
                    }
                };
        okButton.addActionListener(closeAction);
        Utilities.runActionOnEscapeKey(getRootPane(), closeAction);
        pane.add(okButton,
                 new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,
                                        GridBagConstraints.SOUTHEAST,
                                        GridBagConstraints.NONE,
                                        new Insets(5, 0, 8, 20), 0, 0));
        pack();
        Utilities.centerOnScreen(this);
    }

    /*
    public static void main(String[] args) {
        Gui.setInstance(Gui.createGui(null, new SsgManagerStub()));
        new AboutBox().show();
        System.exit(1);
    }
    */
}
