package com.l7tech.gui.widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A simple holder of a dialog that just prompts for a password.
 */
public class PasswordEntryDialog {
    private JPasswordField pwd;
    private JOptionPane pane;
    private JDialog dialog;
    private boolean confirmed = false;

    public PasswordEntryDialog(Component parent, String prompt) {
        pwd = new JPasswordField(22);
        pane = new JOptionPane(pwd, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        dialog = pane.createDialog(parent, prompt);
        dialog.addWindowFocusListener(new WindowAdapter(){
            public void windowGainedFocus(WindowEvent e) {
                pwd.requestFocusInWindow();
            }
        });
    }

    public static PasswordEntryDialog createDialog(Component parent, String prompt) {
        return new PasswordEntryDialog(parent, prompt);
    }

    public JPasswordField getPwd() {
        return pwd;
    }

    public JOptionPane getPane() {
        return pane;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public char[] getCurrentPassword() {
        return pwd.getPassword();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public char[] showDialogAndObtainPassword() {
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.dispose();
        Object value = pane.getValue();
        confirmed = value != null && (Integer) value == JOptionPane.OK_OPTION;
        return confirmed ? pwd.getPassword() : null;
    }

    public static char[] promptForPassword(Component parentComponent, String prompt) {
        return createDialog(parentComponent, prompt).showDialogAndObtainPassword();
    }
}
