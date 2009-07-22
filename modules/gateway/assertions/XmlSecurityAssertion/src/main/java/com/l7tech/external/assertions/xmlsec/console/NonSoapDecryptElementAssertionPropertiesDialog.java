package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class NonSoapDecryptElementAssertionPropertiesDialog extends XpathBasedAssertionPropertiesDialog {
    private JPanel contentPane;
    private final NonSoapDecryptElementAssertion assertion;

    public NonSoapDecryptElementAssertionPropertiesDialog(Window owner, NonSoapDecryptElementAssertion assertion) {
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
