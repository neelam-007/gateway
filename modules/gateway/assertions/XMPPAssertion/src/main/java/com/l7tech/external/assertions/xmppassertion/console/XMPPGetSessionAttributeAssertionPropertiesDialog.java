package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmppassertion.XMPPGetSessionAttributeAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPGetSessionAttributeAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPGetSessionAttributeAssertion> {
    protected static final Logger logger = Logger.getLogger(XMPPGetSessionAttributeAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JComboBox directionComboBox;
    private JTextField sessionIdField;
    private JTextField attributeNameField;
    private JTextField variableNameField;

    public XMPPGetSessionAttributeAssertionPropertiesDialog(Window owner, XMPPGetSessionAttributeAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {"Client \u2192 Server", "Server \u2192 Client"}));
        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPGetSessionAttributeAssertion getData(XMPPGetSessionAttributeAssertion assertion) throws ValidationException {
        if(sessionIdField.getText().trim().isEmpty()) {
            throw new ValidationException("The session ID is required.");
        }
        if(attributeNameField.getText().trim().isEmpty()) {
            throw new ValidationException("The attribute name is required.");
        }
        if(variableNameField.getText().trim().isEmpty()) {
            throw new ValidationException("The output variable name is required.");
        }

        assertion.setInbound(directionComboBox.getSelectedIndex() == 0);
        assertion.setSessionId(sessionIdField.getText().trim());
        assertion.setAttributeName(attributeNameField.getText().trim());
        assertion.setVariableName(variableNameField.getText().trim());

        return assertion;
    }

    @Override
    public void setData(XMPPGetSessionAttributeAssertion assertion) {
        directionComboBox.setSelectedIndex(assertion.isInbound() ? 0 : 1);
        sessionIdField.setText(assertion.getSessionId() == null ? "" : assertion.getSessionId());
        attributeNameField.setText(assertion.getAttributeName() == null ? "" : assertion.getAttributeName());
        variableNameField.setText(assertion.getVariableName() == null ? "" : assertion.getVariableName());
    }
}
