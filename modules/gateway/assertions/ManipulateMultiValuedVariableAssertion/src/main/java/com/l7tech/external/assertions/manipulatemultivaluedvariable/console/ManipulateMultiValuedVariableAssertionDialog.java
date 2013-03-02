package com.l7tech.external.assertions.manipulatemultivaluedvariable.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.manipulatemultivaluedvariable.ManipulateMultiValuedVariableAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ManipulateMultiValuedVariableAssertionDialog extends AssertionPropertiesOkCancelSupport<ManipulateMultiValuedVariableAssertion> {
    private JPanel contentPane;
    private JTextField valueVariableTextField;
    private JPanel targetVariablePanel;

    public ManipulateMultiValuedVariableAssertionDialog(Window owner, ManipulateMultiValuedVariableAssertion assertion) {
        super(ManipulateMultiValuedVariableAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public ManipulateMultiValuedVariableAssertion getData(ManipulateMultiValuedVariableAssertion assertion) throws ValidationException {
        validateData();

        assertion.setTargetVariableName(targetVariableTextField.getVariable());
        assertion.setSourceVariableName(VariablePrefixUtil.fixVariableName(valueVariableTextField.getText()));

        return assertion;
    }

    @Override
    public void setData(ManipulateMultiValuedVariableAssertion assertion) {

        targetVariableTextField.setVariable(assertion.getTargetVariableName());
        valueVariableTextField.setText(assertion.getSourceVariableName());

        enableDisableComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        targetVariableTextField = new TargetVariablePanel();
        targetVariablePanel.setLayout(new BorderLayout());
        targetVariablePanel.add(targetVariableTextField, BorderLayout.CENTER);
        targetVariableTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(targetVariableTextField .isEntryValid());
            }
        });

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        valueVariableTextField.getDocument().addDocumentListener(enableDisableListener);
        targetVariableTextField.addChangeListener(enableDisableListener);

    }

    //- PRIVATE

    private TargetVariablePanel targetVariableTextField;

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly() && targetVariableTextField.isEntryValid();

        getOkButton().setEnabled(enableAny);
    }

    private void validateData() {
        String message = VariableMetadata.validateName(VariablePrefixUtil.fixVariableName(valueVariableTextField.getText()), true);

        if ( message != null ) {
            throw new ValidationException( message, "Invalid Property", null );
        }

    }
}
