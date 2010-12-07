package com.l7tech.external.assertions.splitjoin.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ResourceBundle;

public class JoinVariablePropertiesDialog extends AssertionPropertiesOkCancelSupport<JoinAssertion> {

    public JoinVariablePropertiesDialog(final Window parent, final JoinAssertion assertion) {
        super(JoinAssertion.class, parent, assertion, true);
        initComponents();
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
       }

    @Override
    public JoinAssertion getData(JoinAssertion assertion) throws ValidationException {
        validateData();

        assertion.setInputVariable(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        assertion.setOutputVariable(VariablePrefixUtil.fixVariableName(targetVariableTextField.getVariable()));
        assertion.setJoinSubstring(joinStringTextField.getText());//do not modify what the user entered

        return assertion;
    }

    @Override
    public void setData(JoinAssertion assertion) {
        final String sourceVariable = assertion.getInputVariable();
        if(sourceVariable != null && !sourceVariable.trim().isEmpty()){
            sourceVariableTextField.setText(sourceVariable);
        }

        final String targetVariable = assertion.getOutputVariable();
        if(targetVariable != null && !targetVariable.trim().isEmpty()){
            targetVariableTextField.setVariable(targetVariable);
        }

        final String joinString = assertion.getJoinSubstring();
        if (joinString != null) { //don't test for empty, it's ok
            joinStringTextField.setText(joinString);
        }
        targetVariableTextField.setAssertion(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private void validateData() throws ValidationException {
        String message = VariableMetadata.validateName(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        String propertyName = "sourceVariable";

        if (message != null) {
            throw new ValidationException(message, "Invalid " + getPropertyValue(propertyName) + " value", null);
        }
    }

    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    private JPanel propertyPanel;
    private JTextField sourceVariableTextField;
    private TargetVariablePanel targetVariableTextField;
    private JTextField joinStringTextField;
    private JPanel targetVariablePanel;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(JoinVariablePropertiesDialog.class.getName());
}
