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

/**
 * @param <V> the payload value type
 */
public class OkCancelDialog<V> extends JDialog {
    private V value;
    private final boolean readOnly;
    private boolean wasoked = false;

    private JButton cancelButton;
    private JButton okButton;
    private JPanel innerPanel;
    private JPanel mainPanel;

    private final ValidatedPanel validatedPanel;

    public static OkCancelDialog createOKCancelDialog(Component owner, String title, boolean modal, ValidatedPanel panel) {
        OkCancelDialog dialog;
        Window window = SwingUtilities.getWindowAncestor(owner);

        if (window instanceof Frame) {
            dialog = new OkCancelDialog((Frame)window, title, modal, panel);
        } else {
            dialog = new OkCancelDialog((Dialog)window, title, modal, panel);
        }

        return dialog;
    }

    public OkCancelDialog(Frame owner, String title, boolean modal, ValidatedPanel panel) {
        this( owner, title, modal, panel, false );
    }

    public OkCancelDialog(Frame owner, String title, boolean modal, ValidatedPanel panel, boolean readOnly) {
        super(owner, title, modal);
        this.validatedPanel = panel;
        this.readOnly = readOnly;
        initialize();
    }

    public OkCancelDialog(Dialog owner, String title, boolean modal, ValidatedPanel panel) {
        this( owner, title, modal, panel, false );
    }

    public OkCancelDialog(Dialog owner, String title, boolean modal, ValidatedPanel panel, boolean readOnly) {
        super(owner, title, modal);
        this.validatedPanel = panel;
        this.readOnly = readOnly;
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
                okButton.setEnabled(!readOnly && evt.getNewValue() == Boolean.TRUE);
            }
        });

        validatedPanel.addPropertyChangeListener(validatedPanel.getPropertyName(), new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                value = (V) evt.getNewValue();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wasoked = true;
                validatedPanel.updateModel();
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
        okButton.setEnabled(validatedPanel.isSyntaxOk() && !readOnly);
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

    public V getValue() {
        return value;
    }

    public boolean wasOKed() {
        return wasoked;
    }
}
