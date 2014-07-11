package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmppassertion.XMPPAssociateSessionsAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPAssociateSessionsAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPAssociateSessionsAssertion> {
    protected static final Logger logger = Logger.getLogger(XMPPGetRemoteCertificateAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JTextField inboundSessionIdField;
    private JTextField outboundSessionIdField;

    public XMPPAssociateSessionsAssertionPropertiesDialog(Window owner, XMPPAssociateSessionsAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPAssociateSessionsAssertion getData(XMPPAssociateSessionsAssertion assertion) throws AssertionPropertiesOkCancelSupport.ValidationException {
        if(inboundSessionIdField.getText().trim().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The inbound session ID is required.");
        }
        if(outboundSessionIdField.getText().trim().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The outbound session ID is required.");
        }

        assertion.setInboundSessionId(inboundSessionIdField.getText().trim());
        assertion.setOutboundSessionId(outboundSessionIdField.getText().trim());

        return assertion;
    }

    @Override
    public void setData(XMPPAssociateSessionsAssertion assertion) {
        inboundSessionIdField.setText(assertion.getInboundSessionId() == null ? "" : assertion.getInboundSessionId());
        outboundSessionIdField.setText(assertion.getOutboundSessionId() == null ? "" : assertion.getOutboundSessionId());
    }
}
