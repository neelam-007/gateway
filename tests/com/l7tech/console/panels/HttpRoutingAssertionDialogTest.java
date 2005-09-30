/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.identity.StubDataStore;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.util.Collection;

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
        owner.setVisible(true);

        HttpRoutingAssertion a = new HttpRoutingAssertion();
        StubDataStore dataStore = StubDataStore.defaultStore();
        Collection services = dataStore.getPublishedServices().values();
        if (services.isEmpty()) {
            throw new RuntimeException("Emtpy Stub DataStore");
        }
        PublishedService svc = (PublishedService)services.iterator().next();
        HttpRoutingAssertionDialog d = new HttpRoutingAssertionDialog(owner, a, svc);
        d.setModal(true);
        d.pack();
        d.setVisible(true);
    }

}
