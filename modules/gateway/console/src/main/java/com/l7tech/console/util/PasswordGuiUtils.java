package com.l7tech.console.util;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.nio.CharBuffer;
import java.util.regex.Pattern;

/**
 * Utilities for password-handling GUI controls.
 */
public class PasswordGuiUtils {

    // Match a context variable name that refers to a secure password's plaintext
    public static final Pattern SECPASS_VAR_PATTERN = Pattern.compile("^\\$\\{secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext\\}$");

    // Match an expression that consists of a single %{}-enclosed secpsas reference.
    private static final Pattern SINGLE_SECPASS_PATTERN = Pattern.compile("^\\$\\{secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext\\}$");

    /**
     * Configure GUI controls for a field that can contain either a literal password or a ${secpass} reference but where
     * the secpass reference is strongly preferred.
     *
     * @param passwordField the password field to configure.  Required.
     * @param showPasswordCheckBox a "Show Password" checkbox to wire up to remove the masking character from the password field, or null.
     * @param plaintextPasswordWarningLabel a label in which to show a warning when a non-empty plaintext password is present, or null.
     */
    public static void configureOptionalSecurePasswordField(final JPasswordField passwordField, final JCheckBox showPasswordCheckBox, final JLabel plaintextPasswordWarningLabel) {
        if (showPasswordCheckBox != null) {
            Utilities.configureShowPasswordButton(showPasswordCheckBox, passwordField);
        }

        if (plaintextPasswordWarningLabel != null) {
            plaintextPasswordWarningLabel.setText("");
            passwordField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
                @Override
                public void run() {
                    if (!passwordField.isEnabled())
                        return;

                    final String msg;
                    final String tooltip;
                    char[] currentPass = passwordField.getPassword();
                    if (currentPass != null && currentPass.length > 0 && !SINGLE_SECPASS_PATTERN.matcher(CharBuffer.wrap(currentPass)).matches()) {
                        msg = "<HTML><FONT COLOR=\"RED\">Note: plaintext password.  Consider rewriting as secure password reference instead.";
                        tooltip="To avoid saving a plaintext password, write as ${secpass.NAME.plaintext} where NAME is the name of an entry in Manage Stored Passwords";
                    } else {
                        msg = "";
                        tooltip = null;
                    }
                    plaintextPasswordWarningLabel.setText(msg);
                    plaintextPasswordWarningLabel.setToolTipText(tooltip);
                }
            }));
        }
    }
}
