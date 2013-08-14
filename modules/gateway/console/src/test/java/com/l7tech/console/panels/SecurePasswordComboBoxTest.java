package com.l7tech.console.panels;

import com.l7tech.console.TrustedCertAdminStub;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;
import com.l7tech.test.util.GuiTestException;
import com.l7tech.test.util.GuiTestLauncher;
import com.l7tech.test.util.GuiTestMethod;
import org.junit.Ignore;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.ServiceLoader;

/**
 *
 */
@Ignore("This is an interactive test")
public class SecurePasswordComboBoxTest {
    public static void main(String[] args) throws GuiTestException {
        ServiceLoader<GuiTestLauncher> loader = ServiceLoader.load(GuiTestLauncher.class);
        loader.iterator().next().startTest(new SecurePasswordComboBoxTest());
    }

    @GuiTestMethod
    public void testPasswordComboBox(JFrame frame) throws Exception {
        Registry.setDefault(new RegistryStub());
        TrustedCertAdminStub tcas = (TrustedCertAdminStub) Registry.getDefault().getTrustedCertManager();
        tcas.securePasswords.put(new Goid(0,444L), new SecurePassword("foo.test1", new Date().getTime()));
        tcas.securePasswords.put(new Goid(0,445L), new SecurePassword("foo.test2", new Date().getTime()));
        tcas.securePasswords.put(new Goid(0,446L), new SecurePassword("wsdl.server", new Date().getTime()));
        JDialog dlg = new JDialog(frame, "Doing-Something-with-a-Password Properties", true);
        final Container p = dlg.getContentPane();
        p.setLayout(new BorderLayout(8, 8));
        p.add(Box.createGlue(), BorderLayout.NORTH);
        p.add(Box.createGlue(), BorderLayout.WEST);
        p.add(new SecurePasswordComboBox(), BorderLayout.CENTER);
        p.add(new JButton("Manage Passwords"), BorderLayout.EAST);
        p.add(Box.createGlue(), BorderLayout.SOUTH);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }
}
