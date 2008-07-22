/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.RegexPredicate;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.PauseListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.awt.*;

/**
 * @author alex
 */
public class RegexPredicatePanel extends PredicatePanel<RegexPredicate> {
    private SquigglyTextField regexField;
    private JPanel mainPanel;
    private JLabel statusLabel;

    public RegexPredicatePanel(RegexPredicate predicate, String expression) {
        super(predicate, expression);
        setStatusLabel(statusLabel);
        TextComponentPauseListenerManager.registerPauseListener(regexField, new PauseListener() {
            public void textEntryPaused(JTextComponent component, long msecs) {
                checkSyntax();
                checkSemantic();
            }

            public void textEntryResumed(JTextComponent component) {
                syntaxOk = false;
                statusLabel.setText(null);
            }
        }, 500);
        init();
    }

    protected void initComponents() {
        regexField.setText(predicate.getPattern());

        regexField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkSyntax(); }
            public void removeUpdate(DocumentEvent e) { checkSyntax(); }
            public void changedUpdate(DocumentEvent e) { checkSyntax(); }
        });
        
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void doUpdateModel() {
        predicate.setPattern(regexField.getText());
    }

    @Override
    protected String getSyntaxError(RegexPredicate model) {
        try {
            Pattern.compile(regexField.getText());
            return null;
        } catch (PatternSyntaxException e) {
            return ExceptionUtils.getMessage(e);
        }
    }

    public void focusFirstComponent() {
        regexField.requestFocus();
    }

    protected void goodSyntax() {
        regexField.setNone();
    }

    protected void badSyntax() {
        regexField.setColor(Color.RED);
        regexField.setDotted();
        regexField.setAll();
    }

}
