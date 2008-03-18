/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.TargetMessagePanel;
import com.l7tech.common.gui.widgets.ValidatedPanel;
import com.l7tech.common.util.TimeUnit;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

/**
 * @author alex
 */
public class RequestWssTimestampPanel extends ValidatedPanel<RequestWssTimestamp> {
    private JPanel mainPanel;
    private JFormattedTextField expiryTimeField;
    private JComboBox expiryTimeUnitCombo;
    private JCheckBox requireSignatureCheckBox;
    private TargetMessagePanel targetMessagePanel;

    private final RequestWssTimestamp assertion;
    private TimeUnit oldTimeUnit;

    private volatile boolean targetPanelValid = true;

    public RequestWssTimestampPanel(RequestWssTimestamp model) {
        super("expiryTime");
        this.assertion = model;
        targetMessagePanel.setModel(model);
        init();
    }

    protected void initComponents() {
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel(TimeUnit.ALL);
        final TimeUnit timeUnit = assertion.getTimeUnit();
        oldTimeUnit = timeUnit;
        comboModel.setSelectedItem(timeUnit);
        expiryTimeUnitCombo.setModel(comboModel);
        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum(Double.valueOf(0));

        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // Propagate inner panel's validity to this panel
                targetPanelValid = Boolean.TRUE.equals(evt.getNewValue());
                firePropertyChange("valid", null, evt.getNewValue());
                checkSyntax();
            }
        });

        requireSignatureCheckBox.setSelected(assertion.isSignatureRequired());
        requireSignatureCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkSyntax();
            }
        });
        expiryTimeField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });
        expiryTimeField.setValue(new Double((double)assertion.getMaxExpiryMilliseconds() / timeUnit.getMultiplier()));
        expiryTimeField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {checkSyntax();}
            public void removeUpdate(DocumentEvent e) {checkSyntax();}
            public void changedUpdate(DocumentEvent e) {checkSyntax();}
        });

        expiryTimeUnitCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
                Double time = (Double)expiryTimeField.getValue();
                if (newTimeUnit != oldTimeUnit) {
                    long oldMillis = (long)(oldTimeUnit.getMultiplier() * time.doubleValue());
                    expiryTimeField.setValue(new Double((double)oldMillis / newTimeUnit.getMultiplier()));
                }
                checkSyntax();
                oldTimeUnit = newTimeUnit;
            }
        });

        add(mainPanel, BorderLayout.CENTER);
    }

    protected void doUpdateModel() {
        TimeUnit tu = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
        Double num = (Double)expiryTimeField.getValue();
        assertion.setTimeUnit(tu);
        assertion.setMaxExpiryMilliseconds((int)(num.doubleValue() * tu.getMultiplier()));
        assertion.setSignatureRequired(requireSignatureCheckBox.isSelected());
        targetMessagePanel.updateModel(assertion);
    }

    protected RequestWssTimestamp getModel() {
        return assertion;
    }

    @Override
    protected String getSyntaxError(RequestWssTimestamp model) {
        if (!targetPanelValid) return "Variable name is required";
        return null;
    }

    public void focusFirstComponent() {
        expiryTimeField.requestFocus();
    }

}
