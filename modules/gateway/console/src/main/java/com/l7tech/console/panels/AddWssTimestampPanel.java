/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.util.TimeUnit;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;

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
public class AddWssTimestampPanel extends ValidatedPanel<AddWssTimestamp> {
    private JPanel mainPanel;
    private JFormattedTextField expiryTimeField;
    private JCheckBox signatureRequiredCheckBox;
    private JComboBox expiryTimeUnitCombo;
    private JPanel signingOptionsPanel;
    private JRadioButton bstRadio;
    private JRadioButton strRadio;
    private JRadioButton issuerSerialRadio;

    private final AddWssTimestamp assertion;
    private TimeUnit oldTimeUnit;

    public AddWssTimestampPanel(AddWssTimestamp model) {
        super("expiryTime");
        this.assertion = model;
        init();
    }

    @Override
    protected void initComponents() {
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel(TimeUnit.ALL);
        final TimeUnit timeUnit = assertion.getTimeUnit();

        ButtonGroup bg = new ButtonGroup();
        bg.add(bstRadio);
        bg.add(strRadio);
        bg.add(issuerSerialRadio);

        oldTimeUnit = timeUnit;
        comboModel.setSelectedItem(timeUnit);
        expiryTimeUnitCombo.setModel(comboModel);
        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);

        expiryTimeField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });
        expiryTimeField.setValue((double) assertion.getExpiryMilliseconds() / timeUnit.getMultiplier());
        expiryTimeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {checkSyntax();}
            @Override
            public void removeUpdate(DocumentEvent e) {checkSyntax();}
            @Override
            public void changedUpdate(DocumentEvent e) {checkSyntax();}
        });

        expiryTimeUnitCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
                Double time = (Double)expiryTimeField.getValue();
                if (newTimeUnit != oldTimeUnit) {
                    long oldMillis = (long)(oldTimeUnit.getMultiplier() * time);
                    expiryTimeField.setValue((double) oldMillis / newTimeUnit.getMultiplier());
                }
                checkSyntax();
                oldTimeUnit = newTimeUnit;
            }
        });

        ActionListener modelUpdateActionListener = new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
                checkSyntax();
            }
        };
        signatureRequiredCheckBox.addActionListener(modelUpdateActionListener);
        signatureRequiredCheckBox.setSelected(assertion.isSignatureRequired());

        if ( KeyReference.BST.getName().equals(assertion.getKeyReference()) ) {
            bstRadio.setSelected(true);
        } else if ( KeyReference.ISSUER_SERIAL.getName().equals(assertion.getKeyReference()) ) {
            issuerSerialRadio.setSelected(true);
        } else {
            strRadio.setSelected(true);
        }
        bstRadio.addActionListener(modelUpdateActionListener);
        strRadio.addActionListener(modelUpdateActionListener);

        add(mainPanel, BorderLayout.CENTER);
        updateComponents();
    }

    private void updateComponents() {
        Utilities.setEnabled(signingOptionsPanel, signatureRequiredCheckBox.isSelected());
    }

    @Override
    protected void doUpdateModel() {
        TimeUnit tu = (TimeUnit)expiryTimeUnitCombo.getSelectedItem();
        Double num = (Double)expiryTimeField.getValue();
        assertion.setTimeUnit(tu);
        assertion.setExpiryMilliseconds((int)(num * tu.getMultiplier()));
        assertion.setSignatureRequired(signatureRequiredCheckBox.isSelected());
        if (bstRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.BST.getName());
        } else if (strRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.SKI.getName());
        } else if (issuerSerialRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.ISSUER_SERIAL.getName());
        } else {
            throw new IllegalStateException("Neither BST nor SKI selected");
        }
    }

    @Override
    protected AddWssTimestamp getModel() {
        return assertion;
    }

    @Override
    public void focusFirstComponent() {
        expiryTimeField.requestFocus();
    }
}
