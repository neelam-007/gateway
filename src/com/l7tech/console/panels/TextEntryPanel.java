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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author alex
 */
public abstract class TextEntryPanel extends JPanel {
    private static final Pattern SPACER = Pattern.compile("\\s+");

    protected SquigglyTextField textField;
    protected JLabel statusLabel;
    protected JLabel promptLabel;
    protected JPanel mainPanel;
    private JScrollPane statusScrollPane;

    protected volatile boolean syntaxOk;
    private String propertyName;

    protected TextEntryPanel() {
        this("Prompt:", null, null);
    }

    public TextEntryPanel(String label, String propertyName, String initialValue) {
        promptLabel.setText(label);
        this.propertyName = propertyName;
        textField.setText(initialValue);
        init();
    }

    private void init() {
        statusScrollPane.setBorder(null);

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

        statusLabel.setText(null);
        Font font = statusLabel.getFont();
        float size = Math.round(font.getSize() * 1.5) / 2f;
        statusLabel.setFont(font.deriveFont(size));
        FontMetrics fm = statusLabel.getFontMetrics(font);
        statusScrollPane.setMinimumSize(new Dimension(-1, fm.getHeight()));

        checkSyntax();
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);
    }

    private void checkSemantic() {
        if (!syntaxOk) return;

        String err = getSemanticError(textField.getText());
        if (err == null) {
            textField.setNone();
        } else {
            Font font = statusLabel.getFont();
            statusLabel.setFont(font.deriveFont(Font.PLAIN));
            statusLabel.setText("<html>" + nospace(err));
        }
    }

    private String nospace(String s) {
        Matcher m = SPACER.matcher(s);
        return m.replaceAll("&nbsp;");
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
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

    public void focusText() {
        textField.requestFocus();
    }

    /**
     * Validate the syntax of the entered text, and return null if it's valid, or an error message if it's invalid.
     * Returning non-null will cause a {@link java.beans.PropertyChangeEvent} to be fired on the property
     * "ok" with the value {@link Boolean#FALSE}.
     * @param text the text to validate
     * @return null if the text is valid, or an error message if it's invalid
     */
    protected abstract String getSyntaxError(String text);

    /**
     * Validate the entered text semantically, and return null if it's valid, or an error message if it's invalid.
     * Returning non-null will cause the supplied message to be displayed in the GUI, but will not prevent
     * the value from being submitted to the caller.
     * @param text the text to validate
     * @return null if the text is valid, or an error message if it's invalid
     */
    protected abstract String getSemanticError(String text);

    private void fireGood() {
        firePropertyChange(propertyName, null, getModel(textField.getText()));
        firePropertyChange("ok", false, true);
    }

    /**
     * Override to send something other than the text in our {@link java.beans.PropertyChangeEvent}s
     * @param text the text from the text field.
     * @return a new model object.
     */
    protected Object getModel(String text) {
        return text;
    }

    public boolean isSyntaxOk() {
        return syntaxOk;
    }

    private void fireBad() {
        firePropertyChange(propertyName, null, null);
        firePropertyChange("ok", true, false);
    }

    public void checkSyntax() {
        String text = textField.getText();
        if (text == null || text.length() == 0) {
            badSyntax(null);
            return;
        }
        String statusString = getSyntaxError(text);
        if (statusString == null) {
            goodSyntax();
        } else {
            badSyntax(statusString);
        }
    }

    private void goodSyntax() {
        syntaxOk = true;
        textField.setNone();
        statusLabel.setText(null);
        fireGood();
    }

    private void badSyntax(String message) {
        syntaxOk = false;
        if (message != null) {
            statusLabel.setText("<html>" + nospace(message));
            Font font = statusLabel.getFont();
            statusLabel.setFont(font.deriveFont(Font.BOLD));
            textField.setColor(Color.RED);
            textField.setDotted();
            textField.setAll();
        }
        fireBad();
    }
}
