package com.l7tech.server.config.ui.gui;

import javax.swing.*;

/**
 * User: megery
 * Date: May 23, 2007
 * Time: 2:21:27 PM
 */
public class GenericPasswordPanel extends JPanel{
    JPasswordField pwdFld;

    public GenericPasswordPanel(String msg) {
        super();
        pwdFld = new JPasswordField();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JLabel(msg));
        add(pwdFld);
    }

    public char[] getPassword() {
        return pwdFld.getPassword();
    }
}
