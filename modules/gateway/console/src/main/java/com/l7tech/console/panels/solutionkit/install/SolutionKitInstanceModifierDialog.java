package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A dialog gets an input as an instance modifier.
 */
public class SolutionKitInstanceModifierDialog extends JDialog {
    private JTextField instancePrefixTextField;
    private JPanel mainPanel;
    private OkCancelPanel okCancelPanel;

    private boolean isOk;
    private String instanceModifier;

    public SolutionKitInstanceModifierDialog() {
        setTitle("Input Instance Modifier");
        setContentPane(mainPanel);

        Utilities.setMaxLength(instancePrefixTextField.getDocument(), 255);

        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isOk = true;
                instanceModifier = instancePrefixTextField.getText().trim();
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
        return instanceModifier;
    }
}