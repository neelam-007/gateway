package com.l7tech.console.panels;

import java.awt.Frame;
import java.awt.Dialog;
import java.awt.event.*;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import javax.swing.*;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.TopComponents;

/**
 * Dialog for changing a password.
 *
 * <p>This dialog is modal and by default disposes of itself when closed.</p>
 *
 * @author Steve Jones
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

    public boolean isCancel() {
        return cancel;
    }

    public boolean isHelp() {
        return help;
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
     * This method will modify the behaviour of the cancel button to act as a disconnect button.  It will also
     * change the behaviour of the windows "x" closing button on the top right corner of the window.  Essentially
     * it will disconnect the user from the user when either one of these actions are performed.
     */
    public void changeCancelBehaviourToDisconnectBehaviour() {
        //remove old one if any
        cancelButton.removeActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
            }
        });

        //if click on cancel button we want to disconnect them from the server
        cancelButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                TopComponents.getInstance().disconnectFromGateway();
            }
        });

        //if close window using top right corner (x) button, disconnect them from the server
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                TopComponents.getInstance().disconnectFromGateway();
            }
        });
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
    private boolean cancel;
    private boolean help;

    private void initComponents(String message) {
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.add(mainPanel);

        ok = false;
        cancel = false;
        help = false;

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
                cancel = true;
                dispose();
            }
        });

        helpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help = true;
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

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                if (usernameEditable)
                    userNameTextField.requestFocusInWindow();
                else
                    currentPasswordField.requestFocusInWindow();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
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
