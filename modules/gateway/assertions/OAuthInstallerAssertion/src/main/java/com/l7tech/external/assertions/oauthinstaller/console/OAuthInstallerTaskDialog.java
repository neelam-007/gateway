package com.l7tech.external.assertions.oauthinstaller.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OAuthInstallerTaskDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboBox1;
    private JButton manageJDBCConnectionsButton;
    private JCheckBox coreServicesAndTestCheckBox;
    private JCheckBox authServerAndTestCheckBox;
    private JCheckBox managerUtilityForClientCheckBox;
    private JCheckBox OVPTokenStoreAndCheckBox;
    private JCheckBox prefixResolutionURIsAndCheckBox;
    private JTextField ma1TextField;
    private JTextField OAuthTextField;

    public OAuthInstallerTaskDialog(Frame owner) {
        super(owner, "OAuth Toolkit Installer", true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        OAuthInstallerTaskDialog dialog = new OAuthInstallerTaskDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
