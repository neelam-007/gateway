package com.l7tech.external.assertions.simplepolicybundleinstaller.console;

import com.l7tech.console.panels.bundles.BundleInstallerDialog;
import com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimplePolicyBundleInstallerDialog extends BundleInstallerDialog {
    protected static String BASE_FOLDER_NAME = "Simple Policy Bundle";

    public SimplePolicyBundleInstallerDialog(Frame owner) {
        super(owner, BASE_FOLDER_NAME, SimplePolicyBundleInstallerAssertion.class.getName());

        JButton button = new JButton("Button in Customizable Button Panel");
        button.setLayout(new BorderLayout());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(customizableButtonPanel, "Button action performed.");
            }
        });
        customizableButtonPanel.add(button);
    }

    @Override
    protected Dimension getSizingPanelPreferredSize() {
        return null;
    }
}
