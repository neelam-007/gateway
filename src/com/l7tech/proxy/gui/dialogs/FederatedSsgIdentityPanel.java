/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import java.awt.*;

/**
 * Identity panel for a Federated Ssg.
 */
public class FederatedSsgIdentityPanel extends SsgIdentityPanel {
    private JPanel mainPanel;
    private JPasswordField wstPasswordField;
    private JTextField wstUsernameField;
    private JCheckBox wstSavePasswordCheckBox;
    private JButton ssgCertButton;
    private JButton clientCertButton;
    private JPanel wsTrustPanel;
    private JPanel trustedGatewayPanel;
    private JLabel trustedSsgLabel;
    private JButton testButton;
    private JTextField wsTrustUrlTextField;
    private JButton wsTrustCertButton;
    private JTextField wspAppliesToField;

    public FederatedSsgIdentityPanel(Ssg ssg) {
        setLayout(new BorderLayout());
        add(mainPanel);

        if (!ssg.isFederatedGateway())
            throw new RuntimeException("Internal error - can't use Federated property pane on non-Federated Gateway");

        if (ssg.getTrustedGateway() != null) {
            trustedGatewayPanel.setVisible(true);
            wsTrustPanel.setVisible(false);
            clientCertButton.setVisible(true);
            trustedSsgLabel.setText(ssg.getTrustedGateway().toString());
        } else {
            trustedGatewayPanel.setVisible(false);
            wsTrustPanel.setVisible(true);
            clientCertButton.setVisible(false);

            // TODO enable this setting as soon as it is configurable
            wstSavePasswordCheckBox.setSelected(true);
            wstSavePasswordCheckBox.setVisible(false);
        }
    }

    public JPasswordField getWstPasswordField() {
        return wstPasswordField;
    }

    public JTextField getWstUsernameField() {
        return wstUsernameField;
    }

    public JCheckBox getWstSavePasswordCheckBox() {
        return wstSavePasswordCheckBox;
    }

    public JButton getSsgCertButton() {
        return ssgCertButton;
    }

    public JButton getClientCertButton() {
        return clientCertButton;
    }

    public JButton getTestButton() {
        return testButton;
    }

    public JTextField getWsTrustUrlTextField() {
        return wsTrustUrlTextField;
    }

    public JButton getWsTrustCertButton() {
        return wsTrustCertButton;
    }

    public JTextField getWspAppliesToField() {
        return wspAppliesToField;
    }
}
