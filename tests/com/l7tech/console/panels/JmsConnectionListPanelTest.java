/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

    public static void main(String[] args) throws Exception {
        // Use stub mode
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        setLnf();
        JFrame main = new JFrame("JmsConnectionListPanelTest");
        Container p = main.getContentPane();
        p.setLayout(new GridBagLayout());
        JmsConnectionListPanel jpl = new JmsConnectionListPanel();
        p.add(jpl,
              new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(0, 0, 0, 0), 0, 0));

        main.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        main.pack();
        main.show();
    }
}
