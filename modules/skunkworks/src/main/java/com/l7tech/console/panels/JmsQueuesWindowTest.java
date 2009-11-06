/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import org.springframework.context.ApplicationContext;

import javax.swing.*;

//import com.l7tech.server.ApplicationContexts;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;

/**
 * Standalone GUI test harness for the JmsEndpointsWindow.  Runs in stub mode.
 * @author mike
 */
public class JmsQueuesWindowTest {
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
        ApplicationContext applicationContext =  null;//ApplicationContexts.getTestApplicationContext();
        Registry.setDefault(new RegistryStub());
        final JFrame owner = new JFrame("main");
        owner.setVisible(true);
        JmsQueuesWindow w = new JmsQueuesWindow(owner);
        w.setVisible(true);
    }
}
