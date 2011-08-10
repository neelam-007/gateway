package com.l7tech.console.panels;

import com.l7tech.util.SyspropUtil;
import org.junit.Ignore;

import javax.swing.*;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;

/**
 * Standalone GUI test harness for the JmsEndpointsWindow.  Runs in stub mode.
 * @author mike
 */
@Ignore("Developer Test")
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
        SyspropUtil.setProperty( "com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator" );
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Registry.setDefault(new RegistryStub());
        final JFrame owner = new JFrame("main");
        owner.setVisible(true);
        JmsQueuesWindow w = new JmsQueuesWindow(owner);
        w.setVisible(true);
    }
}
