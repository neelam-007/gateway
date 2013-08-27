package com.l7tech.console.panels.licensing;

import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class LicenseDetailsDialog extends JDialog {
    public static final String TITLE = "License Details";

    private JPanel rootPanel;
    private JPanel licenseDetailsPanelHolder;
    private JButton viewEulaButton;
    private JButton closeButton;

    private final FeatureLicense featureLicense;

    public LicenseDetailsDialog(Window owner, FeatureLicense featureLicense) {
        super(owner);
        this.featureLicense = featureLicense;
        init();
    }

    private void init() {
        setModal(true);
        setTitle(TITLE);
        setContentPane(rootPanel);

        licenseDetailsPanelHolder.setLayout(new BorderLayout());

        FeatureLicenseDetailsPanel licenseDetailsPanel = new FeatureLicenseDetailsPanel(featureLicense);

        licenseDetailsPanelHolder.add(licenseDetailsPanel);

        viewEulaButton.setEnabled(null != featureLicense.getEulaText());
        viewEulaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewEula();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        pack();
    }

    private void viewEula() {
        ViewEulaDialog eulaDialog = new ViewEulaDialog(this, featureLicense);
        Utilities.centerOnParentWindow(eulaDialog);
        DialogDisplayer.display(eulaDialog);
    }
}