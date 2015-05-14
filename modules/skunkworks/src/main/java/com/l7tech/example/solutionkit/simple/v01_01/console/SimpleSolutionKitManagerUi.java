package com.l7tech.example.solutionkit.simple.v01_01.console;

import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple example of a customized UI for the Solution Kit Manager.
 */
public class SimpleSolutionKitManagerUi extends SolutionKitManagerUi {
    final SolutionKitManagerContext context = new SolutionKitManagerContext();
    final StringBuilder message = new StringBuilder();

    @Override
    public JButton getButton() {
        JButton button = new JButton("Custom UI");
        button.setLayout(new BorderLayout());
        message.setLength(0);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                message.append("Button action performed.");
                JOptionPane.showMessageDialog(getParentPanel(), message.toString());
            }
        });

        return button;
    }

    @Override
    public SolutionKitManagerUi initialize() {
        setContext(context);
        context.setCustomDataObject(message);
        return this;
    }
}
