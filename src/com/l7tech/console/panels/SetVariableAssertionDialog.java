/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.PolicyVariableUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

/**
 * GUI for {@link com.l7tech.policy.assertion.SetVariableAssertion}
 */
public class SetVariableAssertionDialog extends JDialog {
    private JButton cancelButton;
    private JButton okButton;
    private JTextField variableNameField;
    private JPanel mainPanel;
    private JTextField variableNameStatusField;
    private JTextArea expressionStatusField;
    private JTextArea expressionField;

    private boolean assertionModified;
    private final Set<String> predecessorVariables;

    public SetVariableAssertionDialog(Frame owner, final SetVariableAssertion assertion) throws HeadlessException {
        this(owner, assertion, null);
    }

    public SetVariableAssertionDialog(Frame owner, final SetVariableAssertion assertion, final Assertion contextAssertion) throws HeadlessException {
        super(owner, "Set Variable", true);

        add(mainPanel);

        variableNameField.setText(assertion.getVariableToSet());
        expressionField.setText(assertion.getExpression());

        updateFast();

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFast(); }
            public void removeUpdate(DocumentEvent e) { updateFast(); }
            public void changedUpdate(DocumentEvent e) { updateFast(); }
        };

        predecessorVariables = contextAssertion==null ?
                PolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet() :
                PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(contextAssertion).keySet();

        variableNameField.getDocument().addDocumentListener(dl);
        expressionField.getDocument().addDocumentListener(dl);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setVariableToSet(variableNameField.getText());
                assertion.setExpression(expressionField.getText());
                assertionModified = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });


        TextComponentPauseListenerManager.registerPauseListener(expressionField, new PauseListener() {
            public void textEntryPaused(JTextComponent component, long msecs) {
                updateSlow();
            }

            public void textEntryResumed(JTextComponent component) {
                expressionStatusField.setText("");
            }
        }, 500);

        TextComponentPauseListenerManager.registerPauseListener(variableNameField, new PauseListener() {
            public void textEntryPaused(JTextComponent component, long msecs) {
                updateSlow();
            }

            public void textEntryResumed(JTextComponent component) {
                variableNameStatusField.setText("");
            }
        }, 500);
    }

    private void updateFast() {
        okButton.setEnabled(variableNameField.getText().length() > 0 && expressionField.getText().length() > 0);
    }

    private void updateSlow() {
        boolean varOk = false;
        boolean exprOk;
        String var = variableNameField.getText();
        String expr = expressionField.getText();
        if (var == null || var.length() == 0 || expr == null || expr.length() == 0) {
            okButton.setEnabled(false);
            return;
        }

        VariableMetadata meta = BuiltinVariables.getMetadata(var);
        if (meta != null) {
            if (meta.isSettable()) {
                variableNameStatusField.setText("OK (Built-in, settable)");
                varOk = true;
            } else {
                variableNameStatusField.setForeground(Color.RED);
                variableNameStatusField.setFont(variableNameStatusField.getFont().deriveFont(Font.BOLD));
                variableNameStatusField.setText("Built-in, not settable!");
            }
        } else {
            variableNameStatusField.setText("OK");
            varOk = true;
        }

        String[] names = ExpandVariables.getReferencedNames(expr);
        exprOk = true;
        StringBuilder messages = new StringBuilder();
        for (String name : names) {
            meta = BuiltinVariables.getMetadata(name);
            if (meta == null && !predecessorVariables.contains(name)) {
                exprOk = false;
                if (messages.length() > 0) messages.append("\n");
                messages.append(name).append(": No such variable");
            }
        }
        expressionStatusField.setText(exprOk ? "OK" : messages.toString());

        okButton.setEnabled(varOk && exprOk);
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
