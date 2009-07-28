package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;

import javax.swing.*;
import java.awt.*;

public class NonSoapVerifyElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapVerifyElementAssertion> {
    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;

    public NonSoapVerifyElementAssertionPropertiesDialog(Window owner, NonSoapVerifyElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("dsig:Signature element(s) to verify XPath:");
    }
}
