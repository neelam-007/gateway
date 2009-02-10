/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.util.ComparisonOperator;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author alex
 */
public class BinaryPredicatePanel extends PredicatePanel<BinaryPredicate> {
    private JPanel mainPanel;
    private JTextField rightValueField;
    private JComboBox operatorCombo;
    private JCheckBox caseCheckbox;
    private JTextField leftValueField;
    private JComboBox negateCombo;
    private JLabel statusLabel;

    private final BinaryPredicate predicate;
    private final UpdateActionListener updateListener = new UpdateActionListener();

    private final NegateComboEntry NOT_NEGATED = new NegateComboEntry(false);
    private final NegateComboEntry NEGATED = new NegateComboEntry(true);

    public BinaryPredicatePanel(BinaryPredicate predicate, String expression) {
        super(predicate, expression);
        this.predicate = predicate;
        setStatusLabel(statusLabel);
        init();
    }

    private static final String IS = ComparisonAssertion.resources.getString("verb.is");
    private static final String IS_NOT = ComparisonAssertion.resources.getString("verb.isNot");
    private static final String DOES = ComparisonAssertion.resources.getString("verb.does");
    private static final String DOES_NOT = ComparisonAssertion.resources.getString("verb.doesNot");

    private class NegateComboEntry {
        public NegateComboEntry(boolean negate) {
            this.negate = negate;
        }

        @Override
        public String toString() {
            ComparisonOperator op = (ComparisonOperator) operatorCombo.getSelectedItem();
            if (op == ComparisonOperator.CONTAINS) {
                return negate ? DOES_NOT : DOES;
            } else {
                return negate ? IS_NOT : IS;
            }
        }

        private final boolean negate;
    }

    protected void initComponents() {
        operatorCombo.setModel(new DefaultComboBoxModel(ComparisonOperator.values()));
        operatorCombo.setSelectedItem(ComparisonOperator.EQ);
        operatorCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateState();
            }
        });

        operatorCombo.addActionListener(updateListener);
        rightValueField.getDocument().addDocumentListener(updateListener);
        negateCombo.addActionListener(updateListener);
        caseCheckbox.addActionListener(updateListener);

        leftValueField.setText(expression);
        rightValueField.setText(predicate.getRightValue());
        ComparisonOperator op = predicate.getOperator();
        operatorCombo.setSelectedItem(op == null ? ComparisonOperator.EQ : op);
        negateCombo.setModel(new DefaultComboBoxModel(new NegateComboEntry[]{NOT_NEGATED, NEGATED}));
        negateCombo.setSelectedItem(predicate.isNegated() ? NEGATED : NOT_NEGATED);
        caseCheckbox.setSelected(predicate.isCaseSensitive());

        updateState();

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateState() {
        ComparisonOperator op = (ComparisonOperator)operatorCombo.getSelectedItem();
        if (op != null && op.isUnary()) {
            rightValueField.setEnabled(false);
        } else {
            rightValueField.setEnabled(true);
        }
    }

    public void focusFirstComponent() {
        rightValueField.requestFocus();
    }

    protected void doUpdateModel() {
        predicate.setOperator((ComparisonOperator)operatorCombo.getSelectedItem());
        predicate.setRightValue(rightValueField.getText());
        final NegateComboEntry sel = (NegateComboEntry) negateCombo.getSelectedItem();
        predicate.setNegated(sel.negate);
        predicate.setCaseSensitive(caseCheckbox.isSelected());
    }

    @Override
    protected String getSyntaxError(BinaryPredicate model) {
        ComparisonOperator op = (ComparisonOperator)operatorCombo.getSelectedItem();
        if (op == null) return ComparisonAssertion.resources.getString("binaryPredicatePanel.error.operatorRequired");
        if (op.isUnary()) return null;

        String s = rightValueField.getText();
        if (s == null || s.length() == 0) return ComparisonAssertion.resources.getString("binaryPredicatePanel.error.rightValueRequired");
        return null;
    }

    private class UpdateActionListener implements ActionListener, DocumentListener {
        public void actionPerformed(ActionEvent e) {
            updateState();
            checkSyntax();
        }

        public void insertUpdate(DocumentEvent e) {
            updateState();
            checkSyntax();
        }

        public void removeUpdate(DocumentEvent e) {
            updateState();
            checkSyntax();
        }

        public void changedUpdate(DocumentEvent e) {
            updateState();
            checkSyntax();
        }
    }
}
