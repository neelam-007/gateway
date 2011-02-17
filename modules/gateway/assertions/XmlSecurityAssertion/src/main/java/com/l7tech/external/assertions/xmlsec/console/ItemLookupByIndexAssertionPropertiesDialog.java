package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.xmlsec.ItemLookupByIndexAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ItemLookupByIndexAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ItemLookupByIndexAssertion> {
    private JPanel contentPane;
    private JTextField indexValueField;
    private JFormattedTextField multivaluedVarNameField;
    private JPanel outputVarNamePanel;
    private TargetVariablePanel outputVarNameField;

    public ItemLookupByIndexAssertionPropertiesDialog(Frame owner, ItemLookupByIndexAssertion assertion) {
        super(ItemLookupByIndexAssertion.class, owner, assertion.meta().get(AssertionMetadata.SHORT_NAME) + " Properties", true);
        initComponents();
        setData(assertion);
    }

    private String nonull(String s) { return s == null ? "" : s; }

    @Override
    public void setData(ItemLookupByIndexAssertion assertion) {
        indexValueField.setText(nonull(assertion.getIndexValue()));
        multivaluedVarNameField.setText(nonull(assertion.getMultivaluedVariableName()));
        outputVarNameField.setVariable(nonull(assertion.getOutputVariableName()));
        outputVarNameField.setAssertion(assertion,getPreviousAssertion());
    }

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    @Override
    public ItemLookupByIndexAssertion getData(ItemLookupByIndexAssertion assertion) throws ValidationException {
        final String indexValue = indexValueField.getText();
        if (Syntax.getReferencedNames(indexValue).length <= 0 && !isNumber(indexValue)) {
            throw new ValidationException("The index value must either be a number or an interpolated ${variable} that will expand to a number.");
        }

        assertion.setIndexValue(indexValue);
        assertion.setMultivaluedVariableName(VariablePrefixUtil.fixVariableName(multivaluedVarNameField.getText()));
        assertion.setOutputVariableName(VariablePrefixUtil.fixVariableName(outputVarNameField.getVariable()));
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {

        outputVarNameField = new TargetVariablePanel();
        outputVarNamePanel.setLayout(new BorderLayout());
        outputVarNamePanel.add(outputVarNameField, BorderLayout.CENTER);
        outputVarNameField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(!isReadOnly() && outputVarNameField .isEntryValid());
            }
        });
        
        return contentPane;
    }
}
