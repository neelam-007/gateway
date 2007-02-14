/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.common.logic.CardinalityPredicate;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
 * @author alex
 */
public class CardinalityPredicatePanel extends PredicatePanel<CardinalityPredicate> {
    private JSpinner minSpinner;
    private JSpinner maxSpinner;
    private JPanel mainPanel;

    private final SpinnerNumberModel maxModel = new SpinnerNumberModel(1, 0, 9999, 1);
    private final SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 9999, 1);

    public CardinalityPredicatePanel(CardinalityPredicate predicate, String expression) {
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
        predicate.setMinValues(minModel.getNumber().intValue());
        predicate.setMaxValues(maxModel.getNumber().intValue());
    }

    public boolean isSyntaxOk() {
        return maxModel.getNumber().intValue() >= minModel.getNumber().intValue();
    }

    public void focusFirstComponent() {
        minSpinner.requestFocus();
    }
}
