package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.XmlElementVerifierConfigPanel;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.security.xml.XmlElementVerifierConfig;

import javax.swing.*;
import java.awt.*;

public class NonSoapVerifyElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapVerifyElementAssertion> {

    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;
    private XmlElementVerifierConfigPanel configPanel = new XmlElementVerifierConfigPanel(new XmlElementVerifierConfig());

    public NonSoapVerifyElementAssertionPropertiesDialog(Window owner, NonSoapVerifyElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("dsig:Signature element(s) to verify XPath:");

        // create extra panel components -- copied from WsSecurityAssertion
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    protected JPanel createExtraPanel() {
        contentPane.setLayout(new BorderLayout());
        contentPane.add(configPanel, BorderLayout.CENTER);
        return contentPane;
    }


    @Override
    public void setData(NonSoapVerifyElementAssertion assertion) {
        super.setData(assertion);
        configPanel.setData(assertion.config());
        configPanel.setPolicyPosition(assertion, getPreviousAssertion());
    }

    @Override
    public NonSoapVerifyElementAssertion getData(NonSoapVerifyElementAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);
        assertion.config(configPanel.getData());
        return assertion;
    }

    @Override
    protected void policyPositionUpdated() {
        configPanel.setPolicyPosition(assertion, getPreviousAssertion());
    }
}
