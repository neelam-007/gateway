/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.util.ClientLogger;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.PasswordAuthentication;

/**
 * Small dialog box that propts for a username and password.
 * This class is the Client Proxy Logon dialog, used by the GuiCredentialManager.
 * Copied and modified from Emil's com.l7tech.console.LogonDialog
 */
public class LogonDialog extends JDialog {
    static final ClientLogger log = ClientLogger.getInstance(LogonDialog.class);
    private static final String DFG = "defaultForeground";

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authentication = null;

    /* Command string for a cancel action (e.g.,a button or menu item). */
    private String CMD_CANCEL = "cmd.cancel";

    /* Command string for a login action (e.g.,a button or menu item). */
    private String CMD_LOGIN = "cmd.login";

    private JButton loginButton = null;

    /** username text field */
    private JTextField userNameTextField = null;

    /** password text field */
    private JPasswordField passwordField = null;

    private static JFrame frame;
    private boolean badPasswordMessage;
    private boolean lockUsername;
    private String ssgName;

    /**
     * Create a new LogonDialog
     */
    public LogonDialog(JFrame parent, String ssgName, String defaultUsername, boolean lockUsername, boolean badPasswordMessage) {
        super(parent, ssgName, true);
        this.ssgName = ssgName;
        this.frame = parent;   // XXX using a static for this is admittedly fugly
        this.badPasswordMessage = badPasswordMessage;

        // Mustn't lock an empty username
        this.lockUsername = defaultUsername != null && defaultUsername.length() > 0 ? lockUsername : false;

        setTitle("Log on to Gateway");
        initComponents();

        if (defaultUsername != null)
            userNameTextField.setText(defaultUsername);

        if (defaultUsername == null)
            userNameTextField.requestFocus();
        else
            passwordField.requestFocus();

        updateOkButton();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        Container contents = this.getRootPane().getContentPane();
        contents.setLayout(new GridBagLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        constraints = new GridBagConstraints();

        if (badPasswordMessage) {
            log.info("displaying bad password dialog");

            JLabel icon = new JLabel(UIManager.getLookAndFeelDefaults().getIcon("OptionPane.warningIcon"));
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            constraints.gridheight = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(15, 15, 5, 15);
            contents.add(icon, constraints);

            JLabel badPasswordMessage = new JLabel("Your username or password was unauthorized.");
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.gridwidth = 3;
            constraints.gridheight = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(10, 5, 15, 15);
            contents.add(badPasswordMessage, constraints);

            constraints.gridwidth = 1;
            constraints.gridheight = 1;
        }

        userNameTextField = new JTextField(); //needed below
        userNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateOkButton(); }
            public void removeUpdate(DocumentEvent e) { updateOkButton(); }
            public void changedUpdate(DocumentEvent e) { updateOkButton(); }
        });
        userNameTextField.setEditable(!lockUsername);

        // ssg name label
        JLabel ssgNameLabel = new JLabel("Gateway:");
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(7, 5, 5, 0);
        contents.add(ssgNameLabel, constraints);

        // ssg name
        JLabel ssgNameContent = new JLabel(ssgName);
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(7, 5, 5, 10);
        contents.add(ssgNameContent, constraints);

        // user name label
        JLabel userNameLabel = new JLabel();
        userNameLabel.setDisplayedMnemonic('U');
        userNameLabel.setLabelFor(userNameTextField);
        userNameLabel.setText("Username:");

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        contents.add(userNameLabel, constraints);

        // user name text field
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(userNameTextField, constraints);

        passwordField = new JPasswordField(); // needed below

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setDisplayedMnemonic('P');
        passwordLabel.setText("Password:");
        passwordLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(passwordField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(10, 0, 10, 10);
        JPanel buttonPanel = createButtonPanel(); // sets global loginButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(loginButton);

    } // initComponents()


    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable loginButton
     *
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // login button (global variable)
        loginButton = new JButton();
        initDfg(loginButton);
        loginButton.setText("Ok");
        loginButton.setActionCommand(CMD_LOGIN);
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(loginButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText("Cancel");
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(cancelButton);

        Utilities.
          equalizeButtonSizes(new JButton[]{cancelButton, loginButton});
        return panel;
    }


    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     *
     * @param actionCommand
     *               may be null
     */
    private void windowAction(String actionCommand) {
        authentication = null;
        if (actionCommand == null) {
            // no action required
        } else if (actionCommand.equals(CMD_CANCEL)) {
            // no action required
        } else if (actionCommand.equals(CMD_LOGIN)) {
            if (!validateInput()) {
                // This shouldn't be possible -- ok button should have been greyed -- but just in case
                JOptionPane.
                        showMessageDialog(this,
                                          "Please enter your user name to connect to the web service.",
                                          "User name is required",
                                          JOptionPane.ERROR_MESSAGE);
                return;
            }

            authentication =
              new PasswordAuthentication(userNameTextField.getText(),
                passwordField.getPassword());
        }
        hide();
    }

    /**
     * Update the state of the Ok button.
     */
    private void updateOkButton() {
        setEnabled(loginButton, validateInput());
    }

    /**
     * invoke logon dialog
     *
     * @param ssgName SSG name to display in the prompt
     * @param defaultUsername what to fill in the Username field with by default.
     * @return PasswordAuthentication containing the username and password, or NULL if the dialog was canceled.
     */
    public static PasswordAuthentication logon(JFrame parent,
                                               String ssgName,
                                               String defaultUsername,
                                               boolean lockUsername,
                                               boolean badPasswordMessage)
    {
        final LogonDialog dialog = new LogonDialog(parent, ssgName, defaultUsername, lockUsername, badPasswordMessage);
        dialog.setResizable(false);
        dialog.setSize(300, 275);

        // service available attempt authenticating
        PasswordAuthentication pw = dialog.getAuthentication();
        dialog.dispose();
        return pw;
    }


    /**
     * Before displaying dialog, ensure that correct fields are selected.
     */
    public void show() {
        userNameTextField.requestFocus();
        userNameTextField.selectAll();

        addWindowListener(new WindowAdapter() {
            boolean didOpen = false;

            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                frame.setState(Frame.NORMAL);
                frame.show();
                frame.toFront();
                LogonDialog.this.toFront();
                LogonDialog.this.requestFocus();
            }
        });
        super.show();
    }


    /**
     *
     * @return the <CODE>PasswordAuthentication</CODE> collected from
     *         the dialog.
     */
    private PasswordAuthentication getAuthentication() {
        pack();
        Utilities.centerOnScreen(this);
        show();
        return authentication;
    }


    /**
     * validate the username field
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        String userName = userNameTextField.getText();
        return userName != null && userName.length() > 0;
    }

    private static void initDfg(JComponent component) {
        component.putClientProperty(DFG, component.getForeground());
    }

    private static void setEnabled(JComponent component, boolean enabled) {
        component.setEnabled(enabled);
        component.setForeground(enabled ? (Color)component.getClientProperty(DFG) : Color.GRAY);
    }
}
