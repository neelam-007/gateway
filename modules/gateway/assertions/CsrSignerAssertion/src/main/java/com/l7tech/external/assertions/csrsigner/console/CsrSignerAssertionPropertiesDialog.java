package com.l7tech.external.assertions.csrsigner.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.csrsigner.CsrSignerAssertion;

import javax.swing.*;
import java.awt.*;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

public class CsrSignerAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CsrSignerAssertion> {
    private JPanel contentPane;
    private JTextField prefixField;
    private TargetVariablePanel csrVariablePanel;
    private PrivateKeysComboBox privateKeyComboBox;
    private TargetVariablePanel certDnVariablePanel;
    private JTextField expiryAgeField;

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

        String expiryAgeDays = assertion.getExpiryAgeDays();
        if (StringUtils.isBlank(expiryAgeDays)){
            if (StringUtils.isBlank(assertion.getCertDNVariableName())){
                expiryAgeDays = String.valueOf(CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_NO_DN_OVERRIDE);
            } else {
                expiryAgeDays = String.valueOf(CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_DN_OVERRIDE);
            }
        }

        expiryAgeField.setText(expiryAgeDays.trim());
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

        final String expiryAgeString = expiryAgeField.getText().trim();
        if (StringUtils.isBlank(expiryAgeString)) {
            throw new ValidationException("An Expiry Age value must be specified.");
        }

        if ((StringUtils.isNumeric(expiryAgeString)) &&
                (!ValidationUtils.isValidInteger(expiryAgeString, false, CsrSignerAssertion.MIN_CSR_AGE, CsrSignerAssertion.MAX_CSR_AGE))) {
                throw new ValidationException(CsrSignerAssertion.ERR_EXPIRY_AGE_MUST_BE_IN_RANGE);
        }

        assertion.setExpiryAgeDays(expiryAgeString);
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