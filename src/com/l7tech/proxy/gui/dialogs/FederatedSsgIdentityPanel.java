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
 * @author mike
 */
public class FederatedSsgIdentityPanel extends SsgIdentityPanel {
    private JPanel mainPanel;
    private JPasswordField userPasswordField;
    private JTextField usernameTextField;
    private JCheckBox savePasswordCheckBox;
    private JButton ssgCertButton;
    private JButton clientCertButton;
    private JPanel wsTrustPanel;
    private JPanel trustedGatewayPanel;
    private JLabel trustedSsgLabel;
    private JCheckBox useClientCredentialCheckBox = new JCheckBox(); // dummy component, not relevant to this type of ssg

    public FederatedSsgIdentityPanel(Ssg ssg) {
        setLayout(new BorderLayout());
        add(mainPanel);

        // TODO if ssg is using third-party token service
        boolean tokensFromTrusted = true;

        if (tokensFromTrusted) {
            trustedGatewayPanel.setVisible(true);
            wsTrustPanel.setVisible(false);
            trustedSsgLabel.setText(ssg.getTrustedGateway().toString());
        } else {
            trustedGatewayPanel.setVisible(false);
            wsTrustPanel.setVisible(true);
        }
    }

    public JPasswordField getUserPasswordField() {
        return userPasswordField;
    }

    public JTextField getUsernameTextField() {
        return usernameTextField;
    }

    public JCheckBox getSavePasswordCheckBox() {
        return savePasswordCheckBox;
    }

    public JButton getSsgCertButton() {
        return ssgCertButton;
    }

    public JCheckBox getUseClientCredentialCheckBox() {
        return useClientCredentialCheckBox;
    }

    public JButton getClientCertButton() {
        return clientCertButton;
    }
}
