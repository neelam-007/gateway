package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the Password change dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PasswordDialog extends JDialog {
    static final Logger log = Logger.getLogger(PasswordDialog.class.getName());

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    /**
     * Command string for a cancel action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_CANCEL = "cmd.cancel";

    /**
     * Command string for a login action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_OK = "cmd.ok";

    private JButton okButton = null;

    /* new password text field */
    private JPasswordField newPasswordField = null;
    /* 'retype' password text field */
    private JPasswordField confirmPasswordField = null;

    /* the user to change the password for */
    private InternalUser user;

    private EntityListener listener;
    private UserPanel userPanel;

    private String title = null;
    /**
     * Create a new PasswordDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public PasswordDialog(Frame parent, UserPanel userPanel, InternalUser user, EntityListener l) {
        super(parent, true);
        this.userPanel = userPanel;
        this.user = user;
        this.listener = l;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
    }

    public PasswordDialog(Frame parent, UserPanel userPanel, InternalUser user, EntityListener l, String title) {
        super(parent, true);
        this.userPanel = userPanel;
        this.user = user;
        this.listener = l;
        this.title = title;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);

    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.PasswordDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        if (title == null || title.equalsIgnoreCase("")) {
            setTitle(resources.getString("dialog.title"));
        } else {
            setTitle(title);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        newPasswordField = new JPasswordField(); // needed below

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("newPasswordField.tooltip"));
        passwordLabel.setText(resources.getString("newPasswordField.label"));
        passwordLabel.setLabelFor(newPasswordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field
        newPasswordField.setToolTipText(resources.getString("newPasswordField.tooltip"));
        Font echoCharFont = new Font("Lucida Sans", Font.PLAIN, 12);
        newPasswordField.setFont(echoCharFont);
        newPasswordField.setEchoChar('\u2022');

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(newPasswordField, constraints);


        // retype password label
        JLabel confirmPasswordLabel = new JLabel();
        confirmPasswordLabel.setToolTipText(resources.getString("confirmPasswordField.tooltip"));
        confirmPasswordLabel.setText(resources.getString("confirmPasswordField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(confirmPasswordLabel, constraints);

        // retype password field
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setToolTipText(resources.getString("confirmPasswordField.tooltip"));
        confirmPasswordField.setFont(echoCharFont);
        confirmPasswordField.setEchoChar('\u2022');

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(12, 7, 0, 11);
        contents.add(confirmPasswordField, constraints);

        Utilities.
          equalizeLabelSizes(
            new JLabel[]{confirmPasswordLabel, passwordLabel});

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(17, 12, 11, 11);
        JPanel buttonPanel = createButtonPanel(); // sets global okButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(okButton);
    } // initComponents()

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable okButton
     *
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // login button (global variable)
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.setToolTipText(resources.getString("okButton.tooltip"));
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(okButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(cancelButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});
        return panel;
    } // createButtonPanel()


    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     *
     * @param actionCommand
     *               may be null
     */
    private void windowAction(Object actionCommand) {
        String cmd = null;

        if (actionCommand != null) {
            if (actionCommand instanceof ActionEvent) {
                cmd = ((ActionEvent)actionCommand).getActionCommand();
            } else {
                cmd = actionCommand.toString();
            }
        }
        if (cmd == null) {
            // do nothing
        } else if (cmd.equals(CMD_CANCEL)) {
            dispose();
        } else if (cmd.equals(CMD_OK)) {
            if (validateInput()) {
                changePassword(newPasswordField.getPassword());
            } else {
                newPasswordField.requestFocus();
            }
        }
    }

    /**
     * validate the username and context
     * @return true validated, false otherwise
     */
    private boolean validateInput() {
        char[] newPass = newPasswordField.getPassword();
        char[] cfmPass = confirmPasswordField.getPassword();
        if (newPass == null) {
            JOptionPane.
              showMessageDialog(this,
                resources.getString("newPasswordField.error.empty"),
                resources.getString("newPasswordField.error.title"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!Arrays.equals(newPass, cfmPass)) {
            JOptionPane.
              showMessageDialog(this,
                resources.getString("newAndConfirmPasswordField.mismatch"),
                resources.getString("newAndConfirmPasswordField.title"),
                JOptionPane.ERROR_MESSAGE);
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            return false;
        }

        return true;
    }

    /**
     * change password
     *
     * @param newPass new password
     */
    private void changePassword(char[] newPass) {
        try {
            if (userPanel.certExist()) {
                int res = JOptionPane.showConfirmDialog(
                        null,
                        resources.getString("revokeClientCert.question"),
                        resources.getString("revokeClientCert.title"),
                        JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            Registry.getDefault().getIdentityAdmin().changeUsersPassword(user, new String(newPass));
            dispose();
            if (listener != null) {
                EntityHeader eh = new EntityHeader();
                eh.setStrId(user.getId());
                eh.setName(user.getName());
                eh.setType(EntityType.USER);
                listener.entityUpdated(new EntityEvent(this, eh));
            }
        } catch (InvalidPasswordException e) {
            JOptionPane.showMessageDialog(this,
                    ExceptionUtils.getMessage(e),
                    resources.getString("newPasswordField.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }catch (Exception e) {
            log.log(Level.WARNING, "changePassword()", e);
            JOptionPane.showMessageDialog(null,
                    "There was an system error saving the password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            newPasswordField.requestFocus();
        }
    }
}
