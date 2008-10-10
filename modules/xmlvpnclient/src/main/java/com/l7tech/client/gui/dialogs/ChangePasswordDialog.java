package com.l7tech.client.gui.dialogs;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import javax.swing.*;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;

/**
 * Dialog for changing a password.
 *
 * <p>This dialog is modal and by default disposes of itself when closed.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ChangePasswordDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a password change dialog with the given values.
     *
     * @param owner             The owner for the dialog
     * @param message           Optional message text (may be null)
     * @param username          Optional initial user (may be null)
     * @param usernameEditable  true if the username should be editable
     */
    public ChangePasswordDialog(Frame owner, String message, String username, boolean usernameEditable) {
        super(owner, TITLE, true);
        this.usernameEditable = usernameEditable;
        this.username = username;
        initComponents(message);
    }

    /**
     * Create a password change dialog with the given values.
     *
     * @param owner             The owner for the dialog
     * @param message           Optional message text (may be null)
     * @param username          Optional initial user (may be null)
     * @param usernameEditable  true if the username should be editable
     */
    public ChangePasswordDialog(Dialog owner, String message, String username, boolean usernameEditable) {
        super(owner, TITLE, true);
        this.usernameEditable = usernameEditable;
        this.username = username;
        initComponents(message);
    }

    /**
     * Check if the dialog was OK'd
     *
     * @return true if OK was selected.
     */
    public boolean wasOk() {
        return ok;
    }

    /**
     * Get the current password authentication.
     *
     * <p>Note that you can only call this once, because it is a destructive read.</p>
     *
     * <p>The username is from the dialog.</p>
     *
     * @return The current/old PasswordAuthentication
     */
    public PasswordAuthentication getCurrentPasswordAuthentication() {
        char[] password = currentPasswordField.getPassword();

        PasswordAuthentication pa = new PasswordAuthentication(
                userNameTextField.getText(),
                password);

        // Clear document
        currentPasswordField.setText("");

        // Clear copy
        blank(password);

        return pa;
    }

    /**
     * Get the new password authentication.
     *
     * <p>Note that you can only call this once, because it is a destructive read.</p>
     *
     * <p>The username is from the dialog.</p>
     *
     * @return The new PasswordAuthentication
     */
    public PasswordAuthentication getNewPasswordAuthentication() {
        char[] password = newPasswordField.getPassword();

        PasswordAuthentication pa = new PasswordAuthentication(
                userNameTextField.getText(),
                password);

        // Clear document
        newPasswordField.setText("");

        // Clear copy
        blank(password);

        return pa;
    }

    //- PRIVATE

    private static final String TITLE = "Change Password";

    private JTextField userNameTextField;
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField newPasswordConfirmationField;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel infoLabel;
    private JPanel mainPanel;
    private JButton helpBtn;

    private final boolean usernameEditable;
    private String username;
    private boolean ok;

    private void initComponents(String message) {
        this.setDefaultCloseOperation(ChangePasswordDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.add(mainPanel);

        ok = false;

        if (message != null && message.length() > 0) {
            infoLabel.setText(message);
        } else {
            infoLabel.setVisible(false);
        }

        if (username != null)
            userNameTextField.setText(username);

        if (!usernameEditable)
            userNameTextField.setEditable(false);

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){
            public void run() {
                updateButtons();
            }
        });

        userNameTextField.getDocument().addDocumentListener(rocl);
        currentPasswordField.getDocument().addDocumentListener(rocl);
        newPasswordField.getDocument().addDocumentListener(rocl);
        newPasswordConfirmationField.getDocument().addDocumentListener(rocl);

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        helpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final PasswordHelpDialog dialog = new PasswordHelpDialog();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    public void run() {
                        if (!dialog.isOk()) {
                            dispose();
                        }
                    }
                });
            }
        });

        updateButtons();
        this.pack();
        Utilities.centerOnScreen(this);
    }

    private void blank(char[] password) {
        for (int c=0; c<password.length; c++) {
            password[c] = 0;
        }
    }

    private void updateButtons() {
        boolean okState = false;

        if (userNameTextField.getText().length() > 0 || !usernameEditable) {
            char[] oldpass = currentPasswordField.getPassword();
            char[] newpass = newPasswordField.getPassword();
            char[] reppass = newPasswordConfirmationField.getPassword();

            if (oldpass.length > 0 && newpass.length > 0 && reppass.length==newpass.length) {
                if (Arrays.equals(newpass, reppass)) {
                    okState = true;
                }
            }

            blank(oldpass);
            blank(newpass);
            blank(reppass);
        }

        okButton.setEnabled(okState);
    }
}
