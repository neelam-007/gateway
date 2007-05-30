package com.l7tech.server.config.ui.gui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class KeystoreInformationDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPasswordField pwdField;
    private JComboBox keystoreTypes;
    private JLabel passwordMessage;
    private JLabel keystoreTypeMessage;

    public KeystoreInformationDialog(Dialog owner, String title, String passwordMessage, String ksTypeMessage, String[] allowedTypes) {
        super(owner, title, true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        this.passwordMessage.setText(passwordMessage);
        this.keystoreTypeMessage.setText(ksTypeMessage);
        this.keystoreTypes.setModel(new DefaultComboBoxModel(allowedTypes));

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

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();
    }

    private void onOK() {
// add your code here
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public char[] getPassword() {
        return pwdField.getPassword();
    }

    public String getKeystoreType() {
        return (String) keystoreTypes.getSelectedItem();
    }
}
