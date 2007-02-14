/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.common.logic.StringLengthPredicate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author alex
 */
public class StringLengthPredicatePanel extends PredicatePanel<StringLengthPredicate> {
    private JSpinner minSpinner;
    private JSpinner maxSpinner;
    private JPanel mainPanel;

    private final SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 9999, 1);
    private final SpinnerNumberModel maxModel = new SpinnerNumberModel(1, 0, 9999, 1);

    public StringLengthPredicatePanel(StringLengthPredicate predicate, String expression) {
        super(predicate, expression);
        init();
    }

    protected void initComponents() {
        minSpinner.setModel(minModel);
        maxSpinner.setModel(maxModel);

        final ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                checkSyntax();
            }
        };

        minSpinner.addChangeListener(listener);
        maxSpinner.addChangeListener(listener);

        checkSyntax();
        add(mainPanel);
    }

    protected void doUpdateModel() {
        predicate.setMinLength(minModel.getNumber().intValue());
        predicate.setMaxLength(maxModel.getNumber().intValue());
    }

    public boolean isSyntaxOk() {
        return maxModel.getNumber().intValue() >= minModel.getNumber().intValue();
    }

    public void focusFirstComponent() {
        minSpinner.requestFocus();
    }
}