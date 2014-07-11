package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmppassertion.XMPPSendToRemoteHostAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 13/03/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPSendToRemoteHostAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPSendToRemoteHostAssertion> {
    private static class XMPPConnectionEntry {
        private String name;
        private long id;
        
        public XMPPConnectionEntry(String name, long id) {
            this.name = name;
            this.id = id;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    protected static final Logger logger = Logger.getLogger(XMPPSendToRemoteHostAssertionPropertiesDialog.class.getName());
    
    private static final String TO_SERVER = "To Server";
    private static final String TO_CLIENT = "To Client";
    
    private JPanel mainPanel;
    private JComboBox directionComboBox;
    private JTextField clientSessionIdField;

    public XMPPSendToRemoteHostAssertionPropertiesDialog(Window owner, XMPPSendToRemoteHostAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {TO_SERVER, "To Client"}));
        directionComboBox.setSelectedIndex(0);

        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPSendToRemoteHostAssertion getData(XMPPSendToRemoteHostAssertion assertion) throws ValidationException {
        if(clientSessionIdField.getText().trim().isEmpty()) {
            throw new ValidationException("The client session ID field is required.");
        }
        
        assertion.setToOutboundConnection(TO_SERVER.equals(directionComboBox.getSelectedItem()));
        assertion.setSessionId(clientSessionIdField.getText().trim());
        
        return assertion;
    }
    
    @Override
    public void setData(XMPPSendToRemoteHostAssertion assertion) {
        directionComboBox.setSelectedItem(assertion.isToOutboundConnection() ? TO_SERVER : TO_CLIENT);

        clientSessionIdField.setText(assertion.getSessionId() == null ? "" : assertion.getSessionId());
    }
}
