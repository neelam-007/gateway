/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.gui.widgets.SquigglyTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author alex
 */
public abstract class TextEntryPanel extends ValidatedPanel {
    protected SquigglyTextField textField;
    protected JLabel promptLabel;
    protected JPanel mainPanel;
    private JLabel statusLabel;
    private JScrollPane statusScrollPane;

    private final String label;
    private final String initialValue;

    protected TextEntryPanel() {
        this("Prompt:", null, null);
    }

    public TextEntryPanel(String label, String propertyName, String initialValue) {
        super(propertyName);
        setStatusLabel(statusLabel);
        this.label = label;
        this.initialValue = initialValue;
        init();
    }

    protected void initComponents() {
        statusScrollPane.setBorder(null);
        promptLabel.setText(label);
        textField.setText(initialValue);
        FontMetrics fm = statusLabel.getFontMetrics(statusLabel.getFont());
        statusScrollPane.setMinimumSize(new Dimension(-1, fm.getHeight()));

        TextComponentPauseListenerManager.registerPauseListener(textField, new PauseListener() {
            public void textEntryPaused(JTextComponent component, long msecs) {
                checkSemantic();
            }

            public void textEntryResumed(JTextComponent component) {
                syntaxOk = false;
                statusLabel.setText(null);
            }
        }, 1500);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                checkSyntax();
            }

            public void removeUpdate(DocumentEvent e) {
                checkSyntax();
            }

            public void changedUpdate(DocumentEvent e) {
                checkSyntax();
            }
        });

        add(mainPanel, BorderLayout.CENTER);
    }

    public String getPrompt() {
        return promptLabel.getText();
    }

    public void setPrompt(String label) {
        promptLabel.setText(label);
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String s) {
        textField.setText(s);
    }

    public void focusFirstComponent() {
        textField.requestFocus();
    }

    protected void goodSyntax() {
        textField.setNone();
    }

    protected Object getModel() {
        if (textField == null) return null;
        return textField.getText();
    }

    protected void badSyntax() {
        textField.setColor(Color.RED);
        textField.setDotted();
        textField.setAll();
    }
}
