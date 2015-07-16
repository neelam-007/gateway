package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gateway.common.api.solutionkit.InstanceModifier;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                        // Return a validation warning by calling a validation method, validatePrefixedURI.
                        return InstanceModifier.validatePrefixedURI(s);
                    }
                });
            }
        }, 500);

        instanceModifierTextField.getDocument().addDocumentListener(new RunOnChangeListener(){
            @Override
            protected void run() {
                final String validationWarning = InstanceModifier.validatePrefixedURI(instanceModifierTextField.getText());
                // If the validation warning is null, then set the OK button enabled.  Otherwise, set to be disabled.
                okCancelPanel.getOkButton().setEnabled(validationWarning == null);
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
}