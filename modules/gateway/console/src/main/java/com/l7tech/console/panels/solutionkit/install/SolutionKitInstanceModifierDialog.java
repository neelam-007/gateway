package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URLDecoder;

/**
 * A dialog gets an input as an instance modifier.
 */
public class SolutionKitInstanceModifierDialog extends JDialog {
    private SquigglyTextField instanceModifierTextField;
    private JPanel mainPanel;
    private OkCancelPanel okCancelPanel;

    private boolean isOk;

    public SolutionKitInstanceModifierDialog(final Frame owner, final String instanceModifier, final int maxInstanceModifierLength) {
        super(owner, "Add an Instance Modifier", true);

        instanceModifierTextField.setText(instanceModifier);

        setContentPane(mainPanel);
        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okCancelPanel.getCancelButton());

        Utilities.setMaxLength(instanceModifierTextField.getDocument(), maxInstanceModifierLength);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(instanceModifierTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyTextFieldState(instanceModifierTextField, new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        return getPrefixedUrlErrorMsg(s);
                    }
                });
            }
        }, 500);

        instanceModifierTextField.getDocument().addDocumentListener(new RunOnChangeListener(){
            @Override
            protected void run() {
                okCancelPanel.getOkButton().setEnabled(
                    getPrefixedUrlErrorMsg(instanceModifierTextField.getText()) == null
                );
            }
        });

        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isOk = true;
                dispose();
            }
        });

        okCancelPanel.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
    }

    public boolean isOK() {
        return isOk;
    }

    public String getInstanceModifier() {
        return instanceModifierTextField.getText();
    }

    private String getPrefixedUrlErrorMsg(String prefix){
        // Service Routing URI must not start with '/ssg'
        if (prefix.startsWith("ssg")) {
            return "Instance modifier must not start with 'ssg', since Service Routing URI must not start with '/ssg'";
        }

        // validate for XML chars and new line char
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">", "\n"};
        for (String invalidChar : invalidChars) {
            if (prefix.contains(invalidChar)) {
                if (invalidChar.equals("\n")) invalidChar = "\\n";
                return "Invalid character '" + invalidChar + "' is not allowed in the installation prefix.";
            }
        }

        String testUri = "http://ssg.com:8080/" + prefix + "/query";
        if (!ValidationUtils.isValidUrl(testUri)) {
            return "Invalid prefix '" + prefix + "'. It must be possible to construct a valid routing URI using the prefix.";
        }

        try {
            URLDecoder.decode(prefix, "UTF-8");
        } catch (Exception e) {
            return "Invalid prefix '" + prefix + "'. It must be possible to construct a valid routing URL using the prefix.";
        }

        return null;
    }
}