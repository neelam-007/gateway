package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class NonSoapSignElementAssertionPropertiesDialog extends XpathBasedAssertionPropertiesDialog {
    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    private JPanel contentPane;
    private final NonSoapSignElementAssertion assertion;

    public NonSoapSignElementAssertionPropertiesDialog(Window owner, NonSoapSignElementAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
    }

    @Override
    public JDialog getDialog() {
        JDialog dlg = super.getDialog();
        dlg.setTitle(String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)));
        return dlg;
    }
}
