/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;

import javax.swing.*;

import org.springframework.context.ApplicationContext;

/**
 * Standalone GUI test harness for the JmsRoutingAssertionDialog.  Runs in stub mode.
 * @author mike
 */
public class JmsRoutingAssertionDialogTest {
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

        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        Registry.setDefault(new RegistryStub());

        final JFrame owner = new JFrame("main");
        owner.setVisible(true);

        JmsRoutingAssertion a = new JmsRoutingAssertion();
        JmsRoutingAssertionDialog d = new JmsRoutingAssertionDialog(owner, a, false);
        d.setModal(true);
        d.pack();
        d.setVisible(true);
    }
}
