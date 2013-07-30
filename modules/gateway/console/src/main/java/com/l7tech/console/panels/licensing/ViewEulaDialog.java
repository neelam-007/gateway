package com.l7tech.console.panels.licensing;

import com.l7tech.gateway.common.licensing.FeatureLicense;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class ViewEulaDialog extends JDialog {
    public static final String DIALOG_TITLE = "License Agreement";

    private JPanel rootPanel;
    private JPanel eulaDetailsHolderPanel;
    private JButton closeButton;

    private FeatureLicense featureLicense;

    public ViewEulaDialog(Window owner, FeatureLicense featureLicense) {
        super(owner);
        this.featureLicense = featureLicense;

        init();
    }

    private void init() {
        setModal(true);
        setTitle(DIALOG_TITLE);
        setContentPane(rootPanel);

        eulaDetailsHolderPanel.setLayout(new BorderLayout());
        eulaDetailsHolderPanel.add(new EulaPanel(featureLicense.getEulaText()));

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
    }
}
