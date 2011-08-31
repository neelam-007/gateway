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
    private JCheckBox limitMaximumIterationsCheckBox;
    private JTextField limitField;
    private JPanel prefixFieldPanel;
    private JPanel loopVariableFieldPanel;

    private TargetVariablePanel loopVariableField;
    private TargetVariablePanel prefixField;

    public ForEachLoopAssertionPropertiesDialog(Frame parent, ForEachLoopAssertion assertion) {
        super(ForEachLoopAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);

        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        prefixField = new TargetVariablePanel();
        prefixFieldPanel.setLayout(new BorderLayout());
        prefixFieldPanel.add(prefixField, BorderLayout.CENTER);

        loopVariableField = new TargetVariablePanel();
        loopVariableField.setValueWillBeRead(true);
        loopVariableField.setValueWillBeWritten(false);
        loopVariableFieldPanel.setLayout(new BorderLayout());
        loopVariableFieldPanel.add(loopVariableField, BorderLayout.CENTER);

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
        loopVariableField.setVariable(loopVar == null ? "" : loopVar);
        loopVariableField.setAssertion(assertion, getPreviousAssertion());

        final String prefix = assertion.getVariablePrefix();
        prefixField.setVariable(prefix == null ? "" : prefix);
        prefixField.setAssertion(assertion, getPreviousAssertion());

        final int limit = assertion.getIterationLimit();
        final boolean haveLimit = limit > 0;
        limitMaximumIterationsCheckBox.setSelected(haveLimit);
        limitField.setText(haveLimit ? String.valueOf(limit) : "");

        enableOrDisable();
    }

    @Override
    public ForEachLoopAssertion getData(ForEachLoopAssertion assertion) throws ValidationException {
        if (loopVariableField.getVariable().trim().length() < 1)
            throw new ValidationException("A loop variable is required.");
        assertion.setLoopVariableName(loopVariableField.getVariable().trim());

        if (!prefixField.isEntryValid())
            throw new ValidationException("A valid variable prefix is required.");
        assertion.setVariablePrefix(prefixField.getVariable().trim());

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
