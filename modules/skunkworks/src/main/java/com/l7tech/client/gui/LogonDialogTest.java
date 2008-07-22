/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.client.gui;

import com.l7tech.client.ClientProxy;
import com.l7tech.proxy.datamodel.SsgManager;
//import com.l7tech.proxy.SsgManagerStub;
import com.l7tech.client.gui.dialogs.LogonDialog;
import com.l7tech.client.gui.Gui;

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
        SsgManager ssgManager = null;//new SsgManagerStub();
        final ClientProxy clientProxy = new ClientProxy(ssgManager, null, 0, 0, 0);
        Gui.setInstance(Gui.createGui(new Gui.GuiParams(ssgManager, 0)));
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
