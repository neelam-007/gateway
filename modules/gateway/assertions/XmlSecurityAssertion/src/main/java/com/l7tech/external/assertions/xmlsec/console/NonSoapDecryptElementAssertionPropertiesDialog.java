package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;

import javax.swing.*;
import java.awt.*;

public class NonSoapDecryptElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapDecryptElementAssertion> {
    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;
    private JCheckBox reportCheckBox;

    public NonSoapDecryptElementAssertionPropertiesDialog(Window owner, NonSoapDecryptElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("xenc:EncryptedData element(s) to decrypt XPath:");
    }
}
