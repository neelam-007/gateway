/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This panel includes a "Requires username and password" checkbox, and allows entry of the username
 * and password.
 */
public class OptionalCredentialsPanel extends JPanel {
    private JCheckBox requireCheckbox;
    private JPasswordField passwordField;
    private JTextField usernameField;

    public OptionalCredentialsPanel() {
        setLayout(new GridBagLayout());

        requireCheckbox = new JCheckBox("Requires user name and password:");
        requireCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        });
        add(requireCheckbox,
            new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 0, 5, 0), 0, 0));

        add(new JLabel("User Name:"),
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 32, 5, 3), 0, 0));

        usernameField = new JTextField();
        Utilities.enableGrayOnDisabled(usernameField);
        usernameField.setPreferredSize(new Dimension(90, 20));
        add(usernameField,
            new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 0, 5, 0), 0, 0));

        add(new JLabel("Password:"),
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 32, 5, 3), 0, 0));

        passwordField = new JPasswordField();
        Utilities.enableGrayOnDisabled(passwordField);
        passwordField.setPreferredSize(new Dimension(90, 20));
        add(passwordField,
            new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 0, 5, 0), 0, 0));

        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {
        boolean r = requireCheckbox.isSelected();
        usernameField.setEnabled(r);
        passwordField.setEnabled(r);
    }

    public boolean isUsernameAndPasswordRequired() {
        return requireCheckbox.isSelected();
    }

    /**
     * Set the state of the control.
     * @param required   True if the username and password are needed.
     * @param username  The username to display.  If null, the empty string will be set.
     * @param password  The password to display.  If null, the empty string will be set.
     */
    public void setUsernameAndPasswordRequired(boolean required, String username, String password) {
        if (username == null) username = "";
        if (password == null) password = "";
        requireCheckbox.setSelected(required);
        usernameField.setEnabled(required);
        usernameField.setText(username);
        passwordField.setEnabled(required);
        passwordField.setText(password);
        enableOrDisableComponents();
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }
}
