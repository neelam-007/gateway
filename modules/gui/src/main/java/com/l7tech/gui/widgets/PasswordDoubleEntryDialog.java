/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.Utilities;

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
 * Small dialog box that prompts for a new password, requiring the user to type it twice for confirmation.
 * Doesn't ask about usernames, and so not to be confused with LogonDialog, which does.
 * Interaction design is intended to be error-dialog-free.
 */
public class PasswordDoubleEntryDialog extends JDialog {
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
    private boolean singleInputOnly;

    public PasswordDoubleEntryDialog(Window owner, String title, boolean singleInputOnly) {
        super(owner, title, ModalityType.DOCUMENT_MODAL);
        this.singleInputOnly = singleInputOnly;
        setContentPane(getMainPanel());
        init();
    }

    public PasswordDoubleEntryDialog(Window owner, String title) {
        this(owner, title, false);
        init();
    }

    private void init() {
        Utilities.equalizeButtonSizes(new JButton[] { getButtonOk(), getButtonCancel() });
        getRootPane().setDefaultButton(getButtonOk());
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.centerOnParentWindow(this);
        pack();
        fieldPassword.requestFocusInWindow();
    }

    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel();
            mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(capsMessage);
            mainPanel.add(getWidgetPanel());
            mainPanel.add(getButtonPanel());
            PasswordDoubleEntryDialog.this.addKeyListener(new CapslockKeyListener());
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
                    dispose();
                }
            });
            Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    passwordValid = false;
                    dispose();
                }
            });
        }
        return buttonCancel;
    }

    private JButton getButtonOk() {
        if (buttonOk == null) {
            buttonOk = new JButton("OK");
            buttonOk.putClientProperty(DFG, buttonOk.getForeground());
            buttonOk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (isPasswordValid()) {
                        passwordValid = true;
                        dispose();
                    }
                }
            });
        }
        return buttonOk;
    }

    public void setVisible(boolean b) {
        boolean wasVisible = isVisible();
        super.setVisible(b);
        if (b && !wasVisible)
            fieldPassword.requestFocusInWindow();
    }

    private boolean isPasswordValid() {
        if (singleInputOnly)
            return true;
        char[] p1 = getFieldPassword().getPassword();
        char[] p2 = getFieldPasswordVerify().getPassword();
        return Arrays.equals(p1, p2);
    }

    private JPanel getWidgetPanel() {
        if (widgetPanel == null) {
            widgetPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridheight = 1;
            gbc.gridwidth = 1;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.ipadx = 1;
            gbc.ipady = 1;
            gbc.insets = new Insets(2, 2, 2, 2);

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
            widgetPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
            widgetPanel.add(getFieldPassword(), gbc);
            if (!singleInputOnly) {
                gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
                widgetPanel.add(new JLabel("Verify password:"), gbc);
                gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
                widgetPanel.add(getFieldPasswordVerify(), gbc);
            }
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 1;
            widgetPanel.add(Box.createGlue(), gbc);
        }
        return widgetPanel;
    }

    private JPasswordField getFieldPassword() {
        if (fieldPassword == null) {
            fieldPassword = new JPasswordField();
            fieldPassword.setPreferredSize(new Dimension(285, 20));
            fieldPassword.getDocument().addDocumentListener(getDocumentListener());
            fieldPassword.putClientProperty(DFG, fieldPassword.getForeground());
            fieldPassword.addKeyListener(new CapslockKeyListener());
        }
        return fieldPassword;
    }

    private JPasswordField getFieldPasswordVerify() {
        if (fieldPasswordVerify == null) {
            fieldPasswordVerify = new JPasswordField();
            fieldPasswordVerify.setPreferredSize(new Dimension(285, 20));
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
        try {
            capsMessage.setText(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) ?
                                        "(CAPS LOCK ON)" : " ");
        } catch (UnsupportedOperationException e) {
            capsMessage.setText(" "); // Work around JRE 1.5 bug 5100701
        }
    }

    /** @return true if this dialog was closed with the Ok button. */
    public boolean isConfirmed() {
        return passwordValid;
    }

    /** @return the password that was entered. */
    public char[] getPassword() {
        return fieldPassword.getPassword();
    }

    private char[] runPasswordPrompt() {
        pack();
        setResizable(true);
        Utilities.centerOnScreen(this);
        setVisible(true);
        return passwordValid ? fieldPassword.getPassword() : null;
    }

    /**
     * Prompt the user for a password.
     * @param parent
     * @param title
     * @return The password the user typed, or null if the dialog was canceled.
     */
    public static char[] getPassword(Frame parent, String title, boolean singleInputOnly) {
        PasswordDoubleEntryDialog pd = new PasswordDoubleEntryDialog(parent, title, singleInputOnly);
        char[] word = pd.runPasswordPrompt();
        pd.dispose();
        return word;
    }

    public static char[] getPassword(Frame parent, String title) {
        return getPassword(parent, title, false);
    }

    public static void main(String[] argv) {
        JFrame frame = new JFrame("test");
        frame.setVisible(true);
        char[] word = PasswordDoubleEntryDialog.getPassword(frame,
                                             "Enter new password for Gateway <My gateway with a long long very extremely longish long name>");
//        char[] word = PasswordDoubleEntryDialog.getPassword(null, "Get Password", true);
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
