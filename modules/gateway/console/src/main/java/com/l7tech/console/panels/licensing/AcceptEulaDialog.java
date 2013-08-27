package com.l7tech.console.panels.licensing;

import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class AcceptEulaDialog extends JDialog {
    private JPanel rootPanel;
    private JPanel eulaDetailsHolderPanel;
    private JButton agreeButton;
    private JButton declineButton;

    private FeatureLicense featureLicense;

    private boolean licenseAccepted = false;

    public AcceptEulaDialog(Window owner, FeatureLicense featureLicense) {
        super(owner);
        this.featureLicense = featureLicense;

        init();
    }

    private void init() {
        setModal(true);
        setTitle("License Agreement");
        setContentPane(rootPanel);

        eulaDetailsHolderPanel.setLayout(new BorderLayout());
        eulaDetailsHolderPanel.add(new EulaPanel(featureLicense.getEulaText()));

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doDecline();
            }
        });

        agreeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAccept();
            }
        });

        declineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDecline();
            }
        });

        Utilities.equalizeButtonSizes(agreeButton, declineButton);

        pack();
    }

    public void doAccept() {
        licenseAccepted = true;
        dispose();
    }

    public void doDecline() {
        licenseAccepted = false;
        dispose();
    }

    public boolean isLicenseAccepted() {
        return licenseAccepted;
    }
}
