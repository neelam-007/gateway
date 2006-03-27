/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class OkCancelDialog extends JDialog {
    private Object value;

    private JButton cancelButton;
    private JButton okButton;
    private JPanel innerPanel;
    private JPanel mainPanel;

    private final TextEntryPanel textEntryPanel;

    public OkCancelDialog(Frame owner, String title, boolean modal, TextEntryPanel panel) {
        super(owner, title, modal);
        this.textEntryPanel = panel;
        initialize();
    }

    public OkCancelDialog(Dialog owner, String title, boolean modal, TextEntryPanel panel) {
        super(owner, title, modal);
        this.textEntryPanel = panel;
        initialize();
    }

    private void initialize() {
        Actions.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        textEntryPanel.addPropertyChangeListener("ok", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(evt.getNewValue() == Boolean.TRUE);
            }
        });

        textEntryPanel.addPropertyChangeListener(textEntryPanel.getPropertyName(), new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                value = evt.getNewValue();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        okButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(okButton);
        okButton.setEnabled(textEntryPanel.isSyntaxOk());
        innerPanel.setLayout(new BorderLayout());
        innerPanel.add(textEntryPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textEntryPanel.focusText();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        setContentPane(mainPanel);
    }

    private void cancel() {
        value = null;
        dispose();
    }

    public Object getValue() {
        return value;
    }


}
