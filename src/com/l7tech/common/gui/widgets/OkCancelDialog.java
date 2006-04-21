/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

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

    private final ValidatedPanel validatedPanel;

    public OkCancelDialog(Frame owner, String title, boolean modal, ValidatedPanel panel) {
        super(owner, title, modal);
        this.validatedPanel = panel;
        initialize();
    }

    public OkCancelDialog(Dialog owner, String title, boolean modal, ValidatedPanel panel) {
        super(owner, title, modal);
        this.validatedPanel = panel;
        initialize();
    }

    private void initialize() {
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        validatedPanel.addPropertyChangeListener("ok", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(evt.getNewValue() == Boolean.TRUE);
            }
        });

        validatedPanel.addPropertyChangeListener(validatedPanel.getPropertyName(), new PropertyChangeListener() {
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
        okButton.setEnabled(validatedPanel.isSyntaxOk());
        innerPanel.add(validatedPanel);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                validatedPanel.focusFirstComponent();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getContentPane().add(mainPanel);
    }

    private void cancel() {
        value = null;
        dispose();
    }

    public Object getValue() {
        return value;
    }


}
