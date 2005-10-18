/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.proxy.gui.dialogs;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.l7tech.common.util.HexUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.gui.util.IconManager;

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
    private JButton wsTrustTestButton;
    private JTextField wsTrustUrlTextField;
    private JButton wsTrustCertButton;
    private JTextField wspAppliesToField;
    private JTextField wstIssuerField;
    private JComboBox requestTypeCombo;
    private JTextField wsFedUrlTextField;
    private JTextField wsFedRealmTextField;
    private JCheckBox wsFedTimestampCheckBox;
    private JTextField wsFedUsernameField;
    private JButton wsFedTestButton;
    private JButton wsFedCertButton;
    private JPasswordField wsFedPasswordField;
    private JPanel wsFedPanel;

    private final ImageIcon imageIcon;

    public FederatedSsgIdentityPanel(Ssg ssg) {
        setLayout(new BorderLayout());
        add(mainPanel);

        if (!ssg.isFederatedGateway())
            throw new RuntimeException("Internal error - can't use Federated property pane on non-Federated Gateway");

        if (ssg.isFederatedTrusted()) {
            trustedGatewayPanel.setVisible(true);
            wsTrustPanel.setVisible(false);
            wsFedPanel.setVisible(false);
            clientCertButton.setVisible(true);
            trustedSsgLabel.setText(ssg.getTrustedGateway().toString());
            imageIcon = IconManager.getSmallFederatedSsgDiagram();
        } else {
            trustedGatewayPanel.setVisible(false);
            if(ssg.isFederatedWsTrust()) {
                wsTrustPanel.setVisible(true);
                wsFedPanel.setVisible(false);
                imageIcon = IconManager.getSmallFederatedSsgWithTokenServiceDiagram();
            }
            else {
                wsTrustPanel.setVisible(false);
                wsFedPanel.setVisible(true);
                imageIcon = IconManager.getSmallFederatedSsgWithFederationServiceDiagram();
            }
            clientCertButton.setVisible(false);

            wsTrustUrlTextField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { updateButtons(); }
                public void insertUpdate(DocumentEvent e) { updateButtons(); }
                public void removeUpdate(DocumentEvent e) { updateButtons(); }
            });
            wsFedUrlTextField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { updateButtons(); }
                public void insertUpdate(DocumentEvent e) { updateButtons(); }
                public void removeUpdate(DocumentEvent e) { updateButtons(); }
            });

            // TODO enable this setting as soon as it is configurable
            wstSavePasswordCheckBox.setSelected(true);
            wstSavePasswordCheckBox.setVisible(false);
        }

        updateButtons();
    }

    public ImageIcon getGeneralPaneImageIcon() {
        return imageIcon;
    }

    private void updateButtons() {
        wsTrustTestButton.setEnabled(HexUtils.isValidUrl(wsTrustUrlTextField.getText()));
        wsFedTestButton.setEnabled(HexUtils.isValidUrl(wsFedUrlTextField.getText()));
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

    public JButton getWsTrustTestButton() {
        return wsTrustTestButton;
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

    public JTextField getWstIssuerField() {
        return wstIssuerField;
    }

    public JComboBox getRequestTypeCombo() {
        return requestTypeCombo;
    }

    public JTextField getWsFedUrlTextField() {
        return wsFedUrlTextField;
    }

    public JTextField getWsFedRealmTextField() {
        return wsFedRealmTextField;
    }

    public JCheckBox getWsFedTimestampCheckBox() {
        return wsFedTimestampCheckBox;
    }

    public JTextField getWsFedUsernameField() {
        return wsFedUsernameField;
    }

    public JPasswordField getWsFedPasswordField() {
        return wsFedPasswordField;
    }

    public JButton getWsFedTestButton() {
        return wsFedTestButton;
    }

    public JButton getWsFedCertButton() {
        return wsFedCertButton;
    }
}
