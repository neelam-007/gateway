package com.l7tech.proxy.gui;

import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private JButton gatewayCertButton;
    private JCheckBox useClientCredentialCheckBox;
    private JCheckBox savePasswordCheckBox;

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

        federatedSSGRadioButton.addActionListener(new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     setFederatedSSGFormEnabled(federatedSSGRadioButton.isSelected());
                     setTrustedSSGFormEnabled(!federatedSSGRadioButton.isSelected());
                 }
        });

        trustedSSGRadioButton.addActionListener(new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     setTrustedSSGFormEnabled(trustedSSGRadioButton.isSelected());
                     setFederatedSSGFormEnabled(!trustedSSGRadioButton.isSelected());
                 }
        });

        trustedSSGComboBox.setRenderer(new DefaultListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Ssg g = (Ssg) value;
                setText(g.getLocalEndpoint());
                return c;
            }
        });

        // select Trusted SSG form by default
        trustedSSGRadioButton.setSelected(true);
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

    public JButton getGatewayCertButton() {
        return gatewayCertButton;
    }

    public void setGatewayCertButton(JButton gatewayCertButton) {
        this.gatewayCertButton = gatewayCertButton;
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
    }

    public void setTrustedSSGFormEnabled(boolean enabled) {
        usernameTextField.setEnabled(enabled);
        userPasswordField.setEnabled(enabled);
        useClientCredentialCheckBox.setEnabled(enabled);
        savePasswordCheckBox.setEnabled(enabled);
        clientCertButton.setEnabled(enabled);
        gatewayCertButton.setEnabled(enabled);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        trustedSSGRadioButton = new JRadioButton();
        trustedSSGRadioButton.setText("Trusted Gateway");
        panel1.add(trustedSSGRadioButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(5, 20, 5, 5), -1, -1));
        panel1.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Your username and password  "));
        final JLabel label1 = new JLabel();
        label1.setText("Username:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Password:");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        usernameTextField = new JTextField();
        panel3.add(usernameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        savePasswordCheckBox = new JCheckBox();
        savePasswordCheckBox.setText("Save this password to your hard disk");
        panel4.add(savePasswordCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        useClientCredentialCheckBox = new JCheckBox();
        useClientCredentialCheckBox.setText("Use credentials from client (HTTP Basic Authentication)");
        panel4.add(useClientCredentialCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        userPasswordField = new JPasswordField();
        panel3.add(userPasswordField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(5, 10, 5, 5), -1, -1));
        panel2.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Certificates "));
        clientCertButton = new JButton();
        clientCertButton.setText("View your client certificate");
        panel5.add(clientCertButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        gatewayCertButton = new JButton();
        gatewayCertButton.setText("View Gateway's certificate");
        panel5.add(gatewayCertButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        federatedSSGRadioButton = new JRadioButton();
        federatedSSGRadioButton.setText("Federated Gateway");
        panel1.add(federatedSSGRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 20, 10, 10), -1, -1));
        panel1.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Trusted Gateway:");
        panel7.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        trustedSSGComboBox = new JComboBox();
        panel7.add(trustedSSGComboBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
    }
}
