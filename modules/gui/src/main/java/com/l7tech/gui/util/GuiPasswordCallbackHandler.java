package com.l7tech.gui.util;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

/**
 * A CallbackHandler that knows how to display a password prompt dialog (using JOptionPane)
 * in response to PasswordCallback callbacks.
 */
public class GuiPasswordCallbackHandler implements CallbackHandler {

    //- PUBLIC

    public GuiPasswordCallbackHandler() {
        parent = null;
    }

    public GuiPasswordCallbackHandler( final Window parent ) {
        this.parent = parent;
    }

    @Override
    public void handle(Callback[] callbacks) {
        for (Callback callback : callbacks)
            if (callback instanceof PasswordCallback)
                handlePasswordCallback((PasswordCallback)callback);
    }

    //- PRIVATE

    private final Window parent;


    private void handlePasswordCallback(PasswordCallback passwordCallback) {
        final JPasswordField pwd = new JPasswordField(22);
        JOptionPane pane = new JOptionPane(pwd, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(parent, "Enter " + passwordCallback.getPrompt());
        dialog.addWindowFocusListener(new WindowAdapter(){
            @Override
            public void windowGainedFocus(WindowEvent e) {
                pwd.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        Object value = pane.getValue();
        if (value != null && (Integer)value == JOptionPane.OK_OPTION)
            passwordCallback.setPassword(pwd.getPassword());
    }
}
