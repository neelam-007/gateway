/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Test for JmsConnectionListPanel
 *
 */
public class JmsConnectionListPanelTest {
    private static void setLnf() throws Exception {
        UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < feels.length; i++) {
            UIManager.LookAndFeelInfo feel = feels[i];
            if (feel.getName().indexOf("indows") >= 0) {
                UIManager.setLookAndFeel(feel.getClassName());
                break;
            }
        }
    }

    public static void main(String[] args) {
        try {
            doMain();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void doMain() throws Exception {
        // Use stub mode
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        setLnf();
        JFrame main = new JFrame("JmsConnectionListPanelTest");
        Container p = main.getContentPane();
        p.setLayout(new GridBagLayout());
        p.add(new JLabel("Blah blah JMS connection blah blah blah?"),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.NONE,
                                     new Insets(15, 15, 0, 15), 0, 0));
        final JmsConnectionListPanel jpl = new JmsConnectionListPanel(main);
        p.add(jpl,
              new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(0, 0, 0, 0), 0, 0));

        JButton checkButton = new JButton("Check");
        checkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Selected connection: " + jpl.getSelectedJmsConnection());
            }
        });
        p.add(checkButton,
              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 0, 0), 0, 0));

        main.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        main.pack();
        Utilities.centerOnScreen(main);
        main.show();
    }
}
