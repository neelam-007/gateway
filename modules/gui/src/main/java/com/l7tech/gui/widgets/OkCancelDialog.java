/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gui.widgets;

import com.l7tech.gui.util.Utilities;

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
    private boolean readOnly;
    private boolean wasoked = false;

    private JButton cancelButton;
    private JButton okButton;
    private JPanel innerPanel;
    private JPanel mainPanel;

    private final ValidatedPanel validatedPanel;

    public static OkCancelDialog createOKCancelDialog(Component owner, String title, boolean modal, ValidatedPanel panel) {
        Window window = SwingUtilities.getWindowAncestor(owner);
        return new OkCancelDialog(window, title, modal, panel);
    }

    public OkCancelDialog(Window owner, String title, boolean modal, ValidatedPanel panel) {
        this( owner, title, modal, panel, false );
    }

    public OkCancelDialog(Window owner, String title, boolean modal, ValidatedPanel panel, boolean readOnly) {
        super(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS );
        this.validatedPanel = panel;
        this.readOnly = readOnly;
        initialize();
    }

    private void initialize() {
        Utilities.setEscAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        validatedPanel.addPropertyChangeListener("ok", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(!readOnly && evt.getNewValue() == Boolean.TRUE);
            }
        });

        validatedPanel.addPropertyChangeListener(validatedPanel.getPropertyName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                value = (V) evt.getNewValue();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wasoked = true;
                validatedPanel.updateModel();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        okButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(okButton);
        okButton.setEnabled(validatedPanel.isSyntaxOk() && !readOnly);
        innerPanel.add(validatedPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                validatedPanel.focusFirstComponent();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        okButton.setEnabled(validatedPanel.isSyntaxOk() && !readOnly);
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
