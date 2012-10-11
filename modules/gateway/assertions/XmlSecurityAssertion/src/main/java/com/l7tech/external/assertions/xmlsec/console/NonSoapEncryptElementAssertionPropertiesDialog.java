package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.XmlElementEncryptionConfigPanel;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.security.xml.XmlElementEncryptionConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Properties dialog for non-SOAP encrypt element assertion.
 */
public class NonSoapEncryptElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapEncryptElementAssertion> {
    private JPanel contentPane;
    private final XmlElementEncryptionConfigPanel encryptionConfigPanel = new XmlElementEncryptionConfigPanel(new XmlElementEncryptionConfig(), false, true);

    public NonSoapEncryptElementAssertionPropertiesDialog(Frame parent, NonSoapEncryptElementAssertion assertion) {
        super(parent, assertion);
        initComponents();
        setData(assertion);
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    protected JPanel createExtraPanel() {
        contentPane.setLayout(new BorderLayout());
        contentPane.add(encryptionConfigPanel, BorderLayout.CENTER);
        return contentPane;
    }

    @Override
    public void setData(NonSoapEncryptElementAssertion assertion) {
        super.setData(assertion);
        encryptionConfigPanel.setData(assertion.config());
        encryptionConfigPanel.setPolicyPosition(assertion, getPreviousAssertion());
    }

    @Override
    public NonSoapEncryptElementAssertion getData(NonSoapEncryptElementAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);
        assertion.config(encryptionConfigPanel.getData());
        return assertion;
    }

    @Override
    protected void policyPositionUpdated() {
        encryptionConfigPanel.setPolicyPosition(assertion, getPreviousAssertion());
    }
}
