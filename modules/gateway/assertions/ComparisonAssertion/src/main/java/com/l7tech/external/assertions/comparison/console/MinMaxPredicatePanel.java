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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

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
        public void stateChanged(ChangeEvent e) { checkSyntax(); }
    };

    private final DocumentListener spinnerDocumentListener = new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { checkSyntax(); }
        public void removeUpdate(DocumentEvent e) { checkSyntax(); }
        public void changedUpdate(DocumentEvent e) { checkSyntax(); }
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

        minSpinner.setEditor(new JSpinner.NumberEditor(minSpinner, "#"));
        maxSpinner.setEditor(new JSpinner.NumberEditor(maxSpinner, "#"));

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

        // TODO http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4760088
        ((JSpinner.DefaultEditor)minSpinner.getEditor()).getTextField().getDocument().addDocumentListener(spinnerDocumentListener);
        ((JSpinner.DefaultEditor)maxSpinner.getEditor()).getTextField().getDocument().addDocumentListener(spinnerDocumentListener);
        minModel.addChangeListener(spinnerChangeListener);
        maxModel.addChangeListener(spinnerChangeListener);

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
        String smax = ((JSpinner.DefaultEditor)maxSpinner.getEditor()).getTextField().getText();
        String smin = ((JSpinner.DefaultEditor)minSpinner.getEditor()).getTextField().getText();
        int min;
        {
            try {
                min = smin == null || smin.length() == 0 ? 0 : Integer.valueOf(smin);
                if (min < 0) return ComparisonAssertion.resources.getString("minMaxPredicatePanel.minBoundError");
            } catch (NumberFormatException e) {
                String msg = ComparisonAssertion.resources.getString("minMaxPredicatePanel.formatError");
                return MessageFormat.format(msg, smin);
            }
        }

        if (unlimitedCheckBox.isSelected()) return null;

        int max;
        {
            try {
                max = smax == null || smax.length() == 0 ? 0 : Integer.valueOf(smax);
                if (max < 0) return ComparisonAssertion.resources.getString("minMaxPredicatePanel.maxBoundError");
            } catch (NumberFormatException e) {
                String msg = ComparisonAssertion.resources.getString("minMaxPredicatePanel.formatError");
                return MessageFormat.format(msg, smax);
            }
        }

        if (max >= min) return null;

        return ComparisonAssertion.resources.getString("minMaxPredicatePanel.boundsError");
    }

    public void focusFirstComponent() {
        minSpinner.requestFocus();
    }
}
