package com.l7tech.console.panels;

import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ForEachLoopAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ForEachLoopAssertion> {
    private JPanel mainPanel;
    private JTextField loopVariableField;
    private JCheckBox limitMaximumIterationsCheckBox;
    private JTextField prefixField;
    private JTextField limitField;

    public ForEachLoopAssertionPropertiesDialog(Frame parent, ForEachLoopAssertion assertion) {
        super(ForEachLoopAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);

        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        limitField.setDocument(new NumberField(7));
        limitMaximumIterationsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisable();
            }
        });
        Utilities.enableGrayOnDisabled(limitField);
        return mainPanel;
    }

    @Override
    public void setData(ForEachLoopAssertion assertion) {
        final String loopVar = assertion.getLoopVariableName();
        loopVariableField.setText(loopVar == null ? "" : loopVar);

        final String prefix = assertion.getVariablePrefix();
        prefixField.setText(prefix == null ? "" : prefix);

        final int limit = assertion.getIterationLimit();
        final boolean haveLimit = limit > 0;
        limitMaximumIterationsCheckBox.setSelected(haveLimit);
        limitField.setText(haveLimit ? String.valueOf(limit) : "");

        enableOrDisable();
    }

    @Override
    public ForEachLoopAssertion getData(ForEachLoopAssertion assertion) throws ValidationException {
        if (loopVariableField.getText().trim().length() < 1)
            throw new ValidationException("A loop variable is required.");
        assertion.setLoopVariableName(loopVariableField.getText().trim());

        if (prefixField.getText().trim().length() < 1)
            throw new ValidationException("A variable prefix is required.");
        assertion.setVariablePrefix(prefixField.getText().trim());

        final boolean haveLimit = limitMaximumIterationsCheckBox.isSelected();
        final String limit = limitField.getText();
        if (haveLimit && (limit == null || limit.trim().length() < 1))
            throw new ValidationException("An iteration limit is required.");
        assertion.setIterationLimit(haveLimit ? Integer.parseInt(limit) : 0);

        return assertion;
    }

    private void enableOrDisable() {
        limitField.setEnabled(limitMaximumIterationsCheckBox.isSelected());
    }
}
