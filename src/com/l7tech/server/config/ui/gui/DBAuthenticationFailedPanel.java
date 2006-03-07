package com.l7tech.server.config.ui.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 11:20:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class DBAuthenticationFailedPanel extends JPanel{
    private JPanel mainPanel;
    private JTextField username;
    private JPasswordField password;

    public DBAuthenticationFailedPanel() {
        super();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String getUsername() {
        return username.getText();
    }

    public char[] getPassword() {
        return password.getPassword();
    }
}

