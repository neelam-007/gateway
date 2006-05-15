package com.l7tech.server.config.ui.gui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class CredentialsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel descriptionLabel;
    private boolean wasCancelled = false;

    public CredentialsDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title);
        initialize();
    }

    public CredentialsDialog(Dialog owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        initialize();
    }

    public CredentialsDialog() {
        super();
        initialize();
    }

    private void initialize() {
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
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        wasCancelled = true;
        dispose();
    }

    public static void main(String[] args) {
        CredentialsDialog dialog = new CredentialsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public boolean wasCancelled() {
        return wasCancelled;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public void setUsername(String username) {
        usernameField.setText(username);
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }
}
