package com.l7tech.external.assertions.splitjoin.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    public SplitAssertion getData(SplitAssertion assertion) throws ValidationException {
        validateData();

        assertion.setInputVariable(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        assertion.setOutputVariable(VariablePrefixUtil.fixVariableName(targetVariableTextField.getVariable()));
        assertion.setSplitPattern(splitPatternTextField.getText());//do not modify what the user entered
        assertion.setSplitPatternRegEx(regularExpressionCheckBox.isSelected());

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
            targetVariableTextField.setVariable(targetVariable);
        }

        final String splitPattern = assertion.getSplitPattern();
        if (splitPattern != null) {//do not check for empty, it is ok
            splitPatternTextField.setText(splitPattern);
        }

        regularExpressionCheckBox.setSelected(assertion.isSplitPatternRegEx());
        targetVariableTextField.setAssertion(assertion,getPreviousAssertion());
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private void validateData() throws ValidationException {
        String message = VariableMetadata.validateName(VariablePrefixUtil.fixVariableName(sourceVariableTextField.getText()));
        String propertyName = "sourceVariable";

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
    private JTextField splitPatternTextField;
    private JCheckBox regularExpressionCheckBox;
    private JPanel targetVariablePanel;
    private TargetVariablePanel targetVariableTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(SplitVariablePropertiesDialog.class.getName());
}
