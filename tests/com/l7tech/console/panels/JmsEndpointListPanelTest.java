/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Interactive test for the JmsEndpoingListPanel gui widget.
 */
public class JmsEndpointListPanelTest {
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
        JFrame main = new JFrame("JmsEndpointListPanelTest");
        Container p = main.getContentPane();
        p.setLayout(new GridBagLayout());

        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setName("IBM MQSeries 4.2.1 on data.l7tech.com");

        p.add(new JLabel("Blah blah endpoint on the JMS connection " + jmsConnection.getName() + "?"),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.NONE,
                                     new Insets(15, 15, 0, 15), 0, 0));
        final JmsEndpointListPanel jelp = new JmsEndpointListPanel(main, jmsConnection);
        p.add(jelp,
              new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(0, 0, 0, 0), 0, 0));

        JButton checkButton = new JButton("Check");
        checkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Selected connection: " + jelp.getSelectedJmsEndpoint());
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
