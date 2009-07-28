package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;

import javax.swing.*;
import java.awt.*;

public class NonSoapSignElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapSignElementAssertion> {
    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;

    public NonSoapSignElementAssertionPropertiesDialog(Window owner, NonSoapSignElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
    }
}
