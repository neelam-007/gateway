package com.l7tech.console.panels;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class UsernamePasswordDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel topLabel;
    private JTextField usernameField;
    private JPasswordField passwordField;

    private boolean confirmed = false;

    /**
     * Create a simple username/password prompt dialog.
     *
     * @param owner owner window.  Required.
     * @param title dialog title.  Required.
     * @param prompt prompt text, or null to use default.
     */
    public UsernamePasswordDialog(@NotNull Window owner, @NotNull String title, @Nullable String prompt) {
        super(owner, title, ModalityType.DOCUMENT_MODAL);
        if (prompt != null)
            topLabel.setText(prompt);

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
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }
}
