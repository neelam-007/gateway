/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.util.TimeUnit;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.common.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * @author alex
 */
public class ResponseWssTimestampPanel extends ValidatedPanel {
    private JPanel mainPanel;
    private JFormattedTextField expiryTimeField;
    private JComboBox expiryTimeUnitCombo;
    private JRadioButton bstRadio;
    private JRadioButton strRadio;

    private final ResponseWssTimestamp assertion;
    private TimeUnit oldTimeUnit;

    public ResponseWssTimestampPanel(ResponseWssTimestamp model) {
        super("expiryTime");
        this.assertion = model;
        init();
    }

    protected void initComponents() {
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel(TimeUnit.ALL);
        final TimeUnit timeUnit = assertion.getTimeUnit();

        ButtonGroup bg = new ButtonGroup();
        bg.add(bstRadio);
        bg.add(strRadio);

        oldTimeUnit = timeUnit;
        comboModel.setSelectedItem(timeUnit);
        expiryTimeUnitCombo.setModel(comboModel);
        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum(Double.valueOf(0));

        expiryTimeField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });
        expiryTimeField.setValue(new Double((double)assertion.getExpiryMilliseconds() / timeUnit.getMultiplier()));
        expiryTimeField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {updateModel();}
            public void removeUpdate(DocumentEvent e) {updateModel();}
            public void changedUpdate(DocumentEvent e) {updateModel();}
        });

        expiryTimeUnitCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
                Double time = (Double)expiryTimeField.getValue();
                if (newTimeUnit != oldTimeUnit) {
                    long oldMillis = (long)(oldTimeUnit.getMultiplier() * time.doubleValue());
                    expiryTimeField.setValue(new Double((double)oldMillis / newTimeUnit.getMultiplier()));
                }
                updateModel();
                oldTimeUnit = newTimeUnit;
            }
        });

        boolean bst = KeyReference.BST.getName().equals(assertion.getKeyReference());
        bstRadio.setSelected(bst);
        strRadio.setSelected(!bst);
        bstRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateModel();
            }
        });

        strRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateModel();
            }
        });

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateModel() {
        TimeUnit tu = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
        Double num = (Double)expiryTimeField.getValue();
        assertion.setTimeUnit(tu);
        assertion.setExpiryMilliseconds((int)(num.doubleValue() * tu.getMultiplier()));
        if (bstRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.BST.getName());
        } else if (strRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.SKI.getName());
        } else {
            throw new IllegalStateException("Neither BST nor SKI selected");
        }
        checkSyntax();
    }

    protected Object getModel() {
        return assertion;
    }

    public void focusFirstComponent() {
        expiryTimeField.requestFocus();
    }

    protected String getSyntaxError(Object model) {
        return null;
    }

    protected String getSemanticError(Object model) {
        return null;
    }
}
