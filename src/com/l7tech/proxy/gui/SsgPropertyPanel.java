package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class SsgPropertyPanel extends JPanel {
    private JPanel mainPanel;
    private JRadioButton federatedSSGRadioButton;
    private JRadioButton trustedSSGRadioButton;
    private JComboBox trustedSSGComboBox;

    private JTextField usernameTextField;
    private JPasswordField userPasswordField;
    private JButton clientCertButton;
    private JButton trustedSSGCertButton;
    private JButton federatedSSGCertButton;
    private JCheckBox useClientCredentialCheckBox;
    private JCheckBox savePasswordCheckBox;
    private JLabel trustedGatewayLabel;
    private JLabel usernameLabel;

    public SsgPropertyPanel() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        // put the radio buttons in a group
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(federatedSSGRadioButton);
        buttonGroup.add(trustedSSGRadioButton);

        trustedSSGComboBox.setRenderer(new DefaultListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!(value instanceof Ssg))
                    return c;
                Ssg g = (Ssg) value;

                if(g != null) {
                    String u = g.getUsername() == null || g.getUsername().length() < 1
                            ? ""
                            : " (" + g.getUsername() + ")";
                    setText(g.getLocalEndpoint() + ": " + g.getSsgAddress() + u);
                }
                return c;
            }
        });

        // select Trusted SSG form by default
        trustedSSGRadioButton.setSelected(true);

        // Equalize leftmost column of each gateway type
        final char[] chars = trustedGatewayLabel.getText().toCharArray();
        final FontMetrics trustedGatewayFontMetrics = trustedGatewayLabel.getFontMetrics(trustedGatewayLabel.getFont());
        final int trustedGatewayLabelWidth = trustedGatewayFontMetrics.charsWidth(chars, 0, chars.length);
        usernameLabel.setPreferredSize(new Dimension(trustedGatewayLabelWidth, -1));

        setFederatedSSGFormEnabled(false);
    }

    public JRadioButton getFederatedSSGRadioButton() {
        return federatedSSGRadioButton;
    }

    public void setFederatedSSGRadioButton(JRadioButton federatedSSGRadioButton) {
        this.federatedSSGRadioButton = federatedSSGRadioButton;
    }

    public JButton getClientCertButton() {
        return clientCertButton;
    }

    public void setClientCertButton(JButton clientCertButton) {
        this.clientCertButton = clientCertButton;
    }

    public JButton getTrustedSSGCertButton() {
        return trustedSSGCertButton;
    }

    public void setTrustedSSGCertButton(JButton trustedSSGCertButton) {
        this.trustedSSGCertButton = trustedSSGCertButton;
    }

    public JButton getFederatedSSGCertButton() {
        return federatedSSGCertButton;
    }

    public void setFederatedSSGCertButton(JButton federatedSSGCertButton) {
        this.federatedSSGCertButton = federatedSSGCertButton;
    }

    public JCheckBox getUseClientCredentialCheckBox() {
        return useClientCredentialCheckBox;
    }

    public void setUseClientCredentialCheckBox(JCheckBox useClientCredentialCheckBox) {
        this.useClientCredentialCheckBox = useClientCredentialCheckBox;
    }

    public JCheckBox getSavePasswordCheckBox() {
        return savePasswordCheckBox;
    }

    public void setSavePasswordCheckBox(JCheckBox savePasswordCheckBox) {
        this.savePasswordCheckBox = savePasswordCheckBox;
    }

    public JRadioButton getTrustedSSGRadioButton() {
        return trustedSSGRadioButton;
    }

    public void setTrustedSSGRadioButton(JRadioButton trustedSSGRadioButton) {
        this.trustedSSGRadioButton = trustedSSGRadioButton;
    }

    public JComboBox getTrustedSSGComboBox() {
        return trustedSSGComboBox;
    }

    public void setTrustedSSGComboBox(JComboBox trustedSSGComboBox) {
        this.trustedSSGComboBox = trustedSSGComboBox;
    }

    public JTextField getUsernameTextField() {
        return usernameTextField;
    }

    public void setUsernameTextField(JTextField usernameTextField) {
        this.usernameTextField = usernameTextField;
    }

    public JPasswordField getUserPasswordField() {
        return userPasswordField;
    }

    public void setUserPasswordField(JPasswordField userPasswordField) {
        this.userPasswordField = userPasswordField;
    }

    public void setFederatedSSGFormEnabled(boolean enabled) {
        trustedSSGComboBox.setEnabled(enabled);
        federatedSSGCertButton.setEnabled(enabled);
    }

    public void setTrustedSSGFormEnabled(boolean enabled) {
        usernameTextField.setEnabled(enabled);
        userPasswordField.setEnabled(enabled);
        useClientCredentialCheckBox.setEnabled(enabled);
        savePasswordCheckBox.setEnabled(enabled);
        clientCertButton.setEnabled(enabled);
        trustedSSGCertButton.setEnabled(enabled);
    }

}
