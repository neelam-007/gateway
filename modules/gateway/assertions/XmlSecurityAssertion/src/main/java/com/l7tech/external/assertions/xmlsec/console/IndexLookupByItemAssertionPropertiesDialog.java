package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.xmlsec.IndexLookupByItemAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Property dialog for {@link com.l7tech.external.assertions.xmlsec.IndexLookupByItemAssertion}.
 */
public class IndexLookupByItemAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<IndexLookupByItemAssertion> {
    private JPanel contentPane;
    private JTextField multivaluedVarNameField;
    private JTextField valueToSearchForVarNameField;
    private JPanel outputVarNamePanel;
    private TargetVariablePanel outputVarNameField;
    private JCheckBox allowMultipleMatchesCheckBox;

    public IndexLookupByItemAssertionPropertiesDialog(Frame owner, IndexLookupByItemAssertion assertion) {
        super(IndexLookupByItemAssertion.class, owner, assertion.meta().get(AssertionMetadata.SHORT_NAME) + " Properties", true);
        initComponents();
        setData(assertion);
    }

    private String nonull(String in) {
        return in == null ? "" : in;
    }

    @Override
    public void setData(IndexLookupByItemAssertion assertion) {
        multivaluedVarNameField.setText(nonull(assertion.getMultivaluedVariableName()));
        valueToSearchForVarNameField.setText(nonull(assertion.getValueToSearchForVariableName()));
        outputVarNameField.setVariable(nonull(assertion.getOutputVariableName()));
        outputVarNameField.setAssertion(assertion,getPreviousAssertion());
        allowMultipleMatchesCheckBox.setSelected(assertion.isAllowMultipleMatches());
    }

    @Override
    public IndexLookupByItemAssertion getData(IndexLookupByItemAssertion assertion) throws ValidationException {
        assertion.setMultivaluedVariableName(VariablePrefixUtil.fixVariableName(multivaluedVarNameField.getText()));
        assertion.setValueToSearchForVariableName(VariablePrefixUtil.fixVariableName(valueToSearchForVarNameField.getText()));
        assertion.setOutputVariableName(VariablePrefixUtil.fixVariableName(outputVarNameField.getVariable()));
        assertion.setAllowMultipleMatches(allowMultipleMatchesCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        allowMultipleMatchesCheckBox.setVisible(false); // Hide for now (Bug #7895)

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
