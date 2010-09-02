package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;

import org.junit.Ignore;

/**
 * @author mike
 */
@Ignore("Developer Test")
public class LicenseDialogTest {

    public static void main(String[] args) {
        Registry.setDefault(new RegistryStub());
        JFrame frame = new JFrame("License dialog test");
        final Container cp = frame.getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
        final LicenseDialog dlg = new LicenseDialog(frame, "My SSG");

        LicensePanelTest.addLicensePanelTestButtons(cp, dlg.licensePanel);

        cp.add(new JButton(new AbstractAction("Pack Dialog") {
            public void actionPerformed(ActionEvent e) {
                dlg.pack();
            }
        }));

        cp.add(new JButton(new AbstractAction("Quit") {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }));

        frame.pack();
        Utilities.centerOnScreen(frame);
        frame.setVisible(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }
}
