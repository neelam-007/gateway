/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;

/**
 * Standalone GUI test harness for the JmsEndpointsWindow.  Runs in stub mode.
 * @author mike
 */
public class JmsEndpointsWindowTest {
    public static void main(String[] args) {
        try {
            realMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void realMain() throws Exception {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final JFrame owner = new JFrame("main");
        owner.show();
        JmsEndpointsWindow w = new JmsEndpointsWindow(owner);
        w.show();
    }
}
