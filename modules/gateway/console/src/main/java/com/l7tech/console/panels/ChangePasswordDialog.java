package com.l7tech.console.panels;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.PasswordAuthentication;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for changing a password.
 *
 * <p>This dialog is modal and by default disposes of itself when closed.</p>
 *
 * @author Steve Jones
 */
public class ChangePasswordDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ChangePasswordDialog.class.getName());

    //- PUBLIC

    /**
     * Create a password change dialog with the given values.
     *
     * @param owner             The owner for the dialog
     * @param message           Optional message text (may be null)
     * @param username          Optional initial user (may be null)
     * @param usernameEditable  true if the username should be editable
     */
    public ChangePasswordDialog(Window owner, String message, String username, boolean usernameEditable) {
        super(owner, TITLE, DEFAULT_MODALITY_TYPE);
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
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        //if click on cancel button we want to disconnect them from the server
        cancelButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponents.getInstance().disconnectFromGateway();
            }
        });

        //if close window using top right corner (x) button, disconnect them from the server
        addWindowListener(new WindowAdapter() {
            @Override
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

    /**
     * Set the password policy description for the help dialog.
     * If left null, the help dialog will try to get the description from the identity admin.
     * 
     * @param passwordPolicyDescription
     */
    public void setPasswordPolicyDescription(String passwordPolicyDescription) {
        this.passwordPolicyDescription = passwordPolicyDescription;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
        tabbedPane1.setEnabledAt(ROLES_TAB, isLoggedIn);
        updateButtons();
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
    private JTabbedPane tabbedPane1;
    private JLabel rolesLabel;
    private JList<Role> rolesList;

    private final boolean usernameEditable;
    private String username;
    private boolean ok;
    private boolean cancel;
    private boolean help;
    private String passwordPolicyDescription = null;
    private static boolean isLoggedIn = true;

    private static int ROLES_TAB = 1;

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
            @Override
            public void run() {
                updateButtons();
            }
        });

        userNameTextField.getDocument().addDocumentListener(rocl);
        currentPasswordField.getDocument().addDocumentListener(rocl);
        newPasswordField.getDocument().addDocumentListener(rocl);
        newPasswordConfirmationField.getDocument().addDocumentListener(rocl);

        okButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel = true;
                dispose();
            }
        });

        helpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help = true;
                final PasswordHelpDialog dialog = new PasswordHelpDialog(ChangePasswordDialog.this, passwordPolicyDescription);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if (!dialog.isOk()) {
                            dispose();
                        }
                    }
                });
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (usernameEditable)
                    userNameTextField.requestFocusInWindow();
                else
                    currentPasswordField.requestFocusInWindow();
            }
        });

        rolesLabel.setText(MessageFormat.format(rolesLabel.getText(), username == null ? "" : username));
        populateRolesList();

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
        updateButtons();
        this.pack();
        Utilities.centerOnScreen(this);
    }

    private void populateRolesList() {
        Collection<Role> roles = new LinkedHashSet<Role>();

        if (Registry.getDefault().isAdminContextPresent()) {
            SecurityProvider sec = Registry.getDefault().getSecurityProvider();
            User user = sec.getUser();
            if (user != null) {
                try {
                    roles.addAll(Registry.getDefault().getRbacAdmin().findRolesForUser(user));
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Unable to look up user roles: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        rolesList.setModel(new DefaultComboBoxModel<Role>(roles.toArray(new Role[roles.size()])));
        rolesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Role) {
                    Role role = (Role) value;
                    try {
                        value = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
                    } catch (final FindException e) {
                        logger.log(Level.WARNING, "Unable to resolve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        value = role.getDescriptiveName();
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    private void blank(char[] password) {
        for (int c=0; c<password.length; c++) {
            password[c] = 0;
        }
    }

    private void updateButtons() {
        boolean okState = false;

        if (permittedToChangePassword()) {
            currentPasswordField.setEnabled(true);
            newPasswordConfirmationField.setEnabled(true);
            newPasswordConfirmationField.setEnabled(true);

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
        } else {
            currentPasswordField.setEnabled(false);
            newPasswordConfirmationField.setEnabled(false);
            newPasswordConfirmationField.setEnabled(false);
        }

        okButton.setEnabled(okState);
    }

    private static boolean permittedToChangePassword() {
        if(!isLoggedIn)
            return true;

        try {
            return Registry.getDefault().isAdminContextPresent() && Registry.getDefault().getIdentityAdmin().currentUsersPasswordCanBeChanged();
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "Unable to check password change permission: " + ExceptionUtils.getMessage(e), e);
            return true;
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check password change permission: " + ExceptionUtils.getMessage(e), e);
            return true;
        }
    }

}
