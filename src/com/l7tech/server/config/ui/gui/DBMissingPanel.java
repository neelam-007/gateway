package com.l7tech.server.config.ui.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 19, 2005
 * Time: 10:46:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DBMissingPanel extends JPanel{
    private JPanel mainPanel;
    private JTextPane textPane1;
    private JTextField username;
    private JPasswordField password;

    public DBMissingPanel() {
        super();
        init();
    }

    private void init() {
        textPane1.setBackground(mainPanel.getBackground());
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
