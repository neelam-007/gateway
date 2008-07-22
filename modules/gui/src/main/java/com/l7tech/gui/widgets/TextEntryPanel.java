/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gui.widgets;

import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author alex
 */
public abstract class TextEntryPanel extends ValidatedPanel<String> {
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

    public void setEnabled(boolean enable) {
        textField.setEnabled(enable);
        super.setEnabled(enable);
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
                checkSyntax();
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

    protected String getModel() {
        if (textField == null) return null;
        return textField.getText();
    }

    /** Nothing to do here--in this case {@link TextField#getText()} <em>is</em> the model */
    protected void doUpdateModel() {
    }

    protected void goodSyntax() {
        textField.setNone();
    }

    protected void badSyntax() {
        textField.setColor(Color.RED);
        textField.setDotted();
        Range r = getHighlightRange();
        if (r.from < 0 && r.to < 0) {
            textField.setAll();
        } else {
            textField.setRange(r.from, r.to);
        }

    }

    /**
     * Override to indicate a range of characters that is erroneous.  Specify negative values for {@link Range#from} and
     * {@link Range#to} == -1 to select the entire string.
     */
    protected Range getHighlightRange() {
        return DEFAULT_RANGE_ALL;
    }

    protected static final Range DEFAULT_RANGE_ALL = new Range(-1, -1);

    protected static class Range {
        protected final int from;
        protected final int to;

        protected Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }
}
