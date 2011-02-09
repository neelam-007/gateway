package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.xmlsec.SelectElementAssertion;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SelectElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<SelectElementAssertion> {
    private JPanel contentPane;
    private JPanel variableNamePanel;
    private TargetVariablePanel variableNameField;


    public SelectElementAssertionPropertiesDialog(Frame owner, SelectElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        getXpathExpressionLabel().setText("Element to select XPath:");
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);

        variableNameField = new TargetVariablePanel();
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(variableNameField, BorderLayout.CENTER);
        variableNameField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(variableNameField.isEntryValid());
            }
        });
    }

    private JPanel createExtraPanel() {
        return contentPane;
    }

    private String nonull(String s) { return s == null ? "" : s; }

    @Override
    public SelectElementAssertion getData(SelectElementAssertion assertion) throws ValidationException {
        final SelectElementAssertion ass = super.getData(assertion);
        ass.setElementVariable(variableNameField.getVariable());
        return ass;
    }

    @Override
    public void setData(SelectElementAssertion assertion) {
        variableNameField.setVariable(nonull(assertion.getElementVariable()));
        variableNameField.setAssertion(assertion,getPreviousAssertion());

        super.setData(assertion);
    }
}
