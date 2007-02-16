/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.external.assertions.comparison.MinMaxPredicate;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author alex
 */
public abstract class MinMaxPredicatePanel<P extends MinMaxPredicate> extends PredicatePanel<P> {
    private JSpinner minSpinner;
    private JSpinner maxSpinner;
    private JPanel mainPanel;
    private JCheckBox unlimitedCheckBox;
    private JLabel statusLabel;
    private JPanel minPanel;
    private JPanel maxPanel;
    private JLabel minUnitsLabel;
    private JLabel maxUnitsLabel;
    private final SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, null, 1);
    private final SpinnerNumberModel maxModel = new SpinnerNumberModel(1, 0, null, 1);
    private final ChangeListener spinnerChangeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            checkSyntax();
        }
    };

    protected MinMaxPredicatePanel(P predicate, String expr) {
        super(predicate, expr);
    }

    protected void initComponents() {
        String prefix = predicate.getSimpleName() + "PredicatePanel.";
        minPanel.setBorder(new TitledBorder(ComparisonAssertion.resources.getString(prefix + "minPanel.label")));
        maxPanel.setBorder(new TitledBorder(ComparisonAssertion.resources.getString(prefix + "maxPanel.label")));

        String u = ComparisonAssertion.resources.getString(prefix + "unitsLabel");
        minUnitsLabel.setText(u);
        maxUnitsLabel.setText(u);
        setStatusLabel(statusLabel);

        minSpinner.setModel(minModel);
        maxSpinner.setModel(maxModel);

        minSpinner.setValue(predicate.getMin());
        final int max = predicate.getMax();
        if (max < 0) {
            unlimitedCheckBox.setSelected(true);
            maxSpinner.setEnabled(false);
        } else {
            unlimitedCheckBox.setSelected(false);
            maxSpinner.setEnabled(true);
            maxSpinner.setValue(max);
        }

        maxModel.addChangeListener(spinnerChangeListener);
        minModel.addChangeListener(spinnerChangeListener);

        unlimitedCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                maxSpinner.setEnabled(!unlimitedCheckBox.isSelected());
                checkSyntax();
            }
        });

        add(mainPanel, BorderLayout.CENTER);
        revalidate();
    }

    protected void doUpdateModel() {
        predicate.setMin(minModel.getNumber().intValue());
        if (unlimitedCheckBox.isSelected()) {
            predicate.setMax(-1);
        } else {
            predicate.setMax(maxModel.getNumber().intValue());
        }
    }


    protected String getSyntaxError(P model) {
        if (maxModel.getNumber().intValue() >= minModel.getNumber().intValue() || unlimitedCheckBox.isSelected()) return null;
        return ComparisonAssertion.resources.getString("minMaxPredicatePanel.boundsError");
    }

    public void focusFirstComponent() {
        minSpinner.requestFocus();
    }
}
