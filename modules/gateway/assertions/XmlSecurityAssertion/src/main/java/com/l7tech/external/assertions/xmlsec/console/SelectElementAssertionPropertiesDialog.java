package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.SelectElementAssertion;

import javax.swing.*;
import java.awt.*;

public class SelectElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<SelectElementAssertion> {
    private JPanel contentPane;
    private JTextField variableNameField;

    public SelectElementAssertionPropertiesDialog(Frame owner, SelectElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("Element to select XPath:");
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    private JPanel createExtraPanel() {
        return contentPane;
    }

    private String nonull(String s) { return s == null ? "" : s; }

    @Override
    public SelectElementAssertion getData(SelectElementAssertion assertion) throws ValidationException {
        final SelectElementAssertion ass = super.getData(assertion);
        ass.setElementVariable(variableNameField.getText());
        return ass;
    }

    @Override
    public void setData(SelectElementAssertion assertion) {
        variableNameField.setText(nonull(assertion.getElementVariable()));
        super.setData(assertion);
    }
}
