package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmppassertion.XMPPCloseSessionAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/03/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPCloseSessionAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPCloseSessionAssertion> {
    protected static final Logger logger = Logger.getLogger(XMPPCloseSessionAssertionPropertiesDialog.class.getName());

    private static final String TO_CLIENT = "To Client";
    private static final String TO_SERVER = "To Server";
    
    private JPanel mainPanel;
    private JTextField sessionIdField;
    private JComboBox directionComboBox;

    public XMPPCloseSessionAssertionPropertiesDialog(Window owner, XMPPCloseSessionAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }
    
    @Override
    protected void initComponents() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {TO_CLIENT, TO_SERVER}));
        
        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPCloseSessionAssertion getData(XMPPCloseSessionAssertion assertion) throws ValidationException {
        if(sessionIdField.getText().trim().isEmpty()) {
            throw new ValidationException("The session ID is required.");
        }

        assertion.setSessionId(sessionIdField.getText().trim());
        assertion.setInbound(directionComboBox.getSelectedIndex() == 0);

        return assertion;
    }

    @Override
    public void setData(XMPPCloseSessionAssertion assertion) {
        sessionIdField.setText(assertion.getSessionId() == null ? "" : assertion.getSessionId());
        directionComboBox.setSelectedIndex(assertion.isInbound() ? 0 : 1);
    }
}
