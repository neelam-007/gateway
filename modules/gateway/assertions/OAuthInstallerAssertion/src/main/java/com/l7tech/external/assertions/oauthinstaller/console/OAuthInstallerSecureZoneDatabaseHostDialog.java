package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This dialog allows a user to enter in or edit a MySQL host name. The host name is checked for validity.
 */
public class OAuthInstallerSecureZoneDatabaseHostDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private String hostName;

    public OAuthInstallerSecureZoneDatabaseHostDialog(String hostName, Dialog owner) {
        super(owner, "Mysql Hostname Grant", true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        textField1.setText(hostName);
    }

    /**
     * Returns the hostname entered. This is only set after OK is pressed.
     *
     * @return The hostname entered by the user. This is null if the user clicked cancel.
     */
    public String getHostname() {
        return hostName;
    }

    private void onOK() {
        if (!ValidationUtils.isValidMySQLHostName(textField1.getText())) {
            DialogDisplayer.showMessageDialog(this, "Value for 'Hostname' is invalid", "Invalid value", JOptionPane.WARNING_MESSAGE, null);
            return;
        }
        hostName = textField1.getText();
        dispose();
    }

    private void onCancel() {
        hostName = null;
        dispose();
    }
}
