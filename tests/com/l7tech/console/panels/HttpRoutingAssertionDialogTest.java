/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.console.Main;
import com.l7tech.common.ApplicationContexts;

import javax.swing.*;
import javax.security.auth.Subject;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.security.PrivilegedAction;

/**
 * Standalone GUI test harness for the HttpRoutingAssertionDialog.  Runs in stub mode.
 * @author mike
 */
public class HttpRoutingAssertionDialogTest {
    public static void main(String[] args) {
        try {
            Registry.setDefault(new RegistryStub());
            realMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void realMain() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final JFrame owner = new JFrame("main");
        owner.show();

        HttpRoutingAssertion a = new HttpRoutingAssertion();
        EntityHeader eh = new EntityHeader("1", EntityType.SERVICE, "TestService", "This is a test service");
        ServiceNode sn = new ServiceNode(eh);
        HttpRoutingAssertionDialog d = new HttpRoutingAssertionDialog(owner, a, sn);
        d.setModal(true);
        d.pack();
        d.show();
    }

}
