/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.gui.dialogs.LogonDialog;

import javax.swing.*;
import java.awt.*;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * Test the logon dialog
 * User: mike
 * Date: Sep 3, 2003
 * Time: 12:53:01 PM
 */
public class LogonDialogTest {
    private static final Logger log = Logger.getLogger(LogonDialogTest.class.getName());

    public static void main(String[] args) {
        SsgManager ssgManager = new SsgManagerStub();
        Gui.setInstance(Gui.createGui(new ClientProxy(ssgManager, null, 0, 0, 0), ssgManager));
        try {
            JFrame frame = new JFrame() {
                public Dimension getPreferredSize() {
                    return new Dimension(200, 100);
                }
            };
            frame.setTitle("Debugging frame");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new JTextField());
            frame.pack();
            frame.setVisible(true);
            Thread.sleep(1000);
            PasswordAuthentication pw = LogonDialog.logon(Gui.getInstance().getFrame(),
                                                          "Testing123Ssg (Default)",
                                                          "Testuser",
                                                          true,
                                                          true,
                                                          false ? "for me to vigorously poop on " : ""); // test longish hint string
            if (pw != null) {
                log.info("Got user name=" + pw.getUserName());
                log.info("Got password=" + new String(pw.getPassword()));
            } else {
                log.info("Dialog was canceled");
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
