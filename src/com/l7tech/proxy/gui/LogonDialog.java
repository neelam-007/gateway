/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.console.panels.Utilities;
import org.apache.log4j.Category;

import javax.swing.*;
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
    static final Category log = Category.getInstance(LogonDialog.class);

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authentication = null;

    /* was the dialog aborted */
    private boolean aborted = false;

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

    /**
     * Create a new LogonDialog
     */
    public LogonDialog(JFrame parent, String title, String defaultUsername) {
        super(parent, true);
        setTitle("Log on to SSG " + title);
        initComponents();
        if (defaultUsername != null)
            userNameTextField.setText(defaultUsername);
        this.frame = parent;
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        //setTitle (resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        constraints = new GridBagConstraints();

        userNameTextField = new JTextField(); //needed below

        // user name label
        JLabel userNameLabel = new JLabel();
        userNameLabel.setDisplayedMnemonic('U');
        userNameLabel.setLabelFor(userNameTextField);
        userNameLabel.setText("Username:");

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        contents.add(userNameLabel, constraints);

        // user name text field
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
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
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(passwordField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
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
    private void windowAction(String actionCommand) {

        authentication = new PasswordAuthentication("", new char[]{});
        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            aborted = true;
        } else if (actionCommand.equals(CMD_LOGIN)) {
            if (!validateInput(userNameTextField.getText())) {
                return;
            }

            authentication =
              new PasswordAuthentication(userNameTextField.getText(),
                passwordField.getPassword());
            aborted = false;
        }
        setVisible(false);
    }

    /**
     *
     * @return true if the logon was aborted, false otherwise
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * invoke logon dialog
     *
     * @param ssgName SSG name to display in the prompt
     * @param defaultUsername what to fill in the Username field with by default.
     * @return PasswordAuthentication containing the username and password, or NULL if the dialog was canceled.
     */
    public static PasswordAuthentication logon(JFrame parent, String ssgName, String defaultUsername) {
        final LogonDialog dialog = new LogonDialog(parent, ssgName, defaultUsername);
        dialog.setResizable(false);
        dialog.setSize(300, 275);

        // service available attempt authenticating
        PasswordAuthentication pw = null;
        pw = dialog.getAuthentication();
        dialog.dispose();
        return dialog.isAborted() ? null : pw;
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
                if (didOpen)
                    return;
                didOpen = true;

                frame.setState(JFrame.ICONIFIED);
                frame.toFront();
                frame.setState(JFrame.NORMAL);
                LogonDialog.this.requestFocus();
            }

            public void windowDeactivated(WindowEvent e) {
                if (didOpen)
                    return;
                didOpen = true;
                frame.setState(JFrame.ICONIFIED);
                frame.toFront();
                frame.setState(JFrame.NORMAL);
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
     * validate the username and context
     *
     * @param userName user name entered
     * @return true validated, false othwerwise
     */
    private boolean validateInput(String userName) {
        if (null == userName || "".equals(userName)) {
            JOptionPane.
                    showMessageDialog(this,
                                      "Please enter your user name to connect to the web service.",
                                      "User name is required",
                                      JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;

    }
}
