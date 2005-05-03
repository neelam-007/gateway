/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.ContextMenuTextField;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * Small dialog box that propts for a user name and password.
 * This class is the Client Proxy Logon dialog, used by the GuiCredentialManager.
 * Copied and modified from Emil's com.l7tech.console.LogonDialog
 */
public class LogonDialog extends JDialog {
    static final Logger log = Logger.getLogger(LogonDialog.class.getName());

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authentication = null;

    /* Command string for a cancel action (e.g.,a button or menu item). */
    private String CMD_CANCEL = "cmd.cancel";

    /* Command string for a login action (e.g.,a button or menu item). */
    private String CMD_LOGIN = "cmd.login";

    private JButton loginButton;
    private JButton cancelButton;
    private JTextField userNameTextField;
    private JPasswordField passwordField;

    private boolean badPasswordMessage;
    private boolean lockUsername;
    private String ssgName;
    private String reasonHint;
    private final JTextComponent focusComponent;

    /**
     * Create a new LogonDialog
     */
    public LogonDialog(JFrame parent,
                       String ssgName,
                       String defaultUsername,
                       boolean lockUsername,
                       boolean badPasswordMessage,
                       String reasonHint)
    {
        super(parent, ssgName, true);
        this.ssgName = ssgName;
        this.badPasswordMessage = badPasswordMessage;
        this.reasonHint = reasonHint;

        // Mustn't lock an empty username
        this.lockUsername = defaultUsername != null && defaultUsername.length() > 0 ? lockUsername : false;

        setTitle("Log On to Gateway");
        initComponents();
        getRootPane().setDefaultButton(loginButton);

        if (defaultUsername == null) {
            focusComponent = userNameTextField;
        } else {
            userNameTextField.setText(defaultUsername);
            focusComponent = passwordField;
        }

        final FocusTraversalPolicy ftp = getFocusTraversalPolicy();
        setFocusTraversalPolicy(new FocusTraversalPolicy() {
            public Component getComponentAfter(Container focusCycleRoot,
                                               Component aComponent) {
                return ftp.getComponentAfter(focusCycleRoot, aComponent);
            }

            public Component getComponentBefore(Container focusCycleRoot,
                                                Component aComponent) {
                return ftp.getComponentBefore(focusCycleRoot, aComponent);
            }

            public Component getFirstComponent(Container focusCycleRoot) {
                return focusComponent;
            }

            public Component getLastComponent(Container focusCycleRoot) {
                return cancelButton;
            }

            public Component getDefaultComponent(Container focusCycleRoot) {
                return focusComponent;
            }
        });
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

            JLabel badPasswordMessage = new JLabel("<html>Your user name or password was unauthorized.<br>Possible reasons:<ul>" +
                                                   "<li> Incorrect user name" +
                                                   "<li> Incorrect password for Gateway" +
                                                   "<li> Incorrect password for client certificate" +
                                                   "<li> Revoked client certificate" +
                                                   "<li> Not authorized to use service");
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.gridwidth = 3;
            constraints.gridheight = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(10, 5, 0, 15);
            contents.add(badPasswordMessage, constraints);

            constraints.gridwidth = 1;
            constraints.gridheight = 1;
        }

        userNameTextField = new ContextMenuTextField(); //needed below
        Utilities.enableSelectAllOnFocus(userNameTextField);
        userNameTextField.setEditable(!lockUsername);

        // Reason
        JLabel reason = new JLabel("Please enter your password " + reasonHint);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(7, 5, 0, 5);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        contents.add(reason, constraints);

        // ssg name label
        JLabel ssgNameLabel = new JLabel("for the SecureSpan Gateway:");
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 5, 0);
        contents.add(ssgNameLabel, constraints);

        // ssg name
        JLabel ssgNameContent = new JLabel(ssgName);
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 5, 5, 10);
        contents.add(ssgNameContent, constraints);

        // user name label
        JLabel userNameLabel = new JLabel();
        userNameLabel.setDisplayedMnemonic('U');
        userNameLabel.setLabelFor(userNameTextField);
        userNameLabel.setText("User Name:");

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        contents.add(userNameLabel, constraints);

        // user name text field
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(userNameTextField, constraints);

        passwordField = new JPasswordField(); // needed below
        Utilities.enableSelectAllOnFocus(passwordField);

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setDisplayedMnemonic('P');
        passwordLabel.setText("Password:");
        passwordLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(passwordField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 5;
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
        loginButton.setText("OK");
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
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(cancelButton);
        Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { windowAction(CMD_CANCEL); }
        });

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
            authentication =
              new PasswordAuthentication(userNameTextField.getText(),
                passwordField.getPassword());
        }
        hide();
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
                                               boolean badPasswordMessage,
                                               String reasonHint)
    {
        final LogonDialog dialog = new LogonDialog(parent, ssgName, defaultUsername, lockUsername, badPasswordMessage, reasonHint);
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
        addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                JFrame parent = (JFrame)getParent();
                parent.setState(Frame.NORMAL);
                parent.show();
                parent.toFront();
                LogonDialog.this.toFront();
                //LogonDialog.this.requestFocus();
                focusComponent.requestFocus();
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
}
