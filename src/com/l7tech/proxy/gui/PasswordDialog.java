/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

/**
 * Dialog that prompts for a password with confirmation.
 * Interaction design is intended to be error-dialog-free.
 * User: mike
 * Date: Jun 30, 2003
 * Time: 1:58:04 PM
 */
public class PasswordDialog extends JDialog {
    private static final String DFG = "defaultForeground";
    private JPanel mainPanel;
    private JPanel widgetPanel;
    private JPanel buttonPanel;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JPasswordField fieldPassword;
    private JPasswordField fieldPasswordVerify;
    private boolean passwordValid = false;
    private DocumentListener passwordDocumentListener;
    private JLabel capsMessage = new JLabel();

    public PasswordDialog(Frame owner, String title) {
        super(owner, title, true);
        setContentPane(getMainPanel());
    }

    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel();
            mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(capsMessage);
            mainPanel.add(getWidgetPanel());
            mainPanel.add(getButtonPanel());
            PasswordDialog.this.addKeyListener(new CapslockKeyListener());
            checkPasswords();
        }
        return mainPanel;
    }

    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(getButtonOk());
            buttonPanel.add(getButtonCancel());
        }
        return buttonPanel;
    }

    private JButton getButtonCancel() {
        if (buttonCancel == null) {
            buttonCancel = new JButton("Cancel");
            buttonCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    passwordValid = false;
                    hide();
                }
            });
        }
        return buttonCancel;
    }

    private JButton getButtonOk() {
        if (buttonOk == null) {
            buttonOk = new JButton("Ok");
            buttonOk.putClientProperty(DFG, buttonOk.getForeground());
            buttonOk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (isPasswordValid()) {
                        passwordValid = true;
                        hide();
                    }
                }
            });
        }
        return buttonOk;
    }

    private boolean isPasswordValid() {
        char[] p1 = getFieldPassword().getPassword();
        char[] p2 = getFieldPasswordVerify().getPassword();
        return Arrays.equals(p1, p2);
    }

    private JPanel getWidgetPanel() {
        if (widgetPanel == null) {
            widgetPanel = new JPanel(new GridLayout(2, 2, 4, 4));
            widgetPanel.add(new JLabel("Password:"));
            widgetPanel.add(getFieldPassword());
            widgetPanel.add(new JLabel("Verify password:"));
            widgetPanel.add(getFieldPasswordVerify());
        }
        return widgetPanel;
    }

    private JPasswordField getFieldPassword() {
        if (fieldPassword == null) {
            fieldPassword = new JPasswordField();
            fieldPassword.getDocument().addDocumentListener(getDocumentListener());
            fieldPassword.putClientProperty(DFG, fieldPassword.getForeground());
            fieldPassword.addKeyListener(new CapslockKeyListener());
        }
        return fieldPassword;
    }

    private JPasswordField getFieldPasswordVerify() {
        if (fieldPasswordVerify == null) {
            fieldPasswordVerify = new JPasswordField();
            fieldPasswordVerify.getDocument().addDocumentListener(getDocumentListener());
            fieldPasswordVerify.putClientProperty(DFG, fieldPasswordVerify.getForeground());
            fieldPasswordVerify.addKeyListener(new CapslockKeyListener());
        }
        return fieldPasswordVerify;
    }

    private DocumentListener getDocumentListener() {
        if (passwordDocumentListener == null) {
            passwordDocumentListener = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    checkPasswords();
                }

                public void removeUpdate(DocumentEvent e) {
                    checkPasswords();
                }

                public void changedUpdate(DocumentEvent e) {
                    checkPasswords();
                }
            };
        }
        return passwordDocumentListener;
    }

    private void checkPasswords() {
        checkCapsLock();
        if (isPasswordValid()) {
            getFieldPassword().setForeground(Color.BLUE);
            getFieldPasswordVerify().setForeground(Color.BLUE);
            getButtonOk().setForeground((Color)getButtonOk().getClientProperty(DFG));
            getButtonOk().setEnabled(true);
        } else {
            //getFieldPassword().setForeground((Color)getFieldPassword().getClientProperty(DFG));
            //getFieldPasswordVerify().setForeground((Color)getFieldPasswordVerify().getClientProperty(DFG));
            getFieldPassword().setForeground(Color.RED);
            getFieldPasswordVerify().setForeground(Color.RED);
            getButtonOk().setForeground(Color.GRAY);
            getButtonOk().setEnabled(false);
        }
    }

    private void checkCapsLock() {
        capsMessage.setText(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) ?
                                    "(CAPS LOCK ON)" : " ");
    }

    private char[] runPasswordPrompt() {
        pack();
        Utilities.centerOnScreen(this);
        show();
        return passwordValid ? fieldPassword.getPassword() : null;
    }

    /**
     * Prompt the user for a password.
     * @param parent
     * @param title
     * @return The password the user typed, or null if the dialog was canceled.
     */
    public static char[] getPassword(Frame parent, String title) {
        PasswordDialog pd = new PasswordDialog(parent, title);
        char[] word = pd.runPasswordPrompt();
        pd.dispose();
        return word;
    }

    public static void main(String[] argv) {
        char[] word = PasswordDialog.getPassword(null, "Get Password");
        System.out.println("Got password: \"" + (word == null ? "<none>" : new String(word)) + "\"");
        System.exit(0);
    }

    private class CapslockKeyListener implements KeyListener {
        public void keyTyped(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)
                checkCapsLock();
        }

        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)
                checkCapsLock();
        }

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)
                checkCapsLock();
        }
    }
}
