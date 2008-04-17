/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.WsFederationPRPSamlTokenStrategy;
import com.l7tech.proxy.datamodel.WsTrustSamlTokenStrategy;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class NewSsgDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(NewSsgDialog.class.getName());
    private JPanel rootPanel;
    private JRadioButton radioTokenOther;
    private JRadioButton radioTokenTrustedGateway;
    private JRadioButton radioFederatedGateway;
    private JRadioButton radioTokenFederatedPassive;
    private JRadioButton radioTrustedGateway;
    private JRadioButton radioRawUrl;
    private JLabel imageLabel;
    private JPanel imagePanel;
    private JComboBox trustedSsgComboBox;
    private JButton cancelButton;
    private JButton createButton;
    private JLabel bottomSpace;

    private ImageIcon lastImage = null;
    private SsgFinder ssgFinder;
    private Ssg ssg;
    private boolean confirmed = false;

    /**
     * Create a New Ssg dialog.
     *
     * @param newSsg       the new-but-not-yet-configured-as-federated Ssg instance
     * @param ssgFinder    SsgFinder for populating list of possible Trusted SSGs
     * @param owner        owning object for proper swing nesting
     * @param title        dialog title
     * @param modal        true for modal dialog
     * @throws HeadlessException
     */
    public NewSsgDialog(Ssg newSsg, SsgFinder ssgFinder, Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        this.ssg = newSsg;
        this.ssgFinder = ssgFinder;
        initialize();
    }

    /**
     * Create a New Ssg dialog.
     *
     * @param newSsg       the new-but-not-yet-configured-as-federated Ssg instance
     * @param ssgFinder    SsgFinder for populating list of possible Trusted SSGs
     * @param owner        owning object for proper swing nesting
     * @param title        dialog title
     * @param modal        true for modal dialog
     * @throws HeadlessException
     */
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
        bg1.add(radioRawUrl);

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(radioTokenOther);
        bg2.add(radioTokenTrustedGateway);
        bg2.add(radioTokenFederatedPassive);

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
                setVisible(false);
                dispose();
            }
        };
        Utilities.runActionOnEscapeKey(getRootPane(), cancelAction);
        cancelButton.addActionListener(cancelAction);
        createButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                setVisible(false);
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
        radioTokenFederatedPassive.addActionListener(radioChanged);
        radioRawUrl.addActionListener(radioChanged);

        checkButtonState();

        Utilities.equalizeButtonSizes(new AbstractButton[] { createButton, cancelButton });
        Utilities.equalizeComponentSizes(new JComponent[] { createButton, cancelButton, bottomSpace });
    }

    private void checkButtonState() {
        final ImageIcon wantImage;
        boolean fed = true;
        boolean haveTrusted = trustedSsgComboBox.getModel().getSize() > 0;
        if (radioTrustedGateway.isSelected()) {
            wantImage = IconManager.getTrustedSsgDiagram();
            fed = false;
        } else if (radioRawUrl.isSelected()) {
            wantImage = IconManager.getGenericServiceDiagram();
            fed = false;
        } else if (radioTokenTrustedGateway.isSelected()) {
            wantImage = IconManager.getFederatedSsgDiagram();
        } else if (radioTokenFederatedPassive.isSelected()) {
            wantImage = IconManager.getFederatedSsgWithFederationServiceDiagram();
        } else {
            wantImage = IconManager.getFederatedSsgWithTokenServiceDiagram();
        }

        radioTokenOther.setEnabled(fed);
        radioTokenFederatedPassive.setEnabled(fed);
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
        ssg.setGeneric(false);
        if (radioTrustedGateway.isSelected()) {
            // Trusted SSG
            ssg.setTrustedGateway(null);
        } else if (radioFederatedGateway.isSelected()) {
            // Federated SSG
            if (radioTokenTrustedGateway.isSelected()) {
                Ssg trustSsg = (Ssg)trustedSsgComboBox.getSelectedItem();
                ssg.setTrustedGateway(trustSsg);
            } else if (radioTokenOther.isSelected()){
                ssg.setTrustedGateway(null);
                WsTrustSamlTokenStrategy newStrat = new WsTrustSamlTokenStrategy();
                ssg.setWsTrustSamlTokenStrategy(newStrat);
            } else if (radioTokenFederatedPassive.isSelected()) {
                ssg.setTrustedGateway(null);
                WsFederationPRPSamlTokenStrategy newStrat = new WsFederationPRPSamlTokenStrategy();
                ssg.setWsTrustSamlTokenStrategy(newStrat);
            } else {
                throw new IllegalStateException("Invalid radio button state!");
            }
        } else if (radioRawUrl.isSelected()) {
            ssg.setTrustedGateway(null);
            ssg.setGeneric(true);
        } else {
            throw new IllegalStateException("Invalid radio button state!");
        }
        return ssg;
    }
}
