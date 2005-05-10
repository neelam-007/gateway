/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.WsTrustSamlTokenStrategy;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author mike
 */
public class NewSsgDialog extends JDialog {
    private JPanel rootPanel;
    private JRadioButton radioTokenOther;
    private JRadioButton radioTokenTrustedGateway;
    private JRadioButton radioFederatedGateway;
    private JRadioButton radioTrustedGateway;
    private JLabel imageLabel;
    private JPanel imagePanel;
    private JComboBox trustedSsgComboBox;
    private JButton cancelButton;
    private JButton createButton;

    private ImageIcon lastImage = null;
    private SsgFinder ssgFinder;
    private Ssg ssg;
    private boolean confirmed = false;

    public NewSsgDialog(Ssg newSsg, SsgFinder ssgFinder, Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        this.ssg = newSsg;
        this.ssgFinder = ssgFinder;
        initialize();
    }

    public NewSsgDialog(Ssg newSsg, SsgFinder ssgFinder, Dialog owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        this.ssg = newSsg;
        this.ssgFinder = ssgFinder;
        initialize();
    }

    private void initialize() {
        if (ssg == null) throw new NullPointerException("A non-null Ssg must be provided");
        setContentPane(rootPanel);

        imagePanel.setBackground(Color.WHITE);
        imagePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.BLACK));

        ButtonGroup bg1 = new ButtonGroup();
        bg1.add(radioTrustedGateway);
        bg1.add(radioFederatedGateway);

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(radioTokenOther);
        bg2.add(radioTokenTrustedGateway);

        radioTrustedGateway.setSelected(true);
        radioTokenTrustedGateway.setSelected(true);

        trustedSsgComboBox.setRenderer(SsgComboBoxUtil.getListCellRenderer());
        SsgComboBoxUtil.populateSsgList(trustedSsgComboBox, ssgFinder, ssg);
        Utilities.enableGrayOnDisabled(trustedSsgComboBox);
        if (trustedSsgComboBox.getModel().getSize() < 1)
            radioTokenOther.setSelected(true);

        getRootPane().setDefaultButton(createButton);
        final AbstractAction cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ssg = null;
                confirmed = false;
                hide();
                dispose();
            }
        };
        Utilities.runActionOnEscapeKey(getRootPane(), cancelAction);
        cancelButton.addActionListener(cancelAction);
        createButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                hide();
                dispose();
            }
        });

        Action radioChanged = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkButtonState();
            }
        };

        radioTrustedGateway.addActionListener(radioChanged);
        radioFederatedGateway.addActionListener(radioChanged);
        radioTokenOther.addActionListener(radioChanged);
        radioTokenTrustedGateway.addActionListener(radioChanged);
        checkButtonState();
    }

    private void checkButtonState() {
        ImageIcon wantImage = null;
        boolean fed = true;
        boolean haveTrusted = trustedSsgComboBox.getModel().getSize() > 0;
        if (radioTrustedGateway.isSelected()) {
            wantImage = IconManager.getTrustedSsgDiagram();
            fed = false;
        } else if (radioTokenTrustedGateway.isSelected()) {
            wantImage = IconManager.getFederatedSsgDiagram();
        } else {
            wantImage = IconManager.getFederatedSsgWithTokenServiceDiagram();
        }

        radioTokenOther.setEnabled(fed);
        radioTokenTrustedGateway.setEnabled(fed && haveTrusted);
        trustedSsgComboBox.setEnabled(radioTokenTrustedGateway.isEnabled() && radioTokenTrustedGateway.isSelected());

        if (lastImage != wantImage) {
            imageLabel.setIcon(wantImage);
        }
    }

    /** @return the appropriately-configured Ssg, or null if the dialog was canceled. */
    public Ssg getSsg() {
        if (ssg == null || !confirmed)
            return null;

        // Configure the Ssg
        if (radioTrustedGateway.isSelected()) {
            // Trusted SSG
            ssg.setTrustedGateway(null);
        } else {
            // Federated SSG
            if (radioTokenTrustedGateway.isSelected()) {
                Ssg trustSsg = (Ssg)trustedSsgComboBox.getSelectedItem();
                ssg.setTrustedGateway(trustSsg);
            } else {
                ssg.setTrustedGateway(null);
                WsTrustSamlTokenStrategy newStrat = new WsTrustSamlTokenStrategy();
                ssg.setWsTrustSamlTokenStrategy(newStrat);
            }
        }
        return ssg;
    }
}
