/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.gui.Gui;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * User: mike
 * Date: Sep 15, 2003
 * Time: 3:14:26 PM
 */
public class AgentAboutBox extends JDialog {
    public AgentAboutBox() {
        super(Gui.getInstance().getFrame(), "About " + Gui.APP_NAME);
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
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AgentAboutBox.this.dispose();
            }
        });
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
        new AgentAboutBox().show();
        System.exit(1);
    }
    */
}
