/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.util.ComparisonOperator;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author alex
 */
public class BooleanBinaryPredicatePanel extends PredicatePanel<BinaryPredicate> {
    private JTextField leftExpressionField;
    private JPanel mainPanel;
    private JRadioButton trueRadio;
    private JRadioButton falseRadio;

    private final ActionListener buttonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            checkSyntax();
        }
    };

    public BooleanBinaryPredicatePanel(BinaryPredicate predicate, String expr) {
        super(predicate, expr);
        init();
    }

    protected void initComponents() {
        leftExpressionField.setText(expression);

        trueRadio.addActionListener(buttonListener);
        falseRadio.addActionListener(buttonListener);

        if (predicate.getRightValue() != null) {
            boolean b = "true".equalsIgnoreCase(predicate.getRightValue());
            trueRadio.setSelected(b);
            falseRadio.setSelected(!b);
        }

        add(mainPanel);
    }

    public void focusFirstComponent() {
        trueRadio.requestFocus();
    }

    @Override
    protected String getSyntaxError(BinaryPredicate model) {
        if (trueRadio.isSelected() || falseRadio.isSelected()) return null;
        return "True or False must be selected";
    }

    protected void doUpdateModel() {
        predicate.setOperator(ComparisonOperator.EQ);
        predicate.setRightValue(trueRadio.isSelected() ? "true" : "false");
        predicate.setNegated(false);
        predicate.setCaseSensitive(false);
    }
}
