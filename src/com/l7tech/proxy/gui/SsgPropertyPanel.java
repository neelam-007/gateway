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
    private JButton trustedSSGCertButton;
    private JButton federatedSSGCertButton;
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JRadioButton _3;
        _3 = new JRadioButton();
        trustedSSGRadioButton = _3;
        _3.setText("Trusted Gateway");
        _2.add(_3, new GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new GridLayoutManager(2, 1, new Insets(5, 20, 5, 5), -1, -1));
        _2.add(_4, new GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        _4.add(_5, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Your username and password  "));
        final JLabel _6;
        _6 = new JLabel();
        _6.setText("Username:");
        _5.add(_6, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _7;
        _7 = new JLabel();
        _7.setText("Password:");
        _5.add(_7, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _8;
        _8 = new JTextField();
        usernameTextField = _8;
        _5.add(_8, new GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _5.add(_9, new GridConstraints(2, 1, 1, 1, 0, 3, 3, 0, null, null, null));
        final JCheckBox _10;
        _10 = new JCheckBox();
        savePasswordCheckBox = _10;
        _10.setText("Save this password to your hard disk");
        _9.add(_10, new GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _11;
        _11 = new JCheckBox();
        useClientCredentialCheckBox = _11;
        _11.setText("Use credentials from client (HTTP Basic Authentication)");
        _9.add(_11, new GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPasswordField _12;
        _12 = new JPasswordField();
        userPasswordField = _12;
        _5.add(_12, new GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPanel _13;
        _13 = new JPanel();
        _13.setLayout(new GridLayoutManager(1, 2, new Insets(5, 10, 5, 5), -1, -1));
        _4.add(_13, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _13.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Certificates "));
        final JButton _14;
        _14 = new JButton();
        clientCertButton = _14;
        _14.setText("View your client certificate");
        _13.add(_14, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _15;
        _15 = new JButton();
        trustedSSGCertButton = _15;
        _15.setText("View Gateway's certificate");
        _13.add(_15, new GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JRadioButton _16;
        _16 = new JRadioButton();
        federatedSSGRadioButton = _16;
        _16.setText("Federated Gateway");
        _2.add(_16, new GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPanel _17;
        _17 = new JPanel();
        _17.setLayout(new GridLayoutManager(2, 1, new Insets(0, 20, 10, 10), -1, -1));
        _2.add(_17, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _18;
        _18 = new JPanel();
        _18.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _17.add(_18, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _19;
        _19 = new JLabel();
        _19.setText("Trusted Gateway:");
        _18.add(_19, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JComboBox _20;
        _20 = new JComboBox();
        trustedSSGComboBox = _20;
        _18.add(_20, new GridConstraints(0, 2, 1, 1, 8, 1, 2, 0, null, null, null));
        final JPanel _21;
        _21 = new JPanel();
        _21.setLayout(new GridLayoutManager(1, 2, new Insets(5, 10, 5, 5), -1, -1));
        _17.add(_21, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _21.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Certificates "));
        final JButton _22;
        _22 = new JButton();
        federatedSSGCertButton = _22;
        _22.setText("View Gateway's certificate");
        _21.add(_22, new GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final Spacer _23;
        _23 = new Spacer();
        _21.add(_23, new GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final Spacer _24;
        _24 = new Spacer();
        _2.add(_24, new GridConstraints(4, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    }

}
