package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class PasswordHelpDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JEditorPane editorPane;
    private boolean ok;

    public PasswordHelpDialog() {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Password Rules");
        getRootPane().setDefaultButton(buttonOK);
        ok = false;

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        String passwordHelpMsg = "<html><body>Password changes must comply all of the following:" +
                "<ul>" +
                "<li>Passwords are a minimum of 8 alphanumeric characters in length and do contain a mix of upper case letters, lower case letters, numbers, and special characters.</li>" +
                "<li>Passwords do not contain consecutively repeating characters.</li>" +
                "<li>Passwords must differ from the previous password by at least four characters.</li>" +
                "<li>Passwords are not reused within 10 password changes.</li>" +
                "</ul>" +
                "</body></html>";

        editorPane.setText(passwordHelpMsg);

        this.pack();
        this.setSize(525, 225);
        this.setResizable(true);
    }

    public boolean isOk() {
        return ok;
    }

    private void onOK() {
        ok = true;
        dispose();
    }

    public static void main(String[] args) {
        PasswordHelpDialog dialog = new PasswordHelpDialog();

        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }


}
