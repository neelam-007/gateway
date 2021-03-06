package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import org.junit.Ignore;

import javax.swing.*;
import java.util.Collection;

/**
 * Standalone GUI test harness for the HttpRoutingAssertionDialog.  Runs in stub mode.
 * @author mike
 */
@Ignore("Developer Test")
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
        HttpRoutingAssertionDialog d = new HttpRoutingAssertionDialog(owner, a, svc.getPolicy(), svc.parsedWsdl(), false);
        d.setModal(true);
        d.pack();
        d.setVisible(true);
    }

}
