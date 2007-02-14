/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.common.logic.BinaryPredicate;
import com.l7tech.common.util.ComparisonOperator;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class BinaryPredicatePanel extends PredicatePanel<BinaryPredicate> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.console.resources.ComparisonAssertion");

    private JPanel mainPanel;
    private JTextField rightValueField;
    private JComboBox operatorCombo;
    private JCheckBox caseCheckbox;
    private JTextField leftValueField;
    private JComboBox negateCombo;
    private JLabel statusLabel;

    private final BinaryPredicate predicate;
    private final UpdateActionListener updateListener = new UpdateActionListener();
    private final DefaultComboBoxModel isModel = new DefaultComboBoxModel(new Object[] { IS, IS_NOT });
    private final DefaultComboBoxModel doesModel = new DefaultComboBoxModel(new Object[] { DOES, DOES_NOT });

    public BinaryPredicatePanel(BinaryPredicate predicate, String expression) {
        super(predicate, expression);
        this.predicate = predicate;
        setStatusLabel(statusLabel);
        init();
    }

    private static final String IS = resources.getString("binaryPredicatePanel.negateCombo.notNegatedLabel");
    private static final String IS_NOT = resources.getString("binaryPredicatePanel.negateCombo.negatedLabel");
    private static final String DOES = resources.getString("binaryPredicatePanel.negateCombo.doesContainLabel");
    private static final String DOES_NOT = resources.getString("binaryPredicatePanel.negateCombo.doesNotContainLabel");

    protected void initComponents() {
        operatorCombo.setModel(new DefaultComboBoxModel(ComparisonOperator.getValues()));
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
        negateCombo.setModel(isModel);
        negateCombo.setSelectedItem(predicate.isNegated() ? IS_NOT : IS);
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

        int sel = negateCombo.getSelectedIndex();
        if (op == ComparisonOperator.CONTAINS) {
            negateCombo.setModel(doesModel);
        } else {
            negateCombo.setModel(isModel);
        }
        negateCombo.setSelectedIndex(sel);
    }

    public void focusFirstComponent() {
        rightValueField.requestFocus();
    }

    protected void doUpdateModel() {
        predicate.setOperator((ComparisonOperator)operatorCombo.getSelectedItem());
        predicate.setRightValue(rightValueField.getText());
        predicate.setNegated(negateCombo.getSelectedItem() == IS_NOT);
        predicate.setCaseSensitive(caseCheckbox.isSelected());
    }

    @Override
    protected String getSyntaxError(BinaryPredicate model) {
        ComparisonOperator op = (ComparisonOperator)operatorCombo.getSelectedItem();
        if (op == null) return resources.getString("binaryPredicatePanel.error.operatorRequired");
        if (op.isUnary()) return null;

        String s = rightValueField.getText();
        if (s == null || s.length() == 0) return resources.getString("binaryPredicatePanel.error.rightValueRequired");
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
