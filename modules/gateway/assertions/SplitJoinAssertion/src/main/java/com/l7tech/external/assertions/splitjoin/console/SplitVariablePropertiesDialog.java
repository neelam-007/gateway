package com.l7tech.external.assertions.splitjoin.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SplitVariablePropertiesDialog extends AssertionPropertiesOkCancelSupport<SplitAssertion> {

    public SplitVariablePropertiesDialog(final Window parent, final SplitAssertion assertion) {
        super(SplitAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    public SplitAssertion getData(SplitAssertion assertion) throws ValidationException {
        validateData();

        assertion.setInputVariable(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        assertion.setOutputVariable(VariablePrefixUtil.fixVariableName(targetVariableTextField.getText()));
        assertion.setSplitPattern(splitPatternTextField.getText());//do not modify what the user entered        

        return assertion;
    }

    @Override
    public void setData(SplitAssertion assertion) {
        final String sourceVariable = assertion.getInputVariable();
        if(sourceVariable != null && !sourceVariable.trim().isEmpty()){
            sourceVariableTextField.setText(sourceVariable);
        }

        final String targetVariable = assertion.getOutputVariable();
        if(targetVariable != null && !targetVariable.trim().isEmpty()){
            targetVariableTextField.setText(targetVariable);
        }

        final String splitPattern = assertion.getSplitPattern();
        if (splitPattern != null) {//do not check for empty, it is ok
            splitPatternTextField.setText(splitPattern);
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
                final VariableMetadata meta = BuiltinVariables.getMetadata(targetVariable);
                if (meta != null) {
                    if(!meta.isSettable()) {
                        message = getPropertyValue("targetVariable") + " '" + targetVariable + "' is not settable.";
                    }
                }
            }
        }

        if (message == null) {
            propertyName = "splitPattern";
            final String pattern = splitPatternTextField.getText();
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                message = "Invalid pattern: " + ExceptionUtils.getMessage(e);
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
    private JTextField splitPatternTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(SplitVariablePropertiesDialog.class.getName());
}
