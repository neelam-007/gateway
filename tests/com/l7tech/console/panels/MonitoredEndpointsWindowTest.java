/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;

/**
 * @author mike
 */
public class MonitoredEndpointsWindowTest {
    public static void main(String[] args) {
        try {
            realMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void realMain() throws Exception {
        final JFrame owner = new JFrame("main");
        owner.show();
        MonitoredEndpointsWindow w = new MonitoredEndpointsWindow(owner);
        w.show();
    }
}
