package com.l7tech.external.assertions.csrsigner.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.csrsigner.CsrSignerAssertion;

import javax.swing.*;
import java.awt.*;

public class CsrSignerAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CsrSignerAssertion> {
    private JPanel contentPane;
    private JTextField prefixField;
    private TargetVariablePanel csrVariablePanel;
    private PrivateKeysComboBox privateKeyComboBox;
    private TargetVariablePanel certDnVariablePanel;

    public CsrSignerAssertionPropertiesDialog(Window parent, CsrSignerAssertion bean) {
        super(bean.getClass(), parent, bean, true);
        initComponents();
        setData(bean);
    }

    @Override
    public void setData(CsrSignerAssertion assertion) {
        certDnVariablePanel.setAssertion(assertion, getPreviousAssertion());
        certDnVariablePanel.setVariable(assertion.getCertDNVariableName());
        csrVariablePanel.setAssertion(assertion, getPreviousAssertion());
        csrVariablePanel.setVariable(assertion.getCsrVariableName());
        String prefix = assertion.getOutputPrefix();
        prefixField.setText(prefix == null ? "" : prefix);
        if (assertion.isUsesDefaultKeyStore())
            privateKeyComboBox.selectDefaultSsl();
        else
            privateKeyComboBox.select(assertion.getNonDefaultKeystoreId(), assertion.getKeyAlias());
    }

    @Override
    public CsrSignerAssertion getData(CsrSignerAssertion assertion) throws ValidationException {
        if (csrVariablePanel.getVariable() == null || csrVariablePanel.getVariable().trim().length() < 1)
            throw new ValidationException("An input variable must be specified.");

        assertion.setCertDNVariableName(certDnVariablePanel.getVariable());
        assertion.setCsrVariableName(csrVariablePanel.getVariable());
        assertion.setOutputPrefix(prefixField.getText());

        assertion.setUsesDefaultKeyStore(privateKeyComboBox.isSelectedDefaultSsl());
        assertion.setNonDefaultKeystoreId(privateKeyComboBox.getSelectedKeystoreId());
        assertion.setKeyAlias(privateKeyComboBox.getSelectedKeyAlias());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}