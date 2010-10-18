package com.l7tech.external.assertions.splitjoin.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class JoinVariablePropertiesDialog extends AssertionPropertiesOkCancelSupport<JoinAssertion> {

    public JoinVariablePropertiesDialog(final Window parent, final JoinAssertion assertion) {
        super(JoinAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    public JoinAssertion getData(JoinAssertion assertion) throws ValidationException {
        validateData();

        assertion.setInputVariable(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        assertion.setOutputVariable(VariablePrefixUtil.fixVariableName(targetVariableTextField.getText()));
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
            targetVariableTextField.setText(targetVariable);
        }

        final String joinString = assertion.getJoinSubstring();
        if (joinString != null) { //don't test for empty, it's ok
            joinStringTextField.setText(joinString);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private void validateData() throws ValidationException {
        String message = VariableMetadata.validateName(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        String propertyName = "sourceVariable";

        if (message == null) {
            propertyName = "targetVariable";
            final String targetVariable = targetVariableTextField.getText();
            message = VariableMetadata.validateName(VariablePrefixUtil.fixVariableName(targetVariable));
            if (message == null) {
                final String fixedTargetVariable = VariablePrefixUtil.fixVariableName(targetVariable);
                final VariableMetadata meta = BuiltinVariables.getMetadata(fixedTargetVariable);
                if (meta != null) {
                    if(!meta.isSettable()) {
                        message = getPropertyValue("targetVariable") + " '" + targetVariable + "' is not settable.";
                    }
                }
            }
        }

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
    private JTextField targetVariableTextField;
    private JTextField joinStringTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(JoinVariablePropertiesDialog.class.getName());
}
